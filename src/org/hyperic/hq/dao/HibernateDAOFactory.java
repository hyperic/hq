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
package org.hyperic.hq.dao;

import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hyperic.dao.DAOFactory;
import org.hyperic.hibernate.Util;
import org.hyperic.hq.appdef.server.session.ApplicationDAO;
import org.hyperic.hq.appdef.server.session.PlatformDAO;
import org.hyperic.hq.appdef.server.session.ServerDAO;
import org.hyperic.hq.appdef.server.session.ServerTypeDAO;
import org.hyperic.hq.appdef.server.session.ServiceDAO;
import org.hyperic.hq.appdef.server.session.ServiceTypeDAO;
import org.hyperic.hq.appdef.server.session.CpropDAO;
import org.hyperic.hq.appdef.server.session.CpropKeyDAO;
import org.hyperic.hq.appdef.server.session.VirtualDAO;
import org.hyperic.hq.authz.server.session.OperationDAO;
import org.hyperic.hq.authz.server.session.ResourceDAO;
import org.hyperic.hq.authz.server.session.ResourceGroupDAO;
import org.hyperic.hq.authz.server.session.ResourceTypeDAO;
import org.hyperic.hq.authz.server.session.RoleDAO;
import org.hyperic.hq.common.server.session.CrispoDAO;
import org.hyperic.hq.common.server.session.CrispoOptionDAO;
import org.hyperic.hq.control.server.session.ControlHistoryDAO;
import org.hyperic.hq.control.server.session.ControlScheduleDAO;
import org.hyperic.hq.events.server.session.ActionDAO;
import org.hyperic.hq.events.server.session.AlertActionLogDAO;
import org.hyperic.hq.events.server.session.AlertConditionDAO;
import org.hyperic.hq.events.server.session.AlertConditionLogDAO;
import org.hyperic.hq.events.server.session.AlertDAO;
import org.hyperic.hq.events.server.session.AlertDefinitionDAO;
import org.hyperic.hq.events.server.session.EventLogDAO;
import org.hyperic.hq.events.server.session.TriggerDAO;
import org.hyperic.hq.galerts.server.session.ExecutionStrategyTypeInfoDAO;
import org.hyperic.hq.measurement.server.session.AvailabilityDataDAO;
import org.hyperic.hq.measurement.server.session.MeasurementDAO;
import org.hyperic.hq.measurement.server.session.CategoryDAO;
import org.hyperic.hq.measurement.server.session.ScheduleRevNumDAO;
import org.hyperic.hq.measurement.server.session.MeasurementTemplateDAO;
import org.hyperic.hq.measurement.server.session.MonitorableTypeDAO;
import org.hyperic.hq.measurement.server.session.MetricProblemDAO;

public class HibernateDAOFactory extends DAOFactory {
    private static final SessionFactory sessionFactory = 
        Util.getSessionFactory();
    private static final HibernateDAOFactory singleton = 
        new HibernateDAOFactory();

    public static HibernateDAOFactory getInstance() {
        return singleton;
    }

    public Session getCurrentSession() {
        return getSessionFactory().getCurrentSession();
    }

    public SessionFactory getSessionFactory() {
        if (sessionFactory == null) {
            throw new IllegalStateException("SessionFactory not initialized");
        }
        return sessionFactory;
    }

    protected HibernateDAOFactory () {}

    public ActionDAO getActionDAO() {
        return new ActionDAO(this);
    }

    public AlertDefinitionDAO getAlertDefDAO() {
        return new AlertDefinitionDAO(this);
    }
    
    public AlertConditionDAO getAlertConditionDAO() {
        return new AlertConditionDAO(this);
    }

    public AgentDAO getAgentDAO() {
        return new AgentDAO(this);
    }

    public AgentTypeDAO getAgentTypeDAO() {
        return new AgentTypeDAO(this);
    }

    public ApplicationDAO getApplicationDAO() {
        return new ApplicationDAO(this);
    }

    public ApplicationTypeDAO getApplicationTypeDAO() {
        return new ApplicationTypeDAO(this);
    }

    public AppServiceDAO getAppServiceDAO() {
        return new AppServiceDAO(this);
    }

    public AppSvcDependencyDAO getAppSvcDepencyDAO() {
        return new AppSvcDependencyDAO(this);
    }

