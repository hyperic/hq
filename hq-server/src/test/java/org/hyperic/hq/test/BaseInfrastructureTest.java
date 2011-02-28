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
package org.hyperic.hq.test;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import javax.persistence.EntityManagerFactory;

import net.sf.ehcache.CacheManager;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hibernate.Session;
import org.hyperic.hq.agent.domain.Agent;
import org.hyperic.hq.appdef.server.session.Application;
import org.hyperic.hq.appdef.server.session.ApplicationManagerImpl;
import org.hyperic.hq.appdef.server.session.Platform;
import org.hyperic.hq.appdef.server.session.PlatformType;
import org.hyperic.hq.appdef.server.session.Server;
import org.hyperic.hq.appdef.server.session.ServerType;
import org.hyperic.hq.appdef.server.session.Service;
import org.hyperic.hq.appdef.server.session.ServiceType;
import org.hyperic.hq.appdef.shared.AIPlatformValue;
import org.hyperic.hq.appdef.shared.AgentCreateException;
import org.hyperic.hq.appdef.shared.AgentManager;
import org.hyperic.hq.appdef.shared.AppdefDuplicateNameException;
import org.hyperic.hq.appdef.shared.AppdefEntityConstants;
import org.hyperic.hq.appdef.shared.AppdefEntityID;
import org.hyperic.hq.appdef.shared.AppdefEntityTypeID;
import org.hyperic.hq.appdef.shared.AppdefGroupNotFoundException;
import org.hyperic.hq.appdef.shared.ApplicationManager;
import org.hyperic.hq.appdef.shared.ApplicationNotFoundException;
import org.hyperic.hq.appdef.shared.ApplicationValue;
import org.hyperic.hq.appdef.shared.PlatformManager;
import org.hyperic.hq.appdef.shared.PlatformNotFoundException;
import org.hyperic.hq.appdef.shared.ServerManager;
import org.hyperic.hq.appdef.shared.ServerNotFoundException;
import org.hyperic.hq.appdef.shared.ServerValue;
import org.hyperic.hq.appdef.shared.ServiceManager;
import org.hyperic.hq.appdef.shared.ValidationException;
import org.hyperic.hq.auth.domain.Role;
import org.hyperic.hq.authz.shared.AuthzSubjectManager;
import org.hyperic.hq.authz.shared.GroupCreationException;
import org.hyperic.hq.authz.shared.PermissionException;
import org.hyperic.hq.authz.shared.ResourceGroupCreateInfo;
import org.hyperic.hq.authz.shared.ResourceGroupManager;
import org.hyperic.hq.authz.shared.ResourceManager;
import org.hyperic.hq.common.ApplicationException;
import org.hyperic.hq.common.NotFoundException;
import org.hyperic.hq.context.IntegrationTestContextLoader;
import org.hyperic.hq.grouping.shared.GroupDuplicateNameException;
import org.hyperic.hq.inventory.domain.Resource;
import org.hyperic.hq.inventory.domain.ResourceGroup;
import org.hyperic.hq.product.ServerTypeInfo;
import org.hyperic.hq.product.ServiceTypeInfo;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.orm.jpa.EntityManagerFactoryUtils;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.transaction.annotation.Transactional;

/**
 * BaseInfrastructureTest
 * 
 * @author Helena Edelson
 * @author Jennifer Hickey
 */
@Transactional
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = "classpath*:META-INF/spring/*-context.xml", loader = IntegrationTestContextLoader.class)
abstract public class BaseInfrastructureTest {

    protected Log logger = LogFactory.getLog(this.getClass());
    protected long startTime;
    protected long endTime;

    @Autowired
    protected PlatformManager platformManager;

    @Autowired
    protected AuthzSubjectManager authzSubjectManager;

    @Autowired
    protected ServerManager serverManager;

    @Autowired
    protected ServiceManager serviceManager;

    @Autowired
    protected AgentManager agentManager;

    @Autowired
    protected EntityManagerFactory entityManagerFactory;

    @Autowired
    protected ResourceManager resourceManager;

    @Autowired
    protected ResourceGroupManager resourceGroupManager;

    @Autowired
    protected ApplicationManager applicationManager;

    //The test plugin is automatically deployed when the integration test starts
    protected static final String TEST_PLUGIN_NAME="test";

    @BeforeClass
    public static void initialize() {

    }

    @Before
    public void before() {
        startTime = System.nanoTime();
        logger.debug("****** Test starting ******");
    }

    @After
    public void after() {
        // Clear the query cache
        ((Session)EntityManagerFactoryUtils
            .getTransactionalEntityManager(entityManagerFactory).getDelegate()).getSessionFactory().getCache().evictQueryRegions();
        // Clear the 2nd level cache including regions with queries
        CacheManager.getInstance().clearAll();
        endTime = System.nanoTime();
        logger.debug(buildMessage());
    }

    @AfterClass
    public static void tearDown() {

    }

