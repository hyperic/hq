/**
 * NOTE: This copyright does *not* cover user programs that use HQ
 * program services by normal system calls through the application
 * program interfaces provided as part of the Hyperic Plug-in Development
 * Kit or the Hyperic Client Development Kit - this is merely considered
 * normal use of the program, and does *not* fall under the heading of
 *  "derived work".
 *
 *  Copyright (C) [2010], VMware, Inc.
 *  This file is part of HQ.
 *
 *  HQ is free software; you can redistribute it and/or modify
 *  it under the terms version 2 of the GNU General Public License as
 *  published by the Free Software Foundation. This program is distributed
 *  in the hope that it will be useful, but WITHOUT ANY WARRANTY; without
 *  even the implied warranty of MERCHANTABILITY or FITNESS FOR A
 *  PARTICULAR PURPOSE. See the GNU General Public License for more
 *  details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program; if not, write to the Free Software
 *  Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307
 *  USA.
 *
 */

/**
 * 
 */
package org.hyperic.hq.appdef.server.session;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.hyperic.hq.appdef.shared.AppdefDuplicateNameException;
import org.hyperic.hq.appdef.shared.ApplicationManager;
import org.hyperic.hq.appdef.shared.PlatformNotFoundException;
import org.hyperic.hq.appdef.shared.ServerNotFoundException;
import org.hyperic.hq.appdef.shared.ServiceNotFoundException;
import org.hyperic.hq.appdef.shared.ServiceTypeValue;
import org.hyperic.hq.appdef.shared.ServiceValue;
import org.hyperic.hq.appdef.shared.ValidationException;
import org.hyperic.hq.authz.server.session.AuthzSubject;
import org.hyperic.hq.authz.shared.PermissionException;
import org.hyperic.hq.common.ApplicationException;
import org.hyperic.hq.common.NotFoundException;
import org.hyperic.hq.common.VetoException;
import org.hyperic.hq.inventory.domain.ResourceType;
import org.hyperic.hq.product.ServiceTypeInfo;
import org.hyperic.hq.test.BaseInfrastructureTest;
import org.hyperic.util.pager.PageControl;
import org.hyperic.util.pager.PageList;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.annotation.DirtiesContext;

/**
 * @author iperumal
 * @author jhickey
 * 
 */
