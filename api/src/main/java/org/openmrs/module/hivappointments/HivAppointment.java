package org.openmrs.module.hivappointments;

import java.util.Date;

/**
 * An HIV appointment, derived from a return visit date obs.
 * The ids are used internally to load details; the uuids and display values are what clients see.
 */
public class HivAppointment {

	// Internal ids, populated from the search query

	private Integer obsId;

	private Integer personId;

	private Integer sourceEncounterId;

	private Integer siteLocationId;

	private Integer lastTrackingEncounterId;

	// Appointment

	private String uuid;

	private AppointmentCategory category;

	private AppointmentStatus status;

	private Date scheduledDate;

	private Integer daysLate;

	private Date visitDate;

	private String sourceEncounterUuid;

	private Date sourceEncounterDate;

	private String sourceEncounterTypeName;

	private String siteLocationUuid;

	private String siteLocationName;

	// Patient

	private String patientUuid;

	private String patientName;

	private String patientIdentifier;

	private String patientGender;

	private Integer patientAge;

	private String patientPhoneNumber;

	// Latest tracking action

	private Date lastTrackingDate;

	private String lastTrackingBy;

	private String lastTrackingComment;

	public Integer getObsId() {
		return obsId;
	}

	public void setObsId(Integer obsId) {
		this.obsId = obsId;
	}

	public Integer getPersonId() {
		return personId;
	}

	public void setPersonId(Integer personId) {
		this.personId = personId;
	}

	public Integer getSourceEncounterId() {
		return sourceEncounterId;
	}

	public void setSourceEncounterId(Integer sourceEncounterId) {
		this.sourceEncounterId = sourceEncounterId;
	}

	public Integer getSiteLocationId() {
		return siteLocationId;
	}

	public void setSiteLocationId(Integer siteLocationId) {
		this.siteLocationId = siteLocationId;
	}

	public Integer getLastTrackingEncounterId() {
		return lastTrackingEncounterId;
	}

	public void setLastTrackingEncounterId(Integer lastTrackingEncounterId) {
		this.lastTrackingEncounterId = lastTrackingEncounterId;
	}

	public String getUuid() {
		return uuid;
	}

	public void setUuid(String uuid) {
		this.uuid = uuid;
	}

	public AppointmentCategory getCategory() {
		return category;
	}

	public void setCategory(AppointmentCategory category) {
		this.category = category;
	}

	public AppointmentStatus getStatus() {
		return status;
	}

	public void setStatus(AppointmentStatus status) {
		this.status = status;
	}

	public Date getScheduledDate() {
		return scheduledDate;
	}

	public void setScheduledDate(Date scheduledDate) {
		this.scheduledDate = scheduledDate;
	}

	public Integer getDaysLate() {
		return daysLate;
	}

	public void setDaysLate(Integer daysLate) {
		this.daysLate = daysLate;
	}

	public Date getVisitDate() {
		return visitDate;
	}

	public void setVisitDate(Date visitDate) {
		this.visitDate = visitDate;
	}

	public String getSourceEncounterUuid() {
		return sourceEncounterUuid;
	}

	public void setSourceEncounterUuid(String sourceEncounterUuid) {
		this.sourceEncounterUuid = sourceEncounterUuid;
	}

	public Date getSourceEncounterDate() {
		return sourceEncounterDate;
	}

	public void setSourceEncounterDate(Date sourceEncounterDate) {
		this.sourceEncounterDate = sourceEncounterDate;
	}

	public String getSourceEncounterTypeName() {
		return sourceEncounterTypeName;
	}

	public void setSourceEncounterTypeName(String sourceEncounterTypeName) {
		this.sourceEncounterTypeName = sourceEncounterTypeName;
	}

	public String getSiteLocationUuid() {
		return siteLocationUuid;
	}

	public void setSiteLocationUuid(String siteLocationUuid) {
		this.siteLocationUuid = siteLocationUuid;
	}

	public String getSiteLocationName() {
		return siteLocationName;
	}

	public void setSiteLocationName(String siteLocationName) {
		this.siteLocationName = siteLocationName;
	}

	public String getPatientUuid() {
		return patientUuid;
	}

	public void setPatientUuid(String patientUuid) {
		this.patientUuid = patientUuid;
	}

	public String getPatientName() {
		return patientName;
	}

	public void setPatientName(String patientName) {
		this.patientName = patientName;
	}

	public String getPatientIdentifier() {
		return patientIdentifier;
	}

	public void setPatientIdentifier(String patientIdentifier) {
		this.patientIdentifier = patientIdentifier;
	}

	public String getPatientGender() {
		return patientGender;
	}

	public void setPatientGender(String patientGender) {
		this.patientGender = patientGender;
	}

	public Integer getPatientAge() {
		return patientAge;
	}

	public void setPatientAge(Integer patientAge) {
		this.patientAge = patientAge;
	}

	public String getPatientPhoneNumber() {
		return patientPhoneNumber;
	}

	public void setPatientPhoneNumber(String patientPhoneNumber) {
		this.patientPhoneNumber = patientPhoneNumber;
	}

	public Date getLastTrackingDate() {
		return lastTrackingDate;
	}

	public void setLastTrackingDate(Date lastTrackingDate) {
		this.lastTrackingDate = lastTrackingDate;
	}

	public String getLastTrackingBy() {
		return lastTrackingBy;
	}

	public void setLastTrackingBy(String lastTrackingBy) {
		this.lastTrackingBy = lastTrackingBy;
	}

	public String getLastTrackingComment() {
		return lastTrackingComment;
	}

	public void setLastTrackingComment(String lastTrackingComment) {
		this.lastTrackingComment = lastTrackingComment;
	}
}
