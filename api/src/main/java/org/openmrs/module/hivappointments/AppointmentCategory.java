package org.openmrs.module.hivappointments;

/**
 * An HIV appointment is either a clinical follow-up visit or an ARV pickup (dispensing).
 * Both are recorded as a return visit date obs, but on different encounter types.
 */
public enum AppointmentCategory {
	CLINICAL,
	ARV_PICKUP
}
