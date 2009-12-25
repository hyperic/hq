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

package org.hyperic.hq.ui.action.resource.autogroup.monitor.visibility;

import java.util.Arrays;
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
import org.hyperic.hq.appdef.server.session.AppdefResourceType;
import org.hyperic.hq.appdef.shared.AppdefEntityConstants;
import org.hyperic.hq.appdef.shared.AppdefEntityID;
import org.hyperic.hq.appdef.shared.AppdefEntityTypeID;
import org.hyperic.hq.appdef.shared.AppdefResourceValue;
import org.hyperic.hq.bizapp.shared.AppdefBoss;
import org.hyperic.hq.bizapp.shared.MeasurementBoss;
import org.hyperic.hq.bizapp.shared.uibeans.ResourceDisplaySummary;
import org.hyperic.hq.ui.Constants;
import org.hyperic.hq.ui.action.resource.common.monitor.visibility.InventoryHelper;
import org.hyperic.hq.ui.action.resource.platform.monitor.visibility.RootInventoryHelper;
import org.hyperic.hq.ui.exception.ParameterNotFoundException;
import org.hyperic.hq.ui.util.RequestUtils;
import org.hyperic.util.timer.StopWatch;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * 
 * Fetch the children resources for the group
 */
public class ListChildrenAction
    extends TilesAction {

    private final Log log = LogFactory.getLog(ListChildrenAction.class.getName());

    private MeasurementBoss measurementBoss;
    
    private AppdefBoss appdefBoss;

    @Autowired
    public ListChildrenAction(MeasurementBoss measurementBoss, AppdefBoss appdefBoss) {
        super();
        this.measurementBoss = measurementBoss;
        this.appdefBoss = appdefBoss;
    }

    public ActionForward execute(ComponentContext context, ActionMapping mapping, ActionForm form,
                                 HttpServletRequest request, HttpServletResponse response) throws Exception {
        AppdefResourceValue resource = RequestUtils.getResource(request);

        if (resource == null) {
            RequestUtils.setError(request, Constants.ERR_RESOURCE_NOT_FOUND);
            return null;
        }

        Integer sessionId = RequestUtils.getSessionId(request);
        ServletContext ctx = getServlet().getServletContext();

        Boolean isInternal = new Boolean((String) context.getAttribute(Constants.CTX_INTERNAL));

        if (isInternal.booleanValue()) {
            // groups don't categorize members as "internal" or
            // "deployed", so we just return the full list of members
            // for deployed
            return null;
        }

        // There are two possibilities for an auto-group. Either it
        // is an auto-group of platforms, in which case there will be
        // no parent entity ids, or it is an auto-group of servers or
        // services.
        InventoryHelper helper = null;
        AppdefEntityID[] entityIds = null;
        AppdefEntityID typeHolder = null;
        try {
            entityIds = RequestUtils.getEntityIds(request);
            // if we get this far, we are dealing with an auto-group
            // of servers or services

            // find the resource type of the autogrouped resources
            typeHolder = entityIds[0];
            helper = InventoryHelper.getHelper(typeHolder);
        } catch (ParameterNotFoundException e) {
            // if we get here, we are dealing with an auto-group of
            // platforms
            helper = new RootInventoryHelper(appdefBoss);
        }

        AppdefEntityTypeID childTypeId;
        try {
            childTypeId = RequestUtils.getChildResourceTypeId(request);
        } catch (ParameterNotFoundException e1) {
            // must be an autogroup resource type
            // childTypeId = RequestUtils.getAutogroupResourceTypeId(request);
            // REMOVE ME?
            throw e1;
        }

        AppdefResourceType selectedType = helper.getChildResourceType(request, ctx, childTypeId);
        request.setAttribute(Constants.CHILD_RESOURCE_TYPE_ATTR, selectedType);

        // get the resource healths
        StopWatch watch = new StopWatch();
        List<ResourceDisplaySummary> healths = getAutoGroupResourceHealths(ctx, sessionId, entityIds, childTypeId);
        if (log.isDebugEnabled()) {
            log.debug("getAutoGroupResourceHealths: " + watch);
        }

        context.putAttribute(Constants.CTX_SUMMARIES, healths);

        return null;
    }

    private List<ResourceDisplaySummary> getAutoGroupResourceHealths(ServletContext ctx, Integer sessionId,
                                                                     AppdefEntityID[] entityIds,
                                                                     AppdefEntityTypeID childTypeId) throws Exception {

        if (null == entityIds) {
            // auto-group of platforms
            log.trace("finding current health for autogrouped platforms " + "of type " + childTypeId);
            return measurementBoss.findAGPlatformsCurrentHealthByType(sessionId.intValue(), childTypeId.getId());
        } else {
            // auto-group of servers or services
            switch (childTypeId.getType()) {
                case AppdefEntityConstants.APPDEF_TYPE_SERVER:
                    return measurementBoss.findAGServersCurrentHealthByType(sessionId.intValue(), entityIds,
                        childTypeId.getId());
                case AppdefEntityConstants.APPDEF_TYPE_SERVICE:
                    log.trace("finding current health for autogrouped services " + "of type " + childTypeId +
                              " for resources " + Arrays.asList(entityIds));
                    return measurementBoss.findAGServicesCurrentHealthByType(sessionId.intValue(), entityIds,
                        childTypeId.getId());
                default:
                    log.trace("finding current health for autogrouped services " + "of type " + childTypeId +
                              " for resources " + Arrays.asList(entityIds));
                    return measurementBoss.findAGServicesCurrentHealthByType(sessionId.intValue(), entityIds,
                        childTypeId.getId());
            }
        }
    }
}
