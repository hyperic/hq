package org.hyperic.hq.ui.action.resource.platform.control;

import java.util.List;
import java.util.ArrayList;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.struts.action.ActionForm;
import org.apache.struts.action.ActionForward;
import org.apache.struts.action.ActionMapping;
import org.hyperic.hq.ui.Portal;
import org.hyperic.hq.ui.action.resource.common.control.ResourceControlController;

public class PlatformControllerAction extends ResourceControlController {
    public ActionForward currentControlStatus(ActionMapping mapping,
                                              ActionForm form,
                                              HttpServletRequest request,
                                              HttpServletResponse response)
    throws Exception {
        List<String> portlets = new ArrayList<String>();
        Portal portal = new Portal();

        portlets.add(".resource.platform.control.list.detail");
        portal.setName("resource.server.ControlSchedule.Title");
        portal.addPortlets(portlets);

        super.currentControlStatus(mapping, form, request, response, portal);

        return null;
    }

    public ActionForward controlStatusHistory(ActionMapping mapping,
                                              ActionForm form,
                                              HttpServletRequest request,
                                              HttpServletResponse response) 
    throws Exception {
        List<String> portlets = new ArrayList<String>();
        Portal portal = new Portal();

        portlets.add(".resource.platform.control.list.history");
        portal.setName("resource.server.ControlHistory.Title");
        portal.addPortlets(portlets);

        super.controlStatusHistory(mapping, form, request, response, portal);

        return null;
    }

    public ActionForward controlStatusHistoryDetail(ActionMapping mapping,
                                                    ActionForm form,
                                                    HttpServletRequest request,
                                                    HttpServletResponse response) 
    throws Exception {
        List<String> portlets = new ArrayList<String>();
        Portal portal = new Portal();

        portlets.add(".page.title.resource.group");
        portlets.add(".resource.group.control.status.history.return");
        portlets.add(".resource.group.control.list.history.detail");
        portlets.add(".form.buttons.deleteCancel");
        portlets.add(".resource.group.control.status.history.return");
        portal.setName("resource.group.Control.PageTitle.New");
        portal.addPortlets(portlets);
        portal.setDialog(true);

        super.controlStatusHistoryDetail(mapping, form, request, response, portal);
        
        return null;
    }

    public ActionForward newScheduledControlAction(ActionMapping mapping,
                                                   ActionForm form,
                                                   HttpServletRequest request,
                                                   HttpServletResponse response) 
    throws Exception {
        Portal portal = Portal.createPortal("resource.server.Control.PageTitle.New", ".resource.platform.control.new");
        portal.setDialog(true);

        super.newScheduledControlAction(mapping, form, request, response, portal);
        
        return null;
    }

    public ActionForward editScheduledControlAction(ActionMapping mapping,
                                                    ActionForm form,
                                                    HttpServletRequest request,
                                                    HttpServletResponse response) 
    throws Exception {
        Portal portal = Portal.createPortal("resource.server.Control.PageTitle.Edit", ".resource.platform.control.edit");
        portal.setDialog(true);

        super.editScheduledControlAction(mapping, form, request, response, portal);
        
        return null;
    }
}