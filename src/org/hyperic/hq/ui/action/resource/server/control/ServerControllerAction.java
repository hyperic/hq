package org.hyperic.hq.ui.action.resource.server.control;

import java.util.ArrayList;
import java.util.Properties;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.hyperic.hq.ui.Constants;
import org.hyperic.hq.ui.Portal;
import org.hyperic.hq.ui.action.resource.common.control.ResourceControlController;
import org.hyperic.hq.ui.util.SessionUtils;

import org.apache.struts.Globals;
import org.apache.struts.action.ActionForm;
import org.apache.struts.action.ActionForward;
import org.apache.struts.action.ActionMapping;

/**
 * The controller class for resource server control.
 * This assembles the list portlets for any given 
 * server control action.
 *
 * Common functionality, such as common header data,
 * can be setup and displayed here.
 *
 */
public class ServerControllerAction extends ResourceControlController {
    
    /**  Provides the mapping from resource key to method name
     * @return          Resource key / method name map
     */
    private static Properties keyMethodMap = new Properties();
    static {
        keyMethodMap.setProperty(
            Constants.MODE_LIST,       "currentControlStatus");
        keyMethodMap.setProperty(
            Constants.MODE_VIEW,       "currentControlStatus"); 
        /*keyMethodMap.setProperty(
         *  Constants.MODE_CRNT_DETAIL, "currentControlStatusDetail"); */
        keyMethodMap.setProperty(
            Constants.MODE_HST,        "controlStatusHistory");
        keyMethodMap.setProperty(
            Constants.MODE_HST_DETAIL, "controlStatusHistory"); 
        keyMethodMap.setProperty(
            Constants.MODE_NEW,        "newScheduledControlAction");
        keyMethodMap.setProperty(
            Constants.MODE_EDIT,       "editScheduledControlAction");
    }
    
    protected Properties getKeyMethodMap() {
        return keyMethodMap;
    }
    
    /** move == view */
    public ActionForward currentControlStatus(ActionMapping mapping,
                                              ActionForm form,
                                              HttpServletRequest request,
                                              HttpServletResponse response)
        throws Exception {

        ArrayList portlets = new ArrayList();
        Portal portal = new Portal();
        
        portlets.add( ".resource.server.control.list.detail" );
        portal.setName("resource.server.ControlSchedule.Title");
        
        portal.addPortlets(portlets);
        request.setAttribute(Constants.PORTAL_KEY, portal);
        
        // move messages and errors from session to request scope
        SessionUtils.moveAttribute(request, Globals.MESSAGE_KEY);
        SessionUtils.moveAttribute(request, Globals.ERROR_KEY);
        
        setResource(request);
        checkControlEnabled(mapping, form, request, response);
        
        super.currentControlStatus(mapping,form,request,response);
        return null;
    }
    
    public ActionForward controlStatusHistory(ActionMapping mapping,
                                              ActionForm form,
                                              HttpServletRequest request,
                                              HttpServletResponse response)
        throws Exception {

        ArrayList portlets = new ArrayList();
        Portal portal = new Portal();
        
        portlets.add( ".resource.server.control.list.history" );
        portal.setName("resource.server.ControlHistory.Title");
        
        portal.addPortlets(portlets);
        request.setAttribute(Constants.PORTAL_KEY, portal);
        
        setResource(request);
        checkControlEnabled(mapping, form, request, response);
        
        super.controlStatusHistory(mapping,form,request,response);
        return null;
    }
    
    public ActionForward controlStatusHistoryDetail(ActionMapping mapping,
                                                    ActionForm form,
                                                    HttpServletRequest request,
                                                    HttpServletResponse response)
        throws Exception {

        ArrayList portlets = new ArrayList();
        Portal portal = new Portal();
        
        setResource(request);
        checkControlEnabled(mapping, form, request, response);
        
        portlets.add( ".page.title.resource.group" );
        portlets.add( ".resource.group.control.status.history.return" );
        portlets.add( ".resource.group.control.list.history.detail" );
        portlets.add( ".form.buttons.deleteCancel" );
        portlets.add( ".resource.group.control.status.history.return" );
        
        portal.setName( "resource.group.Control.PageTitle.New" );
        
        portal.addPortlets(portlets);
        portal.setDialog(true);
        
        request.setAttribute(Constants.PORTAL_KEY, portal);
        
        return null;
    }
    
    public ActionForward newScheduledControlAction(ActionMapping mapping,
                                                   ActionForm form,
                                                   HttpServletRequest request,
                                                   HttpServletResponse response)
        throws Exception {
        
        Portal portal = Portal.createPortal("resource.server.Control.PageTitle.New",
                                            ".resource.server.control.new" );
        portal.setDialog(true);
        request.setAttribute(Constants.PORTAL_KEY, portal);
        setResource(request);
        return null;
    }
    
    public ActionForward editScheduledControlAction(ActionMapping mapping,
                                                    ActionForm form,
                                                    HttpServletRequest request,
                                                    HttpServletResponse response)
        throws Exception {

        Portal portal = Portal.createPortal("resource.server.Control.PageTitle.Edit",
                                            ".resource.server.control.edit");
        portal.setDialog(true);
        request.setAttribute(Constants.PORTAL_KEY, portal);
        setResource(request);
        return null;
    }
}
