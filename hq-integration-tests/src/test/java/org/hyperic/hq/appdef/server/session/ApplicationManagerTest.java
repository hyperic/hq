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

package org.hyperic.hq.appdef.server.session;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.hyperic.hq.appdef.Agent;
import org.hyperic.hq.appdef.AppService;
import org.hyperic.hq.appdef.shared.AppServiceValue;
import org.hyperic.hq.appdef.shared.AppdefEntityID;
import org.hyperic.hq.appdef.shared.AppdefResourceTypeValue;
import org.hyperic.hq.appdef.shared.ApplicationNotFoundException;
import org.hyperic.hq.appdef.shared.ApplicationValue;
import org.hyperic.hq.authz.server.session.ResourceGroup;
import org.hyperic.hq.common.ApplicationException;
import org.hyperic.hq.common.NotFoundException;
import org.hyperic.hq.context.IntegrationTestSpringJUnit4ClassRunner;
import org.hyperic.hq.test.BaseInfrastructureTest;
import org.hyperic.util.pager.PageControl;
import org.hyperic.util.pager.PageList;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.transaction.annotation.Transactional;

@RunWith(IntegrationTestSpringJUnit4ClassRunner.class)
@Transactional
@DirtiesContext

public class ApplicationManagerTest extends BaseInfrastructureTest {

    @Autowired
    private ApplicationTypeDAO applTypeDAO;
    
    @Autowired
    private AppServiceDAO appServiceDAO;

    @Test
    public void testCreateRetrieveApplicationType() {
        String name = "TestApplicationType";
        try {
            ApplicationType created = applTypeDAO.create(name, "Test Application Type");
            Integer id = created.getId();
            ApplicationType retrievedById = applTypeDAO.findById(id);
            assertNotNull(retrievedById);
            assertEquals(id, retrievedById.getId());
            assertEquals(name, retrievedById.getName());
            ApplicationType retrievedByName = applTypeDAO.findByName(name);
            assertNotNull(retrievedByName);
            assertEquals(id, retrievedByName.getId());
            assertEquals(name, retrievedByName.getName());
        } catch (Exception e) {
            e.printStackTrace();
            fail(e.toString());
        }
    }
        
    @Test
    public void testCreateUpdateApplication() {
        try {
            
            Holder holder = setupInventory();
            List<AppdefEntityID> appServices = new ArrayList<AppdefEntityID>(1);
            appServices.add(holder.service.getEntityId());
            
            // Create one-service application
            Application application = createApplication("swf-booking-mvc", "Spring Travel",
                GENERIC_APPLICATION_TYPE, appServices);
            Integer appId = application.getId();
            flushSession();
            clearSession();
            assertNotNull(application);
            application = applicationManager.findApplicationById(authzSubjectManager.getOverlordPojo(), appId);
            assertEquals(1, application.getAppServices().size());

            // Create no-service application
            Application managerApplication = createApplication("manager", "Manages", J2EE_APPLICATION_TYPE,
                new ArrayList<AppdefEntityID>(0));
            Integer managerAppId = managerApplication.getId();
            flushSession();
            assertNotNull(managerApplication);
            assertEquals(0, managerApplication.getAppServices().size());
            
            // Update, add a service
            ServiceType serviceType2 = createServiceType("WebAppService", "test", holder.serverType);
            Service service2 = createService(holder.server, serviceType2, "Service2", "desc", "location");
            ApplicationValue appVal = managerApplication.getApplicationValue();
            AppService appService = appServiceDAO.create(service2.getId(), managerApplication, true);
            assertNotNull(appService);
            appVal.addAppServiceValue(appService.getAppServiceValue());
            appVal.setName("new name for updated application");
            applicationManager.updateApplication(authzSubjectManager.getOverlordPojo(), appVal);
            flushSession();
            clearSession();
            managerApplication = applicationManager.findApplicationById(authzSubjectManager.getOverlordPojo(), managerAppId);
            assertEquals(1, managerApplication.getAppServices().size());
            
        } catch (Exception e) {
            e.printStackTrace();
            fail(e.toString());
        }
    }
    
