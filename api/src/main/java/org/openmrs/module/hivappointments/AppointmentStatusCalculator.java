package org.openmrs.module.hivappointments;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.Date;

/**
 * Computes the status of an appointment from what happened after the encounter that scheduled it.
 * Kept free of any database access so the rules can be unit tested.
 */
public class AppointmentStatusCalculator {

	private final LocalDate today;

	private final int ltfuThresholdDays;

	public AppointmentStatusCalculator(Date today, int ltfuThresholdDays) {
		this.today = toLocalDate(today);
		this.ltfuThresholdDays = ltfuThresholdDays;
	}

	/**
	 * @param appointment the appointment, with its scheduled date set
	 * @param nextVisitDate the first qualifying visit after the scheduling encounter, or null if none
	 * @param laterScheduleCount the number of return visit date obs recorded after the scheduling encounter
	 */
	public void apply(HivAppointment appointment, Date nextVisitDate, int laterScheduleCount) {
		LocalDate scheduled = toLocalDate(appointment.getScheduledDate());
		appointment.setVisitDate(nextVisitDate);
		appointment.setDaysLate(null);

		if (nextVisitDate != null) {
			LocalDate visit = toLocalDate(nextVisitDate);
			if (visit.isAfter(scheduled)) {
				appointment.setStatus(AppointmentStatus.HONORED_LATE);
				appointment.setDaysLate((int) ChronoUnit.DAYS.between(scheduled, visit));
			} else {
				appointment.setStatus(AppointmentStatus.HONORED);
			}
		} else if (laterScheduleCount > 0) {
			appointment.setStatus(AppointmentStatus.RESCHEDULED);
		} else if (!scheduled.isBefore(today)) {
			appointment.setStatus(AppointmentStatus.UPCOMING);
		} else {
			int daysLate = (int) ChronoUnit.DAYS.between(scheduled, today);
			appointment.setDaysLate(daysLate);
			appointment.setStatus(daysLate > ltfuThresholdDays ? AppointmentStatus.LTFU : AppointmentStatus.MISSED);
		}
	}

	static LocalDate toLocalDate(Date date) {
		return Instant.ofEpochMilli(date.getTime()).atZone(ZoneId.systemDefault()).toLocalDate();
	}
}
