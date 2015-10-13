package org.hyperic.hq.ui.action.admin.config;

import javax.annotation.Resource;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hyperic.hq.bizapp.shared.EventsBoss;
import org.hyperic.hq.ui.action.BaseActionNG;
import org.hyperic.hq.ui.util.RequestUtils;
import org.springframework.stereotype.Component;

@Component(value = "removeEscalationActionNG")
public class RemoveEscalationActionNG extends BaseActionNG {

    private final Log log = LogFactory.getLog(RemoveEscalationActionNG.class.getName());
    
    @Resource
    private EventsBoss eventsBoss;
    
    public String execute() throws Exception {
    	
        log.debug("entering RemoveEscalationActionNG");
        Integer escId = RequestUtils.getIntParameter(request, "esc");

        Integer sessionId = RequestUtils.getSessionId(request);

        try {
            eventsBoss.deleteEscalationById(sessionId.intValue(), escId);
        } catch (Exception e) {
            addActionError(getText( "admin.config.error.escalation.CannotDelete") );
        }

        return SUCCESS;
    }
}
