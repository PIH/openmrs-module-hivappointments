package org.openmrs.module.hivappointments;

import org.apache.commons.lang.StringUtils;
import org.hibernate.SQLQuery;
import org.openmrs.Concept;
import org.openmrs.Encounter;
import org.openmrs.EncounterRole;
import org.openmrs.EncounterType;
import org.openmrs.Location;
import org.openmrs.Obs;
import org.openmrs.OpenmrsObject;
import org.openmrs.Patient;
import org.openmrs.PatientIdentifier;
import org.openmrs.PatientIdentifierType;
import org.openmrs.PersonAttribute;
import org.openmrs.PersonAttributeType;
import org.openmrs.Program;
import org.openmrs.Provider;
import org.openmrs.User;
import org.openmrs.api.APIException;
import org.openmrs.api.EncounterService;
import org.openmrs.api.LocationService;
import org.openmrs.api.ObsService;
import org.openmrs.api.PatientService;
import org.openmrs.api.ProviderService;
import org.openmrs.api.context.Context;
import org.openmrs.api.db.hibernate.DbSessionFactory;
import org.openmrs.api.impl.BaseOpenmrsService;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Transactional
public class HivAppointmentsServiceImpl extends BaseOpenmrsService implements HivAppointmentsService {

	private HivAppointmentsConfig config;

	private DbSessionFactory sessionFactory;

	private PatientService patientService;

	private EncounterService encounterService;

	private LocationService locationService;

	private ObsService obsService;

	private ProviderService providerService;

	@Override
	@Transactional(readOnly = true)
	public HivAppointmentSearchResult getAppointments(HivAppointmentSearchCriteria criteria) {
		validateDateRange(criteria.getFromDate(), criteria.getToDate());

		Program program = config.getHivProgram();
		Concept returnVisitDate = config.getReturnVisitDateConcept();
		AppointmentStatusCalculator calculator = new AppointmentStatusCalculator(new Date(), config.getLtfuThresholdDays());

		List<AppointmentCategory> categories = criteria.getCategory() == null
				? Arrays.asList(AppointmentCategory.values())
				: Collections.singletonList(criteria.getCategory());

		List<HivAppointment> appointments = new ArrayList<>();
		for (AppointmentCategory category : categories) {
			appointments.addAll(queryAppointments(category, criteria, program, returnVisitDate, calculator));
		}
		appointments.sort(Comparator.comparing(HivAppointment::getScheduledDate)
				.thenComparing(HivAppointment::getPersonId)
				.thenComparing(HivAppointment::getCategory));

		HivAppointmentSearchResult result = new HivAppointmentSearchResult();
		List<HivAppointment> matching = new ArrayList<>();
		for (HivAppointment appointment : appointments) {
			result.increment(appointment);
			if (criteria.getStatuses() == null || criteria.getStatuses().isEmpty()
					|| criteria.getStatuses().contains(appointment.getStatus())) {
				matching.add(appointment);
			}
		}
		result.setTotalCount(matching.size());

		int start = Math.max(0, criteria.getStartIndex() == null ? 0 : criteria.getStartIndex());
		int limit = criteria.getLimit() == null ? matching.size() : Math.max(0, criteria.getLimit());
		int end = (int) Math.min((long) start + limit, matching.size());
		List<HivAppointment> page = start >= matching.size() ? new ArrayList<>() : new ArrayList<>(matching.subList(start, end));
		populateDetails(page);
		result.setResults(page);
		return result;
	}

