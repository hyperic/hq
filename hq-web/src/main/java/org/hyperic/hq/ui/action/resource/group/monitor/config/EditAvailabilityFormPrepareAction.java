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

package org.hyperic.hq.ui.action.resource.group.monitor.config;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.struts.action.Action;
import org.apache.struts.action.ActionForm;
import org.apache.struts.action.ActionForward;
import org.apache.struts.action.ActionMapping;
import org.hyperic.hq.appdef.shared.AppdefResourceValue;
import org.hyperic.hq.ui.Constants;
import org.hyperic.hq.ui.util.RequestUtils;

/**
 * An Action that retrieves data from the BizApp to facilitate display of the
 * <em>Edit Availability</em> pages for a compatible group's metrics.
 */
public class EditAvailabilityFormPrepareAction
    extends Action {

    /**
     * Retrieve this data and store it in the specified request parameters:
     * 
     * <ul>
     * <li><code>AppdefEntityId</code> object identified by
     * <code>Constants.RESOURCE_ID_PARAM</code> and
     * <code>Constants.RESOURCE_TYPE_PARAM</code></li>
     * <li><code>List</code> of available <code>Metrics</code> objects (those
     * not already associated with the resource) in
     * <code>Constants.AVAIL_METRICS_ATTR</code></li>
     * <li><code>Integer</code> number of available metrics in
     * <code>Constants.NUM_AVAIL_METRICS_ATTR</code></li>
     * <li><code>List</code> of pending <code>Metrics</code> objects (those in
     * queue to be associated with the resource) in
     * <code>Constants.PENDING_METRICS_ATTR</code></li>
     * <li><code>Integer</code> number of pending metrics in
     * <code>Constants.NUM_PENDING_METRICS_ATTR</code></li>
     * </ul>
     */
    public ActionForward execute(ActionMapping mapping, ActionForm form, HttpServletRequest request,
                                 HttpServletResponse response) throws Exception {

        Log log = LogFactory.getLog(EditAvailabilityFormPrepareAction.class.getName());
        GroupMonitoringConfigForm addForm = (GroupMonitoringConfigForm) form;

        Integer resourceId = addForm.getRid();
        Integer entityType = addForm.getType();

        if (resourceId == null) {
            resourceId = RequestUtils.getResourceId(request);
        }
        if (entityType == null) {
            entityType = RequestUtils.getResourceTypeId(request);
        }

        AppdefResourceValue resource = RequestUtils.getResource(request);
        if (resource == null) {
            RequestUtils.setError(request, Constants.ERR_RESOURCE_NOT_FOUND);
            return null;
        }

        log.trace("Getting availability threshold metrics");

        // XXX actually set the availability and unavailability thresholds

        return null;

    }
}
