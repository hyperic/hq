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

package org.hyperic.hq.ui.action.resource.common.monitor.visibility;

import java.rmi.RemoteException;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;

import org.hyperic.hq.appdef.shared.AppdefEntityConstants;
import org.hyperic.hq.appdef.shared.AppdefEntityID;
import org.hyperic.hq.appdef.shared.AppdefEntityNotFoundException;
import org.hyperic.hq.appdef.shared.AppdefEntityTypeID;
import org.hyperic.hq.appdef.shared.AppdefResourceTypeValue;
import org.hyperic.hq.appdef.shared.AppdefResourceValue;
import org.hyperic.hq.appdef.shared.ConfigFetchException;
import org.hyperic.hq.auth.shared.SessionNotFoundException;
import org.hyperic.hq.auth.shared.SessionTimeoutException;
import org.hyperic.hq.authz.shared.PermissionException;
import org.hyperic.hq.bizapp.shared.ProductBoss;
import org.hyperic.hq.product.ProductPlugin;
import org.hyperic.hq.ui.action.resource.application.monitor.visibility.ApplicationInventoryHelper;
import org.hyperic.hq.ui.action.resource.group.monitor.visibility.GroupInventoryHelper;
import org.hyperic.hq.ui.action.resource.platform.monitor.visibility.PlatformInventoryHelper;
import org.hyperic.hq.ui.action.resource.server.monitor.visibility.ServerInventoryHelper;
import org.hyperic.hq.ui.action.resource.service.monitor.visibility.ServiceInventoryHelper;
import org.hyperic.hq.ui.exception.ParameterNotFoundException;
import org.hyperic.hq.ui.util.ContextUtils;
import org.hyperic.hq.ui.util.MonitorUtils;
import org.hyperic.hq.ui.util.RequestUtils;
import org.hyperic.util.StringUtil;
import org.hyperic.util.config.EncodingException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.struts.action.ActionMessage;
import org.apache.struts.action.ActionMessages;

/**
 * A class that provides utility methods for common monitoring
 * tasks.
 *
 * Typical usage: an action class uses the <code>getHelper</code>
 * factory method to obtain an <code>InventoryHelper</code> specific
 * to the entity type of a particular resource.
 *
 */
public abstract class InventoryHelper {

    protected Log log = LogFactory.getLog(this.getClass().getName());

    protected AppdefEntityID entityId = null;
    
    private static final String CFG_ERR_RES =
        "resource.common.inventory.configProps.Unconfigured.error";
    private static final String CFG_INVALID_RES =
        "resource.common.inventory.configProps.InvalidConfig.error";

    protected InventoryHelper(AppdefEntityID entityId) {
        this.entityId = entityId;
    }

    /**
     * Return a subclass of <code>InventoryHelper</code> specific to
     * the entity type of a particular resource.
     */
    public static InventoryHelper getHelper(AppdefEntityID entityId) {
        switch(entityId.getType()) {
            case AppdefEntityConstants.APPDEF_TYPE_PLATFORM:
                return new PlatformInventoryHelper(entityId);
            case AppdefEntityConstants.APPDEF_TYPE_SERVER:
                return new ServerInventoryHelper(entityId);
            case AppdefEntityConstants.APPDEF_TYPE_SERVICE:
                return new ServiceInventoryHelper(entityId);
            case AppdefEntityConstants.APPDEF_TYPE_APPLICATION:
                return new ApplicationInventoryHelper(entityId);
            case AppdefEntityConstants.APPDEF_TYPE_GROUP:
                return new GroupInventoryHelper(entityId);
            default:
                throw new IllegalArgumentException(entityId.getTypeName());
        }
    }

    /**
     * Get the set of child resource types representing a resource's
     * child resources.
     */
    public abstract List getChildResourceTypes(HttpServletRequest request,
                                               ServletContext ctx,
                                               AppdefResourceValue resource)
        throws Exception;

    /**
     * Get a child resource type from the Bizapp.
     * @param id the id of the child resource type
     */
    public abstract AppdefResourceTypeValue getChildResourceType(HttpServletRequest req,
                                                            ServletContext ctx,
                                                            AppdefEntityTypeID atid)
        throws Exception;

    /**
     * Get from the Bizapp the numbers of children of the given
     * resource. Returns a <code>Map</code> of counts keyed by
     * child resource type.
     * @param resource the appdef resource whose children we are
     * counting
     */
    public abstract Map getChildCounts(HttpServletRequest request,
                                       ServletContext ctx,
                                       AppdefResourceValue resource)
        throws Exception;

