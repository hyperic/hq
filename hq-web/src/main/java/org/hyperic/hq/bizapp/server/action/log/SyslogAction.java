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

import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hyperic.hq.appdef.shared.AppdefEntityID;
import org.hyperic.hq.appdef.shared.AppdefUtil;
import org.hyperic.hq.authz.shared.ResourceManager;
import org.hyperic.hq.bizapp.shared.action.SyslogActionConfig;
import org.hyperic.hq.context.Bootstrap;
import org.hyperic.hq.escalation.server.session.Escalatable;
import org.hyperic.hq.escalation.server.session.EscalationStateChange;
import org.hyperic.hq.events.ActionExecuteException;
import org.hyperic.hq.events.ActionExecutionInfo;
import org.hyperic.hq.events.ActionInterface;
import org.hyperic.hq.events.AlertDefinitionInterface;
import org.hyperic.hq.events.AlertInterface;
import org.hyperic.hq.events.EventConstants;
import org.hyperic.hq.events.InvalidActionDataException;
import org.hyperic.hq.events.Notify;
import org.hyperic.util.StringUtil;
import org.hyperic.util.config.ConfigResponse;

public class SyslogAction extends SyslogActionConfig
    implements ActionInterface, Notify
{
    private Log _log = LogFactory.getLog(SyslogAction.class);
    
    public SyslogAction() {
    }

    protected int convertToDBPriority(int priority) {
        // Deutsche Bank priority numbers
        switch (priority) {
            case EventConstants.PRIORITY_HIGH :
                return 5;
            case EventConstants.PRIORITY_MEDIUM :
                return 4;
            default :
            case EventConstants.PRIORITY_LOW :
                return 2;
        }
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
    protected String hackDBString(String resName) {
        resName = StringUtil.replace(resName, " ", "_");
        resName = StringUtil.replace(resName, "Q", "q");
        return resName;
    }

    public String execute(AlertInterface alert, ActionExecutionInfo info) 
        throws ActionExecuteException 
    {
        AlertDefinitionInterface alertDef =
            alert.getAlertDefinitionInterface();
        AppdefEntityID aeid = AppdefUtil.newAppdefEntityId(alertDef.getResource());
        String resName =Bootstrap.getBean(ResourceManager.class).getAppdefEntityName(aeid);
        resName = hackDBString(resName);

        _log.info("DB_1 " + convertToDBPriority(alertDef.getPriority()) +
                  ' ' + getMeta() + '/' + getProduct() +'/' +
                  getVersion() + ' ' + resName + " :" +
                  alertDef.getName() + " - " + info.getLongReason());

        return "Syslog: " + alertDef.getName() + " logged to " +
               getMeta() + '/' + getProduct() +'/' + getVersion();
    }
    
    public void setParentActionConfig(AppdefEntityID aeid, ConfigResponse cfg)
        throws InvalidActionDataException 
    {
        init(cfg);
    }

    
    public void send(Escalatable e, EscalationStateChange change, 
                           String message, Set notified) 
    {
        _log.info(message);
    }
}