    public AvailabilityDataDAO getAvailabilityDataDAO() {
        return new AvailabilityDataDAO(this);
    }

    public ConfigResponseDAO getConfigResponseDAO() {
        return new ConfigResponseDAO(this);
    }

    public CpropDAO getCpropDAO() {
        return new CpropDAO(this);
    }

    public CpropKeyDAO getCpropKeyDAO() {
        return new CpropKeyDAO(this);
    }

    public PlatformDAO getPlatformDAO() {
        return new PlatformDAO(this);
    }

    public PlatformTypeDAO getPlatformTypeDAO() {
        return new PlatformTypeDAO(this);
    }

    public ServerDAO getServerDAO() {
        return new ServerDAO(this);
    }

    public ServerTypeDAO getServerTypeDAO() {
        return new ServerTypeDAO(this);
    }

    public ServiceDAO getServiceDAO() {
        return new ServiceDAO(this);
    }

    public TriggerDAO getTriggerDAO() {
        return new TriggerDAO(this);
    }

    public ServiceTypeDAO getServiceTypeDAO() {
        return new ServiceTypeDAO(this);
    }

    public CategoryDAO getCategoryDAO() {
        return new CategoryDAO(this);
    }

    public PrincipalDAO getPrincipalDAO() {
        return new PrincipalDAO(this);
    }

    public ResourceDAO getResourceDAO() {
        return new ResourceDAO(this);
    }

    public ResourceGroupDAO getResourceGroupDAO() {
        return new ResourceGroupDAO(this);
    }

    public ResourceTypeDAO getResourceTypeDAO() {
        return new ResourceTypeDAO(this);
    }

    public RoleDAO getRoleDAO() {
        return new RoleDAO(this);
    }

    public OperationDAO getOperationDAO() {
        return new OperationDAO(this);
    }

    public MonitorableTypeDAO getMonitorableTypeDAO() {
        return new MonitorableTypeDAO(this);
    }

    public MeasurementDAO getMeasurementDAO() {
        return new MeasurementDAO(this);
    }

    public MeasurementTemplateDAO getMeasurementTemplateDAO() {
        return new MeasurementTemplateDAO(this);
    }
    
    public MetricProblemDAO getMetricProblemDAO() {
        return new MetricProblemDAO(this);
    }

    public ScheduleRevNumDAO getScheduleRevNumDAO() {
        return new ScheduleRevNumDAO(this);
    }

    public AIPlatformDAO getAIPlatformDAO() {
        return new AIPlatformDAO(this);
    }

    public AIServerDAO getAIServerDAO() {
        return new AIServerDAO(this);
    }

    public AIServiceDAO getAIServiceDAO() {
        return new AIServiceDAO(this);
    }

    public AIIpDAO getAIIpDAO() {
        return new AIIpDAO(this);
    }

    public AIHistoryDAO getAIHistoryDAO() {
        return new AIHistoryDAO(this);
    }

    public AIScheduleDAO getAIScheduleDAO() {
        return new AIScheduleDAO(this);
    }

    public ConfigPropertyDAO getConfigPropertyDAO() {
        return new ConfigPropertyDAO(this);
    }

    public PluginDAO getPluginDAO() {
        return new PluginDAO(this);
    }

    public AlertActionLogDAO getAlertActionLogDAO() {
        return new AlertActionLogDAO(this);
    }

    public AlertConditionLogDAO getAlertConditionLogDAO() {
        return new AlertConditionLogDAO(this);
    }

    public AlertDAO getAlertDAO() {
        return new AlertDAO(this);
    }

    public VirtualDAO getVirtualDAO() {
        return new VirtualDAO(this);
    }

    public CrispoDAO getCrispoDAO() {
        return new CrispoDAO(this);
    }

    public CrispoOptionDAO getCrispoOptionDAO() {
        return new CrispoOptionDAO(this);
    }

    public EventLogDAO getEventLogDAO() {
        return new EventLogDAO(this);
    }

    public ControlHistoryDAO getControlHistoryDAO() {
        return new ControlHistoryDAO(this);
    }

    public ControlScheduleDAO getControlScheduleDAO() {
        return new ControlScheduleDAO(this);
    }

    public ExecutionStrategyTypeInfoDAO getExecutionStrategyTypeInfoDAO() {
        return new ExecutionStrategyTypeInfoDAO(this);
    }
}
