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

import java.util.HashMap;

import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hyperic.dao.DAOFactory;
import org.hyperic.hibernate.Util;
import org.hyperic.hq.appdef.Agent;
import org.hyperic.hq.appdef.AgentType;
import org.hyperic.hq.appdef.AppService;
import org.hyperic.hq.appdef.AppSvcDependency;
import org.hyperic.hq.appdef.ConfigResponseDB;
import org.hyperic.hq.appdef.Cprop;
import org.hyperic.hq.appdef.CpropKey;
import org.hyperic.hq.appdef.ServiceCluster;
import org.hyperic.hq.appdef.server.session.Application;
import org.hyperic.hq.appdef.server.session.ApplicationType;
import org.hyperic.hq.appdef.server.session.Platform;
import org.hyperic.hq.appdef.server.session.PlatformType;
import org.hyperic.hq.appdef.server.session.Server;
import org.hyperic.hq.appdef.server.session.ServerType;
import org.hyperic.hq.appdef.server.session.Service;
import org.hyperic.hq.appdef.server.session.ServiceType;
import org.hyperic.hq.auth.Principal;
import org.hyperic.hq.authz.server.session.AuthzSubject;
import org.hyperic.hq.authz.server.session.AuthzSubjectDAO;
import org.hyperic.hq.authz.server.session.Operation;
import org.hyperic.hq.authz.server.session.OperationDAO;
import org.hyperic.hq.authz.server.session.Resource;
import org.hyperic.hq.authz.server.session.ResourceDAO;
import org.hyperic.hq.authz.server.session.ResourceGroup;
import org.hyperic.hq.authz.server.session.ResourceGroupDAO;
import org.hyperic.hq.authz.server.session.ResourceType;
import org.hyperic.hq.authz.server.session.ResourceTypeDAO;
import org.hyperic.hq.authz.server.session.Role;
import org.hyperic.hq.authz.server.session.Virtual;
import org.hyperic.hq.autoinventory.AIHistory;
import org.hyperic.hq.autoinventory.AIIp;
import org.hyperic.hq.autoinventory.AIPlatform;
import org.hyperic.hq.autoinventory.AISchedule;
import org.hyperic.hq.autoinventory.AIServer;
import org.hyperic.hq.autoinventory.AIService;
import org.hyperic.hq.common.ConfigProperty;
import org.hyperic.hq.common.server.session.Crispo;
import org.hyperic.hq.common.server.session.CrispoDAO;
import org.hyperic.hq.control.server.session.ControlHistory;
import org.hyperic.hq.control.server.session.ControlHistoryDAO;
import org.hyperic.hq.control.server.session.ControlSchedule;
import org.hyperic.hq.control.server.session.ControlScheduleDAO;
import org.hyperic.hq.events.server.session.Action;
import org.hyperic.hq.events.server.session.ActionDAO;
import org.hyperic.hq.events.server.session.Alert;
import org.hyperic.hq.events.server.session.AlertActionLog;
import org.hyperic.hq.events.server.session.AlertActionLogDAO;
import org.hyperic.hq.events.server.session.AlertCondition;
import org.hyperic.hq.events.server.session.AlertConditionDAO;
import org.hyperic.hq.events.server.session.AlertConditionLog;
import org.hyperic.hq.events.server.session.AlertConditionLogDAO;
import org.hyperic.hq.events.server.session.AlertDAO;
import org.hyperic.hq.events.server.session.AlertDefinition;
import org.hyperic.hq.events.server.session.AlertDefinitionDAO;
import org.hyperic.hq.events.server.session.Escalation;
import org.hyperic.hq.events.server.session.EscalationDAO;
import org.hyperic.hq.events.server.session.EscalationState;
import org.hyperic.hq.events.server.session.EscalationStateDAO;
import org.hyperic.hq.events.server.session.EventLog;
import org.hyperic.hq.events.server.session.EventLogDAO;
import org.hyperic.hq.events.server.session.RegisteredTrigger;
import org.hyperic.hq.events.server.session.TriggerDAO;
import org.hyperic.hq.galerts.server.session.ExecutionStrategyTypeInfoDAO;
import org.hyperic.hq.galerts.server.session.GalertDefDAO;
import org.hyperic.hq.galerts.server.session.GtriggerTypeInfoDAO;
import org.hyperic.hq.measurement.server.session.Baseline;
import org.hyperic.hq.measurement.server.session.BaselineDAO;
import org.hyperic.hq.measurement.server.session.Category;
import org.hyperic.hq.measurement.server.session.CategoryDAO;
import org.hyperic.hq.measurement.server.session.DerivedMeasurement;
import org.hyperic.hq.measurement.server.session.DerivedMeasurementDAO;
import org.hyperic.hq.measurement.server.session.MeasurementArg;
import org.hyperic.hq.measurement.server.session.MeasurementArgDAO;
import org.hyperic.hq.measurement.server.session.MeasurementTemplate;
import org.hyperic.hq.measurement.server.session.MeasurementTemplateDAO;
import org.hyperic.hq.measurement.server.session.MetricProblem;
import org.hyperic.hq.measurement.server.session.MetricProblemDAO;
import org.hyperic.hq.measurement.server.session.MonitorableType;
import org.hyperic.hq.measurement.server.session.MonitorableTypeDAO;
import org.hyperic.hq.measurement.server.session.RawMeasurement;
import org.hyperic.hq.measurement.server.session.RawMeasurementDAO;
import org.hyperic.hq.measurement.server.session.ScheduleRevNum;
import org.hyperic.hq.measurement.server.session.ScheduleRevNumDAO;
import org.hyperic.hq.product.Plugin;

