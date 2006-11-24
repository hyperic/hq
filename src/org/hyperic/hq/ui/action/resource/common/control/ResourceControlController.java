package org.hyperic.hq.ui.action.resource.common.control;

import java.rmi.RemoteException;
import java.util.List;

import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.struts.action.ActionForm;
import org.apache.struts.action.ActionForward;
import org.apache.struts.action.ActionMapping;
import org.hyperic.hq.appdef.shared.AppdefEntityConstants;
import org.hyperic.hq.appdef.shared.AppdefEntityID;
import org.hyperic.hq.bizapp.shared.ControlBoss;
import org.hyperic.hq.common.ApplicationException;
import org.hyperic.hq.product.PluginNotFoundException;
import org.hyperic.hq.ui.Constants;
import org.hyperic.hq.ui.action.resource.ResourceController;
import org.hyperic.hq.ui.util.ContextUtils;
import org.hyperic.hq.ui.util.RequestUtils;

/*
 * An abstract subclass of <code>ResourceControllerAction</code> that
 * provides common methods for resource control controller actions.
 */
public abstract class ResourceControlController extends ResourceController {

    protected static final Log log =
        LogFactory.getLog(ResourceControlController.class.getName());
    
    /**
     * Checks to see if control is enabled for this resource. Sets
     * Constants.CONTROL_ENABLED_ATTR in request scope.
     */
    protected void checkControlEnabled(ActionMapping mapping,
                                        ActionForm form,
                                        HttpServletRequest request,
                                        HttpServletResponse response) {
        // check to see if control is enabled                                    
        try {
            ServletContext ctx = getServlet().getServletContext();
            ControlBoss cBoss = ContextUtils.getControlBoss(ctx);
            int sessionId = RequestUtils.getSessionId(request).intValue();
            AppdefEntityID appdefId = RequestUtils.getEntityId(request);
            int type = appdefId.getType();

            boolean isEnabled = false;
            switch (type) {
                case AppdefEntityConstants.APPDEF_TYPE_GROUP:
                case AppdefEntityConstants.APPDEF_TYPE_GROUP_COMPAT_SVC:
                case AppdefEntityConstants.APPDEF_TYPE_GROUP_COMPAT_PS:
                    isEnabled = cBoss.isGroupControlEnabled(sessionId, appdefId);
                    break;
                default:
                    isEnabled = cBoss.isControlEnabled(sessionId, appdefId);
            }
            request.setAttribute( Constants.CONTROL_ENABLED_ATTR, 
                                  new Boolean(isEnabled) );
            if (isEnabled) {
                List actions;
                try {
                    actions = cBoss.getActions(sessionId, appdefId);
                    Boolean hasControls
                        = (actions.size() > 0) ? Boolean.TRUE : Boolean.FALSE;
                    request.setAttribute("hasControlActions", hasControls);
                } catch (PluginNotFoundException e) {
                    log.warn("Error loading plugin for " + appdefId + ": " + e);
                    request.setAttribute("hasControlActions", Boolean.FALSE);
                }
            } else {
                request.setAttribute("hasControlActions", Boolean.FALSE);
            }
        } catch (RemoteException e) {
            // couldn't get servlet context. oh well.   
            log.error("Unexpected exception: " + e, e);
        } catch (ServletException e) {
            // couldn't get servlet context. oh well.   
            log.error("Unexpected exception: " + e, e);
        } catch (ApplicationException e) {
            log.error("Unexpected exception: " + e, e);
        }
    }
    public ActionForward currentControlStatus(ActionMapping mapping,
                                              ActionForm form,
                                              HttpServletRequest request,
                                              HttpServletResponse response)
        throws Exception {
        super.setNavMapLocation(request,mapping,
                                Constants.CONTROL_LOC); 
        return null;
    }
    
    public ActionForward controlStatusHistory(ActionMapping mapping,
                                              ActionForm form,
                                              HttpServletRequest request,
                                              HttpServletResponse response)
        throws Exception {
        super.setNavMapLocation(request,mapping,
                                Constants.CONTROL_LOC); 
        return null;
    }
}
