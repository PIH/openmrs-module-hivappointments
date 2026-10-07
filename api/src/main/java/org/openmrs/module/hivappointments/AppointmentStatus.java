package org.openmrs.module.hivappointments;

public enum AppointmentStatus {

	/** Scheduled date is today or later, and the patient has not come yet */
	UPCOMING,

	/** The patient came on or before the scheduled date */
	HONORED,

	/** The patient came, but after the scheduled date */
	HONORED_LATE,

	/** A newer return visit date was recorded (tracking reschedule) before the patient came */
	RESCHEDULED,

	/** Scheduled date has passed, patient has not come, within the LTFU threshold */
	MISSED,

	/** Scheduled date has passed by more than the LTFU threshold, patient has not come */
	LTFU
}
