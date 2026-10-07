package org.openmrs.module.hivappointments;

import org.openmrs.Encounter;
import org.openmrs.Location;
import org.openmrs.annotation.Authorized;
import org.openmrs.api.OpenmrsService;

import java.util.Date;

public interface HivAppointmentsService extends OpenmrsService {

	/**
	 * Lists the HIV appointments scheduled in a date range, with their computed status.
	 * Only patients enrolled in the HIV program (and alive) on the scheduled date are included.
	 */
	@Authorized(HivAppointmentsConstants.PRIV_VIEW_HIV_APPOINTMENTS)
	HivAppointmentSearchResult getAppointments(HivAppointmentSearchCriteria criteria);

	/**
	 * Records a tracking action on an appointment: a contact attempt (comment) and/or a new date.
	 * This creates a tracking encounter for the patient; a new date supersedes the appointment.
	 *
	 * @param appointmentUuid the uuid of the return visit date obs that scheduled the appointment
	 * @param newDate the new scheduled date, or null to only record a comment
	 * @param comment free text (call outcome, reason for missed visit...), or null
	 * @param location where the tracking happens; defaults to the location of the scheduling encounter
	 * @throws HivAppointmentConflictException if the appointment was rescheduled or honored since it was loaded
	 */
	@Authorized(HivAppointmentsConstants.PRIV_MANAGE_HIV_APPOINTMENTS)
	Encounter recordTracking(String appointmentUuid, Date newDate, String comment, Location location);
}
