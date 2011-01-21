package org.hyperic.hq.hqu.grails.hqugapi;

import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hyperic.hibernate.PageInfo;
import org.hyperic.hq.context.Bootstrap;
import org.hyperic.hq.events.AlertSeverity;
import org.hyperic.hq.events.server.session.AlertDefinition;
import org.hyperic.hq.events.shared.AlertDefinitionManager;

/**
 * Middleware api to handle info for alert definitions.
 */
public class AlertDefinitionHQUGApi extends BaseHQUGApi {

	private static final Log log = LogFactory.getLog(AlertDefinitionHQUGApi.class);
	
    private AlertDefinitionManager alertDefMan = Bootstrap.getBean(AlertDefinitionManager.class);
    
    public AlertDefinitionHQUGApi() {
    	super();
    }

    /**
     * 
     * @param minSeverity
     * @param enabled
     * @param excludeTypeBased
     * @param pInfo
     * @return
     */
    public  List<AlertDefinition> findDefinitions(AlertSeverity minSeverity, Boolean enabled, boolean excludeTypeBased, PageInfo pInfo) {
    	return alertDefMan.findAlertDefinitions(getSubject(), minSeverity, enabled, excludeTypeBased, pInfo);
    }
    
}