	/**
	 * Every return visit date obs in the range is an appointment. For each one, the query also returns what happened
	 * after the encounter that recorded it: the first qualifying visit, whether a newer date was recorded, and the
	 * latest tracking encounter. MySQL 5.6 compatible (no window functions or CTEs).
	 */
	private List<HivAppointment> queryAppointments(AppointmentCategory category, HivAppointmentSearchCriteria criteria,
	                                               Program program, Concept returnVisitDate,
	                                               AppointmentStatusCalculator calculator) {
		StringBuilder sql = new StringBuilder();
		sql.append("select o.obs_id as obs_id, o.uuid as obs_uuid, o.person_id as person_id, o.value_datetime as scheduled_date, ");
		sql.append("       e.encounter_id as encounter_id, e.uuid as encounter_uuid, e.encounter_datetime as encounter_datetime, e.encounter_type as encounter_type, ");
		sql.append("       coalesce(pp.location_id, e.location_id) as site_id, ");
		sql.append("       (select min(v.encounter_datetime) from encounter v ");
		sql.append("         where v.patient_id = o.person_id and v.voided = 0 and v.encounter_type in (:visitTypes) ");
		sql.append("           and v.encounter_datetime > e.encounter_datetime) as next_visit, ");
		sql.append("       (select count(*) from obs lo inner join encounter le on le.encounter_id = lo.encounter_id ");
		sql.append("         where lo.person_id = o.person_id and lo.concept_id = o.concept_id and lo.voided = 0 and le.voided = 0 ");
		sql.append("           and le.encounter_type in (:schedulingTypes) and ").append(laterThanSource("le")).append(") as later_schedules, ");
		sql.append("       (select t.encounter_id from encounter t ");
		sql.append("         where t.patient_id = o.person_id and t.voided = 0 and t.encounter_type = :trackingType ");
		sql.append("           and (t.encounter_id = e.encounter_id or ").append(laterThanSource("t")).append(") ");
		sql.append("         order by t.encounter_datetime desc, t.encounter_id desc limit 1) as last_tracking ");
		sql.append("from obs o ");
		sql.append("inner join encounter e on e.encounter_id = o.encounter_id and e.voided = 0 ");
		sql.append("inner join person pe on pe.person_id = o.person_id and pe.voided = 0 ");
		sql.append("inner join patient_program pp on pp.patient_id = o.person_id and pp.program_id = :programId and pp.voided = 0 ");
		sql.append("       and date(pp.date_enrolled) <= date(o.value_datetime) ");
		sql.append("       and (pp.date_completed is null or date(pp.date_completed) >= date(o.value_datetime)) ");
		sql.append("where o.concept_id = :conceptId and o.voided = 0 ");
		sql.append("  and e.encounter_type in (:schedulingTypes) ");
		sql.append("  and o.value_datetime >= :fromDate and o.value_datetime < :toDateExclusive ");
		sql.append("  and not (pe.dead = 1 and (pe.death_date is null or pe.death_date <= o.value_datetime)) ");

		List<Integer> locationIds = ids(criteria.getLocations());
		if (!locationIds.isEmpty()) {
			sql.append("  and coalesce(pp.location_id, e.location_id) in (:locationIds) ");
		}
		String query = StringUtils.trimToNull(criteria.getQuery());
		if (query != null) {
			sql.append("  and (exists (select 1 from person_name pn where pn.person_id = o.person_id and pn.voided = 0 ");
			sql.append("               and (concat_ws(' ', pn.given_name, pn.family_name) like :query ");
			sql.append("                    or concat_ws(' ', pn.family_name, pn.given_name) like :query)) ");
			sql.append("       or exists (select 1 from patient_identifier pi where pi.patient_id = o.person_id and pi.voided = 0 ");
			sql.append("                  and pi.identifier like :query)) ");
		}

		SQLQuery q = sessionFactory.getCurrentSession().createSQLQuery(sql.toString());
		q.setParameterList("visitTypes", ids(config.getVisitEncounterTypes(category)));
		q.setParameterList("schedulingTypes", ids(config.getSchedulingEncounterTypes(category)));
		q.setParameter("trackingType", config.getTrackingEncounterType(category).getId());
		q.setParameter("programId", program.getId());
		q.setParameter("conceptId", returnVisitDate.getId());
		q.setParameter("fromDate", toDate(AppointmentStatusCalculator.toLocalDate(criteria.getFromDate())));
		q.setParameter("toDateExclusive", toDate(AppointmentStatusCalculator.toLocalDate(criteria.getToDate()).plusDays(1)));
		if (!locationIds.isEmpty()) {
			q.setParameterList("locationIds", locationIds);
		}
		if (query != null) {
			q.setParameter("query", "%" + escapeLike(query) + "%");
		}

		// A patient enrolled more than once over the scheduled date would produce duplicate rows
		Map<Integer, HivAppointment> appointments = new LinkedHashMap<>();
		for (Object row : q.list()) {
			Object[] r = (Object[]) row;
			Integer obsId = toInteger(r[0]);
			if (appointments.containsKey(obsId)) {
				continue;
			}
			HivAppointment a = new HivAppointment();
			a.setObsId(obsId);
			a.setUuid((String) r[1]);
			a.setPersonId(toInteger(r[2]));
			a.setScheduledDate((Date) r[3]);
			a.setSourceEncounterId(toInteger(r[4]));
			a.setSourceEncounterUuid((String) r[5]);
			a.setSourceEncounterDate((Date) r[6]);
			a.setSourceEncounterTypeName(getEncounterTypeName(toInteger(r[7])));
			a.setSiteLocationId(toInteger(r[8]));
			a.setLastTrackingEncounterId(toInteger(r[11]));
			a.setCategory(category);
			calculator.apply(a, (Date) r[9], toInteger(r[10]));
			appointments.put(obsId, a);
		}
		return new ArrayList<>(appointments.values());
	}

