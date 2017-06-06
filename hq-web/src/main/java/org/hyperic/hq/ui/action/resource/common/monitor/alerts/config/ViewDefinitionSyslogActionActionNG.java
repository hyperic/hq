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

package org.hyperic.hq.ui.action.resource.common.monitor.alerts.config;

import java.rmi.RemoteException;

import javax.servlet.ServletException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.tiles.AttributeContext;
import org.apache.tiles.context.TilesRequestContext;
import org.apache.tiles.preparer.ViewPreparer;
import org.hyperic.hq.auth.shared.SessionNotFoundException;
import org.hyperic.hq.auth.shared.SessionTimeoutException;
import org.hyperic.hq.authz.shared.PermissionException;
import org.hyperic.hq.bizapp.shared.ConfigBoss;
import org.hyperic.hq.bizapp.shared.EventsBoss;
import org.hyperic.hq.common.SystemException;
import org.hyperic.hq.common.shared.HQConstants;
import org.hyperic.hq.events.shared.AlertDefinitionValue;
import org.hyperic.hq.ui.action.BaseActionNG;
import org.hyperic.hq.ui.action.resource.common.monitor.alerts.AlertDefUtil;
import org.hyperic.hq.ui.exception.ParameterNotFoundException;
import org.hyperic.hq.ui.util.RequestUtils;
import org.hyperic.util.ConfigPropertyException;
import org.hyperic.util.config.EncodingException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

/**
 * View an alert definition -- syslog action.
 */
@Component("viewDefinitionSyslogActionActionNG")
@Scope(value = "prototype")
public class ViewDefinitionSyslogActionActionNG extends BaseActionNG implements ViewPreparer{

    private final Log log = LogFactory.getLog(ViewDefinitionSyslogActionActionNG.class.getName());
	@Autowired
    private ConfigBoss configBoss;
    @Autowired
    private EventsBoss eventsBoss;

    public void execute(TilesRequestContext tilesContext, AttributeContext attributeContext) {
    	
    	this.request = getServletRequest();
    	int sessionID;
		try {
			sessionID = RequestUtils.getSessionId(request).intValue();

	        String enabledStr = configBoss.getConfig().getProperty(HQConstants.SyslogActionsEnabled);
	        Boolean syslogActionsEnabled = Boolean.valueOf(enabledStr);
	        request.setAttribute(HQConstants.SyslogActionsEnabled, syslogActionsEnabled);
	
	        if (syslogActionsEnabled.booleanValue()) {
	            AlertDefinitionValue adv = AlertDefUtil.getAlertDefinition(request, sessionID, eventsBoss);
	            SyslogActionFormNG saForm = new SyslogActionFormNG();
	            AlertDefUtil.prepareSyslogActionForm(adv, saForm);
	            request.setAttribute("syslogActionForm", saForm);
	        }
		} catch (ServletException e) {
			log.error(e);
		} catch (ConfigPropertyException e) {
			log.error(e);
		} catch (SessionNotFoundException e) {
			log.error(e);
		} catch (SessionTimeoutException e) {
			log.error(e);
		} catch (PermissionException e) {
			log.error(e);
		} catch (SystemException e) {
			log.error(e);
		} catch (RemoteException e) {
			log.error(e);
		} catch (ParameterNotFoundException e) {
			log.error(e);
		} catch (EncodingException e) {
			log.error(e);
		}
	}
}
