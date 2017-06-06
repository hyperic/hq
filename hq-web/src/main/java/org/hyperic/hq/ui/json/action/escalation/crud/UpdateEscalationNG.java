package org.hyperic.hq.ui.json.action.escalation.crud;

import java.io.InputStream;
import java.util.Iterator;
import java.util.Map;

import javax.annotation.Resource;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hyperic.hq.bizapp.shared.EventsBoss;
import org.hyperic.hq.common.DuplicateObjectException;
import org.hyperic.hq.context.Bootstrap;
import org.hyperic.hq.escalation.server.session.Escalation;
import org.hyperic.hq.escalation.shared.EscalationManager;
import org.hyperic.hq.ui.action.BaseActionNG;
import org.hyperic.hq.ui.json.JSONResult;
import org.hyperic.hq.ui.json.action.JsonActionContextNG;
import org.hyperic.util.ArrayUtil;
import org.json.JSONObject;
import org.springframework.stereotype.Component;

@Component(value = "updateEscalationNG")
public class UpdateEscalationNG extends BaseActionNG {
	
	 private final Log log = LogFactory.getLog(UpdateEscalationNG.class);

	@Resource
	private EscalationManager escalationManager;

	private InputStream inputStream;

	public InputStream getInputStream() {
		return inputStream;
	}
	
	public String execute() throws Exception {
		
		JsonActionContextNG context = this.setJSONContext();
        Map p = context.getParameterMap();

        if (log.isDebugEnabled()) {
            for (Iterator i=p.entrySet().iterator(); i.hasNext(); ) {
                Map.Entry ent = (Map.Entry)i.next();
                
                log.debug("key=" + ent.getKey() + " val=" + 
                          ArrayUtil.toString((Object[])ent.getValue()));
            }
        }
        
        String  name     = ((String[])p.get("name"))[0];
        String  desc     = ((String[])p.get("description"))[0];
        long maxWait     = Long.parseLong(((String[])p.get("maxWaitTime"))[0]); 
        boolean pausable =
            Boolean.valueOf(((String[])p.get("allowPause"))[0]).booleanValue();
        boolean notifyAll = 
            Boolean.valueOf(((String[])p.get("notifyAll"))[0]).booleanValue();
        boolean repeat = 
            Boolean.valueOf(((String[])p.get("repeat"))[0]).booleanValue();

        Integer id = Integer.valueOf(((String[]) p.get(JSONConstants.ID))[0]);

        EventsBoss eBoss = 
           Bootstrap.getBean(EventsBoss.class);
        Escalation escalation = eBoss.findEscalationById(context.getSessionId(),
                                                         id);
        JSONObject result;
        try {
            eBoss.updateEscalation(context.getSessionId(), escalation, name,
                    desc, maxWait, pausable, notifyAll, repeat);
            result = Escalation.getJSON(escalation);
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
