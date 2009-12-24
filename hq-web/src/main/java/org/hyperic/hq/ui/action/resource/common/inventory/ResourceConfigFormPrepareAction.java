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

package org.hyperic.hq.ui.action.resource.common.inventory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.struts.action.ActionForm;
import org.apache.struts.action.ActionForward;
import org.apache.struts.action.ActionMapping;
import org.apache.struts.tiles.ComponentContext;
import org.hyperic.hq.appdef.shared.AppdefEntityConstants;
import org.hyperic.hq.appdef.shared.AppdefEntityID;
import org.hyperic.hq.appdef.shared.AppdefResourceValue;
import org.hyperic.hq.appdef.shared.ConfigFetchException;
import org.hyperic.hq.appdef.shared.ServerValue;
import org.hyperic.hq.bizapp.shared.AppdefBoss;
import org.hyperic.hq.bizapp.shared.ProductBoss;
import org.hyperic.hq.product.PluginNotFoundException;
import org.hyperic.hq.product.ProductPlugin;
import org.hyperic.hq.ui.Constants;
import org.hyperic.hq.ui.action.BaseTilesAction;
import org.hyperic.hq.ui.beans.ConfigValues;
import org.hyperic.hq.ui.util.BizappUtils;
import org.hyperic.hq.ui.util.RequestUtils;
import org.hyperic.util.StringUtil;
import org.hyperic.util.config.ConfigResponse;
import org.hyperic.util.config.ConfigSchema;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * A subclass of <code> TilesAction </code> that populates the
 * <code>ResourceConfigForm</code> form with configOptions from the Product
 * plugin.
 */