public class HibernateDAOFactory extends DAOFactory {
    private static SessionFactory sessionFactory = Util.getSessionFactory();
    private static HibernateDAOFactory singleton = new HibernateDAOFactory();
    private HashMap daoMap = new HashMap();

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

    public HibernateDAO getDAO(Class pojo) {
        return (HibernateDAO)daoMap.get(pojo.getName());
    }

    private void addDAO(HibernateDAO dao) {
        daoMap.put(dao.getPersistentClass().getName(), dao);
    }

    protected HibernateDAOFactory () {
        addDAO(new ActionDAO(this));
        addDAO(new AgentDAO(this));
        addDAO(new AgentTypeDAO(this));
        addDAO(new AIHistoryDAO(this));
        addDAO(new AIIpDAO(this));
        addDAO(new AIPlatformDAO(this));
        addDAO(new AIPlatformDAO(this));
        addDAO(new AIScheduleDAO(this));
        addDAO(new AIServiceDAO(this));
        addDAO(new AIServerDAO(this));
        addDAO(new AlertDAO(this));
        addDAO(new AlertActionLogDAO(this));
        addDAO(new AlertConditionDAO(this));
        addDAO(new AlertConditionLogDAO(this));
        addDAO(new AlertDefinitionDAO(this));
        addDAO(new ApplicationDAO(this));
        addDAO(new ApplicationTypeDAO(this));
        addDAO(new AppServiceDAO(this));
        addDAO(new AppSvcDependencyDAO(this));
        addDAO(new AuthzSubjectDAO(this));
        addDAO(new BaselineDAO(this));
        addDAO(new CategoryDAO(this));
        addDAO(new ControlScheduleDAO(this));
        addDAO(new ControlHistoryDAO(this));
        addDAO(new ConfigPropertyDAO(this));
        addDAO(new ConfigResponseDAO(this));
        addDAO(new CpropDAO(this));
        addDAO(new CpropKeyDAO(this));
        addDAO(new CrispoDAO(this));
        addDAO(new EscalationDAO(this));
        addDAO(new EscalationStateDAO(this));
        addDAO(new EventLogDAO(this));
        addDAO(new DerivedMeasurementDAO(this));
        addDAO(new MeasurementArgDAO(this));
        addDAO(new MeasurementTemplateDAO(this));
        addDAO(new MetricProblemDAO(this));
        addDAO(new MonitorableTypeDAO(this));
        addDAO(new OperationDAO(this));
        addDAO(new PlatformDAO(this));
        addDAO(new PlatformTypeDAO(this));
        addDAO(new PluginDAO(this));
        addDAO(new PrincipalDAO(this));
        addDAO(new RawMeasurementDAO(this));
        addDAO(new TriggerDAO(this));
        addDAO(new ResourceDAO(this));
        addDAO(new ResourceTypeDAO(this));
        addDAO(new ResourceGroupDAO(this));
        addDAO(new RoleDAO(this));
        addDAO(new ScheduleRevNumDAO(this));
        addDAO(new ServerDAO(this));
        addDAO(new ServerTypeDAO(this));
        addDAO(new ServiceDAO(this));
        addDAO(new ServiceTypeDAO(this));
        addDAO(new VirtualDAO(this));
        addDAO(new GalertDefDAO(this));
        addDAO(new GtriggerTypeInfoDAO(this));
    }

