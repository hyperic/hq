/*
 * NOTE: This copyright does *not* cover user programs that use HQ program
 * services by normal system calls through the application program interfaces
 * provided as part of the Hyperic Plug-in Development Kit or the Hyperic Client
 * Development Kit - this is merely considered normal use of the program, and
 * does *not* fall under the heading of "derived work". Copyright (C) [2004,
 * 2005, 2006], Hyperic, Inc. This file is part of HQ. HQ is free software; you
 * can redistribute it and/or modify it under the terms version 2 of the GNU
 * General Public License as published by the Free Software Foundation. This
 * program is distributed in the hope that it will be useful, but WITHOUT ANY
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR
 * A PARTICULAR PURPOSE. See the GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License along with
 * this program; if not, write to the Free Software Foundation, Inc., 59 Temple
 * Place, Suite 330, Boston, MA 02111-1307 USA.
 */

package org.hyperic.hq.ui.action.resource.service.control;

import java.util.List;
import java.util.ArrayList;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.hyperic.hq.bizapp.shared.AppdefBoss;
import org.hyperic.hq.bizapp.shared.AuthzBoss;
import org.hyperic.hq.bizapp.shared.ControlBoss;
import org.hyperic.hq.ui.Portal;
import org.hyperic.hq.ui.action.resource.common.control.ResourceControlController;
import org.springframework.beans.factory.annotation.Autowired;

import org.apache.struts.action.ActionForm;
import org.apache.struts.action.ActionForward;
import org.apache.struts.action.ActionMapping;

/**
 * The controller class for resource service control. This assembles the list
 * portlets for any given service control action.
 * 
 * Most of service control is actually handled by Action classes that are
 * located in server control, and presented by server control's jsp pages. I
 * love struts.
 */
public class ServiceControllerAction
    extends ResourceControlController {

    @Autowired
    public ServiceControllerAction(AppdefBoss appdefBoss, AuthzBoss authzBoss, ControlBoss controlBoss) {
        super(appdefBoss, authzBoss, controlBoss);
    }

    public ActionForward currentControlStatus(ActionMapping mapping, ActionForm form, HttpServletRequest request,
                                              HttpServletResponse response) throws Exception {
        List<String> portlets = new ArrayList<String>();
        Portal portal = new Portal();

        portlets.add(".resource.service.control.list.detail");
        portal.setName("resource.server.ControlSchedule.Title");
        portal.addPortlets(portlets);

        super.currentControlStatus(mapping, form, request, response, portal);

        return null;
    }

    public ActionForward controlStatusHistory(ActionMapping mapping, ActionForm form, HttpServletRequest request,
                                              HttpServletResponse response) throws Exception {
        List<String> portlets = new ArrayList<String>();
        Portal portal = new Portal();

        portlets.add(".resource.service.control.list.history");
        portal.setName("resource.server.ControlHistory.Title");
        portal.addPortlets(portlets);

        super.controlStatusHistory(mapping, form, request, response, portal);

        return null;
    }

    public ActionForward controlStatusHistoryDetail(ActionMapping mapping, ActionForm form, HttpServletRequest request,
                                                    HttpServletResponse response) throws Exception {
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

    public ActionForward newScheduledControlAction(ActionMapping mapping, ActionForm form, HttpServletRequest request,
                                                   HttpServletResponse response) throws Exception {
        Portal portal = Portal.createPortal("resource.server.Control.PageTitle.New", ".resource.service.control.new");
        portal.setDialog(true);

        super.newScheduledControlAction(mapping, form, request, response, portal);

        return null;
    }

    public ActionForward editScheduledControlAction(ActionMapping mapping, ActionForm form, HttpServletRequest request,
                                                    HttpServletResponse response) throws Exception {
        Portal portal = Portal.createPortal("resource.server.Control.PageTitle.Edit", ".resource.service.control.edit");
        portal.setDialog(true);

        super.editScheduledControlAction(mapping, form, request, response, portal);

        return null;
    }
}
