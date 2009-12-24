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
 * Created on May 16, 2003
 *
 */
package org.hyperic.hq.ui.action.admin.config;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Properties;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.struts.action.ActionForm;
import org.apache.struts.action.ActionForward;
import org.apache.struts.action.ActionMapping;
import org.hyperic.hq.appdef.shared.PlatformTypeValue;
import org.hyperic.hq.appdef.shared.ServerTypeValue;
import org.hyperic.hq.appdef.shared.ServiceTypeValue;
import org.hyperic.hq.authz.shared.AuthzSubjectValue;
import org.hyperic.hq.authz.shared.PermissionException;
import org.hyperic.hq.bizapp.shared.AppdefBoss;
import org.hyperic.hq.bizapp.shared.AuthzBoss;
import org.hyperic.hq.bizapp.shared.ConfigBoss;
import org.hyperic.hq.common.shared.HQConstants;
import org.hyperic.hq.ui.Constants;
import org.hyperic.hq.ui.Portal;
import org.hyperic.hq.ui.action.BaseDispatchAction;
import org.hyperic.hq.ui.util.BizappUtils;
import org.hyperic.hq.ui.util.RequestUtils;
import org.hyperic.util.pager.PageControl;
import org.hyperic.util.pager.PageList;
import org.springframework.beans.factory.annotation.Autowired;

public class ConfigAction
    extends BaseDispatchAction {

    private AuthzBoss authzBoss;
    private ConfigBoss configBoss;
    private AppdefBoss appdefBoss;

    @Autowired
    public ConfigAction(AuthzBoss authzBoss, ConfigBoss configBoss, AppdefBoss appdefBoss) {
        super();
        this.authzBoss = authzBoss;
        this.configBoss = configBoss;
        this.appdefBoss = appdefBoss;
    }

    protected Properties getKeyMethodMap() {
        Properties map = new Properties();
        map.setProperty(Constants.MODE_EDIT, "editConfig");
        return map;
    }

    private void createPortal(HttpServletRequest request, boolean dialog, String title, String tile) {
        // Create the portal
        Portal p = Portal.createPortal(title, tile);
        p.setDialog(dialog);
        request.setAttribute(Constants.PORTAL_KEY, p);
    }

    public ActionForward editConfig(ActionMapping mapping, ActionForm form, HttpServletRequest request,
                                    HttpServletResponse response) throws Exception {
        Integer sessionId = RequestUtils.getSessionId(request);

        if (!BizappUtils.canAdminHQ(sessionId, authzBoss)) {
            throw new PermissionException("User not authorized to configure " + "server settings");
        }

        createPortal(request, true, "admin.settings.EditServerConfig.Title", ".admin.config.EditConfig");

        return null;
    }

    public ActionForward escalate(ActionMapping mapping, ActionForm form, HttpServletRequest request,
                                  HttpServletResponse resp) throws Exception {
        createPortal(request, false, "admin.home.EscalationSchemes", ".admin.config.EditEscalationConfig");

        Integer sessionId = RequestUtils.getSessionId(request);

        PageList<AuthzSubjectValue> availableUsers = authzBoss.getAllSubjects(sessionId, null, PageControl.PAGE_ALL);
        request.setAttribute(Constants.AVAIL_USERS_ATTR, availableUsers);

        Properties props = configBoss.getConfig();

        // See if the property exists
        if (props.containsKey(HQConstants.SNMPVersion)) {
            String ver = props.getProperty(HQConstants.SNMPVersion);
            request.setAttribute("snmpEnabled", new Boolean(ver.length() > 0));
        }
        return null;
    }

    public ActionForward monitor(ActionMapping mapping, ActionForm form, HttpServletRequest request,
                                 HttpServletResponse resp) throws Exception {
        Integer sessionId = RequestUtils.getSessionId(request);

        if (!BizappUtils.canAdminHQ(sessionId, authzBoss))
            throw new PermissionException("User not authorized to configure " + "monitor defaults");

        int session = sessionId.intValue();
        List<PlatformTypeValue> platTypes = appdefBoss.findAllPlatformTypes(session, PageControl.PAGE_ALL);
        request.setAttribute(Constants.ALL_PLATFORM_TYPES_ATTR, platTypes);

        List<ServerTypeValue> serverTypes = appdefBoss.findAllServerTypes(session, PageControl.PAGE_ALL);

        // Get the special service types sans windows special case
        // XXX: What special case?
        List<ServiceTypeValue> platServices = new ArrayList<ServiceTypeValue>();
        List<ServiceTypeValue> winServices = new ArrayList<ServiceTypeValue>();
        LinkedHashMap<ServerTypeValue, List<ServiceTypeValue>> serverTypesMap = new LinkedHashMap<ServerTypeValue, List<ServiceTypeValue>>();
        for (int i = 0; i < serverTypes.size(); i++) {
            ServerTypeValue stv = serverTypes.get(i);
            List<ServiceTypeValue> serviceTypes = appdefBoss.findServiceTypesByServerType(session, stv.getId()
                .intValue());
            if (stv.getVirtual()) {
                if (stv.getName().startsWith("Win")) {
                    winServices.addAll(serviceTypes);
                } else {
                    platServices.addAll(serviceTypes);
                }
            } else {
                serverTypesMap.put(stv, serviceTypes);
            }
        }
        request.setAttribute(Constants.ALL_SERVER_TYPES_ATTR, serverTypesMap);
        request.setAttribute(Constants.ALL_PLATFORM_SERVICE_TYPES_ATTR, platServices);
        request.setAttribute(Constants.ALL_WINDOWS_SERVICE_TYPES_ATTR, winServices);

        // Create the portal
        createPortal(request, false, "admin.home.ResourceTemplates", ".admin.config.EditMonitorConfig");

        return null;
    }
}
