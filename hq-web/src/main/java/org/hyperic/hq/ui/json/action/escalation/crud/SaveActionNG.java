package org.hyperic.hq.ui.json.action.escalation.crud;

import java.io.InputStream;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import javax.servlet.ServletContext;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hyperic.hq.bizapp.shared.EventsBoss;
import org.hyperic.hq.bizapp.shared.action.EmailActionConfig;
import org.hyperic.hq.bizapp.shared.action.SnmpActionConfig;
import org.hyperic.hq.bizapp.shared.action.SyslogActionConfig;
import org.hyperic.hq.common.SystemException;
import org.hyperic.hq.context.Bootstrap;
import org.hyperic.hq.escalation.server.session.Escalation;
import org.hyperic.hq.events.ActionConfigInterface;
import org.hyperic.hq.events.NoOpAction;
import org.hyperic.hq.ui.action.BaseActionNG;
import org.hyperic.hq.ui.json.action.JsonActionContextNG;
import org.hyperic.util.StringUtil;
import org.springframework.stereotype.Component;

@Component(value = "saveActionNG")
public class SaveActionNG extends BaseActionNG {

	private final Log _log = LogFactory.getLog(SaveActionNG.class);

	private InputStream inputStream;

	public InputStream getInputStream() {
		return inputStream;
	}

	public String execute() throws Exception {
		JsonActionContextNG context = this.setJSONContext();

		ServletContext sctx = context.getServletContext();
		ActionConfigInterface cfg;

		Map map = context.getParameterMap();
		String action = ((String[]) map.get("action"))[0];
		Integer escId = Integer.valueOf(((String[]) map.get("EscId"))[0]);
		EventsBoss eBoss = Bootstrap.getBean(EventsBoss.class);
		int sessId = context.getSessionId();
		Escalation e = eBoss.findEscalationById(sessId, escId);
		long wait = Long.parseLong(((String[]) map.get("waittime"))[0]);

		if (action.equalsIgnoreCase("Email")) {
			cfg = makeEmailActionCfg(e, map, false);
		} else if (action.equalsIgnoreCase("SMS")) {
			cfg = makeEmailActionCfg(e, map, true);
		} else if (action.equalsIgnoreCase("Syslog")) {
			cfg = makeSyslogActionCfg(e, map);
		} else if (action.equalsIgnoreCase("SNMP")) {
			cfg = makeSNMPActionCfg(e, map);
		} else if (action.equalsIgnoreCase("noop")) {
			cfg = new NoOpAction(); // Yow.
		} else {
			throw new SystemException("Unknown action type [" + action + "]");
		}

		eBoss.addAction(sessId, e, cfg, wait);

		inputStream = this.streamJSONResult(context);

		return null;
	}

	private ActionConfigInterface makeSyslogActionCfg(Escalation e, Map p) {
		String meta = ((String[]) p.get("meta"))[0];
		String version = ((String[]) p.get("version"))[0];
		String product = ((String[]) p.get("product"))[0];

		return new SyslogActionConfig(meta, product, version);
	}

	private ActionConfigInterface makeSNMPActionCfg(Escalation e, Map p) {
		String address = ((String[]) p.get("snmpIP"))[0];
		String oid = ((String[]) p.get("snmpOID"))[0];
		String snmpTrapOID = null;
		if ((String[]) p.get("snmpTrapOID") != null) {
			snmpTrapOID = ((String[]) p.get("snmpTrapOID"))[0];
		}
		String snmpNotificationMechanism = ((String[]) p
				.get("snmpNotificationMechanism"))[0];
		String variableBindings = ((String[]) p.get("variableBindings"))[0];

		return new SnmpActionConfig(snmpNotificationMechanism, address,
				snmpTrapOID, oid, variableBindings);
	}

	private ActionConfigInterface makeEmailActionCfg(Escalation e, Map p,
			boolean sms) {
		EmailActionConfig cfg = new EmailActionConfig();
		String sType = ((String[]) p.get("who"))[0];
		String nameVar;

		if (sType.equals("Users")) {
			cfg.setType(EmailActionConfig.TYPE_USERS);
			nameVar = "users";
		} else if (sType.equals("Others")) {
			cfg.setType(EmailActionConfig.TYPE_EMAILS);
			nameVar = "emailinput";
		} else if (sType.equals("Roles")) {
			cfg.setType(EmailActionConfig.TYPE_ROLES);
			nameVar = "roles";
		} else {
			throw new SystemException("Unknown email type [" + sType + "]");
		}

		String[] nameArr = (String[]) p.get(nameVar);
		List nameList = Arrays.asList(nameArr);
		cfg.setNames(StringUtil.implode(nameList, ","));
		cfg.setSms(sms);
		return cfg;
	}

}
