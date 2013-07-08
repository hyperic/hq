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

package org.hyperic.hq.ui.action.resource.platform.autodiscovery;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.apache.struts.action.ActionForm;
import org.apache.struts.action.ActionForward;
import org.apache.struts.action.ActionMapping;
import org.apache.struts.tiles.ComponentContext;
import org.hyperic.hq.agent.AgentConnectionException;
import org.hyperic.hq.appdef.shared.AgentNotFoundException;
import org.hyperic.hq.appdef.shared.AppdefEntityID;
import org.hyperic.hq.appdef.shared.PlatformValue;
import org.hyperic.hq.autoinventory.ScanMethod;
import org.hyperic.hq.autoinventory.scanimpl.FileScan;
import org.hyperic.hq.bizapp.shared.AIBoss;
import org.hyperic.hq.bizapp.shared.AppdefBoss;
import org.hyperic.hq.ui.Constants;
import org.hyperic.hq.ui.action.WorkflowPrepareAction;
import org.hyperic.hq.ui.util.BizappUtils;
import org.hyperic.hq.ui.util.RequestUtils;
import org.hyperic.hq.product.HypericOperatingSystem;
import org.hyperic.util.config.ConfigResponse;
import org.hyperic.util.config.ConfigSchema;
import org.springframework.beans.factory.annotation.Autowired;

public class NewAutoDiscoveryPrepAction
    extends WorkflowPrepareAction {
    
    protected AppdefBoss appdefBoss;
    private AIBoss aiBoss;
    
    @Autowired
    public NewAutoDiscoveryPrepAction(AppdefBoss appdefBoss, AIBoss aiBoss) {
        this.appdefBoss = appdefBoss;
        this.aiBoss = aiBoss;
    }

    /**
     * Create the platform with the attributes specified in the given
     * <code>AutoDiscoveryForm</code>.
     */
    public ActionForward workflow(ComponentContext context, ActionMapping mapping, ActionForm form,
                                  HttpServletRequest request, HttpServletResponse response) throws Exception {

        PlatformAutoDiscoveryForm newForm = (PlatformAutoDiscoveryForm) form;

        try {
            ServletContext ctx = getServlet().getServletContext();

            AppdefEntityID aeid = RequestUtils.getEntityId(request);
            newForm.setRid(aeid.getId());
            newForm.setType(new Integer(aeid.getType()));

            PlatformValue pValue = (PlatformValue) RequestUtils.getResource(request);
            String platType = pValue.getPlatformType().getName();
            newForm.setServerTypes(BizappUtils.buildSupportedAIServerTypes(ctx, request, platType, appdefBoss, aiBoss));

            loadScanConfig(newForm, request, platType);
            request.setAttribute("platformSpecificScanMsg", getPSScanMessage(false, platType));
        } catch (AgentConnectionException e) {
            RequestUtils.setError(request, "resource.platform.inventory.configProps.NoAgentConnection");
            return null;
        } catch (AgentNotFoundException e) {
            RequestUtils.setError(request, "resource.platform.inventory.configProps.NoAgentConnection");
            return null;
        }
        return null;
    }

    protected static final FileScan FILESCAN_WIN32 = new FileScan(true);
    protected static final FileScan FILESCAN_UNIX = new FileScan(false);
    protected static final ConfigSchema FILESCAN_CFG_WIN32;
    protected static final ConfigSchema FILESCAN_CFG_UNIX;
    static {
        try {
            FILESCAN_CFG_WIN32 = FILESCAN_WIN32.getConfigSchema();
            FILESCAN_CFG_UNIX = FILESCAN_UNIX.getConfigSchema();
        } catch (Exception e) {
            throw new IllegalStateException("Error initializing FILESCAN_CFG: " + e);
        }
    }
    protected static final String FILESCAN_NAME = FILESCAN_UNIX.getName();

    private static boolean isWindows(String osName) {
        return HypericOperatingSystem.isWin32(osName);
    }

    protected static ScanMethod getScanMethod(String osName) {
        if (isWindows(osName)) {
            return FILESCAN_WIN32;
        } else {
            return FILESCAN_UNIX;
        }
    }

    protected static ConfigSchema getConfigSchema(String osName) {
        if (isWindows(osName)) {
            return FILESCAN_CFG_WIN32;
        } else {
            return FILESCAN_CFG_UNIX;
        }
    }

    protected static ConfigResponse getConfigResponse(String osName) throws Exception {
        ConfigSchema schema = getConfigSchema(osName);
        return new ConfigResponse(schema);
    }

    private void loadScanConfig(PlatformAutoDiscoveryForm aForm, HttpServletRequest request, String osName)
        throws Exception {

        ConfigSchema schema = getConfigSchema(osName);
        ConfigResponse resp = getConfigResponse(osName);

        aForm.setScanMethod(FILESCAN_NAME);

        HttpSession session = request.getSession();
        session.setAttribute(Constants.CURR_CONFIG_SCHEMA, schema);
        session.setAttribute(Constants.OLD_CONFIG_RESPONSE, resp);

        aForm.setScanMethod(FILESCAN_NAME);
        aForm.buildConfigOptions(schema, resp);
    }

    private static final String WINDOWS_MSG = "resource.autodiscovery.AutoDiscoveryHeader.windows";
    private static final String UNIX_MSG = "resource.autodiscovery.AutoDiscoveryHeader.unix";
    private static final String WINDOWS_AIONLY_MSG = "resource.autodiscovery.AutoDiscoveryHeader.windows.AIonly";
    private static final String UNIX_AIONLY_MSG = "resource.autodiscovery.AutoDiscoveryHeader.unix.AIonly";

    /** Generate a platform specific scan message. */
    private String getPSScanMessage(boolean isAIonly, String ptype) {
        if (isWindows(ptype)) {
            return isAIonly ? WINDOWS_AIONLY_MSG : WINDOWS_MSG;
        }
        return isAIonly ? UNIX_AIONLY_MSG : UNIX_MSG;
    }
}