    @Test
    public void testRemoveApplication() {
        try {
            
            Holder holder = setupInventory();
            List<AppdefEntityID> appServices = new ArrayList<AppdefEntityID>(1);
            appServices.add(holder.service.getEntityId());
            
            // Create one-service application
            String appName = "swf-booking-mvc";
            Application application = createApplication(appName, "Spring Travel",
                GENERIC_APPLICATION_TYPE, appServices);
            flushSession();
            clearSession();
            assertNotNull(application);
            application = applicationManager.findApplicationByName(authzSubjectManager.getOverlordPojo(), appName);
            assertNotNull(application);
            
            applicationManager.removeApplication(authzSubjectManager.getOverlordPojo(), application.getId());
            flushSession();
            clearSession();
            
            try {
                application = applicationManager.findApplicationByName(authzSubjectManager.getOverlordPojo(), appName);
                fail("findApplicationByName should have returned ApplicationNotFoundException");
            } catch (ApplicationNotFoundException nf) {
                // This is what should happen
            }
            
        } catch (Exception e) {
            e.printStackTrace();
            fail(e.toString());
        }
    }
    
    @Test
    public void testList() {
        try {
            Holder holder = setupInventory();
            List<AppdefEntityID> appServices = new ArrayList<AppdefEntityID>(1);
            appServices.add(holder.service.getEntityId());
            
            // Create one-service application
            Application genericApp = createApplication("swf-booking-mvc generic", "Spring Travel Generic",
                GENERIC_APPLICATION_TYPE, appServices);
            Application j2eeApp = createApplication("swf-booking-mvc j2ee", "Spring Travel J2EE",
                J2EE_APPLICATION_TYPE, appServices);
            Integer genericAppID = genericApp.getId();
            Integer j2eeAppId = j2eeApp.getId();
            flushSession();
            clearSession();
            
            // Re-retrieve the apps from the database
            genericApp = applicationManager.findApplicationById(authzSubjectManager.getOverlordPojo(), genericAppID);
            j2eeApp = applicationManager.findApplicationById(authzSubjectManager.getOverlordPojo(), j2eeAppId);
            assertNotNull(genericApp);
            assertNotNull(j2eeApp);
            String genericAppTypeName = genericApp.getAppdefResourceType().getName();
            String j2eeAppTypeName = j2eeApp.getAppdefResourceType().getAppdefResourceTypeValue().getName();

            List<AppdefResourceTypeValue> types = applicationManager.getAllApplicationTypes(authzSubjectManager.getOverlordPojo());
            assertNotNull(types);
            
            // Match against the application created
            boolean foundGeneric = false;
            boolean foundJ2EE = false;
            for (AppdefResourceTypeValue tv : types) {
                if (tv.getName().equals(genericAppTypeName)) {
                    foundGeneric = true;
                    if (foundJ2EE) {
                        break;
                    }
                } else if (tv.getName().equals(j2eeAppTypeName)) {
                    foundJ2EE = true;
                    if (foundGeneric) {
                        break;
                    }
                }
            }
            
            assertTrue("No application of type generic found", foundGeneric);
            assertTrue("No application of type j2ee found", foundJ2EE);
            
        } catch (Exception e) {
            e.printStackTrace();
            fail(e.toString());
        }
    }
    
    @Test
    public void testRemoveAppService() {
        try {
            Holder holder = setupInventory();
            List<AppdefEntityID> appServices = new ArrayList<AppdefEntityID>(1);
            appServices.add(holder.service.getEntityId());
            appServices.add(holder.service2.getEntityId());
            appServices.add(holder.service3.getEntityId());
            Integer service3ID = holder.service3.getId();
            
            // Create one-service application
            Application application = createApplication("swf-booking-mvc generic", "Spring Travel Generic",
                GENERIC_APPLICATION_TYPE, appServices);
            Integer appID = application.getId();
            
            flushSession();
            clearSession();

            // Re-retrieve
            application = applicationManager.findApplicationById(authzSubjectManager.getOverlordPojo(), appID);
            // Grab the id of the AppServiceValue object for the service to be removed
            Integer service3AsvId = null;
            for (AppService as : application.getAppServices()) {
                Integer id = as.getAppServiceValue().getService().getId();
                assertNotNull(id);
                if (id.equals(service3ID)) {
                    service3AsvId = as.getId();
                }
            }
            assertNotNull(service3AsvId);
            List<AppServiceValue> serviceValues = applicationManager.getApplicationServices(authzSubjectManager.getOverlordPojo(), appID);
            assertNotNull(serviceValues);
            assertEquals(3, serviceValues.size());
            
            applicationManager.removeAppService(authzSubjectManager.getOverlordPojo(), appID, service3AsvId);
            flushSession();
            clearSession();

            application = applicationManager.findApplicationById(authzSubjectManager.getOverlordPojo(), appID);
            serviceValues = applicationManager.getApplicationServices(authzSubjectManager.getOverlordPojo(), appID);
            assertNotNull(serviceValues);
            assertEquals(2, serviceValues.size());
            
            // Make sure the removed ID isn't there anymore
            for (AppServiceValue asv : serviceValues) {
                assertFalse(asv.getId().equals(service3AsvId));
            }

        } catch (Exception e) {
            e.printStackTrace();
            fail(e.toString());
        }
    }
    
