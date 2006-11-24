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
import org.hyperic.hq.product.PluginException;
import org.hyperic.hq.ui.Constants;
import org.hyperic.hq.ui.action.BaseAction;
import org.hyperic.hq.ui.util.ContextUtils;
import org.hyperic.hq.ui.util.RequestUtils;
import org.hyperic.hq.ui.util.SessionUtils;

/**
 * An Action that removes a control event from a server.
 */
public class RemoveControlJobsAction extends BaseAction {

    // ---------------------------------------------------- Public Methods

    /** 
     * Removes control jobs from a server. 
     */
    public ActionForward execute(ActionMapping mapping,
                                 ActionForm form,
                                 HttpServletRequest request,
                                 HttpServletResponse response)
        throws Exception {
            
        Log log = LogFactory.getLog(RemoveControlJobsAction.class.getName());
        HashMap parms = new HashMap(2);
        
        try {
            RemoveControlJobsForm rmForm = (RemoveControlJobsForm)form;
            Integer[] jobs = rmForm.getControlJobs();
 
            AppdefEntityID aeid = RequestUtils.getEntityId(request);

            parms.put(Constants.RESOURCE_PARAM, aeid.getId());
            parms.put(Constants.RESOURCE_TYPE_ID_PARAM,
                      new Integer(aeid.getType()));

            if (jobs == null || jobs.length == 0){
                return this.returnSuccess(request, mapping, parms);
            }
            
            Integer sessionId = RequestUtils.getSessionId(request);
            ServletContext ctx = getServlet().getServletContext();            
            ControlBoss cBoss = ContextUtils.getControlBoss(ctx);
           
            cBoss.deleteControlJob(sessionId.intValue(), jobs);
        
            log.trace("Removed resource control jobs.");                                                      
            SessionUtils.setConfirmation(request.getSession(false),
                "resource.common.control.confirm.ScheduledRemoved");
            return this.returnSuccess(request, mapping, parms);
            
        }
        catch (PluginException cpe) {
            log.debug("There was a problem removing control jobs: ", cpe);
            SessionUtils.setError(request.getSession(false), 
                "resource.common.control.error.CouldNotRemoveScheduled");
            return returnFailure(request, mapping, parms);
        }
    }
}
