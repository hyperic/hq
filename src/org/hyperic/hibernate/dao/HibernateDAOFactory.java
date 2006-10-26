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
package org.hyperic.hibernate.dao;

import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hyperic.dao.DAOFactory;
import org.hyperic.hibernate.Util;
import org.hyperic.hq.events.server.session.AlertActionLogDAO;
import org.hyperic.hq.events.server.session.AlertConditionDAO;
import org.hyperic.hq.events.server.session.AlertConditionLogDAO;
import org.hyperic.hq.events.server.session.AlertDefinitionDAO;
import org.hyperic.hq.events.server.session.TriggerDAO;

public class HibernateDAOFactory extends DAOFactory {
    private static SessionFactory sessionFactory;

    public Session getCurrentSession() {
        // XXX:  DCL problem here.  Fix it if we encounter strange bugs later,
        //       since politically, we can't change this now.  *sigh*
        if (sessionFactory == null) {
            synchronized(this) {
                // Cache session factory, we cache it because hibernate 
                // session factor lookup is an rather expensive JNDI call
                if (sessionFactory == null) {
                    sessionFactory  = Util.getSessionFactory();
                }
            }
        }
        return sessionFactory.getCurrentSession();
    }

    public HibernateDAOFactory() {
    }

    public ActionDAO getActionDAO() {
        return new ActionDAO(getCurrentSession());
    }

    public AlertDefinitionDAO getAlertDefDAO() {
        return new AlertDefinitionDAO(getCurrentSession());
    }
    
    public AlertConditionDAO getAlertConditionDAO() {
        return new AlertConditionDAO(getCurrentSession());
    }

    public AgentDAO getAgentDAO() {
        return new AgentDAO(getCurrentSession());
    }

    public AgentTypeDAO getAgentTypeDAO() {
        return new AgentTypeDAO(getCurrentSession());
    }

    public ApplicationDAO getApplicationDAO() {
        return new ApplicationDAO(getCurrentSession());
    }

    public ApplicationTypeDAO getApplicationTypeDAO() {
        return new ApplicationTypeDAO(getCurrentSession());
    }

    public AppServiceDAO getAppServiceDAO() {
        return new AppServiceDAO(getCurrentSession());
    }

    public AppSvcDependencyDAO getAppSvcDepencyDAO() {
        return new AppSvcDependencyDAO(getCurrentSession());
    }

    public ConfigResponseDAO getConfigResponseDAO() {
        return new ConfigResponseDAO(getCurrentSession());
    }

    public CpropDAO getCpropDAO() {
        return new CpropDAO(getCurrentSession());
    }

    public CpropKeyDAO getCpropKeyDAO() {
        return new CpropKeyDAO(getCurrentSession());
    }

    public PlatformDAO getPlatformDAO() {
        return new PlatformDAO(getCurrentSession());
    }

    public PlatformTypeDAO getPlatformTypeDAO() {
        return new PlatformTypeDAO(getCurrentSession());
    }

    public ServerDAO getServerDAO() {
        return new ServerDAO(getCurrentSession());
    }

    public ServiceClusterDAO getServiceClusterDAO() {
        return new ServiceClusterDAO(getCurrentSession());
    }

    public ServerTypeDAO getServerTypeDAO() {
        return new ServerTypeDAO(getCurrentSession());
    }

    public ServiceDAO getServiceDAO() {
        return new ServiceDAO(getCurrentSession());
    }

    public TriggerDAO getTriggerDAO() {
        return new TriggerDAO(getCurrentSession());
    }

    public ServiceTypeDAO getServiceTypeDAO() {
        return new ServiceTypeDAO(getCurrentSession());
    }

    public AuthzSubjectDAO getAuthzSubjectDAO() {
        return new AuthzSubjectDAO(getCurrentSession());
    }

    public BaselineDAO getBaselineDAO() {
        return new BaselineDAO(getCurrentSession());
    }

    public CategoryDAO getCategoryDAO() {
        return new CategoryDAO(getCurrentSession());
    }

    public PrincipalDAO getPrincipalDAO() {
        return new PrincipalDAO(getCurrentSession());
    }

    public ResourceDAO getResourceDAO() {
        return new ResourceDAO(getCurrentSession());
    }

    public ResourceGroupDAO getResourceGroupDAO() {
        return new ResourceGroupDAO(getCurrentSession());
    }

    public ResourceTypeDAO getResourceTypeDAO() {
        return new ResourceTypeDAO(getCurrentSession());
    }

    public RoleDAO getRoleDAO() {
        return new RoleDAO(getCurrentSession());
    }

    public OperationDAO getOperationDAO() {
        return new OperationDAO(getCurrentSession());
    }

    public MonitorableTypeDAO getMonitorableTypeDAO() {
        return new MonitorableTypeDAO(getCurrentSession());
    }
    
    public RawMeasurementDAO getRawMeasurementDAO() {
        return new RawMeasurementDAO(getCurrentSession());
    }

    public DerivedMeasurementDAO getDerivedMeasurementDAO() {
        return new DerivedMeasurementDAO(getCurrentSession());
    }

    public MeasurementTemplateDAO getMeasurementTemplateDAO() {
        return new MeasurementTemplateDAO(getCurrentSession());
    }

    public MeasurementArgDAO getMeasurementArgDAO() {
        return new MeasurementArgDAO(getCurrentSession());
    }

    public MetricProblemDAO getMetricProblemDAO() {
        return new MetricProblemDAO(getCurrentSession());
    }

    public ScheduleRevNumDAO getScheduleRevNumDAO() {
        return new ScheduleRevNumDAO(getCurrentSession());
    }

    public AIPlatformDAO getAIPlatformDAO() {
        return new AIPlatformDAO(getCurrentSession());
    }

    public AIServerDAO getAIServerDAO() {
        return new AIServerDAO(getCurrentSession());
    }

    public AIServiceDAO getAIServiceDAO() {
        return new AIServiceDAO(getCurrentSession());
    }

    public AIIpDAO getAIIpDAO() {
        return new AIIpDAO(getCurrentSession());
    }

    public AIHistoryDAO getAIHistoryDAO() {
        return new AIHistoryDAO(getCurrentSession());
    }

    public AIScheduleDAO getAIScheduleDAO() {
        return new AIScheduleDAO(getCurrentSession());
    }

    public ConfigPropertyDAO getConfigPropertyDAO() {
        return new ConfigPropertyDAO(getCurrentSession());
    }

    public PluginDAO getPluginDAO() {
        return new PluginDAO(getCurrentSession());
    }
    
    public AlertActionLogDAO getAlertActionLogDAO() {
        return new AlertActionLogDAO(getCurrentSession());
    }
    
    public AlertConditionLogDAO getAlertConditionLogDAO() {
        return new AlertConditionLogDAO(getCurrentSession());
    }
}