    @Test
    public void testGetByResource() {
        try {
            Holder holder = setupInventory();
            List<AppdefEntityID> appServices = new ArrayList<AppdefEntityID>(1);
            appServices.add(holder.service.getEntityId());
            appServices.add(holder.service2.getEntityId());
            appServices.add(holder.service3.getEntityId());
            Application application = createApplication("swf-booking-mvc generic", "Spring Travel Generic",
                GENERIC_APPLICATION_TYPE, appServices);
            flushSession();
            clearSession();
            
            // Value object by service...
            PageList<ApplicationValue> appsByResource = applicationManager.getApplicationsByResource(authzSubjectManager.getOverlordPojo(),
                holder.service.getEntityId(), PageControl.PAGE_ALL);
            assertNotNull(appsByResource);
            assertEquals(1, appsByResource.size());
            
            // ID by service...
            Integer[] ids = applicationManager.getApplicationIDsByResource(holder.service2.getEntityId());
            assertNotNull(ids);
            assertEquals(1, ids.length);
            
            // Value object by server...
            appsByResource = applicationManager.getApplicationsByResource(authzSubjectManager.getOverlordPojo(),
                holder.server.getEntityId(), PageControl.PAGE_ALL);
            assertNotNull(appsByResource);
            assertEquals(1, appsByResource.size());
            
            // ID by server...
            ids = applicationManager.getApplicationIDsByResource(holder.server2.getEntityId());
            assertNotNull(ids);
            assertEquals(1, ids.length);
            
            PageList<ApplicationValue> allApps = applicationManager.getAllApplications(authzSubjectManager.getOverlordPojo(),
                PageControl.PAGE_ALL);
            assertNotNull(allApps);
            assertEquals(1, allApps.size());
            
        } catch (Exception e) {
            e.printStackTrace();
            fail(e.toString());
        }
    }
    
    private Holder setupInventory() throws NotFoundException, ApplicationException {
        Holder holder = new Holder();
        String agentToken = "agentToken123";
        holder.agent = createAgent("127.0.0.1", 2144, "authToken", agentToken, "5.0");
        holder.platformType1 = createPlatformType("TestPlatform", "test");
        holder.platformType2 = createPlatformType("MyPlatform", "test");
        holder.platform = createPlatform(agentToken, "TestPlatform", "Platform1", "Platform1");
        holder.platform2 = createPlatform(agentToken, "TestPlatform", "Platform2", "Platform2");
        holder.platform3 = createPlatform(agentToken, "MyPlatform", "Platform3", "Platform3");
        holder.serverType = createServerType("TestServer", "6.0",
            new String[] { "TestPlatform" }, "test", false);
        holder.server = createServer(holder.platform, holder.serverType, "Server1");
        holder.serverType2 = createServerType("SomeServer", "6.0", new String[] { "TestPlatform" }, "test",
            false);
        holder.server2 = createServer(holder.platform, holder.serverType2, "Server2");
        holder.server3 = createServer(holder.platform, holder.serverType2, "Server3");
        holder.server4 = createServer(holder.platform, holder.serverType2, "Server4");
        Set<Server> servers = new HashSet<Server>(3);
        servers.add(holder.server2);
        servers.add(holder.server3);
        servers.add(holder.server4);
        holder.serverGroup = createServerResourceGroup(servers, "ServerGroup");
        holder.serviceType = createServiceType("TestService", "test", holder.serverType);
        holder.service = createService(holder.server, holder.serviceType, "Service1", "desc", "location");
        holder.serviceType2 = createServiceType("TestService2", "test2", holder.serverType2);
        holder.service2 = createService(holder.server2, holder.serviceType, "Service2", "desc2", "location2");
        holder.serviceType3 = createServiceType("TestService3", "test3", holder.serverType2);
        holder.service3 = createService(holder.server3, holder.serviceType, "Service3", "desc3", "location3");

        return holder;
    }
    
    private static class Holder {
        Agent agent;
        PlatformType platformType1;
        PlatformType platformType2;
        Platform platform;
        Platform platform2;
        Platform platform3;
        ServerType serverType;
        ServerType serverType2;
        Server server;
        Server server2;
        Server server3;
        Server server4;
        ResourceGroup serverGroup;
        ServiceType serviceType;
        ServiceType serviceType2;
        ServiceType serviceType3;
        Service service;
        Service service2;
        Service service3;
    }
}