	@Override
	public Encounter recordTracking(String appointmentUuid, Date newDate, String comment, Location location) {
		comment = StringUtils.trimToNull(comment);
		if (newDate == null && comment == null) {
			throw new IllegalArgumentException("A new date or a comment is required");
		}
		if (newDate != null && AppointmentStatusCalculator.toLocalDate(newDate).isBefore(LocalDate.now())) {
			throw new IllegalArgumentException("The new date cannot be in the past");
		}

		Concept returnVisitDate = config.getReturnVisitDateConcept();
		Obs appointment = obsService.getObsByUuid(appointmentUuid);
		if (appointment == null || appointment.getVoided() || !returnVisitDate.equals(appointment.getConcept())
				|| appointment.getEncounter() == null) {
			throw new IllegalArgumentException("No HIV appointment found with uuid " + appointmentUuid);
		}
		Encounter source = appointment.getEncounter();
		AppointmentCategory category = config.getCategoryForEncounterType(source.getEncounterType());
		if (category == null) {
			throw new IllegalArgumentException("Obs " + appointmentUuid + " is not recorded on an HIV encounter type");
		}

		Integer personId = appointment.getPersonId();
		if (countLaterSchedules(personId, returnVisitDate, config.getSchedulingEncounterTypes(category), source) > 0) {
			throw new HivAppointmentConflictException("This appointment has already been rescheduled");
		}
		if (hasVisitSince(personId, config.getVisitEncounterTypes(category), source.getEncounterDatetime())) {
			throw new HivAppointmentConflictException("The patient has already been seen since this appointment was scheduled");
		}

		Encounter tracking = new Encounter();
		tracking.setPatient(patientService.getPatient(personId));
		tracking.setEncounterType(config.getTrackingEncounterType(category));
		tracking.setEncounterDatetime(new Date());
		tracking.setLocation(location != null ? location : source.getLocation());

		Provider provider = getCurrentProvider();
		EncounterRole unknownRole = encounterService.getEncounterRoleByUuid(EncounterRole.UNKNOWN_ENCOUNTER_ROLE_UUID);
		if (provider != null && unknownRole != null) {
			tracking.addProvider(unknownRole, provider);
		}

		if (newDate != null) {
			Obs dateObs = new Obs();
			dateObs.setConcept(returnVisitDate);
			dateObs.setValueDatetime(toDate(AppointmentStatusCalculator.toLocalDate(newDate)));
			tracking.addObs(dateObs);
		}
		if (comment != null) {
			Obs commentObs = new Obs();
			commentObs.setConcept(config.getCommentConcept());
			commentObs.setValueText(comment);
			tracking.addObs(commentObs);
		}
		return encounterService.saveEncounter(tracking);
	}

	private int countLaterSchedules(Integer personId, Concept returnVisitDate, List<EncounterType> types, Encounter source) {
		SQLQuery q = sessionFactory.getCurrentSession().createSQLQuery(
				"select count(*) from obs lo inner join encounter le on le.encounter_id = lo.encounter_id "
				+ "where lo.person_id = :personId and lo.concept_id = :conceptId and lo.voided = 0 and le.voided = 0 "
				+ "and le.encounter_type in (:types) "
				+ "and (le.encounter_datetime > :sourceDate or (le.encounter_datetime = :sourceDate and le.encounter_id > :sourceId))");
		q.setParameter("personId", personId);
		q.setParameter("conceptId", returnVisitDate.getId());
		q.setParameterList("types", ids(types));
		q.setParameter("sourceDate", source.getEncounterDatetime());
		q.setParameter("sourceId", source.getId());
		return toInteger(q.uniqueResult());
	}

	private boolean hasVisitSince(Integer personId, List<EncounterType> types, Date since) {
		SQLQuery q = sessionFactory.getCurrentSession().createSQLQuery(
				"select count(*) from encounter v where v.patient_id = :personId and v.voided = 0 "
				+ "and v.encounter_type in (:types) and v.encounter_datetime > :since");
		q.setParameter("personId", personId);
		q.setParameterList("types", ids(types));
		q.setParameter("since", since);
		return toInteger(q.uniqueResult()) > 0;
	}

