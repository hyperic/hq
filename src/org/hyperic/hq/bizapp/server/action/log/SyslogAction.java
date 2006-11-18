/*
 * NOTE: This copyright does *not* cover user programs that use HQ
 * program services by normal system calls through the application
 * program interfaces provided as part of the Hyperic Plug-in Development
 * Kit or the Hyperic Client Development Kit - this is merely considered
 * normal use of the program, and does *not* fall under the heading of
 * "derived work".
 * 
 * Copyright (C) [2004, 2005, 2006], Hyperic, Inc.
 * This file is part of HQ.
 * 
 * HQ is free software; you can redistribute it and/or modify
 * it under the terms version 2 of the GNU General Public License as
 * published by the Free Software Foundation. This program is distributed
 * in the hope that it will be useful, but WITHOUT ANY WARRANTY; without
 * even the implied warranty of MERCHANTABILITY or FITNESS FOR A
 * PARTICULAR PURPOSE. See the GNU General Public License for more
 * details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307
 * USA.
 */

/*
 * SyslogAction.java
 *
 * Created on October 10, 2002, 4:05 PM
 */

package org.hyperic.hq.bizapp.server.action.log;

import java.util.HashMap;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hyperic.hq.appdef.shared.AppdefEntityID;
import org.hyperic.hq.bizapp.server.action.email.EmailFilter;
import org.hyperic.hq.bizapp.shared.action.SyslogActionConfig;
import org.hyperic.hq.events.ActionExecuteException;
import org.hyperic.hq.events.ActionInterface;
import org.hyperic.hq.events.EventConstants;
import org.hyperic.hq.events.InvalidActionDataException;
import org.hyperic.hq.events.TriggerFiredEvent;
import org.hyperic.hq.events.shared.AlertConditionValue;
import org.hyperic.hq.events.shared.AlertDefinitionBasicValue;
import org.hyperic.hq.events.shared.AlertConditionLogValue;
import org.hyperic.util.StringUtil;
import org.hyperic.util.config.ConfigResponse;

/**
 *
 */
public class SyslogAction extends SyslogActionConfig
    implements ActionInterface {
    private Log log = LogFactory.getLog(SyslogAction.class.getName());
    
    private static final int PRI_HIGH = 3;
    private static final int PRI_MED  = 2;
    private static final int PRI_LOW  = 1;
    
    /** Creates a new instance of SharedEmailAction */
    public SyslogAction() {
    }

    private String createConditions(AlertConditionValue[] conds,
                                    AlertConditionLogValue[] logs, String indent) {
        StringBuffer text = new StringBuffer();

        for (int i = 0; i < conds.length; i++) {
            if (i == 0) {
                text.append(indent).append("If ");
            } else {
                text.append(indent).append(
                    conds[i].getRequired() ? " AND " : " OR ");
            }

            switch (conds[i].getType()) {
                case EventConstants.TYPE_THRESHOLD :
                    text
                        .append(conds[i].getName())
                        .append(" ")
                        .append(conds[i].getComparator())
                        .append(" ")
                        .append(conds[i].getThreshold());

                    text.append(" (actual value = ");

                    // Make sure the event is present to be displayed
                    text.append(logs[i].getCondition().getTriggerId());

                    text.append(")");
                    break;
                default :
                    break;
            }
        }

        return text.toString();
    }

    private int convertToDBPriority(int priority) {
        // Deutsche Bank priority numbers
        switch (priority) {
            case PRI_HIGH :
                return 5;
            case PRI_MED :
                return 4;
            default :
            case PRI_LOW :
                return 2;
        }
    }

    /** Execute the action
     *
     */
    public String execute(AlertDefinitionBasicValue alertdef,
                          AlertConditionLogValue[] logs, Integer alertId)
        throws ActionExecuteException {
//        TriggerFiredEvent[] firedEvents = event.getRootEvents();
//        HashMap eventMap = new HashMap();
//        for (int i = 0; i < firedEvents.length; i++) {
//            eventMap.put(
//                firedEvents[i].getInstanceId(),
//                firedEvents[i].toString());
//        }
        
        EmailFilter filter = EmailFilter.getInstance();
        AppdefEntityID aeid = new AppdefEntityID(alertdef.getAppdefType(),
                                                 alertdef.getAppdefId());
        String resName = filter.getAppdefEntityName(aeid);
        resName = this.hackDBString(resName);

        log.error("DB_1 " + this.convertToDBPriority(alertdef.getPriority()) +
                  ' ' + this.getMeta() + '/' + this.getProduct() +'/' +
                  this.getVersion() + ' ' + resName + " :" +
                  alertdef.getName() + " - " +
                  this.createConditions(alertdef.getConditions(), logs,""));
        
        return "success";
    }
    
    /*
     * Hack for Deutsche Bank
     * 
     * Where "Weblogic 8.1 LON-PROD-GRTkwg ManagedServer1" is the resource that
     * generated the alert. We've found that you're not allowed to have Spaces,
     * or capital Q's (for some reason). To hack this in a really horrible ugly
     * way, could you substitute '_' for space and 'q' for 'Q' in this field?
     * Alternatively, if you'd rather, we can substitute this field with the
     * Resource ID, but I think the former is more user friendly.
     */
    private String hackDBString(String resName) {
        resName = StringUtil.replace(resName, " ", "_");
        resName = StringUtil.replace(resName, "Q", "q");
        return resName;
    }

    public void setParentActionConfig(AppdefEntityID aeid,
                                      ConfigResponse config)
        throws InvalidActionDataException {
        this.init(config);
    }

}
