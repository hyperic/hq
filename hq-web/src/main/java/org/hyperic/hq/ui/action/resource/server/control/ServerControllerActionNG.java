/*
 * NOTE: This copyright does *not* cover user programs that use HQ
 * program services by normal system calls through the application
 * program interfaces provided as part of the Hyperic Plug-in Development
 * Kit or the Hyperic Client Development Kit - this is merely considered
 * normal use of the program, and does *not* fall under the heading of
 * "derived work".
 *
 * Copyright (C) [2004, 2005, 2006], Hyperic, Inc.
 * This file is part of HQ.
 *
 * HQ is free software; you can redistribute it and/or modify
 * it under the terms version 2 of the GNU General Public License as
 * published by the Free Software Foundation. This program is distributed
 * in the hope that it will be useful, but WITHOUT ANY WARRANTY; without
 * even the implied warranty of MERCHANTABILITY or FITNESS FOR A
 * PARTICULAR PURPOSE. See the GNU General Public License for more
 * details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307
 * USA.
 */

package org.hyperic.hq.ui.action.resource.server.control;

import java.util.ArrayList;
import java.util.List;

import org.apache.struts2.interceptor.validation.SkipValidation;
import org.hyperic.hq.ui.Portal;
import org.hyperic.hq.ui.action.resource.common.control.ResourceControlControllerNG;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

/**
 * The controller class for resource server control. This assembles the list
 * portlets for any given server control action.
 * 
 * Common functionality, such as common header data, can be setup and displayed
 * here.
 * 
 */
@Component("serverControllerActionNG")
@Scope(value="prototype")
public class ServerControllerActionNG
    extends ResourceControlControllerNG {

   

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