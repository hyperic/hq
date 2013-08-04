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

package org.hyperic.hq.ui.action.resource.platform.inventory;

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
import org.apache.struts.tiles.actions.TilesAction;
import org.hyperic.hq.appdef.Agent;
import org.hyperic.hq.appdef.shared.AppdefEntityID;
import org.hyperic.hq.appdef.shared.AppdefGroupValue;
import org.hyperic.hq.appdef.shared.AppdefResourceValue;
import org.hyperic.hq.appdef.shared.ConfigFetchException;
import org.hyperic.hq.appdef.shared.PlatformValue;
import org.hyperic.hq.appdef.shared.ServerTypeValue;
import org.hyperic.hq.appdef.shared.ServerValue;
import org.hyperic.hq.appdef.shared.ServiceTypeValue;
import org.hyperic.hq.appdef.shared.ServiceValue;
import org.hyperic.hq.authz.shared.PermissionException;
import org.hyperic.hq.bizapp.shared.AppdefBoss;
import org.hyperic.hq.bizapp.shared.ProductBoss;
import org.hyperic.hq.product.PluginNotFoundException;
import org.hyperic.hq.product.ProductPlugin;
import org.hyperic.hq.ui.Constants;
import org.hyperic.hq.ui.action.resource.RemoveResourceForm;
import org.hyperic.hq.ui.action.resource.common.inventory.RemoveResourceGroupsForm;
import org.hyperic.hq.ui.beans.ConfigValues;
import org.hyperic.hq.ui.exception.ParameterNotFoundException;
import org.hyperic.hq.ui.util.ActionUtils;
import org.hyperic.hq.ui.util.RequestUtils;
import org.hyperic.util.config.ConfigResponse;
import org.hyperic.util.config.ConfigSchema;
import org.hyperic.util.pager.PageControl;
import org.hyperic.util.pager.PageList;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * An Action that retrieves data from the previously fetched platform to
 * facilitate display of that platform.
 */