    /**
     * 
     * @return
     */
    private String buildMessage() {
        return new StringBuilder().append("****** Test executed in ").append(endTime)
            .append(" nanoseconds or ")
            .append(TimeUnit.SECONDS.convert(endTime, TimeUnit.NANOSECONDS)).append(" seconds")
            .toString();
    }

    protected Platform createPlatform(String agentToken, String platformType, String fqdn,
                                      String name, int cpuCount) throws ApplicationException {
        AIPlatformValue aiPlatform = new AIPlatformValue();
        aiPlatform.setCpuCount(cpuCount);
        aiPlatform.setPlatformTypeName(platformType);
        aiPlatform.setAgentToken(agentToken);
        aiPlatform.setFqdn(fqdn);
        aiPlatform.setName(name);
        aiPlatform.setCertdn("SomethingOrOther");
        return platformManager.createPlatform(authzSubjectManager.getOverlordPojo(), aiPlatform);
    }

    protected Server createServer(Platform platform, ServerType serverType, String name)
        throws PlatformNotFoundException, AppdefDuplicateNameException, ValidationException,
        PermissionException, NotFoundException {
        ServerValue server = new ServerValue();
        server.setName(name);
        server.setInstallPath("/somwhere/over/rainbow");
        return serverManager.createServer(authzSubjectManager.getOverlordPojo(), platform.getId(),
            serverType.getId(), server);
    }

    protected ServerType createServerType(String serverTypeName, String serverVersion,
                                          String[] validPlatformTypes) throws NotFoundException {
        ServerTypeInfo serverTypeInfo = new ServerTypeInfo();
        serverTypeInfo.setDescription(serverTypeName);
        serverTypeInfo.setName(serverTypeName);
        serverTypeInfo.setVersion(serverVersion);
        serverTypeInfo.setValidPlatformTypes(validPlatformTypes);
        return serverManager.createServerType(serverTypeInfo, TEST_PLUGIN_NAME);
    }

    protected ServiceType createServiceType(String serviceTypeName,
                                            ServerType serverType) throws NotFoundException {
        ServiceTypeInfo sinfo = new ServiceTypeInfo();
        sinfo.setDescription(serviceTypeName);
        sinfo.setName(serviceTypeName);
        return serviceManager.createServiceType(sinfo, TEST_PLUGIN_NAME, resourceManager.findResourceTypeById(serverType.getId()));
    }
    
    protected ServiceType createServiceType(String serviceTypeName,
                                            PlatformType platformType) throws NotFoundException {
        ServiceTypeInfo sinfo = new ServiceTypeInfo();
        sinfo.setDescription(serviceTypeName);
        sinfo.setName(serviceTypeName);
        return serviceManager.createServiceType(sinfo, TEST_PLUGIN_NAME, new String[] {platformType.getName()});
    }

    protected Service createService(Integer parentId, ServiceType serviceType, String serviceName,
                                    String description, String location)
        throws ServerNotFoundException, AppdefDuplicateNameException, ValidationException,
        PermissionException {
        return serviceManager.createService(authzSubjectManager.getOverlordPojo(), parentId,
            serviceType.getId(), serviceName, "Spring JDBC Template", "my computer");
    }

    protected Agent createAgent(String address, Integer port, String authToken, String agentToken,
                                String version) throws AgentCreateException {
        return agentManager.createLegacyAgent(address, port, authToken, agentToken, version);
    }

    protected PlatformType createPlatformType(String typeName)
        throws NotFoundException {
        return platformManager.createPlatformType(typeName, TEST_PLUGIN_NAME);
    }

    protected ResourceGroup createPlatformResourceGroup(Set<Platform> platforms, String groupName)
        throws ApplicationException, PermissionException {
        return createPlatformResourceGroup(platforms, groupName, new ArrayList<Role>(0));
    }

    protected ResourceGroup createPlatformResourceGroup(Set<Platform> platforms, String groupName,
                                                        List<Role> roles)
        throws ApplicationException, PermissionException {
        List<Resource> resources = new ArrayList<Resource>();
        for (Platform platform : platforms) {
            Resource platformRes = platform.getResource();
            resources.add(platformRes);
        }
        AppdefEntityTypeID appDefEntTypeId = new AppdefEntityTypeID(
            AppdefEntityConstants.APPDEF_TYPE_PLATFORM, platforms.iterator().next()
                .getPlatformType().getId());

        return createResourceGroup(groupName, appDefEntTypeId, roles, resources,AppdefEntityConstants.APPDEF_TYPE_GROUP_COMPAT_PS);
    }

    protected ResourceGroup createServerResourceGroup(AppdefEntityTypeID appDefEntTypeId, String groupName)
        throws ApplicationException, PermissionException {
        return createResourceGroup(groupName, appDefEntTypeId, new ArrayList<Role>(0), new ArrayList<Resource>(0),
            AppdefEntityConstants.APPDEF_TYPE_GROUP_COMPAT_PS);
    }

