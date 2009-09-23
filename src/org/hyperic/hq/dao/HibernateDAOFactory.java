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
import org.hyperic.hibernate.Util;
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
import org.hyperic.hq.events.server.session.ActionDAO;
import org.hyperic.hq.events.server.session.AlertActionLogDAO;
import org.hyperic.hq.events.server.session.AlertConditionLogDAO;
import org.hyperic.hq.events.server.session.AlertDefinitionDAO;
import org.hyperic.hq.events.server.session.TriggerDAO;
import org.hyperic.hq.galerts.server.session.ExecutionStrategyTypeInfoDAO;

public class HibernateDAOFactory extends DAOFactory {

    private SessionFactory sessionFactory;

    private ActionDAO actionDAO;
    private AlertDefinitionDAO alertDefinitionDAO;
    private AgentReportStatusDAO agentReportStatusDAO;
    private ResourceGroupDAO resourceGroupDAO;
    private AIHistoryDAO aiHistoryDAO;
    private AIIpDAO aIpDAO;
    private AIPlatformDAO aiPlatformDAO;
    private AIScheduleDAO aiScheduleDAO;
    private AIServerDAO aiServerDAO;
    private AIServiceDAO aiServiceDAO;
    private AlertActionLogDAO alertActionLogDAO;
    private AlertConditionLogDAO alertConditionLogDAO;
    private AppServiceDAO appServiceDAO;
    private AppSvcDependencyDAO appDependencyDAO;
    private ConfigPropertyDAO configPropertyDAO;
    private ConfigResponseDAO configResponseDAO;
    private CpropKeyDAO cpropKeyDAO;
    private CrispoDAO crispoDAO;
    private PlatformDAO platformDAO;
    private ServerDAO serverDAO;
    private ServerTypeDAO serverTypeDAO;
    private ServiceDAO serviceDAO;
    private TriggerDAO triggerDAO;
    private ServiceTypeDAO serviceTypeDAO;
    private ResourceDAO resourceDAO;
    private ResourceTypeDAO resourceTypeDAO;
    private RoleDAO roleDAO;
    private VirtualDAO virtualDAO;
    private CrispoOptionDAO crispoOptionDAO;
    private ExecutionStrategyTypeInfoDAO executionStrategyTypeInfoDAO;

    public Session getCurrentSession() {
        return sessionFactory.getCurrentSession();
    }


    public ActionDAO getActionDAO() {
        return actionDAO;
    }

    public AlertDefinitionDAO getAlertDefDAO() {
        return alertDefinitionDAO;
    }

    public AgentReportStatusDAO getAgentReportStatusDAO() {
        return agentReportStatusDAO;
    }

    public AppServiceDAO getAppServiceDAO() {
        return appServiceDAO;
    }

    public AppSvcDependencyDAO getAppSvcDepencyDAO() {
        return appDependencyDAO;
    }

    public ConfigResponseDAO getConfigResponseDAO() {
        return configResponseDAO;
    }

    public CpropKeyDAO getCpropKeyDAO() {
        return cpropKeyDAO;
    }

    public PlatformDAO getPlatformDAO() {
        return platformDAO;
    }

    public ServerDAO getServerDAO() {
        return serverDAO;
    }

    public ServerTypeDAO getServerTypeDAO() {
        return serverTypeDAO;
    }

    public ServiceDAO getServiceDAO() {
        return serviceDAO;
    }

    public TriggerDAO getTriggerDAO() {
        return triggerDAO;
    }

    public ServiceTypeDAO getServiceTypeDAO() {
        return serviceTypeDAO;
    }

    public ResourceDAO getResourceDAO() {
        return resourceDAO;
    }

    public ResourceGroupDAO getResourceGroupDAO() {
        return resourceGroupDAO;
    }

    public ResourceTypeDAO getResourceTypeDAO() {
        return resourceTypeDAO;
    }

    public RoleDAO getRoleDAO() {
        return roleDAO;
    }

    public AIPlatformDAO getAIPlatformDAO() {
        return aiPlatformDAO;
    }

    public AIServerDAO getAIServerDAO() {
        return aiServerDAO;
    }

    public AIServiceDAO getAIServiceDAO() {
        return aiServiceDAO;
    }

    public AIIpDAO getAIIpDAO() {
        return aIpDAO;
    }

    public AIHistoryDAO getAIHistoryDAO() {
        return aiHistoryDAO;
    }

    public AIScheduleDAO getAIScheduleDAO() {
        return aiScheduleDAO;
    }

    public ConfigPropertyDAO getConfigPropertyDAO() {
        return configPropertyDAO;
    }

    public AlertActionLogDAO getAlertActionLogDAO() {
        return alertActionLogDAO;
    }

    public AlertConditionLogDAO getAlertConditionLogDAO() {
        return alertConditionLogDAO;
    }

    public VirtualDAO getVirtualDAO() {
        return virtualDAO;
    }

    public CrispoDAO getCrispoDAO() {
        return crispoDAO;
    }

    public CrispoOptionDAO getCrispoOptionDAO() {
        return crispoOptionDAO;
    }

    public ExecutionStrategyTypeInfoDAO getExecutionStrategyTypeInfoDAO() {
        return executionStrategyTypeInfoDAO;
    }
}
