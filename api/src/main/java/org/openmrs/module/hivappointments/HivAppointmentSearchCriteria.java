package org.openmrs.module.hivappointments;

import org.openmrs.Location;

import java.util.Date;
import java.util.Set;

public class HivAppointmentSearchCriteria {

	/** Scheduled date on or after, required */
	private Date fromDate;

	/** Scheduled date on or before, required */
	private Date toDate;

	/** If null, both categories are returned */
	private AppointmentCategory category;

	/** HIV sites to include (descendants are resolved by the caller). If empty, all sites. */
	private Set<Location> locations;

	/** If empty, all statuses. Summary counts are always computed across all statuses. */
	private Set<AppointmentStatus> statuses;

	/** Optional name or identifier filter */
	private String query;

	private Integer startIndex = 0;

	private Integer limit = 50;

	public Date getFromDate() {
		return fromDate;
	}

	public void setFromDate(Date fromDate) {
		this.fromDate = fromDate;
	}

	public Date getToDate() {
		return toDate;
	}

	public void setToDate(Date toDate) {
		this.toDate = toDate;
	}

	public AppointmentCategory getCategory() {
		return category;
	}

	public void setCategory(AppointmentCategory category) {
		this.category = category;
	}

	public Set<Location> getLocations() {
		return locations;
	}

	public void setLocations(Set<Location> locations) {
		this.locations = locations;
	}

	public Set<AppointmentStatus> getStatuses() {
		return statuses;
	}

	public void setStatuses(Set<AppointmentStatus> statuses) {
		this.statuses = statuses;
	}

	public String getQuery() {
		return query;
	}

	public void setQuery(String query) {
		this.query = query;
	}

	public Integer getStartIndex() {
		return startIndex;
	}

	public void setStartIndex(Integer startIndex) {
		this.startIndex = startIndex;
	}

	public Integer getLimit() {
		return limit;
	}

	public void setLimit(Integer limit) {
		this.limit = limit;
	}
}
