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

package org.hyperic.hq.ui.action.resource.platform.monitor.visibility;

import java.rmi.RemoteException;
import java.util.List;

import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.hyperic.hq.appdef.shared.AppdefEntityID;
import org.hyperic.hq.appdef.shared.AppdefEntityNotFoundException;
import org.hyperic.hq.appdef.shared.AppdefResourceValue;
import org.hyperic.hq.auth.shared.SessionNotFoundException;
import org.hyperic.hq.auth.shared.SessionTimeoutException;
import org.hyperic.hq.authz.shared.PermissionException;
import org.hyperic.hq.bizapp.shared.MeasurementBoss;
import org.hyperic.hq.ui.Constants;
import org.hyperic.hq.ui.util.ContextUtils;
import org.hyperic.hq.ui.util.RequestUtils;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.struts.action.ActionForm;
import org.apache.struts.action.ActionForward;
import org.apache.struts.action.ActionMapping;
import org.apache.struts.tiles.ComponentContext;
import org.apache.struts.tiles.actions.TilesAction;

/**
 *
 * Fetch the children resources for the platform
 */
public class ListChildrenAction extends TilesAction {
    
    protected static Log log =
        LogFactory.getLog(ListChildrenAction.class.getName());

    public ActionForward execute(ComponentContext context,
                                 ActionMapping mapping,
                                 ActionForm form,
                                 HttpServletRequest request,
                                 HttpServletResponse response)
        throws Exception {
        AppdefResourceValue resource = RequestUtils.getResource(request);
        
        if (resource == null) {
            RequestUtils.setError(request, Constants.ERR_RESOURCE_NOT_FOUND);
            return null;
        }
        
        AppdefEntityID entityId = resource.getEntityId();

        int sessionId = RequestUtils.getSessionId(request).intValue();
        ServletContext ctx = getServlet().getServletContext();
        MeasurementBoss boss = ContextUtils.getMeasurementBoss(ctx);

        Boolean isInternal =
            new Boolean((String) context.getAttribute(Constants.CTX_INTERNAL));
        
        List internalHealths = getChildHealthSummaries(
            request, boss, sessionId, entityId, isInternal.booleanValue());

        context.putAttribute(Constants.CTX_SUMMARIES, internalHealths);

        return null;
    }

    /**
     * Returns a <code>List</code> of <code>ResourceTypeSummaryBean</code>
     * objects representing health summaries of the child resources of the
     * currently viewed resource.
     * 
     * @param request
     *            the current http request
     * @param boss
     *            the <code>MeasurementBoss</code> used to find metric data
     * @param sessionId
     *            the <code>int</code> representing the user's bizapp session
     * @param entityId
     *            an <code>AppdefEntityID</code> identifying the currently
     *            viewed resource
     * @param isInternal
     *            a boolean indicating whether we are asking for internal or
     *            deployed child resources
     * @return List
     */
    protected List getChildHealthSummaries(HttpServletRequest request,
                                           MeasurementBoss boss,
                                           int sessionId,
                                           AppdefEntityID entityId,
                                           boolean isInternal)
        throws PermissionException, AppdefEntityNotFoundException,
               RemoteException, SessionNotFoundException,
               SessionTimeoutException, ServletException {

        if (isInternal) {
            // cheat and treat platform services as internal so as to not 
            // disturb the whackadocious action class hierarchy
            return boss.findSummarizedPlatformServiceCurrentHealth(sessionId,
                                                                   entityId);
        }

        log.trace("getting deployed server healths for resource [" +
                  entityId + "]");

        return boss.findSummarizedServerCurrentHealth(sessionId, entityId);
    }
}
