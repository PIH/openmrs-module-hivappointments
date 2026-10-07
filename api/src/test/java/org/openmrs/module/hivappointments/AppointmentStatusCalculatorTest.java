package org.openmrs.module.hivappointments;

import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Date;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

public class AppointmentStatusCalculatorTest {

	private static final LocalDate TODAY = LocalDate.of(2026, 9, 24);

	private final AppointmentStatusCalculator calculator = new AppointmentStatusCalculator(date(TODAY), 28);

	@Test
	public void shouldBeHonoredIfSeenOnOrBeforeScheduledDate() {
		HivAppointment a = appointment(TODAY.minusDays(10));
		calculator.apply(a, date(TODAY.minusDays(10)), 1);
		assertEquals(AppointmentStatus.HONORED, a.getStatus());
		assertNull(a.getDaysLate());

		calculator.apply(a, date(TODAY.minusDays(15)), 0);
		assertEquals(AppointmentStatus.HONORED, a.getStatus());
	}

	@Test
	public void shouldBeHonoredLateIfSeenAfterScheduledDate() {
		HivAppointment a = appointment(TODAY.minusDays(10));
		calculator.apply(a, date(TODAY.minusDays(3)), 1);
		assertEquals(AppointmentStatus.HONORED_LATE, a.getStatus());
		assertEquals(7, a.getDaysLate().intValue());
	}

	@Test
	public void shouldBeRescheduledIfNewerDateRecordedAndNotSeen() {
		HivAppointment a = appointment(TODAY.minusDays(5));
		calculator.apply(a, null, 1);
		assertEquals(AppointmentStatus.RESCHEDULED, a.getStatus());
	}

	@Test
	public void shouldBeUpcomingIfTodayOrLater() {
		HivAppointment a = appointment(TODAY);
		calculator.apply(a, null, 0);
		assertEquals(AppointmentStatus.UPCOMING, a.getStatus());

		a = appointment(TODAY.plusDays(30));
		calculator.apply(a, null, 0);
		assertEquals(AppointmentStatus.UPCOMING, a.getStatus());
	}

	@Test
	public void shouldBeMissedUpToThresholdThenLtfu() {
		HivAppointment a = appointment(TODAY.minusDays(1));
		calculator.apply(a, null, 0);
		assertEquals(AppointmentStatus.MISSED, a.getStatus());
		assertEquals(1, a.getDaysLate().intValue());

		a = appointment(TODAY.minusDays(28));
		calculator.apply(a, null, 0);
		assertEquals(AppointmentStatus.MISSED, a.getStatus());

		a = appointment(TODAY.minusDays(29));
		calculator.apply(a, null, 0);
		assertEquals(AppointmentStatus.LTFU, a.getStatus());
		assertEquals(29, a.getDaysLate().intValue());
	}

	private HivAppointment appointment(LocalDate scheduled) {
		HivAppointment a = new HivAppointment();
		a.setScheduledDate(date(scheduled));
		return a;
	}

	private static Date date(LocalDate date) {
		return Date.from(date.atStartOfDay(ZoneId.systemDefault()).toInstant());
	}
}