    protected ResourceGroup createServerResourceGroup(Set<Server> servers, String groupName)
    throws ApplicationException, PermissionException {
        return createServerResourceGroup(servers, groupName, new ArrayList<Role>(0));
    }
    protected ResourceGroup createServerResourceGroup(Set<Server> servers, String groupName,
                                                      List<Role> roles)
        throws ApplicationException, PermissionException {
        List<Resource> resources = new ArrayList<Resource>();
        for (Server server : servers) {
            Resource serverRes = server.getResource();
            resources.add(serverRes);
        }
        AppdefEntityTypeID appDefEntTypeId = new AppdefEntityTypeID(
            AppdefEntityConstants.APPDEF_TYPE_SERVER, servers.iterator().next().getServerType()
                .getId());

        return createResourceGroup(groupName, appDefEntTypeId, roles, resources,AppdefEntityConstants.APPDEF_TYPE_GROUP_COMPAT_PS);
    }
    
    

    protected ResourceGroup createServiceResourceGroup(Set<Service> services, String groupName)
        throws ApplicationException, PermissionException {
        return createServiceResourceGroup(services, groupName, new ArrayList<Role>(0));
    }

    protected ResourceGroup createServiceResourceGroup(Set<Service> services, String groupName,
                                                       List<Role> roles)
        throws ApplicationException, PermissionException {
        List<Resource> resources = new ArrayList<Resource>();
        for (Service service : services) {
            Resource serviceRes = service.getResource();
            resources.add(serviceRes);
        }
        AppdefEntityTypeID appDefEntTypeId = new AppdefEntityTypeID(
            AppdefEntityConstants.APPDEF_TYPE_SERVICE, services.iterator().next().getServiceType()
                .getId());

        return createResourceGroup(groupName, appDefEntTypeId, roles, resources,AppdefEntityConstants.APPDEF_TYPE_GROUP_COMPAT_SVC);
    }
    
    protected ResourceGroup createMixedGroup(String groupName, int groupType) throws GroupDuplicateNameException, GroupCreationException {
        ResourceGroupCreateInfo gCInfo = new ResourceGroupCreateInfo(groupName, "",
            "", false,groupType);
        ResourceGroup resGrp = resourceGroupManager.createResourceGroup(
            authzSubjectManager.getOverlordPojo(), gCInfo, Collections.EMPTY_LIST, Collections.EMPTY_LIST);
        return resGrp;
    }
    
    protected ResourceGroup createMixedGroup(String groupName, int groupType,Collection<Resource> resources) throws GroupDuplicateNameException, GroupCreationException {
        ResourceGroupCreateInfo gCInfo = new ResourceGroupCreateInfo(groupName, "",
            "", false,groupType);
        ResourceGroup resGrp = resourceGroupManager.createResourceGroup(
            authzSubjectManager.getOverlordPojo(), gCInfo, Collections.EMPTY_LIST, resources);
        return resGrp;
    }

    private ResourceGroup createResourceGroup(String groupName, AppdefEntityTypeID appDefEntTypeId,
                                              List<Role> roles, List<Resource> resources, int groupType)
        throws GroupDuplicateNameException, GroupCreationException {
        ResourceGroupCreateInfo gCInfo = new ResourceGroupCreateInfo(groupName, "",
            "", false,groupType);
        gCInfo.setGroupEntType(appDefEntTypeId.getType());
        gCInfo.setGroupEntResType(appDefEntTypeId.getId());
        ResourceGroup resGrp = resourceGroupManager.createResourceGroup(
            authzSubjectManager.getOverlordPojo(), gCInfo, roles, resources);
        return resGrp;
    }

    protected Application createApplication(String name, String desc,
                                            List<AppdefEntityID> services)
        throws AppdefDuplicateNameException, ValidationException, PermissionException,
        NotFoundException, ApplicationNotFoundException, AppdefGroupNotFoundException {
        ApplicationValue app = new ApplicationValue();
        app.setName(name);
        app.setDescription(desc);
        app.setEngContact("admin");
        app.setBusinessContact("admin");
        app.setOpsContact("admin");
        app.setLocation("dataCenter");
        app.setCTime(System.currentTimeMillis());
        Application application = applicationManager.createApplication(
            authzSubjectManager.getOverlordPojo(), app);
        applicationManager.setApplicationServices(authzSubjectManager.getOverlordPojo(),
            application.getId(), services);
        return application;
    }

    protected void flushSession() {
        ((Session)EntityManagerFactoryUtils
            .getTransactionalEntityManager(entityManagerFactory).getDelegate()).flush();
    }

    protected void clearSession() {
        ((Session)EntityManagerFactoryUtils
            .getTransactionalEntityManager(entityManagerFactory).getDelegate()).clear();
    }
    
    protected Session getCurrentSession() {
        return  (Session)EntityManagerFactoryUtils
            .getTransactionalEntityManager(entityManagerFactory).getDelegate();
    }
}
