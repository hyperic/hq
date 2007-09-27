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

package org.hyperic.hq.ui.action.resource.service.inventory;

import java.util.ArrayList;
import java.util.List;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.struts.action.ActionForm;
import org.apache.struts.action.ActionForward;
import org.apache.struts.action.ActionMapping;
import org.apache.struts.tiles.ComponentContext;
import org.apache.struts.tiles.actions.TilesAction;
import org.hyperic.hq.appdef.shared.AppdefEntityID;
import org.hyperic.hq.appdef.shared.ConfigFetchException;
import org.hyperic.hq.appdef.shared.PlatformValue;
import org.hyperic.hq.appdef.shared.ServiceValue;
import org.hyperic.hq.bizapp.shared.AppdefBoss;
import org.hyperic.hq.bizapp.shared.ProductBoss;
import org.hyperic.hq.common.ApplicationException;
import org.hyperic.hq.product.PluginNotFoundException;
import org.hyperic.hq.product.ProductPlugin;
import org.hyperic.hq.ui.Constants;
import org.hyperic.hq.ui.action.resource.common.inventory.RemoveResourceGroupsForm;
import org.hyperic.hq.ui.util.ActionUtils;
import org.hyperic.hq.ui.util.ContextUtils;
import org.hyperic.hq.ui.util.RequestUtils;
import org.hyperic.util.config.ConfigResponse;
import org.hyperic.util.config.ConfigSchema;
import org.hyperic.util.pager.PageControl;

/**
 * Action loads a service for viewing, including the General Properties,
 * Type and Host Properties and Configuration Properties.The 
 * Configuration Text specified in the general Properties are not loaded here.
 */