public class ResourceConfigFormPrepareAction
    extends BaseTilesAction {
    private final Log log = LogFactory.getLog(ResourceConfigFormPrepareAction.class.getName());

    protected AppdefBoss appdefBoss;
    protected ProductBoss productBoss;

    @Autowired
    public ResourceConfigFormPrepareAction(AppdefBoss appdefBoss, ProductBoss productBoss) {
        super();
        this.appdefBoss = appdefBoss;
        this.productBoss = productBoss;
    }

    // if this resource has help text, build up a map of all
    // configuration values which can then be applied to variables
    // in the help text.
    protected void addHelpProperties(Map helpProps, ConfigSchema schema, ConfigResponse config) {

        helpProps.putAll(schema.getDefaultProperties());
        helpProps.putAll(config.toProperties());
    }

    protected void addExtraHelpProperties(Map<String, String> helpProps) {
        String installpath = (String) helpProps.get(ProductPlugin.PROP_INSTALLPATH);

        if (installpath != null) {
            // escaped version of installpath suitable for insertion
            // into agent.properties
            helpProps.put(ProductPlugin.PROP_INSTALLPATH + ".escaped", StringUtil.replace(installpath, "\\", "\\\\"));
        }

        try {
            // Version information
            helpProps.put(Constants.APP_VERSION, productBoss.getVersion());
        } catch (Exception e) {
        }
    }

    /**
     * Retrieve this data and store it in the <code>ResourceConfigForm</code>:
     * 
     */
    public ActionForward execute(ComponentContext context, ActionMapping mapping, ActionForm form,
                                 HttpServletRequest request, HttpServletResponse response) throws Exception {

        checkModifyPermission(request);

        // HACK for the ProblemResources portlet to tell us
        // to redirect back to dash when done
        if (request.getParameter("todash") != null) {
            request.setAttribute("todash", Boolean.TRUE);
        }

        ResourceConfigForm resourceForm = (ResourceConfigForm) form;

        int sessionId = RequestUtils.getSessionId(request).intValue();

        AppdefEntityID aeid = RequestUtils.getEntityId(request);

        AppdefResourceValue resource = appdefBoss.findById(sessionId, aeid);

        resourceForm.loadResourceValue(resource);

        RequestUtils.setResource(request, resource);

        ConfigResponse oldResponse = new ConfigResponse();
        ConfigSchema config = new ConfigSchema();
        String help = null;
        Map<String, String> helpProps = new HashMap<String, String>();

        try {
            oldResponse = productBoss.getMergedConfigResponse(sessionId, ProductPlugin.TYPE_PRODUCT, aeid, false);

            config = productBoss.getConfigSchema(sessionId, aeid, ProductPlugin.TYPE_PRODUCT, oldResponse);

            addHelpProperties(helpProps, config, oldResponse);
        } catch (ConfigFetchException e) {
            log.error("cannot retrieve product config", e);
            RequestUtils.setError(request, "resource.common.inventory.error.productConfigNotSet");
            resourceForm.setResourceConfigOptions(new ArrayList());
            request.setAttribute(Constants.PRODUCT_CONFIG_OPTIONS_COUNT, new Integer(0));
            resourceForm.setControlConfigOptions(new ArrayList());
            request.setAttribute(Constants.CONTROL_CONFIG_OPTIONS_COUNT, new Integer(0));
            resourceForm.setMonitorConfigOptions(new ArrayList());
            request.setAttribute(Constants.MONITOR_CONFIG_OPTIONS_COUNT, new Integer(0));
            return null;

        } catch (PluginNotFoundException e) {
            log.error("Plugin not found for the resource ", e);
            RequestUtils.setError(request, "resource.common.inventory.error.PluginNotFound");
            resourceForm.setResourceConfigOptions(new ArrayList());
            request.setAttribute(Constants.PRODUCT_CONFIG_OPTIONS_COUNT, new Integer(0));
            resourceForm.setControlConfigOptions(new ArrayList());
            request.setAttribute(Constants.CONTROL_CONFIG_OPTIONS_COUNT, new Integer(0));
            resourceForm.setMonitorConfigOptions(new ArrayList());
            request.setAttribute(Constants.MONITOR_CONFIG_OPTIONS_COUNT, new Integer(0));
            return null;
        }

        // XXX add the options through the builder and get them
        String prefix = ProductPlugin.TYPE_PRODUCT + ".";
        List<ConfigValues> uiResourceOptions = BizappUtils.buildLoadConfigOptions(prefix, config, oldResponse);

        resourceForm.setResourceConfigOptions(uiResourceOptions);
        request.setAttribute(Constants.PRODUCT_CONFIG_OPTIONS_COUNT, new Integer(uiResourceOptions.size()));

        prefix = ProductPlugin.TYPE_MEASUREMENT + ".";

        config = new ConfigSchema();
        oldResponse = new ConfigResponse();

        try {
            oldResponse = productBoss.getMergedConfigResponse(sessionId, ProductPlugin.TYPE_MEASUREMENT, aeid, false);
            config = productBoss.getConfigSchema(sessionId, aeid, ProductPlugin.TYPE_MEASUREMENT, oldResponse);

            addHelpProperties(helpProps, config, oldResponse);

            if (aeid.getType() == AppdefEntityConstants.APPDEF_TYPE_SERVER) {
                ServerValue server = (ServerValue) resource;
                if (server.getWasAutodiscovered()) {
                    request.setAttribute(Constants.SERVER_BASED_AUTO_INVENTORY, new Integer(0));
                } else {
                    request.setAttribute(Constants.SERVER_BASED_AUTO_INVENTORY, new Integer(1));
                    resourceForm.setServerBasedAutoInventory(server.getRuntimeAutodiscovery());
                }
                BizappUtils.setRuntimeAIMessage(sessionId, request, server, appdefBoss);
            }

        } catch (PluginNotFoundException e) {
            // do nothing
        }

        List<ConfigValues> uiMonitorOptions = BizappUtils.buildLoadConfigOptions(prefix, config, oldResponse);

        resourceForm.setMonitorConfigOptions(uiMonitorOptions);
        request.setAttribute(Constants.MONITOR_CONFIG_OPTIONS_COUNT, new Integer(uiMonitorOptions.size()));

        if (aeid.getType() != AppdefEntityConstants.APPDEF_TYPE_PLATFORM) {
            setUIOptions(aeid, request, resourceForm, uiMonitorOptions, helpProps);
        }

        addExtraHelpProperties(helpProps);

        try {
            help = productBoss.getMonitoringHelp(sessionId, aeid, helpProps);
        } catch (Exception e) {
            log.error("Error getting help: " + e.getMessage(), e);
        }

        request.setAttribute(Constants.MONITOR_HELP, help);

        prefix = ProductPlugin.TYPE_CONTROL + ".";
        config = new ConfigSchema();
        oldResponse = new ConfigResponse();

        try {
            oldResponse = productBoss.getMergedConfigResponse(sessionId, ProductPlugin.TYPE_CONTROL, aeid, false);
            config = productBoss.getConfigSchema(sessionId, aeid, ProductPlugin.TYPE_CONTROL, oldResponse);

            addHelpProperties(helpProps, config, oldResponse);
        } catch (PluginNotFoundException e) {
            // do nothing
        }

        List<ConfigValues> uiControlOptions = BizappUtils.buildLoadConfigOptions(prefix, config, oldResponse);

        resourceForm.setControlConfigOptions(uiControlOptions);
        request.setAttribute(Constants.CONTROL_CONFIG_OPTIONS_COUNT, new Integer(uiControlOptions.size()));

        return null;

    }

    protected void setUIOptions(AppdefEntityID aeid, HttpServletRequest request, ResourceConfigForm resourceForm,
                                List<ConfigValues> uiMonitorOptions, Map<String, String> helpProps) throws Exception {
    }
}
