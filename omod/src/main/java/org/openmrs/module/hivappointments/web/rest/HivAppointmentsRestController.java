package org.openmrs.module.hivappointments.web.rest;

import org.apache.commons.lang.StringUtils;
import org.openmrs.Encounter;
import org.openmrs.Location;
import org.openmrs.api.LocationService;
import org.openmrs.module.hivappointments.AppointmentCategory;
import org.openmrs.module.hivappointments.AppointmentStatus;
import org.openmrs.module.hivappointments.HivAppointment;
import org.openmrs.module.hivappointments.HivAppointmentConflictException;
import org.openmrs.module.hivappointments.HivAppointmentSearchCriteria;
import org.openmrs.module.hivappointments.HivAppointmentSearchResult;
import org.openmrs.module.hivappointments.HivAppointmentsConfig;
import org.openmrs.module.hivappointments.HivAppointmentsConstants;
import org.openmrs.module.hivappointments.HivAppointmentsService;
import org.openmrs.module.webservices.rest.SimpleObject;
import org.openmrs.module.webservices.rest.web.RestConstants;
import org.openmrs.module.webservices.rest.web.v1_0.controller.BaseRestController;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.servlet.http.HttpServletResponse;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * GET  /ws/rest/v1/hivappointments          list appointments by site, category, date range and status
 * POST /ws/rest/v1/hivappointments/tracking  record a contact attempt and/or reschedule an appointment
 * GET  /ws/rest/v1/hivappointments/config    settings the frontend needs
 */
@Controller
@RequestMapping(value = "/rest/" + RestConstants.VERSION_1 + "/hivappointments")
public class HivAppointmentsRestController extends BaseRestController {

	private static final String DATE_FORMAT = "yyyy-MM-dd";

	private static final String DATETIME_FORMAT = "yyyy-MM-dd'T'HH:mm:ss.SSSZ";

	private static final int MAX_LIMIT = 500;

	@Autowired
	private HivAppointmentsService hivAppointmentsService;

	@Autowired
	private HivAppointmentsConfig hivAppointmentsConfig;

	@Autowired
	@Qualifier("locationService")
	private LocationService locationService;

	@RequestMapping(method = RequestMethod.GET)
	@ResponseBody
	public Object getAppointments(HttpServletResponse response,
	                              @RequestParam(value = "location", required = false) String locationUuid,
	                              @RequestParam(value = "category", required = false) String category,
	                              @RequestParam(value = "fromDate") String fromDate,
	                              @RequestParam(value = "toDate") String toDate,
	                              @RequestParam(value = "status", required = false) String status,
	                              @RequestParam(value = "q", required = false) String query,
	                              @RequestParam(value = "startIndex", required = false, defaultValue = "0") Integer startIndex,
	                              @RequestParam(value = "limit", required = false, defaultValue = "50") Integer limit) {
		try {
			HivAppointmentSearchCriteria criteria = new HivAppointmentSearchCriteria();
			criteria.setFromDate(parseDate(fromDate, "fromDate"));
			criteria.setToDate(parseDate(toDate, "toDate"));
			criteria.setCategory(StringUtils.isBlank(category) ? null : parseEnum(AppointmentCategory.class, category, "category"));
			criteria.setStatuses(parseStatuses(status));
			criteria.setLocations(getLocationAndDescendants(locationUuid));
			criteria.setQuery(query);
			criteria.setStartIndex(startIndex);
			criteria.setLimit(Math.min(limit, MAX_LIMIT));

			HivAppointmentSearchResult result = hivAppointmentsService.getAppointments(criteria);

			List<Map<String, Object>> results = new ArrayList<>();
			for (HivAppointment appointment : result.getResults()) {
				results.add(toJson(appointment));
			}
			SimpleObject ret = new SimpleObject();
			ret.add("results", results);
			ret.add("totalCount", result.getTotalCount());
			ret.add("startIndex", criteria.getStartIndex());
			ret.add("limit", criteria.getLimit());
			ret.add("countsByStatus", result.getCountsByStatus());
			ret.add("countsByCategory", result.getCountsByCategory());
			return ret;
		}
		catch (IllegalArgumentException e) {
			return error(response, HttpServletResponse.SC_BAD_REQUEST, e.getMessage());
		}
	}

	@RequestMapping(value = "/tracking", method = RequestMethod.POST)
	@ResponseBody
	public Object recordTracking(HttpServletResponse response, @RequestBody SimpleObject body) {
		try {
			String appointment = (String) body.get("appointment");
			if (StringUtils.isBlank(appointment)) {
				throw new IllegalArgumentException("appointment is required");
			}
			String newDate = (String) body.get("newDate");
			String locationUuid = (String) body.get("location");
			Location location = null;
			if (StringUtils.isNotBlank(locationUuid)) {
				location = locationService.getLocationByUuid(locationUuid);
				if (location == null) {
					throw new IllegalArgumentException("No location found with uuid " + locationUuid);
				}
			}

			Encounter encounter = hivAppointmentsService.recordTracking(appointment,
					StringUtils.isBlank(newDate) ? null : parseDate(newDate, "newDate"),
					(String) body.get("comment"), location);

			response.setStatus(HttpServletResponse.SC_CREATED);
			SimpleObject ret = new SimpleObject();
			ret.add("encounter", encounter.getUuid());
			ret.add("encounterDatetime", formatDatetime(encounter.getEncounterDatetime()));
			return ret;
		}
		catch (HivAppointmentConflictException e) {
			return error(response, HttpServletResponse.SC_CONFLICT, e.getMessage());
		}
		catch (IllegalArgumentException | ClassCastException e) {
			return error(response, HttpServletResponse.SC_BAD_REQUEST, e.getMessage());
		}
	}