public class ViewServiceAction extends TilesAction {
    /**
     * Retrieve this data and store it in the
     * <code>ServerForm</code>:
     *
     */
    public ActionForward execute(ComponentContext context,
                                 ActionMapping mapping,
                                 ActionForm form,
                                 HttpServletRequest request,
                                 HttpServletResponse response)
    throws Exception{
        Log log =
            LogFactory.getLog(ViewServiceAction.class.getName());

        try {
            ServiceValue service =
                (ServiceValue) RequestUtils.getResource(request);
            
            ServletContext ctx = getServlet().getServletContext();
            AppdefBoss boss = ContextUtils.getAppdefBoss(ctx);
            Integer sessionId = RequestUtils.getSessionId(request);
            AppdefEntityID entityId = service.getEntityId();

            PageControl pcg =
                RequestUtils.getPageControl(request, "psg", "png", "sog", "scg");

            List groups = 
                boss.findAllGroupsMemberInclusive(sessionId.intValue(), pcg,
                                                  service.getEntityId());
            // check to see if this thing is a platform service
            if(service.getServer().getServerType().getVirtual()) {
                // find the platform resource and add it to the request scope
                // TODO handle permission error here
                PlatformValue pv =
                    boss.findPlatformByDependentID(sessionId.intValue(), entityId);
                request.setAttribute(Constants.PARENT_RESOURCE_ATTR, pv);
            }
            request.setAttribute(Constants.ALL_RESGRPS_ATTR, groups);
            if (service == null) {
                RequestUtils.setError(request,
                                      "resource.service.error.ServiceNotFound");
                return null;
            }

            // create and initialize the remove resource groups form
            RemoveResourceGroupsForm rmGroupsForm =
                new RemoveResourceGroupsForm();
            rmGroupsForm.setRid(service.getId());
            rmGroupsForm.setType(new Integer(service.getEntityId().getType()));

            int psg = RequestUtils.getPageSize(request, "psg");
            rmGroupsForm.setPsg(new Integer(psg));

            request.setAttribute(Constants.RESOURCE_REMOVE_GROUPS_MEMBERS_FORM_ATTR,
                                 rmGroupsForm);
            
            ProductBoss pboss = ContextUtils.getProductBoss(ctx);
            
            log.debug("AppdefEntityID = " + entityId.toString()); 
            
            ConfigSchema config  = new ConfigSchema();
            ConfigResponse oldResponse = new ConfigResponse();
            boolean editConfig = false;
            
            try {
                oldResponse =
                    pboss.getMergedConfigResponse(sessionId.intValue(),
                            ProductPlugin.TYPE_PRODUCT, entityId, false);
                config = pboss.getConfigSchema(sessionId.intValue(), entityId,
                        ProductPlugin.TYPE_PRODUCT, oldResponse);

                editConfig = true;
            } catch (ConfigFetchException e) {
                log.warn("Managed Exception ConfigFetchException caught "
                        + e.toString());
            } catch (PluginNotFoundException e) {
                log.warn("Managed Exception PluginNotFoundException caught "
                        + e.toString());
            }

            List uiProductOptions =
                ActionUtils.getConfigValues(config, oldResponse);
            
            request.setAttribute(Constants.PRODUCT_CONFIG_OPTIONS,
                                 uiProductOptions);
            request.setAttribute(Constants.PRODUCT_CONFIG_OPTIONS_COUNT,
                                 new Integer(uiProductOptions.size()));

            config = new ConfigSchema();
            oldResponse = new ConfigResponse();

            try {
                oldResponse =
                    pboss.getMergedConfigResponse(sessionId.intValue(),
                            ProductPlugin.TYPE_MEASUREMENT, entityId, false);
                config = pboss.getConfigSchema(sessionId.intValue(), entityId,
                        ProductPlugin.TYPE_MEASUREMENT, oldResponse);
            } catch (ConfigFetchException e) {
                // do nothing
            } catch (PluginNotFoundException e) {
                //do nothing
            }

            setUIOptions(service, request, config, oldResponse);
            
            config = new ConfigSchema();

            oldResponse = pboss.getMergedConfigResponse(sessionId.intValue(),
                    ProductPlugin.TYPE_CONTROL, entityId, false);
            try {
                config = pboss.getConfigSchema(sessionId.intValue(), entityId,
                        ProductPlugin.TYPE_CONTROL, oldResponse);
            } catch (PluginNotFoundException e) {
                // do nothing
            }

            List uiControlOptions =
                ActionUtils.getConfigValues(config, oldResponse);

            request.setAttribute(Constants.CONTROL_CONFIG_OPTIONS,
                                 uiControlOptions);
            request.setAttribute(Constants.CONTROL_CONFIG_OPTIONS_COUNT,
                                 new Integer(uiControlOptions.size()));

            request.setAttribute(Constants.AUTO_INVENTORY,
                                 new Boolean(service.getServer()
                                             .getRuntimeAutodiscovery()));
            
            if(!editConfig)
                RequestUtils.setError(request, "resource.common.inventory.error.serverConfigNotSet","configServer");
            request.setAttribute(Constants.EDIT_CONFIG, new Boolean(editConfig));
            
            return null;

        } catch(ApplicationException e) {
            log.error("unable to retrieve configuration properties ", e);
            RequestUtils
                    .setError(request,
                              "resource.common.inventory.error.configRetrieveError");
            request.setAttribute(Constants.PRODUCT_CONFIG_OPTIONS,
                                 new ArrayList());
            request.setAttribute(Constants.PRODUCT_CONFIG_OPTIONS_COUNT,
                                 new Integer(0));
            request.setAttribute(Constants.MONITOR_CONFIG_OPTIONS,
                                 new ArrayList());
            request.setAttribute(Constants.MONITOR_CONFIG_OPTIONS_COUNT,
                                 new Integer(0));
            return null;
        }
    }

    protected void setUIOptions(ServiceValue service,
                                HttpServletRequest request,
                                ConfigSchema config,
                                ConfigResponse oldResponse) {
        List uiMonitorOptions =
            ActionUtils.getConfigValues(config, oldResponse);
        
        request.setAttribute(Constants.MONITOR_CONFIG_OPTIONS, uiMonitorOptions);
        request.setAttribute(Constants.MONITOR_CONFIG_OPTIONS_COUNT,
                             new Integer(uiMonitorOptions.size()));
    }
}
