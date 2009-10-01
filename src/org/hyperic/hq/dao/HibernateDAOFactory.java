/*
 * NOTE: This copyright does *not* cover user programs that use HQ
 * program services by normal system calls through the application
 * program interfaces provided as part of the Hyperic Plug-in Development
 * Kit or the Hyperic Client Development Kit - this is merely considered
 * normal use of the program, and does *not* fall under the heading of
 * "derived work".
 *
 * Copyright (C) [2004-2008], Hyperic, Inc.
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
package org.hyperic.hq.dao;

import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hyperic.dao.DAOFactory;
import org.hyperic.hq.appdef.server.session.AppServiceDAO;
import org.hyperic.hq.appdef.server.session.AppSvcDependencyDAO;
import org.hyperic.hq.appdef.server.session.ConfigResponseDAO;
import org.hyperic.hq.appdef.server.session.CpropKeyDAO;
import org.hyperic.hq.appdef.server.session.PlatformDAO;
import org.hyperic.hq.appdef.server.session.ServerDAO;
import org.hyperic.hq.appdef.server.session.ServerTypeDAO;
import org.hyperic.hq.appdef.server.session.ServiceDAO;
import org.hyperic.hq.appdef.server.session.ServiceTypeDAO;
import org.hyperic.hq.appdef.server.session.VirtualDAO;
import org.hyperic.hq.authz.server.session.ResourceDAO;
import org.hyperic.hq.authz.server.session.ResourceGroupDAO;
import org.hyperic.hq.authz.server.session.ResourceTypeDAO;
import org.hyperic.hq.authz.server.session.RoleDAO;
import org.hyperic.hq.autoinventory.server.session.AgentReportStatusDAO;
import org.hyperic.hq.common.server.session.ConfigPropertyDAO;
import org.hyperic.hq.common.server.session.CrispoDAO;
import org.hyperic.hq.common.server.session.CrispoOptionDAO;
import org.hyperic.hq.context.Bootstrap;
import org.hyperic.hq.events.server.session.ActionDAO;
import org.hyperic.hq.events.server.session.AlertActionLogDAO;
import org.hyperic.hq.events.server.session.AlertConditionLogDAO;
import org.hyperic.hq.events.server.session.AlertDefinitionDAO;
import org.hyperic.hq.events.server.session.TriggerDAO;
import org.hyperic.hq.galerts.server.session.ExecutionStrategyTypeInfoDAO;

public class HibernateDAOFactory extends DAOFactory {

    private SessionFactory sessionFactory = Bootstrap.getBean(SessionFactory.class);
    

    public Session getCurrentSession() {
        return sessionFactory.getCurrentSession();
    }


    public ActionDAO getActionDAO() {
        return Bootstrap.getBean(ActionDAO.class);
    }

    public AlertDefinitionDAO getAlertDefDAO() {
        return Bootstrap.getBean(AlertDefinitionDAO.class);
    }

    public AgentReportStatusDAO getAgentReportStatusDAO() {
        return Bootstrap.getBean(AgentReportStatusDAO.class);
    }

    public AppServiceDAO getAppServiceDAO() {
        return Bootstrap.getBean(AppServiceDAO.class);
    }

    public AppSvcDependencyDAO getAppSvcDepencyDAO() {
        return Bootstrap.getBean(AppSvcDependencyDAO.class);
    }

    public ConfigResponseDAO getConfigResponseDAO() {
        return Bootstrap.getBean(ConfigResponseDAO.class);
    }

    public CpropKeyDAO getCpropKeyDAO() {
        return Bootstrap.getBean(CpropKeyDAO .class);
    }

    public PlatformDAO getPlatformDAO() {
        return Bootstrap.getBean(PlatformDAO.class);
    }

    public ServerDAO getServerDAO() {
        return Bootstrap.getBean( ServerDAO.class);
    }

    public ServerTypeDAO getServerTypeDAO() {
        return Bootstrap.getBean(ServerTypeDAO.class);
    }

    public ServiceDAO getServiceDAO() {
        return Bootstrap.getBean(ServiceDAO.class);
    }

    public TriggerDAO getTriggerDAO() {
        return Bootstrap.getBean(TriggerDAO.class);
    }

    public ServiceTypeDAO getServiceTypeDAO() {
        return Bootstrap.getBean(ServiceTypeDAO.class);
    }

    public ResourceDAO getResourceDAO() {
        return Bootstrap.getBean(ResourceDAO.class);
    }

    public ResourceGroupDAO getResourceGroupDAO() {
        return Bootstrap.getBean(ResourceGroupDAO.class);
    }

    public ResourceTypeDAO getResourceTypeDAO() {
        return Bootstrap.getBean(ResourceTypeDAO.class);
    }

    public RoleDAO getRoleDAO() {
        return Bootstrap.getBean(RoleDAO.class);
    }

    public AIPlatformDAO getAIPlatformDAO() {
        return Bootstrap.getBean(AIPlatformDAO.class);
    }

    public AIServerDAO getAIServerDAO() {
        return Bootstrap.getBean(AIServerDAO.class);
    }

    public AIServiceDAO getAIServiceDAO() {
        return Bootstrap.getBean(AIServiceDAO.class);
    }

    public AIIpDAO getAIIpDAO() {
        return Bootstrap.getBean(AIIpDAO.class);
    }

    public AIHistoryDAO getAIHistoryDAO() {
        return Bootstrap.getBean(AIHistoryDAO.class);
    }

    public AIScheduleDAO getAIScheduleDAO() {
        return Bootstrap.getBean(AIScheduleDAO.class);
    }

    public ConfigPropertyDAO getConfigPropertyDAO() {
        return Bootstrap.getBean(ConfigPropertyDAO.class);
    }

    public AlertActionLogDAO getAlertActionLogDAO() {
        return Bootstrap.getBean(AlertActionLogDAO.class);
    }

    public AlertConditionLogDAO getAlertConditionLogDAO() {
        return Bootstrap.getBean(AlertConditionLogDAO .class);
    }

    public VirtualDAO getVirtualDAO() {
        return Bootstrap.getBean(VirtualDAO.class);
    }

    public CrispoDAO getCrispoDAO() {
        return Bootstrap.getBean(CrispoDAO.class);
    }

    public CrispoOptionDAO getCrispoOptionDAO() {
        return Bootstrap.getBean(CrispoOptionDAO.class);
    }

    public ExecutionStrategyTypeInfoDAO getExecutionStrategyTypeInfoDAO() {
        return Bootstrap.getBean(ExecutionStrategyTypeInfoDAO.class);
    }
}
