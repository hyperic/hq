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

package org.hyperic.hq.ui.action.resource;

import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.struts.action.ActionMapping;
import org.hyperic.hq.appdef.shared.AppdefEntityID;
import org.hyperic.hq.appdef.shared.AppdefEntityNotFoundException;
import org.hyperic.hq.appdef.shared.AppdefEntityTypeID;
import org.hyperic.hq.appdef.shared.AppdefResourceTypeValue;
import org.hyperic.hq.appdef.shared.AppdefResourceValue;
import org.hyperic.hq.authz.server.session.AuthzSubject;
import org.hyperic.hq.authz.shared.PermissionException;
import org.hyperic.hq.bizapp.shared.AppdefBoss;
import org.hyperic.hq.bizapp.shared.AuthzBoss;
import org.hyperic.hq.bizapp.shared.ControlBoss;
import org.hyperic.hq.ui.Constants;
import org.hyperic.hq.ui.WebUser;
import org.hyperic.hq.ui.action.BaseDispatchAction;
import org.hyperic.hq.ui.exception.ParameterNotFoundException;
import org.hyperic.hq.ui.util.ActionUtils;
import org.hyperic.hq.ui.util.ContextUtils;
import org.hyperic.hq.ui.util.DashboardUtils;
import org.hyperic.hq.ui.util.RequestUtils;
import org.hyperic.hq.ui.util.SessionUtils;
import org.hyperic.hq.ui.util.UIUtils;
import org.hyperic.util.config.ConfigResponse;

/**
 * An abstract subclass of <code>BaseDispatchAction</code> that
 * provides common methods for resource controller actions.
 */
public abstract class ResourceController extends BaseDispatchAction {

    protected static final Log log =
        LogFactory.getLog(ResourceController.class.getName());
    
    protected AppdefEntityID setResource(HttpServletRequest request)
        throws Exception {
        return setResource(request, false);
    }
    
    protected AppdefEntityID setResource(HttpServletRequest request,
                                         boolean config)
        throws Exception {
        AppdefEntityID entityId = null;
        
        try {
            entityId = RequestUtils.getEntityId(request);
        } catch (ParameterNotFoundException e) {
            // not a problem, this can be null
        }
        
        return setResource(request, entityId, config);
    }
    
    protected AppdefEntityID setResource(HttpServletRequest request,
                                         AppdefEntityID entityId,
                                         boolean config)
        throws Exception {
        Integer sessionId = RequestUtils.getSessionId(request);

        ServletContext ctx = getServlet().getServletContext();            
        AppdefBoss appdefBoss = ContextUtils.getAppdefBoss(ctx);
        AuthzBoss authzBoss = ContextUtils.getAuthzBoss(ctx);
        AppdefEntityTypeID aetid;
        if (null == entityId || entityId instanceof AppdefEntityTypeID) {
            // this can happen if we're an auto-group of platforms
            request.setAttribute(Constants.CONTROL_ENABLED_ATTR,
                                 Boolean.FALSE);
            request.setAttribute(Constants.PERFORMANCE_SUPPORTED_ATTR,
                                 Boolean.FALSE);
            try {
                if (entityId != null)
                    aetid = (AppdefEntityTypeID) entityId;
                else
                    aetid = new AppdefEntityTypeID(
                        RequestUtils.getStringParameter(
                            request, Constants.APPDEF_RES_TYPE_ID));
                
                AppdefResourceTypeValue resourceTypeVal =
                    appdefBoss.findResourceTypeById(sessionId.intValue(),
                                                    aetid);
                request.setAttribute(Constants.RESOURCE_TYPE_ATTR,
                                     resourceTypeVal);
                // Set the title parameters
                request.setAttribute(Constants.TITLE_PARAM_ATTR,
                                     resourceTypeVal.getName());
            } catch (Exception e) {
                log.debug("Error setting resource attributes", e);
            }
        } else {
            try {
                log.trace("finding resource [" + entityId + "]");
                AppdefResourceValue resource =
                    appdefBoss.findById(sessionId.intValue(), entityId);

                log.trace("finding owner for resource [" + entityId + "]");
                AuthzSubject owner =
                    authzBoss.findSubjectByNameNoAuthz(sessionId,
                                                       resource.getOwner());

                log.trace("finding most recent modifier for resource [" +
                          entityId + "]");
                AuthzSubject modifier =
                    authzBoss.findSubjectByNameNoAuthz(sessionId,
                                                       resource.getModifiedBy());

                RequestUtils.setResource(request, resource);
                request.setAttribute(Constants.RESOURCE_OWNER_ATTR, owner);
                request.setAttribute(Constants.RESOURCE_MODIFIER_ATTR, modifier);
                request.setAttribute(Constants.TITLE_PARAM_ATTR,
                                     resource.getName());

                // set the resource controllability flag
                if (!entityId.isApplication()) {
                    ControlBoss controlBoss = ContextUtils.getControlBoss(ctx);
                    // We were doing group Specific isGroupControlEnabled for
                    // groups.
                    // We should just see if the group control is supported
                    // regardless of whether control is enabled or not.  Also we
                    // should be calling isControlSupported on entity and not
                    // controlEnabled.
                    boolean isControllable =
                        controlBoss.isControlSupported(sessionId.intValue(),
                                                       resource);
    
                    request.setAttribute(Constants.CONTROL_ENABLED_ATTR,
                                         new Boolean(isControllable));
                }

                // Set additional flags
                UIUtils utils = ContextUtils.getUIUtils(ctx);
                if (utils != null)
                    utils.setResourceFlags(resource, config, request);
                
                // Get the custom properties
                Properties cprops =
                    appdefBoss.getCPropDescEntries(sessionId.intValue(),
                                                   entityId);
                
                // Set the properties in the request
                if (cprops.size() > 0)
                    request.setAttribute("cprops", cprops);
                
                // Add this resource to the recently used preference
                WebUser user = RequestUtils.getWebUser(request);
                ConfigResponse userPrefs = user.getPreferences();
                
                if (DashboardUtils.addEntityToPreferences(
                        Constants.USERPREF_KEY_RECENT_RESOURCES, userPrefs,
                        entityId, 10)) {
                	AuthzBoss boss = ContextUtils.getAuthzBoss(ctx);
                	boss.setUserPrefs(user.getSessionId(),
                                      user.getSubject().getId(), userPrefs);
                }
            } catch (AppdefEntityNotFoundException aenf) {
                RequestUtils.setError(request, Constants.ERR_RESOURCE_NOT_FOUND);
                throw aenf;
            } catch (PermissionException e) {
                throw e;
            } catch (Exception e) {
                log.error("Unable to find resource", e);
                throw AppdefEntityNotFoundException.build(entityId, e);
            }
        }
        return entityId;
    }

