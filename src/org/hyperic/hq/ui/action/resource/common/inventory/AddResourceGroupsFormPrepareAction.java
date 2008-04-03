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
import java.util.Iterator;
import java.util.List;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.hyperic.hq.appdef.shared.AppdefEntityID;
import org.hyperic.hq.appdef.shared.AppdefResourceValue;
import org.hyperic.hq.authz.server.session.Resource;
import org.hyperic.hq.authz.server.session.ResourceGroup;
import org.hyperic.hq.authz.server.session.ResourceManagerEJBImpl;
import org.hyperic.hq.bizapp.shared.AppdefBoss;
import org.hyperic.hq.ui.Constants;
import org.hyperic.hq.ui.util.ContextUtils;
import org.hyperic.hq.ui.util.RequestUtils;
import org.hyperic.hq.ui.util.SessionUtils;
import org.hyperic.util.pager.PageControl;
import org.hyperic.util.pager.PageList;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.struts.action.Action;
import org.apache.struts.action.ActionForm;
import org.apache.struts.action.ActionForward;
import org.apache.struts.action.ActionMapping;

/**
 * An <code>Action</code> that retrieves data from the BizApp to
 * facilitate display of the various pages to add group memeberships
 * for resources.
 */
public class AddResourceGroupsFormPrepareAction extends Action {

    // ---------------------------------------------------- Public Methods

    /**
     * Retrieve this data and store it in the specified request
     * parameters:
     *
     * <ul>
     *   <li><code>List</code> of available <code>ResourceGroupValue</code>
     *     objects (those not already associated with the resource) in
     *     <code>Constants.AVAIL_RESOURCE_GROUPS_ATTR</code></li>
     *   <li><code>Integer</code> number of available resource groups in
     *     <code>Constants.NUM_AVAIL_RESOURCE_GROUPS_ATTR</code></li>
     *   <li><code>List</code> of pending <code>ResourceGroupValue</code>
     *     objects (those in queue to be associated with the resource) in
     *     <code>Constants.PENDING_RESOURCE_GROUPS_ATTR</code></li>
     *   <li><code>Integer</code> number of pending resource groups in
     *     <code>Constants.NUM_PENDING_RESOURCE_GROUPS_ATTR</code></li>
     * </ul>
     */
    public ActionForward execute(ActionMapping mapping,
                                 ActionForm form,
                                 HttpServletRequest request,
                                 HttpServletResponse response)
        throws Exception {
        Log log = LogFactory
            .getLog(AddResourceGroupsFormPrepareAction.class.getName());

        AddResourceGroupsForm addForm = (AddResourceGroupsForm) form;
        AppdefEntityID entityId = new AppdefEntityID(addForm.getEid());

        AppdefResourceValue resource = RequestUtils.getResource(request);
        if (resource == null) {
            RequestUtils.setError(request, Constants.ERR_RESOURCE_NOT_FOUND);
            return null;
        }

        addForm.setRid(resource.getId());
        addForm.setType(new Integer(entityId.getType()));

        ServletContext ctx = getServlet().getServletContext();
        AppdefBoss boss = ContextUtils.getAppdefBoss(ctx);
        Integer sessionId = RequestUtils.getSessionId(request);
        PageControl pca =
            RequestUtils.getPageControl(request, "psa", "pna", "soa", "sca");
        PageControl pcp =
            RequestUtils.getPageControl(request, "psp", "pnp", "sop", "scp");

        // pending groups are those on the right side of the "add
        // to list" widget- awaiting association with the resource
        // when the form's "ok" button is clicked.
        boolean groupsArePending = (SessionUtils
                .getList(request.getSession(),
                         Constants.PENDING_RESGRPS_SES_ATTR)).length > 0;

        // available groups are all groups in the system that are
        //  _not_ associated with the resource and are not pending 
        // since the bizapp will return all groups, we'll filter out the
        // the ones that are pending from the "available" list and set 
        // _that_ as a request attribute 
        log.trace("getting available groups for resource [" +  entityId + "]");
        Integer[] pendingGroupIds =
            SessionUtils.getList(request.getSession(),
                                 Constants.PENDING_RESGRPS_SES_ATTR);

        Resource r = ResourceManagerEJBImpl.getOne()
                         .findResource(resource.getEntityId()); 
        PageList availableGroups = 
            boss.findAllGroupsMemberExclusive(
                sessionId.intValue(), pca, entityId, pendingGroupIds, 
                r.getPrototype());

        if (log.isTraceEnabled())
            log.trace("findAllGroups(...) returned these " +
                "AppdefGroupValues " + availableGroups);

        if (groupsArePending) {            
            log.trace("getting pending groups for resource [" + entityId + "]");
            PageList pendingGroups =
                boss.findGroups(sessionId.intValue(), pendingGroupIds, pcp);
            request.setAttribute(Constants.PENDING_RESGRPS_ATTR, pendingGroups);
            request.setAttribute(Constants.NUM_PENDING_RESGRPS_ATTR,
                                 new Integer(pendingGroups.getTotalSize()));

            // end filtering
        } else {
            // nothing is pending, so we'll initialize the attributes
            request.setAttribute(Constants.PENDING_RESGRPS_ATTR,
                                 new ArrayList());
            request.setAttribute(Constants.NUM_PENDING_RESGRPS_ATTR,
                                 new Integer(0));
        }
        
        
        request.setAttribute(Constants.AVAIL_RESGRPS_ATTR, availableGroups);            
        request.setAttribute(Constants.NUM_AVAIL_RESGRPS_ATTR,
                             new Integer(availableGroups.getTotalSize()));
        return null;

    }
}
