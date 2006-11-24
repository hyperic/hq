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
import org.hyperic.hq.appdef.shared.AppdefEntityID;
import org.hyperic.hq.bizapp.shared.ControlBoss;
import org.hyperic.hq.ui.action.BaseAction;
import org.hyperic.hq.ui.beans.OptionItem;
import org.hyperic.hq.ui.util.ContextUtils;
import org.hyperic.hq.ui.util.RequestUtils;

/**
 * An <code>Action</code> subclass that prepares to
 * create a control action associated
 * with a server.
 */
public class NewFormPrepareAction extends BaseAction {

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
        
        Log log = LogFactory.getLog(NewFormPrepareAction.class.getName());            
        log.trace("preparing new server control action" );                    

        int sessionId = RequestUtils.getSessionId(request).intValue();
        ControlForm cForm = (ControlForm) form;        
        ServletContext ctx = getServlet().getServletContext();
        ControlBoss cBoss =
            ContextUtils.getControlBoss(getServlet().getServletContext());

        AppdefEntityID appdefId = RequestUtils.getEntityId(request);

        List actions = cBoss.getActions(sessionId, appdefId);
        actions = OptionItem.createOptionsList(actions);
        cForm.setControlActions(actions);
        cForm.setNumControlActions(new Integer(actions.size()));

        return null;

    } 
}