    public ActionDAO getActionDAO() {
        return (ActionDAO)getDAO(Action.class);
    }

    public AlertDefinitionDAO getAlertDefDAO() {
        return (AlertDefinitionDAO)getDAO(AlertDefinition.class);
    }
    
    public AlertConditionDAO getAlertConditionDAO() {
        return (AlertConditionDAO)getDAO(AlertCondition.class);
    }

    public AgentDAO getAgentDAO() {
        return (AgentDAO)getDAO(Agent.class);
    }

    public AgentTypeDAO getAgentTypeDAO() {
        return (AgentTypeDAO)getDAO(AgentType.class);
    }

    public ApplicationDAO getApplicationDAO() {
        return (ApplicationDAO)getDAO(Application.class);
    }

    public ApplicationTypeDAO getApplicationTypeDAO() {
        return (ApplicationTypeDAO)getDAO(ApplicationType.class);
    }

    public AppServiceDAO getAppServiceDAO() {
        return (AppServiceDAO)getDAO(AppService.class);
    }

    public AppSvcDependencyDAO getAppSvcDepencyDAO() {
        return (AppSvcDependencyDAO)getDAO(AppSvcDependency.class);
    }

    public ConfigResponseDAO getConfigResponseDAO() {
        return (ConfigResponseDAO)getDAO(ConfigResponseDB.class);
    }

    public CpropDAO getCpropDAO() {
        return (CpropDAO)getDAO(Cprop.class);
    }

    public CpropKeyDAO getCpropKeyDAO() {
        return (CpropKeyDAO)getDAO(CpropKey.class);
    }

    public PlatformDAO getPlatformDAO() {
        return (PlatformDAO)getDAO(Platform.class);
    }

    public PlatformTypeDAO getPlatformTypeDAO() {
        return (PlatformTypeDAO)getDAO(PlatformType.class);
    }

    public ServerDAO getServerDAO() {
        return (ServerDAO)getDAO(Server.class);
    }

    public ServiceClusterDAO getServiceClusterDAO() {
        return (ServiceClusterDAO)getDAO(ServiceCluster.class);
    }

    public ServerTypeDAO getServerTypeDAO() {
        return (ServerTypeDAO)getDAO(ServerType.class);
    }

    public ServiceDAO getServiceDAO() {
        return (ServiceDAO)getDAO(Service.class);
    }

    public TriggerDAO getTriggerDAO() {
        return (TriggerDAO)getDAO(RegisteredTrigger.class);
    }

    public ServiceTypeDAO getServiceTypeDAO() {
        return (ServiceTypeDAO)getDAO(ServiceType.class);
    }

    public AuthzSubjectDAO getAuthzSubjectDAO() {
        return (AuthzSubjectDAO)getDAO(AuthzSubject.class);
    }

    public BaselineDAO getBaselineDAO() {
        return (BaselineDAO)getDAO(Baseline.class);
    }

    public CategoryDAO getCategoryDAO() {
        return (CategoryDAO)getDAO(Category.class);
    }

    public PrincipalDAO getPrincipalDAO() {
        return (PrincipalDAO)getDAO(Principal.class);
    }

    public ResourceDAO getResourceDAO() {
        return (ResourceDAO)getDAO(Resource.class);
    }

    public ResourceGroupDAO getResourceGroupDAO() {
        return (ResourceGroupDAO)getDAO(ResourceGroup.class);
    }

    public ResourceTypeDAO getResourceTypeDAO() {
        return (ResourceTypeDAO)getDAO(ResourceType.class);
    }

