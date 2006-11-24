package org.hyperic.hq.ui.action.resource.common.control;

import java.util.HashMap;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.struts.action.ActionForm;
import org.apache.struts.action.ActionForward;
import org.apache.struts.action.ActionMapping;
import org.hyperic.hq.appdef.shared.AppdefEntityConstants;
import org.hyperic.hq.appdef.shared.AppdefEntityID;
import org.hyperic.hq.authz.shared.PermissionException;
import org.hyperic.hq.bizapp.shared.ControlBoss;
import org.hyperic.hq.product.PluginException;
import org.hyperic.hq.ui.Constants;
import org.hyperic.hq.ui.action.BaseAction;
import org.hyperic.hq.ui.util.ContextUtils;
import org.hyperic.hq.ui.util.RequestUtils;
import org.hyperic.hq.ui.util.SessionUtils;

/**
 * Perform a quick control action on a resource.
 */
public class QuickControlAction extends BaseAction {

    private static final Log log
        = LogFactory.getLog(QuickControlAction.class.getName());    

    public ActionForward execute(ActionMapping mapping,
                                 ActionForm form,
                                 HttpServletRequest request,
                                 HttpServletResponse response)
        throws Exception {
        
        QuickControlForm qcForm = (QuickControlForm)form;
        log.trace("performing resouce quick control action: " + qcForm.getResourceAction());
 
        HashMap fwdParms = new HashMap(2);
            
        try {    
            ServletContext ctx = getServlet().getServletContext();            
            ControlBoss cBoss = ContextUtils.getControlBoss(ctx);
            int sessionId =  RequestUtils.getSessionIdInt(request);
            
            // create the new action to schedule
            int id = qcForm.getResourceId().intValue();
            int type = qcForm.getResourceType().intValue();
            AppdefEntityID appdefId = new AppdefEntityID(type, id);
            fwdParms.put(Constants.RESOURCE_PARAM, new Integer(id));
            fwdParms.put(Constants.RESOURCE_TYPE_ID_PARAM, new Integer(type));
            
            String action = qcForm.getResourceAction();
            String args = qcForm.getArguments();

            if ( AppdefEntityConstants.APPDEF_TYPE_GROUP == type ) {
                cBoss.doGroupAction(sessionId, appdefId, action, args, null);
            } else {
                cBoss.doAction(sessionId, appdefId, action, args);
            }
            ActionForward fwd 
                = this.returnSuccess(request, mapping, fwdParms); 
            
            // set confirmation message
            String ctrlStr = qcForm.getResourceAction();
            SessionUtils.setConfirmation(request.getSession(false /* dont create */), 
                "resource.server.QuickControl.Confirmation", ctrlStr);
            
            qcForm.reset(mapping, request);
            return fwd;
        } 
        catch (PluginException cpe) {
            log.trace("control not enabled", cpe);
            SessionUtils.setError(request.getSession(false),
                "resource.common.error.ControlNotEnabled");
            return returnFailure(request, mapping, fwdParms);                 
        }
        catch (PermissionException pe) {
            SessionUtils.setError(request.getSession(false),
                "resource.common.control.error.NewPermission");
            return returnFailure(request, mapping, fwdParms);
        }
    }
}
