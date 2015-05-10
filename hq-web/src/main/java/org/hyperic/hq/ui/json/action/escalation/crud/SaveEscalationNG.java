package org.hyperic.hq.ui.json.action.escalation.crud;

import java.io.InputStream;
import java.util.Map;

import javax.annotation.Resource;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hyperic.hq.bizapp.shared.EventsBoss;
import org.hyperic.hq.common.DuplicateObjectException;
import org.hyperic.hq.escalation.server.session.Escalation;
import org.hyperic.hq.escalation.server.session.EscalationAlertType;
import org.hyperic.hq.events.server.session.ClassicEscalationAlertType;
import org.hyperic.hq.galerts.server.session.GalertEscalationAlertType;
import org.hyperic.hq.ui.action.BaseActionNG;
import org.hyperic.hq.ui.json.JSONResult;
import org.hyperic.hq.ui.json.action.JsonActionContextNG;
import org.json.JSONObject;
import org.springframework.stereotype.Component;

@Component(value = "saveEscalationNG")
public class SaveEscalationNG extends BaseActionNG {

	private final Log _log = LogFactory.getLog(SaveEscalationNG.class);

	@Resource
	private EventsBoss eBoss;
	
	private InputStream inputStream;

	public InputStream getInputStream() {
		return inputStream;
	}

	public String execute() throws Exception {
		
		JsonActionContextNG context = this.setJSONContext();
		Map p = context.getParameterMap();

		String name = ((String[]) p.get("name"))[0];
		String desc = ((String[]) p.get("description"))[0];
		long maxWait = Long.parseLong(((String[]) p.get("maxWaitTime"))[0]);
		boolean pausable = Boolean.valueOf(((String[]) p.get("allowPause"))[0])
				.booleanValue();
		boolean notifyAll = Boolean.valueOf(((String[]) p.get("notifyAll"))[0])
				.booleanValue();
		boolean repeat = Boolean.valueOf(((String[]) p.get("repeat"))[0])
				.booleanValue();

		// These specify an optional alert definition to attach to
		String[] aDef = (String[]) p.get(JSONConstants.ALERTDEF_ID);
		String[] gaDef = (String[]) p.get(JSONConstants.GALERTDEF_ID);
		EscalationAlertType alertType = null;
		Integer alertDefId = null;

		if (aDef != null && !"undefined".equals(aDef[0])
				&& aDef[0].length() > 0) {
			alertType = ClassicEscalationAlertType.CLASSIC;
			alertDefId = Integer.valueOf(aDef[0]);
		} else if (gaDef != null && !"undefined".equals(gaDef[0])
				&& aDef[0].length() > 0) {
			alertType = GalertEscalationAlertType.GALERT;
			alertDefId = Integer.valueOf(gaDef[0]);
		}

		// EventsBoss eBoss = Bootstrap.getBean(EventsBoss.class);
		JSONObject result;
		try {
			Escalation e = eBoss.createEscalation(context.getSessionId(), name,
					desc, pausable, maxWait, notifyAll, repeat, alertType,
					alertDefId);
			result = Escalation.getJSON((e));
		} catch (DuplicateObjectException exception) {
			// An escalation by this name already exists show error msg.
			result = new JSONObject();
			result.put("error", "An escalation with this name already exists.");
		}
		context.setJSONResult(new JSONResult(result));
		context.getRequest().setAttribute(Escalation.JSON_NAME, result);

		inputStream = this.streamJSONResult(context);
		
		return null;
	}
}
