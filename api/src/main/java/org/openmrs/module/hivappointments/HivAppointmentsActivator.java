package org.openmrs.module.hivappointments;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.openmrs.EncounterType;
import org.openmrs.api.EncounterService;
import org.openmrs.api.context.Context;
import org.openmrs.module.BaseModuleActivator;
import org.openmrs.util.PrivilegeConstants;

import static org.openmrs.module.hivappointments.HivAppointmentsConstants.*;

public class HivAppointmentsActivator extends BaseModuleActivator {

	protected final Log log = LogFactory.getLog(getClass());

	@Override
	public void started() {
		try {
			Context.addProxyPrivilege(PrivilegeConstants.GET_ENCOUNTER_TYPES);
			Context.addProxyPrivilege(PrivilegeConstants.MANAGE_ENCOUNTER_TYPES);
			ensureEncounterType(CLINICAL_TRACKING_ENCOUNTER_TYPE_UUID, CLINICAL_TRACKING_ENCOUNTER_TYPE_NAME,
					"Follow-up of an HIV clinical appointment: contact attempts and rescheduling");
			ensureEncounterType(ARV_TRACKING_ENCOUNTER_TYPE_UUID, ARV_TRACKING_ENCOUNTER_TYPE_NAME,
					"Follow-up of an ARV pickup appointment: contact attempts and rescheduling");
		}
		finally {
			Context.removeProxyPrivilege(PrivilegeConstants.GET_ENCOUNTER_TYPES);
			Context.removeProxyPrivilege(PrivilegeConstants.MANAGE_ENCOUNTER_TYPES);
		}
		log.info("HIV Appointments module started");
	}

	private void ensureEncounterType(String uuid, String name, String description) {
		EncounterService encounterService = Context.getEncounterService();
		if (encounterService.getEncounterTypeByUuid(uuid) == null) {
			EncounterType type = new EncounterType(name, description);
			type.setUuid(uuid);
			encounterService.saveEncounterType(type);
			log.info("Created encounter type " + name);
		}
	}
}
