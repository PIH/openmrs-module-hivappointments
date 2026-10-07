package org.openmrs.module.hivappointments;

import org.openmrs.api.APIException;

/**
 * Thrown when a user acts on an appointment that has been superseded since they loaded it,
 * typically because a colleague at the same site already rescheduled it.
 */
public class HivAppointmentConflictException extends APIException {

	public HivAppointmentConflictException(String message) {
		super(message);
	}
}
