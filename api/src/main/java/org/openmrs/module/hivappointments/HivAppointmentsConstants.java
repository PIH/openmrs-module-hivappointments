package org.openmrs.module.hivappointments;

/**
 * Constants for the HIV Appointments module.
 * Defaults match the ZL / PIH EMR metadata (openmrs-config-pihemr constants.yml) and can be overridden via global properties.
 */
public class HivAppointmentsConstants {

	public static final String MODULE_ID = "hivappointments";

	// Privileges

	public static final String PRIV_VIEW_HIV_APPOINTMENTS = "View HIV Appointments";

	public static final String PRIV_MANAGE_HIV_APPOINTMENTS = "Manage HIV Appointments";

	// Tracking encounter types, created by the activator if missing

	public static final String CLINICAL_TRACKING_ENCOUNTER_TYPE_UUID = "f5a2c1d8-3b7e-4c9a-8e21-6d4b9f0a7c13";

	public static final String CLINICAL_TRACKING_ENCOUNTER_TYPE_NAME = "HIV Appointment Tracking";

	public static final String ARV_TRACKING_ENCOUNTER_TYPE_UUID = "a9d4e2b7-6c1f-4e8a-b352-0e7f1c9d4a86";

	public static final String ARV_TRACKING_ENCOUNTER_TYPE_NAME = "HIV ARV Pickup Tracking";

	// Global properties

	public static final String GP_HIV_PROGRAM = "hivappointments.hivProgramUuid";

	public static final String GP_RETURN_VISIT_DATE_CONCEPT = "hivappointments.returnVisitDateConcept";

	public static final String GP_COMMENT_CONCEPT = "hivappointments.commentConcept";

	public static final String GP_CLINICAL_ENCOUNTER_TYPES = "hivappointments.clinicalEncounterTypes";

	public static final String GP_ARV_PICKUP_ENCOUNTER_TYPES = "hivappointments.arvPickupEncounterTypes";

	public static final String GP_LTFU_THRESHOLD_DAYS = "hivappointments.ltfuThresholdDays";

	public static final String GP_PHONE_ATTRIBUTE_TYPE = "hivappointments.phoneAttributeTypeUuid";

	public static final String GP_IDENTIFIER_TYPE = "hivappointments.identifierTypeUuid";

	// Global property defaults (PIH EMR metadata)

	public static final String DEFAULT_HIV_PROGRAM = "b1cb1fc1-5190-4f7a-af08-48870975dafc";

	public static final String DEFAULT_RETURN_VISIT_DATE_CONCEPT = "3ce94df0-26fe-102b-80cb-0017a47871b2";

	public static final String DEFAULT_COMMENT_CONCEPT = "CIEL:161011";

	// HIV Intake, HIV Followup, EID Followup, PMTCT Intake, PMTCT Followup - same list as statusData/hiv/nextAppointmentDate.sql
	public static final String DEFAULT_CLINICAL_ENCOUNTER_TYPES = "c31d306a-40c4-11e7-a919-92ebcb67fe33,"
			+ "c31d3312-40c4-11e7-a919-92ebcb67fe33,"
			+ "0f070640-279e-4ec0-9e6c-6ef1f6567030,"
			+ "584124b9-1f10-4757-ba09-91fc9075af92,"
			+ "95e03e7d-9aeb-4a99-bd7a-94e8591ec2c5";

	// HIV drug dispensing - same as statusData/hiv/nextMedicationPickupDate.sql
	public static final String DEFAULT_ARV_PICKUP_ENCOUNTER_TYPES = "cc1720c9-3e4c-4fa8-a7ec-40eeaad1958c";

	// Aligned with statusData/hiv/hivPatientStatus.sql
	public static final int DEFAULT_LTFU_THRESHOLD_DAYS = 28;

	public static final String DEFAULT_PHONE_ATTRIBUTE_TYPE = "14d4f066-15f5-102d-96e4-000c29c2a5d7";

	public static final int MAX_SEARCH_RANGE_DAYS = 366;
}
