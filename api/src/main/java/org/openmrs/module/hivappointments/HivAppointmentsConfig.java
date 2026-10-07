package org.openmrs.module.hivappointments;

import org.apache.commons.lang.StringUtils;
import org.openmrs.Concept;
import org.openmrs.EncounterType;
import org.openmrs.PatientIdentifierType;
import org.openmrs.PersonAttributeType;
import org.openmrs.Program;
import org.openmrs.api.APIException;
import org.openmrs.api.AdministrationService;
import org.openmrs.api.ConceptService;
import org.openmrs.api.EncounterService;
import org.openmrs.api.PatientService;
import org.openmrs.api.PersonService;
import org.openmrs.api.ProgramWorkflowService;

import java.util.ArrayList;
import java.util.List;

import static org.openmrs.module.hivappointments.HivAppointmentsConstants.*;

/**
 * Resolves the metadata used by the module from global properties, falling back to the PIH EMR defaults.
 */
public class HivAppointmentsConfig {

	private AdministrationService administrationService;

	private ConceptService conceptService;

	private EncounterService encounterService;

	private ProgramWorkflowService programWorkflowService;

	private PersonService personService;

	private PatientService patientService;

	public Program getHivProgram() {
		String uuid = getGlobalProperty(GP_HIV_PROGRAM, DEFAULT_HIV_PROGRAM);
		return required(programWorkflowService.getProgramByUuid(uuid), GP_HIV_PROGRAM, uuid);
	}

	public Concept getReturnVisitDateConcept() {
		String ref = getGlobalProperty(GP_RETURN_VISIT_DATE_CONCEPT, DEFAULT_RETURN_VISIT_DATE_CONCEPT);
		return required(getConcept(ref), GP_RETURN_VISIT_DATE_CONCEPT, ref);
	}

	public Concept getCommentConcept() {
		String ref = getGlobalProperty(GP_COMMENT_CONCEPT, DEFAULT_COMMENT_CONCEPT);
		return required(getConcept(ref), GP_COMMENT_CONCEPT, ref);
	}

	/**
	 * @return the encounter types where a patient is seen for this category; a visit of one of these types honors the appointment
	 */
	public List<EncounterType> getVisitEncounterTypes(AppointmentCategory category) {
		if (category == AppointmentCategory.CLINICAL) {
			return getEncounterTypes(GP_CLINICAL_ENCOUNTER_TYPES, DEFAULT_CLINICAL_ENCOUNTER_TYPES);
		}
		return getEncounterTypes(GP_ARV_PICKUP_ENCOUNTER_TYPES, DEFAULT_ARV_PICKUP_ENCOUNTER_TYPES);
	}

	public EncounterType getTrackingEncounterType(AppointmentCategory category) {
		String uuid = category == AppointmentCategory.CLINICAL ? CLINICAL_TRACKING_ENCOUNTER_TYPE_UUID : ARV_TRACKING_ENCOUNTER_TYPE_UUID;
		return required(encounterService.getEncounterTypeByUuid(uuid), "tracking encounter type", uuid);
	}

	/**
	 * @return the encounter types whose return visit date obs schedule an appointment of this category (visits and tracking)
	 */
	public List<EncounterType> getSchedulingEncounterTypes(AppointmentCategory category) {
		List<EncounterType> types = new ArrayList<>(getVisitEncounterTypes(category));
		types.add(getTrackingEncounterType(category));
		return types;
	}

	/**
	 * @return the category scheduled by an encounter of this type, or null if this encounter type does not schedule HIV appointments
	 */
	public AppointmentCategory getCategoryForEncounterType(EncounterType encounterType) {
		for (AppointmentCategory category : AppointmentCategory.values()) {
			if (getSchedulingEncounterTypes(category).contains(encounterType)) {
				return category;
			}
		}
		return null;
	}

	public int getLtfuThresholdDays() {
		String value = getGlobalProperty(GP_LTFU_THRESHOLD_DAYS, null);
		if (StringUtils.isBlank(value)) {
			return DEFAULT_LTFU_THRESHOLD_DAYS;
		}
		try {
			return Integer.parseInt(value.trim());
		}
		catch (NumberFormatException e) {
			throw new APIException("Global property " + GP_LTFU_THRESHOLD_DAYS + " must be a number of days, found: " + value);
		}
	}

	/**
	 * @return the phone number attribute type, or null if not configured on this server
	 */
	public PersonAttributeType getPhoneAttributeType() {
		String uuid = getGlobalProperty(GP_PHONE_ATTRIBUTE_TYPE, DEFAULT_PHONE_ATTRIBUTE_TYPE);
		return personService.getPersonAttributeTypeByUuid(uuid);
	}

	/**
	 * @return the identifier type to display, or null to use each patient's preferred identifier
	 */
	public PatientIdentifierType getIdentifierType() {
		String uuid = getGlobalProperty(GP_IDENTIFIER_TYPE, null);
		return StringUtils.isBlank(uuid) ? null : patientService.getPatientIdentifierTypeByUuid(uuid);
	}

	private List<EncounterType> getEncounterTypes(String globalProperty, String defaultValue) {
		List<EncounterType> types = new ArrayList<>();
		for (String uuid : getGlobalProperty(globalProperty, defaultValue).split(",")) {
			if (StringUtils.isNotBlank(uuid)) {
				types.add(required(encounterService.getEncounterTypeByUuid(uuid.trim()), globalProperty, uuid));
			}
		}
		return types;
	}

	/**
	 * @param ref a concept uuid, or a mapping in the form SOURCE:CODE
	 */
	private Concept getConcept(String ref) {
		int separator = ref.indexOf(':');
		if (separator > 0) {
			return conceptService.getConceptByMapping(ref.substring(separator + 1).trim(), ref.substring(0, separator).trim());
		}
		return conceptService.getConceptByUuid(ref.trim());
	}

	private String getGlobalProperty(String name, String defaultValue) {
		String value = administrationService.getGlobalProperty(name);
		return StringUtils.isBlank(value) ? defaultValue : value;
	}

	private <T> T required(T value, String setting, String ref) {
		if (value == null) {
			throw new APIException("HIV Appointments is misconfigured: " + setting + " refers to " + ref + ", which does not exist");
		}
		return value;
	}

	public void setAdministrationService(AdministrationService administrationService) {
		this.administrationService = administrationService;
	}

	public void setConceptService(ConceptService conceptService) {
		this.conceptService = conceptService;
	}

	public void setEncounterService(EncounterService encounterService) {
		this.encounterService = encounterService;
	}

	public void setProgramWorkflowService(ProgramWorkflowService programWorkflowService) {
		this.programWorkflowService = programWorkflowService;
	}

	public void setPersonService(PersonService personService) {
		this.personService = personService;
	}

	public void setPatientService(PatientService patientService) {
		this.patientService = patientService;
	}
}