    public RoleDAO getRoleDAO() {
        return (RoleDAO)getDAO(Role.class);
    }

    public OperationDAO getOperationDAO() {
        return (OperationDAO)getDAO(Operation.class);
    }

    public MonitorableTypeDAO getMonitorableTypeDAO() {
        return (MonitorableTypeDAO)getDAO(MonitorableType.class);
    }

    public RawMeasurementDAO getRawMeasurementDAO() {
        return (RawMeasurementDAO)getDAO(RawMeasurement.class);
    }

    public DerivedMeasurementDAO getDerivedMeasurementDAO() {
        return (DerivedMeasurementDAO)getDAO(DerivedMeasurement.class);
    }

    public MeasurementTemplateDAO getMeasurementTemplateDAO() {
        return (MeasurementTemplateDAO)getDAO(MeasurementTemplate.class);
    }

    public MeasurementArgDAO getMeasurementArgDAO() {
        return (MeasurementArgDAO)getDAO(MeasurementArg.class);
    }

    public MetricProblemDAO getMetricProblemDAO() {
        return (MetricProblemDAO)getDAO(MetricProblem.class);
    }

    public ScheduleRevNumDAO getScheduleRevNumDAO() {
        return (ScheduleRevNumDAO)getDAO(ScheduleRevNum.class);
    }

    public AIPlatformDAO getAIPlatformDAO() {
        return (AIPlatformDAO)getDAO(AIPlatform.class);
    }

    public AIServerDAO getAIServerDAO() {
        return (AIServerDAO)getDAO(AIServer.class);
    }

    public AIServiceDAO getAIServiceDAO() {
        return (AIServiceDAO)getDAO(AIService.class);
    }

    public AIIpDAO getAIIpDAO() {
        return (AIIpDAO)getDAO(AIIp.class);
    }

    public AIHistoryDAO getAIHistoryDAO() {
        return (AIHistoryDAO)getDAO(AIHistory.class);
    }

    public AIScheduleDAO getAIScheduleDAO() {
        return (AIScheduleDAO)getDAO(AISchedule.class);
    }

    public ConfigPropertyDAO getConfigPropertyDAO() {
        return (ConfigPropertyDAO)getDAO(ConfigProperty.class);
    }

    public PluginDAO getPluginDAO() {
        return (PluginDAO)getDAO(Plugin.class);
    }

    public AlertActionLogDAO getAlertActionLogDAO() {
        return (AlertActionLogDAO)getDAO(AlertActionLog.class);
    }

    public AlertConditionLogDAO getAlertConditionLogDAO() {
        return (AlertConditionLogDAO)getDAO(AlertConditionLog.class);
    }

    public AlertDAO getAlertDAO() {
        return (AlertDAO)getDAO(Alert.class);
    }

    public VirtualDAO getVirtualDAO() {
        return (VirtualDAO)getDAO(Virtual.class);
    }

    public CrispoDAO getCrispoDAO() {
        return (CrispoDAO)getDAO(Crispo.class);
    }

    public EscalationDAO getEscalationDAO() {
        return (EscalationDAO)getDAO(Escalation.class);
    }

    public EscalationStateDAO getEscalationStateDAO() {
        return (EscalationStateDAO)getDAO(EscalationState.class);
    }

    public EventLogDAO getEventLogDAO() {
        return (EventLogDAO)getDAO(EventLog.class);
    }

    public ControlHistoryDAO getControlHistoryDAO() {
        return (ControlHistoryDAO)getDAO(ControlHistory.class);
    }

    public ControlScheduleDAO getControlScheduleDAO() {
        return (ControlScheduleDAO)getDAO(ControlSchedule.class);
    }

    public GalertDefDAO getGalertDefDAO() {
        return new GalertDefDAO(this);
    }

    public ExecutionStrategyTypeInfoDAO getExecutionStrategyTypeInfoDAO() {
        return new ExecutionStrategyTypeInfoDAO(this);
    }

    public GtriggerTypeInfoDAO getGtriggerTypeInfoDAO() {
        return new GtriggerTypeInfoDAO(this);
    }
}
