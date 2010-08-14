package org.hyperic.hq.hqu.rendit

import org.hyperic.hq.appdef.shared.AgentManager;
import org.hyperic.hq.appdef.shared.CPropManager;
import org.hyperic.hq.appdef.shared.ConfigManager;
import org.hyperic.hq.appdef.shared.PlatformManager;
import org.hyperic.hq.appdef.shared.ServerManager;
import org.hyperic.hq.appdef.shared.ServiceManager;
import org.hyperic.hq.auth.shared.AuthManager;
import org.hyperic.hq.authz.server.session.OperationDAO;
import org.hyperic.hq.authz.shared.AuthzSubjectManager;
import org.hyperic.hq.authz.shared.PermissionManager;
import org.hyperic.hq.authz.shared.ResourceGroupManager;
import org.hyperic.hq.authz.shared.ResourceManager;
import org.hyperic.hq.authz.shared.RoleManager;
import org.hyperic.hq.bizapp.shared.AppdefBoss;
import org.hyperic.hq.bizapp.shared.AuthzBoss;
import org.hyperic.hq.bizapp.shared.ProductBoss;
import org.hyperic.hq.context.Bootstrap;
import org.hyperic.hq.control.shared.ControlManager;
import org.hyperic.hq.control.shared.ControlScheduleManager;
import org.hyperic.hq.events.shared.AlertDefinitionManager;
import org.hyperic.hq.events.shared.AlertManager;
import org.hyperic.hq.events.shared.EventLogManager;
import org.hyperic.hq.events.shared.MaintenanceEventManager;
import org.hyperic.hq.livedata.shared.LiveDataManager;
import org.hyperic.hq.measurement.shared.MeasurementManager;
import groovy.util.GroovyTestCase;
import org.easymock.classextension.EasyMock;

abstract class BaseRenditTest extends GroovyTestCase {
    protected ProductBoss productBoss;
    protected AuthzSubjectManager authzSubjectManager;
    protected CPropManager cPropManager;
    protected AppdefBoss appdefBoss;
    protected ConfigManager configManager;
    protected PlatformManager platformManager;
    protected ServerManager serverManager;
    protected ServiceManager serviceManager;
    protected AgentManager agentManager;
    protected AuthManager authManager;
    protected AuthzBoss authzBoss;
    protected OperationDAO operationDAO;
    protected LiveDataManager liveDataManager;
    protected MeasurementManager measurementManager;
    protected ResourceGroupManager resourceGroupManager;
    protected AlertDefinitionManager alertDefinitionManager;
    protected AlertManager alertManager;
    protected EventLogManager eventLogManager;
    protected ControlManager controlManager;
    protected ControlScheduleManager controlScheduleManager;
    protected ResourceManager resourceManager;
    protected PermissionManager permissionManager;
    protected MaintenanceEventManager maintenanceEventManager;
    protected RoleManager roleManager;
    
    protected void setUp() {
        this.productBoss = EasyMock.createMock(ProductBoss.class);
        this.authzSubjectManager = EasyMock.createMock(AuthzSubjectManager.class);
        this.cPropManager = EasyMock.createMock(CPropManager.class);
        this.appdefBoss = EasyMock.createMock(AppdefBoss.class);
        this.configManager = EasyMock.createMock(ConfigManager.class);
        this.platformManager = EasyMock.createMock(PlatformManager.class);
        this.serverManager = EasyMock.createMock(ServerManager.class);
        this.serviceManager = EasyMock.createMock(ServiceManager.class);
        this.agentManager = EasyMock.createMock(AgentManager.class);
        this.authManager = EasyMock.createMock(AuthManager.class);
        this.authzBoss = EasyMock.createMock(AuthzBoss.class);
        this.operationDAO = EasyMock.createMock(OperationDAO.class);
        this.liveDataManager = EasyMock.createMock(LiveDataManager.class);
        this.measurementManager = EasyMock.createMock(MeasurementManager.class);
        this.resourceGroupManager = EasyMock.createMock(ResourceGroupManager.class);
        this.alertDefinitionManager = EasyMock.createMock(AlertDefinitionManager.class);
        this.alertManager = EasyMock.createMock(AlertManager.class);
        this.eventLogManager = EasyMock.createMock(EventLogManager.class);
        this.controlManager = EasyMock.createMock(ControlManager.class);
        this.controlScheduleManager = EasyMock.createMock(ControlScheduleManager.class);
        this.resourceManager = EasyMock.createMock(ResourceManager.class);
        this.permissionManager = EasyMock.createMock(PermissionManager.class);
        this.maintenanceEventManager = EasyMock.createMock(MaintenanceEventManager.class);
        this.roleManager = EasyMock.createMock(RoleManager.class);
        
        Bootstrap.setBean(ProductBoss.class, this.productBoss);
        Bootstrap.setBean(AuthzSubjectManager.class, authzSubjectManager);
        Bootstrap.setBean(CPropManager.class, cPropManager);
        Bootstrap.setBean(AppdefBoss.class, appdefBoss);
        Bootstrap.setBean(ConfigManager.class, configManager);
        Bootstrap.setBean(PlatformManager.class, platformManager);
        Bootstrap.setBean(ServerManager.class, serverManager);
        Bootstrap.setBean(ServiceManager.class, serviceManager);
        Bootstrap.setBean(AgentManager.class, agentManager);
        Bootstrap.setBean(AuthManager.class, authManager);
        Bootstrap.setBean(AuthzBoss.class,authzBoss);
        Bootstrap.setBean(OperationDAO.class, operationDAO);
        Bootstrap.setBean(LiveDataManager.class, liveDataManager);
        Bootstrap.setBean(MeasurementManager.class, measurementManager);
        Bootstrap.setBean(ResourceGroupManager.class, resourceGroupManager);
        Bootstrap.setBean(AlertDefinitionManager.class, alertDefinitionManager)
        Bootstrap.setBean(AlertManager.class, alertManager)
        Bootstrap.setBean(EventLogManager.class, eventLogManager)
        Bootstrap.setBean(ControlManager.class, controlManager)
        Bootstrap.setBean(ControlScheduleManager.class, controlScheduleManager);
        Bootstrap.setBean(ResourceManager.class, resourceManager);
        Bootstrap.setBean(RoleManager.class,roleManager);
        Bootstrap.setBean("permissionManager",permissionManager);
       
        EasyMock.expect(permissionManager.getMaintenanceEventManager()).andReturn(maintenanceEventManager);
    }
}
