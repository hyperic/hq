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

package org.hyperic.dao;

import org.hibernate.Session;
import org.hyperic.hibernate.dao.ActionDAO;
import org.hyperic.hibernate.dao.AgentDAO;
import org.hyperic.hibernate.dao.AgentTypeDAO;
import org.hyperic.hibernate.dao.ApplicationDAO;
import org.hyperic.hibernate.dao.AuthzSubjectDAO;
import org.hyperic.hibernate.dao.BaselineDAO;
import org.hyperic.hibernate.dao.CategoryDAO;
import org.hyperic.hibernate.dao.ConfigResponseDAO;
import org.hyperic.hibernate.dao.CpropDAO;
import org.hyperic.hibernate.dao.CpropKeyDAO;
import org.hyperic.hibernate.dao.DerivedMeasurementDAO;
import org.hyperic.hibernate.dao.HibernateDAOFactory;
import org.hyperic.hibernate.dao.HibernateMockDAOFactory;
import org.hyperic.hibernate.dao.MeasurementArgDAO;
import org.hyperic.hibernate.dao.MeasurementTemplateDAO;
import org.hyperic.hibernate.dao.MetricProblemDAO;
import org.hyperic.hibernate.dao.MonitorableTypeDAO;
import org.hyperic.hibernate.dao.OperationDAO;
import org.hyperic.hibernate.dao.PlatformDAO;
import org.hyperic.hibernate.dao.PlatformTypeDAO;
import org.hyperic.hibernate.dao.RawMeasurementDAO;
import org.hyperic.hibernate.dao.ResourceDAO;
import org.hyperic.hibernate.dao.ResourceGroupDAO;
import org.hyperic.hibernate.dao.ResourceTypeDAO;
import org.hyperic.hibernate.dao.ScheduleRevNumDAO;
import org.hyperic.hibernate.dao.RoleDAO;
import org.hyperic.hibernate.dao.ServerDAO;
import org.hyperic.hibernate.dao.ServerTypeDAO;
import org.hyperic.hibernate.dao.ServiceDAO;
import org.hyperic.hibernate.dao.ServiceTypeDAO;
import org.hyperic.hibernate.dao.ServiceClusterDAO;
import org.hyperic.hibernate.dao.AppServiceDAO;
import org.hyperic.hibernate.dao.AppSvcDependencyDAO;
import org.hyperic.hibernate.dao.ApplicationTypeDAO;
import org.hyperic.hibernate.dao.PrincipalDAO;
import org.hyperic.hibernate.dao.AIPlatformDAO;
import org.hyperic.hibernate.dao.AIServerDAO;
import org.hyperic.hibernate.dao.AIServiceDAO;
import org.hyperic.hibernate.dao.AIIpDAO;
import org.hyperic.hibernate.dao.AIHistoryDAO;
import org.hyperic.hibernate.dao.AIScheduleDAO;
import org.hyperic.hibernate.dao.ConfigPropertyDAO;
import org.hyperic.hibernate.dao.PluginDAO;
import org.hyperic.hq.events.server.session.AlertActionLogDAO;
import org.hyperic.hq.events.server.session.AlertConditionDAO;
import org.hyperic.hq.events.server.session.AlertConditionLogDAO;
import org.hyperic.hq.events.server.session.AlertDAO;
import org.hyperic.hq.events.server.session.AlertDefinitionDAO;
import org.hyperic.hq.events.server.session.TriggerDAO;
import org.hyperic.hq.events.server.session.UserAlertDAO;

public abstract class DAOFactory
{
    public static final int HIBERNATE = 1;
    public static final int HIBERNATE_MOCKTEST = 2;

    protected static int DEFAULT = HIBERNATE;

    public abstract Session getCurrentSession();
    
