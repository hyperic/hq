package org.hyperic.hq.ui.action.resource.group.control;

import java.util.ArrayList;
import java.util.List;

import org.apache.struts2.interceptor.validation.SkipValidation;
import org.hyperic.hq.ui.Portal;
import org.hyperic.hq.ui.action.resource.common.control.ResourceControlControllerNG;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;


@Component("groupControllerActionNG")
@Scope(value="prototype")
public class GroupControllerActionNG extends ResourceControlControllerNG {
    @SkipValidation
	public String currentControlStatus() throws Exception {
   
        List<String> portlets = new ArrayList<String>();
        Portal portal = new Portal();

        portlets.add("ng.resource.server.control.list.detail");
        portal.setName("resource.server.ControlSchedule.Title");
        portal.addPortlets(portlets);

        super.currentControlStatus( portal);

        return "current";
    }

    public String controlStatusHistory() throws Exception {
        List<String> portlets = new ArrayList<String>();
        Portal portal = new Portal();

        portlets.add(".resource.server.control.list.history");
        portal.setName("resource.server.ControlHistory.Title");
        portal.addPortlets(portlets);

        super.controlStatusHistory( portal);

        return "controlStatusHistory";
    }

    public String controlStatusHistoryDetail() throws Exception {
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

        super.controlStatusHistoryDetail( portal);

        return "controlStatusHistoryDetail";
    }

    public String newScheduledControlAction() throws Exception {
        Portal portal = Portal.createPortal("resource.server.Control.PageTitle.New", ".resource.server.control.new");
        portal.setDialog(true);

        super.newScheduledControlAction( portal);

        return "newScheduledControlAction";
    }

    public String editScheduledControlAction() throws Exception {
        Portal portal = Portal.createPortal("resource.server.Control.PageTitle.Edit", ".resource.server.control.edit");
        portal.setDialog(true);

        super.editScheduledControlAction(portal);

        return "editScheduledControlAction";
    }
}