    /**
     * Retrieve the id of the selected child resource type from the
     * request. If no selection was made, then return the default id
     * if <code>selectDefaultSubtab</code> allows.
     *
     * @param childTypes the complete List of child resource types for
     * this entity type
     * @param childCounts the Map of child resource counts keyed by
     * resource type
     * @param defaultOverride whether or not to override
     * <code>selectDefaultSubtab</code>
     */
    public Integer getSelectedChildId(HttpServletRequest request,
                                      List childTypes,
                                      Map childCounts,
                                      boolean defaultOverride) {
        AppdefEntityTypeID childTypeId = null;
        try {
            childTypeId = RequestUtils.getChildResourceTypeId(request);
        }
        catch (ParameterNotFoundException pnfe) {
            ; // it's ok if the request param is not specified
        }

        if (defaultOverride && childTypeId == null) {
            // default to the first child type for which we
            // actually have some deployed childs
            return MonitorUtils.findDefaultChildResourceId(childTypes,
                                                           childCounts);
        }
        
        if (childTypeId != null)
            return childTypeId.getId();
        
        return null;
    }

    /**
     * Retrieve the id of the selected child resource type from the
     * request. If no selection was made, then return the default id
     * if <code>selectDefaultSubtab</code> allows.
     *
     * @param childTypes the complete List of child resource types for
     * this entity type
     * @param childCounts the Map of child resource counts keyed by
     * resource type
     */
    public Integer getSelectedChildId(HttpServletRequest request,
                                      List childTypes,
                                      Map childCounts) {
        return getSelectedChildId(request, childTypes, childCounts, false);
    }

    /**
     * Retrieve the <code>AppdefResourceTypeValue</code> representing
     * the currently selected child resource type (or, if the
     * <code>isPerformance</code> flag is not set, the default
     * child resource type, if none is currently selected).
     *
     * @param childTypes the complete List of child resource types for
     * this entity type
     * @param childCounts the Map of child resource counts keyed by
     * resource type
     * @param isPerformance a Boolean indicating whether or not we are
     * currently displaying a performance page
     */
    public AppdefResourceTypeValue getSelectedChildType (
        HttpServletRequest request,
        List childTypes,
        Map childCounts,
        Integer selectedId) {

        if (selectedId != null) {
            
            for  (Iterator i = childTypes.iterator(); i.hasNext();) {
                AppdefResourceTypeValue t = (AppdefResourceTypeValue) i.next();
                if (t.getId().intValue() == selectedId.intValue()) {
                    return t;
                }
            }
        }

        return null;
    }

    /**
     * Return a boolean indicating whether or not performance pages
     * should show child type subtabs. The default behavior is to show
     * subtabs.
     */
    public boolean showPerformanceSubtabs() {
        return true;
    }

    /**
     * Return a boolean indicating whether or not performance pages
     * should select the default subtab when none is selected. The
     * default behavior is to select the default subtab.
     */
    public boolean selectDefaultSubtab() {
        return true;
    }
    
    public boolean isResourceConfigured(HttpServletRequest request,
                                        ServletContext ctx, boolean setError)
        throws ServletException, AppdefEntityNotFoundException,
               SessionNotFoundException, SessionTimeoutException,
               PermissionException, EncodingException, RemoteException {
        final String CONFIG_ATTR = "IsResourceUnconfigured";

        Boolean configured =
            (Boolean) request.getAttribute(CONFIG_ATTR);
        if (configured != null)
            return !configured.booleanValue();
        
        if (AppdefEntityConstants.APPDEF_TYPE_GROUP == entityId.getType())
            return true;
        
        int sessionId = RequestUtils.getSessionId(request).intValue();

        if (this instanceof ApplicationInventoryHelper) return true;

        ProductBoss pboss = ContextUtils.getProductBoss(ctx);

        String context = request.getContextPath();
        try {
            pboss.getMergedConfigResponse(sessionId,
                    ProductPlugin.TYPE_MEASUREMENT, entityId, true);
        } catch (ConfigFetchException e) {
            if (setError) {
                ActionMessage error
                    = new ActionMessage(CFG_ERR_RES,
                                        new String[] {
                                            context,
                                            String.valueOf(entityId.getType()),
                                            String.valueOf(entityId.getID()) });
                RequestUtils.setError(request, error, 
                                      ActionMessages.GLOBAL_MESSAGE);
            }
            request.setAttribute(CONFIG_ATTR, Boolean.TRUE);
            return false;
        }

        // only check where the config is invalid
        String validationError =
            pboss.getConfigResponse(sessionId, entityId).getValidationError();

        if (validationError == null) {
            request.setAttribute(CONFIG_ATTR, Boolean.FALSE);
            return true;
        }
        if (setError) {
            ActionMessage error
                = new ActionMessage(CFG_INVALID_RES,
                                    new String[] {
                                        StringUtil.replace(validationError, 
                                                           "\n", 
                                                          "<br>&nbsp;&nbsp;"
                                                          + "&nbsp;&nbsp;"),
                                        context,
                                        String.valueOf(entityId.getType()),
                                        String.valueOf(entityId.getID()) });
            RequestUtils.setError(request, error, 
                                  ActionMessages.GLOBAL_MESSAGE);
        }
        request.setAttribute(CONFIG_ATTR, Boolean.TRUE);
        return false;
    }
}
