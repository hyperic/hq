package org.hyperic.hq.ui.action.resource.common.control;

import java.util.List;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.struts.action.ActionForm;
import org.apache.struts.action.ActionForward;
import org.apache.struts.action.ActionMapping;
import org.apache.struts.tiles.ComponentContext;
import org.apache.struts.tiles.actions.TilesAction;
import org.hyperic.hq.appdef.shared.AppdefEntityID;
import org.hyperic.hq.bizapp.shared.ControlBoss;
import org.hyperic.hq.control.shared.ControlScheduleValue;
import org.hyperic.hq.product.PluginException;
import org.hyperic.hq.product.PluginNotFoundException;
import org.hyperic.hq.ui.Constants;
import org.hyperic.hq.ui.beans.OptionItem;
import org.hyperic.hq.ui.util.ContextUtils;
import org.hyperic.hq.ui.util.RequestUtils;

/**
 * This populates the EditForm associated with a server
 * control action.
 */
public class EditFormPrepareAction extends TilesAction {

    /**
     * Retrieve server action data and store it in the specified request
     * parameters.
     */
    public ActionForward execute(ComponentContext context,
                                 ActionMapping mapping,
                                 ActionForm form,
                                 HttpServletRequest request,
                                 HttpServletResponse response)
    throws Exception {

        Log log = LogFactory.getLog(EditFormPrepareAction.class.getName());    
        log.trace("Preparing to modify server control properties action.");
        
        ControlForm cForm = (ControlForm)form;

        // populate the control form from that ControlActionSchedule.
        
        try {
            int sessionId = RequestUtils.getSessionId(request).intValue();
            ServletContext ctx = getServlet().getServletContext();            
            ControlBoss cBoss = ContextUtils.getControlBoss(ctx);
            
            Integer trigger 
                = RequestUtils.getIntParameter(request, Constants.CONTROL_BATCH_ID_PARAM);

            ControlScheduleValue job = cBoss.getControlJob(sessionId, trigger);
            
            cForm.populateFromSchedule(job.getScheduleValue(), request.getLocale());
            cForm.setControlAction(job.getAction());
            cForm.setDescription(job.getScheduleValue().getDescription());
 
            AppdefEntityID appdefId = RequestUtils.getEntityId(request);
            
            List actions = cBoss.getActions(sessionId, appdefId);
            actions = OptionItem.createOptionsList(actions);
            cForm.setControlActions(actions);
            cForm.setNumControlActions(new Integer(actions.size()));

            return null;
        } catch (PluginNotFoundException pnfe) {
            log.trace("no plugin available", pnfe);
            RequestUtils.setError(request,
                "resource.common.error.PluginNotFound");
            return null;
        } catch (PluginException cpe) {
            log.trace("could not find trigger", cpe);
            RequestUtils.setError(request,
                "resource.common.error.ControlNotEnabled");
            return null;
        }
    }
}
