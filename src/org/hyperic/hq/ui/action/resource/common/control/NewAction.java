package org.hyperic.hq.ui.action.resource.common.control;

import java.util.HashMap;
import java.util.List;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.struts.action.ActionForm;
import org.apache.struts.action.ActionForward;
import org.apache.struts.action.ActionMapping;
import org.hyperic.hq.appdef.shared.AppdefEntityID;
import org.hyperic.hq.authz.shared.PermissionException;
import org.hyperic.hq.bizapp.shared.ControlBoss;
import org.hyperic.hq.product.PluginException;
import org.hyperic.hq.product.PluginNotFoundException;
import org.hyperic.hq.scheduler.ScheduleValue;
import org.hyperic.hq.ui.Constants;
import org.hyperic.hq.ui.action.BaseAction;
import org.hyperic.hq.ui.util.ContextUtils;
import org.hyperic.hq.ui.util.RequestUtils;
import org.hyperic.hq.ui.util.SessionUtils;

/**
 * An <code>Action</code> subclass that creates a control action associated
 * with a server.
 */
public class NewAction extends BaseAction {

    // ---------------------------------------------------- Public Methods

    /**
     * Create the control action and associate it with the server.
     * <code>NewForm</code> and save it into the session attribute
     * <code>Constants.ACTION_ATTR</code>.
     */
    public ActionForward execute(ActionMapping mapping, 
                                 ActionForm form, 
                                 HttpServletRequest request, 
                                 HttpServletResponse response) 
        throws Exception {
        
        Log log = LogFactory.getLog(NewAction.class.getName());            
        log.trace("creating new action" );                    

        HashMap parms = new HashMap(2);
        
        try {                                  
 
            int sessionId = RequestUtils.getSessionId(request).intValue();
            AppdefEntityID appdefId = RequestUtils.getEntityId(request);
            ControlForm cForm = (ControlForm) form;

            parms.put(Constants.RESOURCE_PARAM, appdefId.getId());
            parms.put(Constants.RESOURCE_TYPE_ID_PARAM,
                      new Integer(appdefId.getType()));

            ActionForward forward 
                = checkSubmit(request, mapping, cForm, parms);
            if (forward != null) {
                return forward;
            }

            ServletContext ctx = getServlet().getServletContext();            
            ControlBoss cBoss = ContextUtils.getControlBoss(ctx);
            
            // create the new action to schedule

            ScheduleValue sv = cForm.createSchedule();
            sv.setDescription(cForm.getDescription());
            
            // make sure that the ControlAction is valid.
            String action = cForm.getControlAction();
            List validActions = cBoss.getActions(sessionId, appdefId);
            if (!validActions.contains(action)) {
                RequestUtils.setError(request,
                    "resource.common.control.error.ControlActionNotValid",
                    action);
                return returnFailure(request, mapping, parms);
            }
            
            if (cForm.getStartTime().equals(cForm.START_NOW)) {
                cBoss.doAction(sessionId, appdefId, action, (String)null);
            } else {
                cBoss.doAction(sessionId, appdefId, action, sv);
            }

            // set confirmation message
            SessionUtils.setConfirmation(request.getSession(), 
                "resource.common.scheduled.Confirmation");
            
            return returnSuccess(request, mapping, parms);
        } catch (PluginNotFoundException pnfe) {
            log.trace("no plugin available", pnfe);
            RequestUtils.setError(request,
                "resource.common.control.PluginNotFound");
            return returnFailure(request, mapping, parms);                 
        } catch (PluginException cpe) {
            log.trace("control not enabled", cpe);
            RequestUtils.setError(request,
                "resource.common.error.ControlNotEnabled");
            return returnFailure(request, mapping, parms);                 
        } catch (PermissionException pe) {
            RequestUtils.setError(request,
                "resource.common.control.error.NewPermission");
            return returnFailure(request, mapping, parms);
        }
    } 
}