    // Appdef DAOs
    public abstract AgentDAO getAgentDAO();
    public abstract AgentTypeDAO getAgentTypeDAO();
    public abstract ApplicationDAO getApplicationDAO();
    public abstract ApplicationTypeDAO getApplicationTypeDAO();
    public abstract AppServiceDAO getAppServiceDAO();
    public abstract AppSvcDependencyDAO getAppSvcDepencyDAO();
    public abstract ConfigResponseDAO getConfigResponseDAO();
    public abstract CpropDAO getCpropDAO();
    public abstract CpropKeyDAO getCpropKeyDAO();
    public abstract PlatformDAO getPlatformDAO();
    public abstract PlatformTypeDAO getPlatformTypeDAO();
    public abstract ServerDAO getServerDAO();
    public abstract ServerTypeDAO getServerTypeDAO();
    public abstract ServiceClusterDAO getServiceClusterDAO();
    public abstract ServiceDAO getServiceDAO();
    public abstract ServiceTypeDAO getServiceTypeDAO();

    // Autoinventory DAOs
    public abstract AIPlatformDAO getAIPlatformDAO();
    public abstract AIServerDAO getAIServerDAO();
    public abstract AIServiceDAO getAIServiceDAO();
    public abstract AIIpDAO getAIIpDAO();
    public abstract AIHistoryDAO getAIHistoryDAO();
    public abstract AIScheduleDAO getAIScheduleDAO();

    // Auth DAO
    public abstract PrincipalDAO getPrincipalDAO();

    // Event DAOs
    public abstract ActionDAO            getActionDAO();
    public abstract AlertDefinitionDAO   getAlertDefDAO();
    public abstract AlertConditionDAO    getAlertConditionDAO();
    public abstract TriggerDAO           getTriggerDAO();
    public abstract AlertActionLogDAO    getAlertActionLogDAO();
    public abstract AlertConditionLogDAO getAlertConditionLogDAO();
    public abstract AlertDAO             getAlertDAO();
    public abstract UserAlertDAO         getUserAlertDAO();
    
    // Common DAO
    public abstract ConfigPropertyDAO getConfigPropertyDAO();

    // Plugin DAO
    public abstract PluginDAO getPluginDAO();

    // Authz DAOs
    public abstract AuthzSubjectDAO  getAuthzSubjectDAO();
    public abstract OperationDAO     getOperationDAO();
    public abstract ResourceTypeDAO  getResourceTypeDAO();
    public abstract ResourceDAO      getResourceDAO();
    public abstract ResourceGroupDAO getResourceGroupDAO();
    public abstract RoleDAO          getRoleDAO();

    // Measurement DAOs
    public abstract BaselineDAO getBaselineDAO();
    public abstract CategoryDAO getCategoryDAO();
    public abstract MonitorableTypeDAO getMonitorableTypeDAO();
    public abstract RawMeasurementDAO getRawMeasurementDAO();
    public abstract DerivedMeasurementDAO getDerivedMeasurementDAO();
    public abstract MeasurementTemplateDAO getMeasurementTemplateDAO();
    public abstract MeasurementArgDAO getMeasurementArgDAO();
    public abstract MetricProblemDAO getMetricProblemDAO();
    public abstract ScheduleRevNumDAO getScheduleRevNumDAO();

    public static ThreadLocal defaultSession = new ThreadLocal();

    public static DAOFactory getDAOFactory()
    {
        return getDAOFactory(DEFAULT);
    }

    /**
     * @return mock hibernate factory suitable for use with mockejb
     */
    public static DAOFactory getMockDAOFactory(Session session)
    {
        HibernateMockDAOFactory factory =
            (HibernateMockDAOFactory)getDAOFactory(HIBERNATE_MOCKTEST);
        factory.setCurrentSession(session);
        return factory;
    }

    public static DAOFactory getDAOFactory(int which)
    {
        switch (which) {
        case HIBERNATE:
            return new HibernateDAOFactory();
        case HIBERNATE_MOCKTEST:
            HibernateMockDAOFactory factory = new HibernateMockDAOFactory();
            factory.setCurrentSession((Session)defaultSession.get());
            return factory;
        }
        throw new RuntimeException("DAOFactory type not found: " + which);
    }

    public static void setDefaultDAOFactory(int which)
    {
        DEFAULT = which;
    }

    public static void setMockSession(Session session)
    {
        defaultSession.set(session);
    }
}