    protected void fetchReturnPathParams(HttpServletRequest request,
                                         Map params) {
        AppdefEntityID aeid = RequestUtils.getEntityId(request);
        params.put(Constants.ENTITY_ID_PARAM, aeid.getAppdefKey());

        try {
            AppdefEntityTypeID ctype =
                RequestUtils.getChildResourceTypeId(request);
            params.put(Constants.CHILD_RESOURCE_TYPE_ID_PARAM, ctype);
        } catch (ParameterNotFoundException pnfe) {
            // that's ok!
        }

        try {
            Integer autogrouptype =
                RequestUtils.getAutogroupResourceTypeId(request);
            params.put(Constants.AUTOGROUP_TYPE_ID_PARAM, autogrouptype);
        } catch (ParameterNotFoundException pnfe) {
            // that's ok!
        }

        // sets the returnPath to match the mode we're in.
        String mode = request.getParameter(Constants.MODE_PARAM);
        params.put(Constants.MODE_PARAM, mode);
    }
    
    /** 
     * This sets the return path for a ResourceAction by appending
     * the type and resource id to the forward url.
     * 
     * @param request The current controller's request.
     * @param mapping The current controller's mapping that contains the input.
     *
     * @exception ParameterNotFoundException if the type or id are not found
     * @exception ServletException If there is not input defined for this form
     */
    protected void setReturnPath(HttpServletRequest request,
                                 ActionMapping mapping,
                                 Map params) 
        throws Exception {
        this.fetchReturnPathParams(request, params);
        String returnPath = ActionUtils.findReturnPath(mapping, params);
        if (log.isTraceEnabled()) {
            log.trace("setting return path: " + returnPath);
        }
        SessionUtils.setReturnPath(request.getSession(), returnPath); 
    }

    protected void setReturnPath(HttpServletRequest request,
                                 ActionMapping mapping)
        throws Exception {
        setReturnPath(request, mapping, new HashMap());
    }
    
    /**
     * This method sets the current location for the nav map
     */
    protected void setNavMapLocation(HttpServletRequest request,
                                     ActionMapping mapping, String currLoc)
        throws Exception                                       
    {
        HashMap parms = new HashMap();
        // sets the returnPath to match the mode we're in.
        String mode = request.getParameter(Constants.MODE_PARAM);
        parms.put(Constants.MODE_PARAM, mode);
        
        String newUrl = ActionUtils.changeUrl(currLoc, parms);
        
        request.setAttribute(Constants.CURR_RES_LOCATION_MODE,  
                                        new String(mode));
        request.setAttribute(Constants.CURR_RES_LOCATION_TYPE,  
                                        new String(currLoc));
        request.setAttribute(Constants.CURR_RES_LOCATION_TAG, newUrl);
        
    }
}