	@RequestMapping(value = "/config", method = RequestMethod.GET)
	@ResponseBody
	public Object getConfig() {
		SimpleObject ret = new SimpleObject();
		ret.add("ltfuThresholdDays", hivAppointmentsConfig.getLtfuThresholdDays());
		ret.add("maxSearchRangeDays", HivAppointmentsConstants.MAX_SEARCH_RANGE_DAYS);
		Map<String, String> trackingEncounterTypes = new LinkedHashMap<>();
		for (AppointmentCategory category : AppointmentCategory.values()) {
			trackingEncounterTypes.put(category.name(), hivAppointmentsConfig.getTrackingEncounterType(category).getUuid());
		}
		ret.add("trackingEncounterTypes", trackingEncounterTypes);
		ret.add("viewPrivilege", HivAppointmentsConstants.PRIV_VIEW_HIV_APPOINTMENTS);
		ret.add("managePrivilege", HivAppointmentsConstants.PRIV_MANAGE_HIV_APPOINTMENTS);
		return ret;
	}

	private Map<String, Object> toJson(HivAppointment a) {
		Map<String, Object> ret = new LinkedHashMap<>();
		ret.put("uuid", a.getUuid());
		ret.put("category", a.getCategory());
		ret.put("status", a.getStatus());
		ret.put("scheduledDate", formatDate(a.getScheduledDate()));
		ret.put("daysLate", a.getDaysLate());
		ret.put("visitDate", formatDatetime(a.getVisitDate()));

		Map<String, Object> patient = new LinkedHashMap<>();
		patient.put("uuid", a.getPatientUuid());
		patient.put("name", a.getPatientName());
		patient.put("identifier", a.getPatientIdentifier());
		patient.put("gender", a.getPatientGender());
		patient.put("age", a.getPatientAge());
		patient.put("phoneNumber", a.getPatientPhoneNumber());
		ret.put("patient", patient);

		Map<String, Object> site = new LinkedHashMap<>();
		site.put("uuid", a.getSiteLocationUuid());
		site.put("name", a.getSiteLocationName());
		ret.put("site", site);

		Map<String, Object> source = new LinkedHashMap<>();
		source.put("uuid", a.getSourceEncounterUuid());
		source.put("encounterDatetime", formatDatetime(a.getSourceEncounterDate()));
		source.put("encounterType", a.getSourceEncounterTypeName());
		ret.put("sourceEncounter", source);

		if (a.getLastTrackingDate() != null) {
			Map<String, Object> tracking = new LinkedHashMap<>();
			tracking.put("date", formatDatetime(a.getLastTrackingDate()));
			tracking.put("by", a.getLastTrackingBy());
			tracking.put("comment", a.getLastTrackingComment());
			ret.put("lastTracking", tracking);
		} else {
			ret.put("lastTracking", null);
		}
		return ret;
	}

	private Set<Location> getLocationAndDescendants(String locationUuid) {
		Set<Location> locations = new HashSet<>();
		if (StringUtils.isNotBlank(locationUuid)) {
			Location location = locationService.getLocationByUuid(locationUuid);
			if (location == null) {
				throw new IllegalArgumentException("No location found with uuid " + locationUuid);
			}
			locations.add(location);
			locations.addAll(location.getDescendantLocations(true));
		}
		return locations;
	}

	private Set<AppointmentStatus> parseStatuses(String status) {
		Set<AppointmentStatus> statuses = EnumSet.noneOf(AppointmentStatus.class);
		if (StringUtils.isNotBlank(status)) {
			for (String s : status.split(",")) {
				if (StringUtils.isNotBlank(s)) {
					statuses.add(parseEnum(AppointmentStatus.class, s, "status"));
				}
			}
		}
		return statuses;
	}

	private <T extends Enum<T>> T parseEnum(Class<T> type, String value, String param) {
		try {
			return Enum.valueOf(type, value.trim().toUpperCase());
		}
		catch (IllegalArgumentException e) {
			throw new IllegalArgumentException("Invalid " + param + ": " + value);
		}
	}

	private Date parseDate(String value, String param) {
		SimpleDateFormat format = new SimpleDateFormat(DATE_FORMAT);
		format.setLenient(false);
		try {
			return format.parse(value.trim());
		}
		catch (ParseException e) {
			throw new IllegalArgumentException(param + " must be in the format " + DATE_FORMAT);
		}
	}

	private String formatDate(Date date) {
		return date == null ? null : new SimpleDateFormat(DATE_FORMAT).format(date);
	}

	private String formatDatetime(Date date) {
		return date == null ? null : new SimpleDateFormat(DATETIME_FORMAT).format(date);
	}

	private SimpleObject error(HttpServletResponse response, int status, String message) {
		response.setStatus(status);
		SimpleObject error = new SimpleObject();
		error.add("message", message);
		SimpleObject ret = new SimpleObject();
		ret.add("error", error);
		return ret;
	}
}