public class ViewPlatformAction
    extends TilesAction {

    private final Log log = LogFactory.getLog(ViewPlatformAction.class.getName());
    private AppdefBoss appdefBoss;
    private ProductBoss productBoss;

    @Autowired
    public ViewPlatformAction(AppdefBoss appdefBoss, ProductBoss productBoss) {
        super();
        this.appdefBoss = appdefBoss;
        this.productBoss = productBoss;
    }

    /**
     * Retrieve this data and store it in request attributes:
     * 
     * <ul>
     * <li><code>Collection</code> of servers for the platform as
     * <code>Constants.CHILD_RESOURCES_ATTR</code>
     * <li>number of servers for the platform as
     * <code>Constants.NUM_CHILD_RESOURCES_ATTR</code>
     * <li>map of server counts by server type for the platform as
     * <code>Constants.RESOURCE_TYPE_MAP_ATTR</code>
     * </ul>
     * 
     */
    public ActionForward execute(ComponentContext context, ActionMapping mapping, ActionForm form,
                                 HttpServletRequest request, HttpServletResponse response) throws Exception {

        PlatformValue platform = (PlatformValue) RequestUtils.getResource(request);
        if (platform == null) {
            RequestUtils.setError(request, Constants.ERR_PLATFORM_NOT_FOUND);
            return null;
        }
        Integer platformId = platform.getId();
        AppdefEntityID entityId = platform.getEntityId();
        Integer appdefType = new Integer(entityId.getType());
        Agent agent = platform.getAgent();

        log.debug("Agent is = " + agent);

        try {

            Integer sessionId = RequestUtils.getSessionId(request);
            int sessionInt = sessionId.intValue();

            int resType = 0;
            try {
                resType = RequestUtils.getIntParameter(request, "fs").intValue();
            } catch (ParameterNotFoundException ignore) {
                // This is OK . do nothing
                log.debug("No resType passed in parameters");
            }

            Integer ctype = new Integer(-1);
            try {
                ctype = RequestUtils.getIntParameter(request, "resourceType");
            } catch (ParameterNotFoundException ignore) {
                // This is OK . do nothing
                log.debug("No ctype passed in parameters");
            }

            log.trace("getting servers for platform [" + platformId + "]");
            PageControl pcs = RequestUtils.getPageControl(request, "pss", "pns", "sos", "scs");
            PageList<ServerValue> servers;
            if (resType == 0) {
                servers = appdefBoss.findViewableServersByPlatform(sessionInt, platformId, pcs);
            } else {
                servers = appdefBoss.findServersByTypeAndPlatform(sessionInt, platformId, resType, pcs);
            }

            log.trace("getting all server types");
            List<ServerTypeValue> serverTypes = appdefBoss.findServerTypesByPlatform(sessionInt, platformId,
                PageControl.PAGE_ALL);

            request.setAttribute(Constants.CHILD_RESOURCES_ATTR, servers);

            log.trace("getting all platform services");
            PageList<ServiceTypeValue> serviceTypes = appdefBoss.findViewablePlatformServiceTypes(sessionInt,
                platformId);

            RemoveResourceForm rmServicesForm = new RemoveResourceForm();
            rmServicesForm.setResourceTypes(serviceTypes);
            rmServicesForm.setResourceType(ctype);

            request.setAttribute(Constants.RESOURCE_REMOVE_FORM_ATTR, rmServicesForm);

            PageControl pcsvc = RequestUtils.getPageControl(request, "ps", "pn", "so", "sc");

            PageList<ServiceValue> svcArr;
            if (ctype.intValue() != -1) {
                svcArr = appdefBoss.findPlatformServices(sessionInt, platformId, ctype, pcsvc);
            } else {
                svcArr = appdefBoss.findPlatformServices(sessionInt, platformId, pcsvc);
            }

            // Make a list with the entry set for Struts tags
            request.setAttribute(Constants.SERVICES_ATTR, svcArr);

            log.trace("getting groups for platform [" + platformId + "]");
            PageControl pcg = RequestUtils.getPageControl(request, "psg", "png", "sog", "scg");
            PageList<AppdefGroupValue> groups = appdefBoss.findAllGroupsMemberInclusive(sessionInt, pcg, platform
                .getEntityId());

            // XXX: stub
            log.debug("AllGroups is " + groups);
            request.setAttribute(Constants.ALL_RESGRPS_ATTR, groups);

            log.trace("getting server type map for platform [" + platformId + "]");
            Map<String, Integer> typeMap = AppdefResourceValue.getServerTypeCountMap(servers);
            request.setAttribute(Constants.RESOURCE_TYPE_MAP_ATTR, typeMap);

            // create and initialize the remove servers form
            RemoveServersForm rmServersForm = new RemoveServersForm();
            rmServersForm.setRid(platformId);
            rmServersForm.setType(appdefType);
            rmServersForm.setResourceTypes(serverTypes);

            int fs = RequestUtils.getPageSize(request, "fs");
            rmServersForm.setFs(new Integer(fs));

            int pss = RequestUtils.getPageSize(request, "pss");
            rmServersForm.setPss(new Integer(pss));

            request.setAttribute(Constants.RESOURCE_REMOVE_SERVERS_FORM_ATTR, rmServersForm);

            // create and initialize the remove resource groups form
            RemoveResourceGroupsForm rmGroupsForm = new RemoveResourceGroupsForm();
            rmGroupsForm.setRid(platformId);
            rmGroupsForm.setType(appdefType);

            int psg = RequestUtils.getPageSize(request, "psg");
            rmGroupsForm.setPsg(new Integer(psg));

            request.setAttribute(Constants.RESOURCE_REMOVE_GROUPS_MEMBERS_FORM_ATTR, rmGroupsForm);

            log.debug("AppdefEntityID = " + entityId.toString());
            ConfigResponse oldResponse = productBoss.getMergedConfigResponse(sessionInt, ProductPlugin.TYPE_PRODUCT,
                entityId, false);
            log.debug(oldResponse);

            ConfigSchema config = productBoss.getConfigSchema(sessionInt, entityId, ProductPlugin.TYPE_PRODUCT,
                oldResponse);
            log.debug("configSchema = " + config.getOptions().toString());

            List<ConfigValues> uiResourceOptions = ActionUtils.getConfigValues(config, oldResponse);

            config = new ConfigSchema();
            oldResponse = new ConfigResponse();

            try {
                oldResponse = productBoss.getMergedConfigResponse(sessionInt, ProductPlugin.TYPE_MEASUREMENT, entityId,
                    false);
                config = productBoss.getConfigSchema(sessionInt, entityId, ProductPlugin.TYPE_MEASUREMENT, oldResponse);
            } catch (ConfigFetchException e) {
                // do nothing as this could happen when the prodyct config is
                // not set
            } catch (PluginNotFoundException e) {
                // do nothing as this could happen when the prodyct config is
                // not set
            }

            List<ConfigValues> uiMonitorOptions = ActionUtils.getConfigValues(config, oldResponse);

            request.setAttribute(Constants.PRODUCT_CONFIG_OPTIONS, uiResourceOptions);
            request.setAttribute(Constants.PRODUCT_CONFIG_OPTIONS_COUNT, new Integer(uiResourceOptions.size()));
            request.setAttribute(Constants.MONITOR_CONFIG_OPTIONS, uiMonitorOptions);
            request.setAttribute(Constants.MONITOR_CONFIG_OPTIONS_COUNT, new Integer(uiMonitorOptions.size()));
            request.setAttribute(Constants.AGENT, agent);
        } catch (PermissionException pe) {
            RequestUtils.setError(request, "resource.platform.inventory.error.ViewServersPermission");
        }

        setConfigModifier(request, entityId);

        return null;

    }

    protected void setConfigModifier(HttpServletRequest request, AppdefEntityID entityId) {};
    
}