@DirtiesContext
public class ServiceManagerTest
    extends BaseInfrastructureTest {

    @Autowired
    ApplicationManager applicationManager;

    private List<Platform> testPlatforms;

    private List<Server> testServers;

    private ServerType testServerType;

    private AuthzSubject subject;

    private String agentToken = "agentToken123";
    
    private ResourceType serverResType;

    private List<Platform> createPlatforms(String agentToken) throws ApplicationException {
        List<Platform> platforms = new ArrayList<Platform>(2);
        for (int i = 1; i <= 2; i++) {
            platforms.add(i - 1,
                createPlatform(agentToken, "Linux", "Test Platform" + i, "Test Platform" + i, 2));
        }
        return platforms;
    }

    private List<Server> createServers(List<Platform> platforms, ServerType serverType)
        throws PlatformNotFoundException, AppdefDuplicateNameException, ValidationException,
        PermissionException, NotFoundException {
        List<Server> servers = new ArrayList<Server>(2);

        for (Platform platform : platforms) {
            servers.add(createServer(platform, serverType, "server"));
        }
        return servers;
    }

    @Before
    public void initializeTestData() throws ApplicationException, NotFoundException {
        subject = authzSubjectManager.getOverlordPojo();
        agentManager.createLegacyAgent("127.0.0.1", 2144, "authToken", agentToken, "4.5");
        createPlatformType("Linux");
        testPlatforms = createPlatforms(agentToken);
        // Create ServerType
        testServerType = createServerType("Test Server", "6.0", new String[] { "Linux" });
        serverResType = resourceManager.findResourceTypeById(testServerType.getId());
        // Create test server
        testServers = createServers(testPlatforms, testServerType);
        Set<Platform> platforms = new HashSet<Platform>(1);
        platforms.add(testPlatforms.get(0));
        createPlatformResourceGroup(platforms, "AllPlatformGroup");
    }

    /**
     * Test method for
     * {@link org.hyperic.hq.appdef.server.session.ServiceManagerImpl#createServiceType(org.hyperic.hq.product.ServiceTypeInfo, java.lang.String, org.hyperic.hq.appdef.server.session.ServerType)}
     * .
     */
    @Test
    public void testCreateServiceType() throws NotFoundException {
        ServiceTypeInfo sinfo = new ServiceTypeInfo();
        sinfo.setDescription("Test ServiceType Desc");
        sinfo.setName("Test ServiceType Name");
        ServiceType serviceType = serviceManager.createServiceType(sinfo, "test", serverResType);
        assertEquals(serviceType.getName(), "Test ServiceType Name");
        assertEquals(serviceType.getDescription(), "Test ServiceType Desc");
        assertEquals("test", serviceType.getPlugin());
    }

    /**
     * Test method for
     * {@link org.hyperic.hq.appdef.server.session.ServiceManagerImpl#createService(org.hyperic.hq.authz.server.session.AuthzSubject, org.hyperic.hq.appdef.server.session.Server, org.hyperic.hq.appdef.server.session.ServiceType, java.lang.String, java.lang.String, java.lang.String, org.hyperic.hq.appdef.server.session.Service)}
     * .
     */
    @Test
    public void testCreateService() throws ApplicationException, NotFoundException {
        ServiceTypeInfo sinfo = new ServiceTypeInfo();
        sinfo.setDescription("Test ServiceType Desc");
        sinfo.setName("Test ServiceType Name");
        ServiceType serviceType = serviceManager.createServiceType(sinfo, "test", serverResType);
        Service service = serviceManager.createService(subject, testServers.get(0).getId(),
            serviceType.getId(), "Test Service Name", "Test Service Desc", "my computer");
        assertEquals(service.getName(), "Test Service Name");
        assertEquals(service.getDescription(), "Test Service Desc");
        assertEquals(service.getServiceType(), serviceType);
        assertEquals(service.getLocation(), "my computer");
        assertFalse(service.getServiceValue().getServiceRt());
        assertFalse(service.getServiceValue().getEndUserRt());
        assertFalse(service.isAutodiscoveryZombie());
        assertNotNull(service.getResource());
        assertEquals(service.getResource().getName(), "Test Service Name");
    }

    /**
     * Test method for
     * {@link org.hyperic.hq.appdef.server.session.ServiceManagerImpl#getServiceIds(org.hyperic.hq.authz.server.session.AuthzSubject, java.lang.Integer)}
     * .
     */
    @Test
    public void testGetServiceIds() throws ApplicationException, NotFoundException {
        ServiceTypeInfo sinfo = new ServiceTypeInfo();
        sinfo.setDescription("Test ServiceType Desc");

        sinfo.setName("Test ServiceType Name");
        ServiceType serviceType = serviceManager.createServiceType(sinfo, "test", serverResType);
        List<Integer> serviceIds = new ArrayList<Integer>(10);
        for (int i = 1; i < 10; i++) {
            serviceIds.add(serviceManager.createService(subject, testServers.get(0).getId(),
                serviceType.getId(), "Test Service Name" + i, "Test Service Desc" + i,
                "my computer").getId());
        }
        Integer[] ids = serviceManager.getServiceIds(subject, serviceType.getId());
        Set<Integer> expectedIds = new HashSet<Integer>(serviceIds);
        Set<Integer> actualIds = new HashSet<Integer>(Arrays.asList(ids));
        assertEquals(expectedIds, actualIds);
    }

    /**
     * Test method for
     * {@link org.hyperic.hq.appdef.server.session.ServiceManagerImpl#findServiceById(java.lang.Integer)}
     * .
     */
    @Test
    public void testFindServiceById() throws ApplicationException, NotFoundException {
        ServiceTypeInfo sinfo = new ServiceTypeInfo();
        sinfo.setDescription("Test ServiceType Desc");
        sinfo.setName("Test ServiceType Name");
        ServiceType serviceType = serviceManager.createServiceType(sinfo, "test", serverResType);
        Service service = serviceManager.createService(subject, testServers.get(0).getId(),
            serviceType.getId(), "Test Service Name", "Test Service Desc", "my computer");
        assertEquals(service, serviceManager.findServiceById(service.getId()));
    }

    @Test(expected = ServiceNotFoundException.class)
    public void testFindServiceByInvalidId() throws ApplicationException, NotFoundException {
        serviceManager.findServiceById(12345789);
    }

    /**
     * Test method for
     * {@link org.hyperic.hq.appdef.server.session.ServiceManagerImpl#getServiceById(java.lang.Integer)}
     * .
     */
    @Test
    public void testGetServiceByIdInteger() throws ApplicationException, NotFoundException {
        ServiceTypeInfo sinfo = new ServiceTypeInfo();
        sinfo.setDescription("Test ServiceType Desc");

        sinfo.setName("Test ServiceType Name");
        ServiceType serviceType = serviceManager.createServiceType(sinfo, "test", serverResType);
        Service service = serviceManager.createService(subject, testServers.get(0).getId(),
            serviceType.getId(), "Test Service Name", "Test Service Desc", "my computer");
        assertEquals(service, serviceManager.getServiceById(service.getId()));
    }

    @Test
    public void testGetServiceByInvalidIdInteger() throws ApplicationException, NotFoundException {
        assertNull(serviceManager.getServiceById(123456789));
    }

    /**
     * Test method for
     * {@link org.hyperic.hq.appdef.server.session.ServiceManagerImpl#getServiceById(org.hyperic.hq.authz.server.session.AuthzSubject, java.lang.Integer)}
     * .
     */
    @Test
    public void testGetServiceByIdAuthzSubjectInteger() throws ApplicationException,
        NotFoundException {
        ServiceTypeInfo sinfo = new ServiceTypeInfo();
        sinfo.setDescription("Test ServiceType Desc");

        sinfo.setName("Test ServiceType Name");
        ServiceType serviceType = serviceManager.createServiceType(sinfo, "test", serverResType);
        Service service = serviceManager.createService(subject, testServers.get(0).getId(),
            serviceType.getId(), "Test Service Name", "Test Service Desc", "my computer");
        assertEquals(service, serviceManager.getServiceById(subject, service.getId()));
    }

    /**
     * Test method for
     * {@link org.hyperic.hq.appdef.server.session.ServiceManagerImpl#getServicesByAIID(org.hyperic.hq.appdef.server.session.Server, java.lang.String)}
     * .
     */
    @Test
    public void testGetServicesByAIID() throws ApplicationException, NotFoundException {
        // AutoInventory Identifier is same as that of the service name
        ServiceTypeInfo sinfo = new ServiceTypeInfo();
        sinfo.setDescription("Test ServiceType Desc");

        sinfo.setName("Test ServiceType Name");
        ServiceType serviceType = serviceManager.createServiceType(sinfo, "test", serverResType);
        Service service = serviceManager.createService(subject, testServers.get(0).getId(),
            serviceType.getId(), "Test Service Name", "Test Service Desc", "my computer");
        assertEquals(
            service,
            serviceManager.getServicesByAIID(testServers.get(0),
                service.getAutoinventoryIdentifier()).get(0));
    }

    /**
     * Test method for
     * {@link org.hyperic.hq.appdef.server.session.ServiceManagerImpl#findServiceType(java.lang.Integer)}
     * .
     */
    @Test
    public void testFindServiceType() throws NotFoundException {
        ServiceTypeInfo sinfo = new ServiceTypeInfo();
        sinfo.setDescription("Test ServiceType Desc");
        sinfo.setName("Test ServiceType Name");
        ServiceType serviceType = serviceManager.createServiceType(sinfo, "test", serverResType);
        assertEquals(serviceType, serviceManager.findServiceType(serviceType.getId()));
    }

    /**
     * Test method for
     * {@link org.hyperic.hq.appdef.server.session.ServiceManagerImpl#findServiceTypeByName(java.lang.String)}
     * .
     */
    @Test
    public void testFindServiceTypeByName() throws NotFoundException {
        ServiceTypeInfo sinfo = new ServiceTypeInfo();
        sinfo.setDescription("Test ServiceType Desc");
        sinfo.setName("Test ServiceType Name");
        ServiceType serviceType = serviceManager.createServiceType(sinfo, "test", serverResType);
        assertEquals(serviceType, serviceManager.findServiceTypeByName(serviceType.getName()));
    }

    /**
     * Test method for
     * {@link org.hyperic.hq.appdef.server.session.ServiceManagerImpl#getAllServiceTypes(org.hyperic.hq.authz.server.session.AuthzSubject, org.hyperic.util.pager.PageControl)}
     * .
     */
    @Test
    public void testGetAllServiceTypes() throws NotFoundException {
        Set<ServiceTypeValue> pgList = new HashSet<ServiceTypeValue>();
        ServiceTypeInfo sinfo = new ServiceTypeInfo();
        for (int i = 1; i <= 5; i++) {
            sinfo.setDescription("Test ServiceType Desc");
            sinfo.setName("Test ServiceType Name");
            pgList.add(serviceManager.createServiceType(sinfo, "test", serverResType)
                .getServiceTypeValue());
        }
        PageControl pc = new PageControl();
        assertTrue(serviceManager.getAllServiceTypes(subject, pc).containsAll(pgList));
    }

    /**
     * Test method for
     * {@link org.hyperic.hq.appdef.server.session.ServiceManagerImpl#getViewableServiceTypes(org.hyperic.hq.authz.server.session.AuthzSubject, org.hyperic.util.pager.PageControl)}
     * .
     */
    @Test
    public void testGetViewableServiceTypes() throws NotFoundException, ApplicationException {
        PageList<ServiceTypeValue> pgList = new PageList<ServiceTypeValue>();
        ServiceTypeInfo sinfo = new ServiceTypeInfo();
        ServiceType serviceType = null;

        for (int i = 1; i <= 5; i++) {
            sinfo.setDescription("Test ServiceType Desc");

            sinfo.setName("Test ServiceType Name" + i);
            serviceType = serviceManager.createServiceType(sinfo, "test", serverResType);
            pgList.add(serviceType.getServiceTypeValue());
            // This method only returns service types which have services
            // instantiated
            serviceManager.createService(authzSubjectManager.getOverlordPojo(), testServers.get(0)
                .getId(), serviceType.getId(), "Test Service" + i, "A svc", "Somewhere");
        }
        assertEquals(serviceManager.getViewableServiceTypes(subject, PageControl.PAGE_ALL), pgList);
    }

    /**
     * Test method for
     * {@link org.hyperic.hq.appdef.server.session.ServiceManagerImpl#getServiceTypesByServerType(org.hyperic.hq.authz.server.session.AuthzSubject, int)}
     * .
     */
    @Test
    public void testGetServiceTypesByServerType() throws NotFoundException {
        Set<ServiceTypeValue> pgList = new HashSet<ServiceTypeValue>();
        ServiceTypeInfo sinfo = new ServiceTypeInfo();
        for (int i = 1; i <= 5; i++) {
            sinfo.setDescription("Test ServiceType Desc");

            sinfo.setName("Test ServiceType Name");
            pgList.add(serviceManager.createServiceType(sinfo, "test", serverResType)
                .getServiceTypeValue());
        }
        assertEquals(
            pgList,
            new HashSet<ServiceTypeValue>(serviceManager.getServiceTypesByServerType(subject,
                testServerType.getId())));
    }

    @Test
    public void testGetServiceTypesByInvalidServerType() throws NotFoundException {
        PageList<ServiceTypeValue> pgList = new PageList<ServiceTypeValue>();
        ServiceTypeInfo sinfo = new ServiceTypeInfo();
        for (int i = 1; i <= 5; i++) {
            sinfo.setDescription("Test ServiceType Desc");

            sinfo.setName("Test ServiceType Name");
            pgList.add(serviceManager.createServiceType(sinfo, "test", serverResType)
                .getServiceTypeValue());
        }
        assertTrue(serviceManager.getServiceTypesByServerType(subject, 435878).isEmpty());
    }

    /**
     * Test method for
     * {@link org.hyperic.hq.appdef.server.session.ServiceManagerImpl#getServicesByServer(org.hyperic.hq.authz.server.session.AuthzSubject, java.lang.Integer, org.hyperic.util.pager.PageControl)}
     * .
     */
    @Test
    public void testGetServicesByServer() throws ApplicationException, NotFoundException {
        Set<Service> svalues = new HashSet<Service>(5);
        ServiceTypeInfo sinfo = new ServiceTypeInfo();
        sinfo.setDescription("Test ServiceType Desc");

        sinfo.setName("Test ServiceType Name");
        ServiceType serviceType = serviceManager.createServiceType(sinfo, "test", serverResType);
        for (int i = 1; i <= 5; i++) {
            svalues.add(serviceManager.createService(subject, testServers.get(0).getId(),
                serviceType.getId(), "Test Service Name" + i, "Test Service From Server" + i,
                "my computer"));
        }
        List<Service> svalues1 = serviceManager.getServicesByServer(subject, testServers.get(0));
        assertEquals(svalues, new HashSet<Service>(svalues1));
    }

    /**
     * Test method for
     * {@link org.hyperic.hq.appdef.server.session.ServiceManagerImpl#getServicesByServer(org.hyperic.hq.authz.server.session.AuthzSubject, java.lang.Integer, java.lang.Integer, org.hyperic.util.pager.PageControl)}
     * .
     */
    @Test
    public void testGetServiceIdsByServerAndServiceTypeId() throws ApplicationException,
        NotFoundException {
        PageList<ServiceValue> svalues = new PageList<ServiceValue>();
        ServiceTypeInfo sinfo = new ServiceTypeInfo();
        sinfo.setDescription("Test ServiceType Desc");

        sinfo.setName("Test ServiceType Name");
        ServiceType serviceType = serviceManager.createServiceType(sinfo, "test", serverResType);
        for (int i = 1; i <= 5; i++) {
            svalues.add(serviceManager.createService(subject, testServers.get(0).getId(),
                serviceType.getId(), "Test Service Name" + i, "Test Service From Server" + i,
                "my computer").getServiceValue());
        }
        PageList<ServiceValue> svalues1 = serviceManager.getServicesByServer(subject, testServers
            .get(0).getId(), serviceType.getId(), new PageControl());
        assertEquals(svalues, svalues1);
    }

    /**
     * Test method for
     * {@link org.hyperic.hq.appdef.server.session.ServiceManagerImpl#getServiceIdsByServer(org.hyperic.hq.authz.server.session.AuthzSubject, java.lang.Integer, java.lang.Integer)}
     * .
     */
    @Test
    public void testGetServiceIdsByServer() throws ApplicationException, NotFoundException {
        List<Integer> svalues = new ArrayList<Integer>(5);
        ServiceTypeInfo sinfo = new ServiceTypeInfo();
        sinfo.setDescription("Test ServiceType Desc");

        sinfo.setName("Test ServiceType Name");
        ServiceType serviceType = serviceManager.createServiceType(sinfo, "test", serverResType);
        for (int i = 1; i <= 5; i++) {
            svalues.add(serviceManager.createService(subject, testServers.get(0).getId(),
                serviceType.getId(), "Test Service Name" + i, "Test Service From Server" + i,
                "my computer").getId());
        }
        Integer[] svalues1 = serviceManager.getServiceIdsByServer(subject, testServers.get(0)
            .getId(), serviceType.getId());
        assertTrue(Arrays.equals(svalues.toArray(new Integer[5]), svalues1));
    }

    /**
     * Test method for
     * {@link org.hyperic.hq.appdef.server.session.ServiceManagerImpl#getPlatformServices(org.hyperic.hq.authz.server.session.AuthzSubject, java.lang.Integer, org.hyperic.util.pager.PageControl)}
     * .
     */
    @Test
    public void testGetPlatformServices() throws ApplicationException, NotFoundException {

        PageList<ServiceValue> svalues = new PageList<ServiceValue>();
        ServiceTypeInfo sinfo = new ServiceTypeInfo();
        sinfo.setDescription("Test ServiceType Desc");
        sinfo.setName("Test ServiceType Name");
        ServiceType serviceType = serviceManager.createServiceType(sinfo, "test", new String[] {testPlatforms
            .get(0).getPlatformType().getName()});
        for (int i = 1; i <= 5; i++) {
            svalues.add(serviceManager.createService(subject, testPlatforms.get(0).getId(),
                serviceType.getId(), "Test Service Name" + i, "Test Service From Server" + i,
                "my computer").getServiceValue());
        }
        PageList<ServiceValue> svalues1 = serviceManager.getPlatformServices(subject, testPlatforms
            .get(0).getId(), new PageControl());
        assertEquals(svalues, svalues1);
    }

    /**
     * Test method for
     * {@link org.hyperic.hq.appdef.server.session.ServiceManagerImpl#getPlatformServices(org.hyperic.hq.authz.server.session.AuthzSubject, java.lang.Integer, java.lang.Integer, org.hyperic.util.pager.PageControl)}
     * .
     */
    @Test
    public void testGetPlatformServicesAddServiceType() throws ApplicationException,
        NotFoundException {
        PageList<ServiceValue> svalues = new PageList<ServiceValue>();
        ServiceTypeInfo sinfo = new ServiceTypeInfo();
        sinfo.setDescription("Test ServiceType Desc");
        sinfo.setName("Test ServiceType Name");
        ServiceType serviceType = serviceManager.createServiceType(sinfo, "test", new String[] {testPlatforms
            .get(0).getPlatformType().getName()});
        for (int i = 1; i <= 5; i++) {
            svalues.add(serviceManager.createService(subject, testPlatforms.get(0).getId(),
                serviceType.getId(), "Test Service Name" + i, "Test Service From Server" + i,
                "my computer").getServiceValue());
        }
        PageList<ServiceValue> svalues1 = serviceManager.getPlatformServices(subject, testPlatforms
            .get(0).getId(), serviceType.getId(), new PageControl());
        assertEquals(svalues, svalues1);
    }

    /**
     * Test method for
     * {@link org.hyperic.hq.appdef.server.session.ServiceManagerImpl#getPlatformServices(org.hyperic.hq.authz.server.session.AuthzSubject, java.lang.Integer)}
     * .
     */
    @Test
    public void testGetPlatformServicesByPlatformId() throws ApplicationException,
        NotFoundException {

        List<Service> svalues = new ArrayList<Service>();
        ServiceTypeInfo sinfo = new ServiceTypeInfo();
        sinfo.setDescription("Test ServiceType Desc");

        sinfo.setName("Test ServiceType Name");
        ServiceType serviceType = serviceManager.createServiceType(sinfo, "test", new String[] {testPlatforms
            .get(0).getPlatformType().getName()});
        for (int i = 1; i <= 5; i++) {
            svalues.add(serviceManager.createService(subject, testPlatforms.get(0).getId(),
                serviceType.getId(), "Test Service Name" + i, "Test Service From Server" + i,
                "my computer"));
        }
        List<Service> svalues1 = (List<Service>) serviceManager.getPlatformServices(subject,
            testPlatforms.get(0).getId());
        assertEquals(svalues, svalues1);
    }

    /**
     * Test method for
     * {@link org.hyperic.hq.appdef.server.session.ServiceManagerImpl#updateServiceZombieStatus(org.hyperic.hq.authz.server.session.AuthzSubject, org.hyperic.hq.appdef.server.session.Service, boolean)}
     * .
     */
    @Test
    public void testUpdateServiceZombieStatus() throws ApplicationException, NotFoundException {
        ServiceTypeInfo sinfo = new ServiceTypeInfo();
        sinfo.setDescription("Test ServiceType Desc");
        sinfo.setName("Test ServiceType Name");
        ServiceType serviceType = serviceManager.createServiceType(sinfo, "test", serverResType);
        Service service = serviceManager.createService(subject, testServers.get(0).getId(),
            serviceType.getId(), "Test Service Name", "Test Service From Server", "my computer");
        assertFalse(service.isAutodiscoveryZombie());
        serviceManager.updateServiceZombieStatus(subject, service, true);
        Service updatedService = serviceManager.findServiceById(service.getId());
        assertTrue(updatedService.isAutodiscoveryZombie());
    }

    /**
     * Test method for
     * {@link org.hyperic.hq.appdef.server.session.ServiceManagerImpl#updateService(org.hyperic.hq.authz.server.session.AuthzSubject, org.hyperic.hq.appdef.shared.ServiceValue)}
     * .
     */
    @Test
    public void testUpdateService() throws ApplicationException, NotFoundException {
        ServiceTypeInfo sinfo = new ServiceTypeInfo();
        sinfo.setDescription("Test ServiceType Desc");
        sinfo.setName("Test ServiceType Name");
        ServiceType serviceType = serviceManager.createServiceType(sinfo, "test", serverResType);
        Service service = serviceManager.createService(subject, testServers.get(0).getId(),
            serviceType.getId(), "Test Service Name", "Test Service From Server", "my computer");
        ServiceValue sValue = new ServiceValue();
        sValue.setName("Changed Name");
        sValue.setDescription("Changed Description");
        sValue.setLocation("Changed Location");
        sValue.setAutodiscoveryZombie(true);
        sValue.setServiceRt(true);
        sValue.setEndUserRt(true);
        sValue.setId(service.getId());
        serviceManager.updateService(subject, sValue);
        Service updatedService = serviceManager.findServiceById(sValue.getId());
        assertEquals(updatedService.getName(), "Changed Name");
        assertEquals(updatedService.getDescription(), "Changed Description");
        assertEquals(updatedService.getServiceType(), serviceType);
        assertEquals(updatedService.getLocation(), "Changed Location");
        assertTrue(updatedService.getServiceValue().getServiceRt());
        assertTrue(updatedService.getServiceValue().getEndUserRt());
        assertTrue(updatedService.isAutodiscoveryZombie());
    }

    /**
     * Test method for
     * {@link org.hyperic.hq.appdef.server.session.ServiceManagerImpl#removeService(org.hyperic.hq.authz.server.session.AuthzSubject, org.hyperic.hq.appdef.server.session.Service)}
     * .
     * @throws VetoException
     * @throws PermissionException
     * @throws ValidationException
     * @throws AppdefDuplicateNameException
     * @throws ServerNotFoundException
     * @throws NotFoundException
     * @throws ServiceNotFoundException
     */
    @Test(expected = ServiceNotFoundException.class)
    public void testRemoveService() throws PermissionException, VetoException,
        ServerNotFoundException, AppdefDuplicateNameException, ValidationException,
        NotFoundException, ServiceNotFoundException {
        ServiceType serviceType = createServiceType("My ServiceType", testServerType);
        Service service = serviceManager.createService(subject, testServers.get(0).getId(),
            serviceType.getId(), "Test Service Name", "Test Service Desc", "my computer");
        serviceManager.removeService(authzSubjectManager.getOverlordPojo(), service);
        serviceManager.findServiceById(service.getId());
    }

    /**
     * Test method for
     * {@link org.hyperic.hq.appdef.server.session.ServiceManagerImpl#getServiceTypeCounts()}
     * .
     */
    @Test
    public void testGetServiceTypeCounts() throws NotFoundException, ApplicationException {
        Map<String, Integer> actuals = new HashMap<String, Integer>(10);
        ServiceTypeInfo sinfo = new ServiceTypeInfo();
        ServiceType serviceType;
        actuals.put("PluginTestServer 1.0 Web Module Stats", 0);
        for (int i = 1; i <= 9; i++) {
            sinfo.setDescription("Test ServiceType Desc" + i);

            sinfo.setName("Test ServiceType Name" + i);
            serviceType = serviceManager.createServiceType(sinfo, "test", serverResType);
            // Create Services as well here as the query uses join from Service
            serviceManager.createService(subject, testServers.get(0).getId(), serviceType.getId(),
                "Test Service Name" + i, "Test Service From Server" + i, "my computer");
            actuals.put("Test ServiceType Name" + i, 1);
        }

        Map<String, Integer> counts = serviceManager.getServiceTypeCounts();
        assertEquals(actuals, counts);
    }

}
