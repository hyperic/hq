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
import org.hyperic.hq.appdef.shared.AppdefEntityID;
import org.hyperic.hq.bizapp.shared.ControlBoss;
import org.hyperic.hq.ui.Constants;
import org.hyperic.hq.ui.action.BaseAction;
import org.hyperic.hq.ui.util.ContextUtils;
import org.hyperic.hq.ui.util.RequestUtils;

/**
 * An Action that removes a control event from a resource.
 */
public class RemoveHistoryAction extends BaseAction {

    // ---------------------------------------------------- Public Methods

    /** 
     * removes controlactions from a resource
     */
    public ActionForward execute(ActionMapping mapping,
                                 ActionForm form,
                                 HttpServletRequest request,
                                 HttpServletResponse response)
        throws Exception {
            
        Log log = LogFactory.getLog(RemoveHistoryAction.class.getName());
        HashMap parms = new HashMap(2);
        
        RemoveHistoryForm rmForm = (RemoveHistoryForm)form;
        Integer[] actions = rmForm.getControlActions();
        AppdefEntityID aeid = RequestUtils.getEntityId(request);

        parms.put(Constants.RESOURCE_PARAM, aeid.getId());
        parms.put(Constants.RESOURCE_TYPE_ID_PARAM,
                  new Integer(aeid.getType()));

        if (actions == null || actions.length == 0){
            return this.returnSuccess(request, mapping, parms);
        }

        Integer sessionId = RequestUtils.getSessionId(request);
        ServletContext ctx = getServlet().getServletContext();            
        ControlBoss cBoss = ContextUtils.getControlBoss(ctx);

        cBoss.deleteJobHistory(sessionId.intValue(), actions);

        log.trace("Removed server control events.");     
        RequestUtils.setConfirmation(request, 
            "resource.server.ControlHistory.Confirmation");

        return this.returnSuccess(request, mapping, parms);

    }
}
