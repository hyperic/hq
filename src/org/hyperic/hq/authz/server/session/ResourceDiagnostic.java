/*
 * NOTE: This copyright does *not* cover user programs that use HQ program
 * services by normal system calls through the application program interfaces
 * provided as part of the Hyperic Plug-in Development Kit or the Hyperic Client
 * Development Kit - this is merely considered normal use of the program, and
 * does *not* fall under the heading of "derived work".
 * 
 * Copyright (C) [2004-2008], Hyperic, Inc. This file is part of HQ.
 * 
 * HQ is free software; you can redistribute it and/or modify it under the terms
 * version 2 of the GNU General Public License as published by the Free Software
 * Foundation. This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General
 * Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License along with
 * this program; if not, write to the Free Software Foundation, Inc., 59 Temple
 * Place, Suite 330, Boston, MA 02111-1307 USA.
 */

package org.hyperic.hq.authz.server.session;

import java.text.DateFormat;
import java.util.List;

import org.hibernate.Session;
import org.hyperic.hq.appdef.server.session.AgentManagerEJBImpl;
import org.hyperic.hq.appdef.server.session.ApplicationManagerEJBImpl;
import org.hyperic.hq.appdef.server.session.PlatformManagerEJBImpl;
import org.hyperic.hq.appdef.server.session.ServerManagerEJBImpl;
import org.hyperic.hq.appdef.server.session.ServiceManagerEJBImpl;
import org.hyperic.hq.escalation.server.session.EscalationManagerEJBImpl;
import org.hyperic.hq.events.server.session.AlertManagerEJBImpl;

import org.hyperic.hq.appdef.shared.AgentManagerLocal;
import org.hyperic.hq.appdef.shared.ApplicationManagerLocal;
import org.hyperic.hq.appdef.shared.PlatformManagerLocal;
import org.hyperic.hq.appdef.shared.ServerManagerLocal;
import org.hyperic.hq.appdef.shared.ServiceManagerLocal;
import org.hyperic.hq.escalation.shared.EscalationManagerLocal;
import org.hyperic.hq.authz.shared.ResourceManagerLocal;
import org.hyperic.hq.authz.shared.ResourceGroupManagerLocal;
import org.hyperic.hq.authz.shared.RoleManagerLocal;
import org.hyperic.hq.events.shared.AlertManagerLocal;

import org.hyperic.hq.common.DiagnosticObject;
import org.hyperic.hq.dao.HibernateDAOFactory;

import org.hyperic.util.PrintfFormat;

/**
 * A diagnostic object for event tracker operations.
 */
class ResourceDiagnostic implements DiagnosticObject {

    private static final ResourceDiagnostic INSTANCE = new ResourceDiagnostic();

    /**
     * @return The singleton instance.
     */
    public static ResourceDiagnostic getInstance() {
        return INSTANCE;
    }

    /**
     * Private constructor for a singleton.
     */
    private ResourceDiagnostic() {
    }

    /**
     * @see org.hyperic.hq.common.DiagnosticObject#getName()
     */
    public String getName() {
        return "Resource Stats";
    }

    /**
     * @see org.hyperic.hq.common.DiagnosticObject#getShortName()
     */
    public String getShortName() {
        return "ResourceStats";
    }

    /**
     * @see org.hyperic.hq.common.DiagnosticObject#getStatus()
     */
    public String getStatus() {

        StringBuffer rslt = new StringBuffer();

        rslt.append("Resource Report").append(
                "\n     Platform Count:     " + getPlatformCount()).append(
                "\n     CPU Count:     " + getCPUCount()).append(
                "\n     Agent Count:     " + getAgentCount()).append(
                "\n     Active Agent Count:     " + getAgentCountUsed()).append(
                "\n     Server Count:     " + getServerCount()).append(
                "\n     Service Count:     " + getServiceCount()).append(
                "\n     Application Count:     " + getApplicationCount()).append(
                "\n     Role Count:     " + getRoleCount()).append(
                "\n     User Count:     " + getUserCount()).append(
                "\n     Alert Count:     " + getAlertCount()).append(
                "\n     Resource Count:     " + getResourceCount()).append(
                "\n     ResourceType Count:     " + getResourceTypeCount())
                .append("\n     Group Count:     " + getGroupCount())
                .append("\n     Escalations Count:     " + getEscalationCount())
                .append("\n     Active Escalations Count:     " + getActiveEscalationCount())
                .append("\n");

        return rslt.toString();
    }

    private int getPlatformCount() {
        PlatformManagerLocal pm = PlatformManagerEJBImpl.getOne();
        return pm.getPlatformCount().intValue();
    }

    private int getCPUCount() {
        PlatformManagerLocal pm = PlatformManagerEJBImpl.getOne();
        return pm.getCpuCount().intValue();
    }

    private int getAgentCount() {
        AgentManagerLocal am = AgentManagerEJBImpl.getOne();
        return am.getAgentCount();
    }

    private int getAgentCountUsed() {
        AgentManagerLocal am = AgentManagerEJBImpl.getOne();
        return am.getAgentCountUsed();
    }

    private int getServerCount() {
        ServerManagerLocal sm = ServerManagerEJBImpl.getOne();
        return sm.getServerCount().intValue();
    }

    private int getServiceCount() {
        ServiceManagerLocal sm = ServiceManagerEJBImpl.getOne();
        return sm.getServiceCount().intValue();
    }

    private int getApplicationCount() {
        ApplicationManagerLocal am = ApplicationManagerEJBImpl.getOne();
        return am.getApplicationCount().intValue();
    }

    private int getRoleCount() {
        RoleManagerLocal rm = RoleManagerEJBImpl.getOne();
        return rm.getRoleCount().intValue();
    }

    private int getUserCount() {
        RoleManagerLocal rm = RoleManagerEJBImpl.getOne();
        return rm.getSubjectCount().intValue();
    }

    private int getAlertCount() {
        AlertManagerLocal am = AlertManagerEJBImpl.getOne();
        return am.getAlertCount().intValue();
    }

    private int getResourceCount() {
        ResourceManagerLocal rm = ResourceManagerEJBImpl.getOne();
        return rm.getResourceCount().intValue();
    }

    private int getResourceTypeCount() {
        ResourceManagerLocal rm = ResourceManagerEJBImpl.getOne();
        return rm.getResourceTypeCount().intValue();
    }

    private int getGroupCount() {
        ResourceGroupManagerLocal rm = ResourceGroupManagerEJBImpl.getOne();
        return rm.getGroupCount().intValue();
    }

    private int getActiveEscalationCount() {
        EscalationManagerLocal sm = EscalationManagerEJBImpl.getOne();
        return sm.getActiveEscalationCount().intValue();
    }
    
    private int getEscalationCount() {
        EscalationManagerLocal sm = EscalationManagerEJBImpl.getOne();
        return sm.getEscalationCount().intValue();
    }
}