	/**
	 * Loads patient, site and tracking details, only for the page being returned
	 */
	private void populateDetails(List<HivAppointment> appointments) {
		if (appointments.isEmpty()) {
			return;
		}
		PersonAttributeType phoneAttributeType = config.getPhoneAttributeType();
		PatientIdentifierType identifierType = config.getIdentifierType();
		Concept commentConcept = config.getCommentConcept();

		for (HivAppointment a : appointments) {
			Patient patient = patientService.getPatient(a.getPersonId());
			a.setPatientUuid(patient.getUuid());
			a.setPatientName(patient.getPersonName() == null ? null : patient.getPersonName().getFullName());
			PatientIdentifier identifier = identifierType == null ? patient.getPatientIdentifier() : patient.getPatientIdentifier(identifierType);
			a.setPatientIdentifier(identifier == null ? null : identifier.getIdentifier());
			a.setPatientGender(patient.getGender());
			a.setPatientAge(patient.getAge());
			if (phoneAttributeType != null) {
				PersonAttribute phone = patient.getAttribute(phoneAttributeType);
				a.setPatientPhoneNumber(phone == null ? null : phone.getValue());
			}

			if (a.getSiteLocationId() != null) {
				Location site = locationService.getLocation(a.getSiteLocationId());
				if (site != null) {
					a.setSiteLocationUuid(site.getUuid());
					a.setSiteLocationName(site.getName());
				}
			}

			if (a.getLastTrackingEncounterId() != null) {
				Encounter tracking = encounterService.getEncounter(a.getLastTrackingEncounterId());
				a.setLastTrackingDate(tracking.getEncounterDatetime());
				User creator = tracking.getCreator();
				a.setLastTrackingBy(creator == null || creator.getPersonName() == null ? null : creator.getPersonName().getFullName());
				for (Obs obs : tracking.getObsAtTopLevel(false)) {
					if (obs.getConcept().equals(commentConcept)) {
						a.setLastTrackingComment(obs.getValueText());
					}
				}
			}
		}
	}

	private Provider getCurrentProvider() {
		User user = Context.getAuthenticatedUser();
		if (user == null || user.getPerson() == null) {
			return null;
		}
		Collection<Provider> providers = providerService.getProvidersByPerson(user.getPerson(), false);
		return providers.isEmpty() ? null : providers.iterator().next();
	}

	private final Map<Integer, String> encounterTypeNames = new HashMap<>();

	private String getEncounterTypeName(Integer encounterTypeId) {
		return encounterTypeNames.computeIfAbsent(encounterTypeId, id -> {
			EncounterType type = encounterService.getEncounterType(id);
			return type == null ? null : type.getName();
		});
	}

	private void validateDateRange(Date from, Date to) {
		if (from == null || to == null) {
			throw new IllegalArgumentException("Both fromDate and toDate are required");
		}
		long days = ChronoUnit.DAYS.between(AppointmentStatusCalculator.toLocalDate(from), AppointmentStatusCalculator.toLocalDate(to));
		if (days < 0) {
			throw new IllegalArgumentException("toDate must be on or after fromDate");
		}
		if (days > HivAppointmentsConstants.MAX_SEARCH_RANGE_DAYS) {
			throw new IllegalArgumentException("The date range cannot exceed " + HivAppointmentsConstants.MAX_SEARCH_RANGE_DAYS + " days");
		}
	}

	private static String laterThanSource(String alias) {
		return "(" + alias + ".encounter_datetime > e.encounter_datetime or (" + alias + ".encounter_datetime = e.encounter_datetime and "
				+ alias + ".encounter_id > e.encounter_id))";
	}

	private static List<Integer> ids(Collection<? extends OpenmrsObject> objects) {
		List<Integer> ids = new ArrayList<>();
		if (objects != null) {
			for (OpenmrsObject o : objects) {
				ids.add(o.getId());
			}
		}
		return ids;
	}

	private static Integer toInteger(Object value) {
		return value == null ? null : ((Number) value).intValue();
	}

	private static Date toDate(LocalDate date) {
		return Date.from(date.atStartOfDay(java.time.ZoneId.systemDefault()).toInstant());
	}

	private static String escapeLike(String value) {
		return value.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_");
	}

	public void setConfig(HivAppointmentsConfig config) {
		this.config = config;
	}

	public void setSessionFactory(DbSessionFactory sessionFactory) {
		this.sessionFactory = sessionFactory;
	}

	public void setPatientService(PatientService patientService) {
		this.patientService = patientService;
	}

	public void setEncounterService(EncounterService encounterService) {
		this.encounterService = encounterService;
	}

	public void setLocationService(LocationService locationService) {
		this.locationService = locationService;
	}

	public void setObsService(ObsService obsService) {
		this.obsService = obsService;
	}

	public void setProviderService(ProviderService providerService) {
		this.providerService = providerService;
	}
}
