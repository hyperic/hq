package org.hyperic.hq.ui.action.resource.common.control;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.struts.action.ActionForm;
import org.apache.struts.action.ActionForward;
import org.apache.struts.action.ActionMapping;
import org.hyperic.hq.appdef.shared.AppdefEntityID;
import org.hyperic.hq.bizapp.shared.ControlBoss;
import org.hyperic.hq.control.shared.ControlHistoryValue;
import org.hyperic.hq.ui.Constants;
import org.hyperic.hq.ui.action.BaseAction;
import org.hyperic.hq.ui.exception.ParameterNotFoundException;
import org.hyperic.hq.ui.util.ContextUtils;
import org.hyperic.hq.ui.util.RequestUtils;


/**
 * An Action that the current status of actions on a resource.
 */
public class UpdateStatusAction extends BaseAction {

    // ---------------------------------------------------- Public Methods

    /** 
     * Displays state of current actions of a resource. 
     */
    public ActionForward execute(ActionMapping mapping,
                                 ActionForm form,
                                 HttpServletRequest request,
                                 HttpServletResponse response)
        throws Exception {
        ServletContext ctx = getServlet().getServletContext();
        Log log = LogFactory.getLog(UpdateStatusAction.class.getName());
                
        log.trace("determining current status.");              
        int sessionId = RequestUtils.getSessionId(request).intValue(); 
        ControlBoss cBoss = ContextUtils.getControlBoss(ctx);
        AppdefEntityID appId = RequestUtils.getEntityId(request);

        Integer batchId = null;
        try {
            batchId = RequestUtils.getIntParameter(request,
                        Constants.CONTROL_BATCH_ID_PARAM);
        } 
        /* failed to get that param, that's ok, use current */
        catch (NullPointerException npe) {}
        catch (ParameterNotFoundException pnfe) {}
        catch (NumberFormatException nfe) {}

        ControlHistoryValue cValue = null;
        if (null == batchId) {
            cValue = cBoss.getCurrentJob(sessionId, appId);
        } else {
            cValue = cBoss.getJobByJobId(sessionId, batchId);
        }
        
        if (cValue == null /* no current job */) {
            cValue = cBoss.getLastJob(sessionId, appId);
        }
        else {
            // Check for valid duration
            if (cValue.getDuration() == 0) {
                cValue.setDuration(System.currentTimeMillis() -
                                   cValue.getStartTime());
            }
        }
        
        request.setAttribute(Constants.CONTROL_CURRENT_STATUS_ATTR, cValue);
        
        return returnSuccess(request, mapping);
    }
}
