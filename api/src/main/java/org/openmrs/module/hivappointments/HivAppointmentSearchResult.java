package org.openmrs.module.hivappointments;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

public class HivAppointmentSearchResult {

	private List<HivAppointment> results = new ArrayList<>();

	/** Number of appointments matching the criteria, including the status filter */
	private int totalCount;

	/** Count per status, ignoring the status filter, so a dashboard can show all tiles at once */
	private final Map<AppointmentStatus, Integer> countsByStatus = new EnumMap<>(AppointmentStatus.class);

	/** Count per category, ignoring the status filter */
	private final Map<AppointmentCategory, Integer> countsByCategory = new EnumMap<>(AppointmentCategory.class);

	public HivAppointmentSearchResult() {
		for (AppointmentStatus status : AppointmentStatus.values()) {
			countsByStatus.put(status, 0);
		}
		for (AppointmentCategory category : AppointmentCategory.values()) {
			countsByCategory.put(category, 0);
		}
	}

	public List<HivAppointment> getResults() {
		return results;
	}

	public void setResults(List<HivAppointment> results) {
		this.results = results;
	}

	public int getTotalCount() {
		return totalCount;
	}

	public void setTotalCount(int totalCount) {
		this.totalCount = totalCount;
	}

	public Map<AppointmentStatus, Integer> getCountsByStatus() {
		return countsByStatus;
	}

	public Map<AppointmentCategory, Integer> getCountsByCategory() {
		return countsByCategory;
	}

	void increment(HivAppointment appointment) {
		countsByStatus.put(appointment.getStatus(), countsByStatus.get(appointment.getStatus()) + 1);
		countsByCategory.put(appointment.getCategory(), countsByCategory.get(appointment.getCategory()) + 1);
	}
}
