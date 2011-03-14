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
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.hyperic.hq.agent.mgmt.domain.Agent;
import org.hyperic.hq.appdef.AppService;
import org.hyperic.hq.appdef.shared.AppdefEntityID;
import org.hyperic.hq.appdef.shared.AppdefResourceTypeValue;
import org.hyperic.hq.appdef.shared.ApplicationNotFoundException;
import org.hyperic.hq.appdef.shared.ApplicationValue;
import org.hyperic.hq.common.ApplicationException;
import org.hyperic.hq.common.NotFoundException;
import org.hyperic.hq.inventory.domain.ResourceGroup;
import org.hyperic.hq.test.BaseInfrastructureTest;
import org.hyperic.util.pager.PageControl;
import org.hyperic.util.pager.PageList;
import org.junit.Test;
import org.springframework.test.annotation.DirtiesContext;


@DirtiesContext
public class ApplicationManagerTest
    extends BaseInfrastructureTest {

    @Test
    public void testCreateUpdateApplication() throws Exception {
        Holder holder = setupInventory();
        List<AppdefEntityID> appServices = new ArrayList<AppdefEntityID>(1);
        appServices.add(holder.service.getEntityId());

        // Create one-service application
        Application application = createApplication("swf-booking-mvc", "Spring Travel", appServices);
        Integer appId = application.getId();
        flushSession();
        clearSession();
        assertNotNull(application);
        application = applicationManager.findApplicationById(authzSubjectManager.getOverlordPojo(),
            appId);
        assertEquals(1, application.getAppServices().size());

        // Create no-service application
        Application managerApplication = createApplication("manager", "Manages",
            new ArrayList<AppdefEntityID>(0));
        Integer managerAppId = managerApplication.getId();
        flushSession();
        assertNotNull(managerApplication);
        assertEquals(0, managerApplication.getAppServices().size());

        // Update, add a service
        ServiceType serviceType2 = createServiceType("WebAppService", holder.serverType);
        Service service2 = createService(holder.server.getId(), serviceType2, "Service2", "desc",
            "location");
        ApplicationValue appVal = managerApplication.getApplicationValue();
        AppService appService = new AppService();
        appService.setService(service2);
        appService.setId(service2.getId());
        appVal.addAppServiceValue(appService.getAppServiceValue());
        appVal.setName("new name for updated application");
        applicationManager.updateApplication(authzSubjectManager.getOverlordPojo(), appVal);
        flushSession();
        clearSession();
        managerApplication = applicationManager.findApplicationById(
            authzSubjectManager.getOverlordPojo(), managerAppId);
        assertEquals(1, managerApplication.getAppServices().size());
    }

    @Test
    public void testRemoveApplication() throws Exception {

        Holder holder = setupInventory();
        List<AppdefEntityID> appServices = new ArrayList<AppdefEntityID>(1);
        appServices.add(holder.service.getEntityId());

        // Create one-service application
        String appName = "swf-booking-mvc";
        Application application = createApplication(appName, "Spring Travel", appServices);
        flushSession();
        clearSession();
        assertNotNull(application);
        application = applicationManager.findApplicationById(authzSubjectManager.getOverlordPojo(),
            application.getId());
        assertNotNull(application);

        applicationManager.removeApplication(authzSubjectManager.getOverlordPojo(),
            application.getId());
        flushSession();
        clearSession();

        try {
            application = applicationManager.findApplicationById(
                authzSubjectManager.getOverlordPojo(), application.getId());
            fail("findApplicationByName should have returned ApplicationNotFoundException");
        } catch (ApplicationNotFoundException nf) {
            // This is what should happen
        }
    }

    @Test
    public void testList() throws Exception {

        Holder holder = setupInventory();
        List<AppdefEntityID> appServices = new ArrayList<AppdefEntityID>(1);
        appServices.add(holder.service.getEntityId());

        // Create one-service application
        Application genericApp = createApplication("swf-booking-mvc generic",
            "Spring Travel Generic", appServices);
       
        Integer genericAppID = genericApp.getId();
        flushSession();
        clearSession();

        // Re-retrieve the apps from the database
        genericApp = applicationManager.findApplicationById(authzSubjectManager.getOverlordPojo(),
            genericAppID);
        
        assertNotNull(genericApp);
       
        String genericAppTypeName = genericApp.getAppdefResourceType().getName();
       

        List<AppdefResourceTypeValue> types = applicationManager
            .getAllApplicationTypes(authzSubjectManager.getOverlordPojo());
        assertNotNull(types);

        // Match against the application created
        boolean foundGeneric = false;
        for (AppdefResourceTypeValue tv : types) {
            if (tv.getName().equals(genericAppTypeName)) {
                foundGeneric = true;
                break;
            } 
        }

        assertTrue("No application of type generic found", foundGeneric);
    }

    @Test
    public void testRemoveAppService() throws Exception {
        Holder holder = setupInventory();
        List<AppdefEntityID> appServices = new ArrayList<AppdefEntityID>(1);
        appServices.add(holder.service.getEntityId());
        appServices.add(holder.service2.getEntityId());
        appServices.add(holder.service3.getEntityId());
        Integer service3ID = holder.service3.getId();

        // Create one-service application
        Application application = createApplication("swf-booking-mvc generic",
            "Spring Travel Generic", appServices);
        Integer appID = application.getId();

        flushSession();
        clearSession();

        // Re-retrieve
        application = applicationManager.findApplicationById(authzSubjectManager.getOverlordPojo(),
            appID);
        // Grab the id of the AppServiceValue object for the service to be
        // removed
        Integer service3AsvId = null;
        for (AppService as : application.getAppServices()) {
            Integer id = as.getAppServiceValue().getService().getId();
            assertNotNull(id);
            if (id.equals(service3ID)) {
                service3AsvId = as.getId();
            }
        }
        assertNotNull(service3AsvId);
        Collection<AppService> serviceValues = application.getAppServices();
        assertNotNull(serviceValues);
        assertEquals(3, serviceValues.size());

        applicationManager.removeAppService(authzSubjectManager.getOverlordPojo(), appID,
            service3AsvId);
        flushSession();
        clearSession();

        application = applicationManager.findApplicationById(authzSubjectManager.getOverlordPojo(),
            appID);
        serviceValues = application.getAppServices();
        assertNotNull(serviceValues);
        assertEquals(2, serviceValues.size());

        // Make sure the removed ID isn't there anymore
        for (AppService asv : serviceValues) {
            assertFalse(asv.getId().equals(service3AsvId));
        }

    }

    @Test
    public void testGetByResource() throws Exception {
        Holder holder = setupInventory();
        List<AppdefEntityID> appServices = new ArrayList<AppdefEntityID>(1);
        appServices.add(holder.service.getEntityId());
        appServices.add(holder.service2.getEntityId());
        appServices.add(holder.service3.getEntityId());
        createApplication("swf-booking-mvc generic", "Spring Travel Generic", appServices);
        flushSession();
        clearSession();

        // Value object by service...
        PageList<ApplicationValue> appsByResource = applicationManager.getApplicationsByResource(
            authzSubjectManager.getOverlordPojo(), holder.service.getEntityId(),
            PageControl.PAGE_ALL);
        assertNotNull(appsByResource);
        assertEquals(1, appsByResource.size());

        // Value object by server...
        appsByResource = applicationManager.getApplicationsByResource(
            authzSubjectManager.getOverlordPojo(), holder.server.getEntityId(),
            PageControl.PAGE_ALL);
        assertNotNull(appsByResource);
        assertEquals(1, appsByResource.size());

    }

    private Holder setupInventory() throws NotFoundException, ApplicationException {
        Holder holder = new Holder();
        String agentToken = "agentToken123";
        holder.agent = createAgent("127.0.0.1", 2144, "authToken", agentToken, "5.0");
        holder.platformType1 = createPlatformType("TestPlatform");
        holder.platformType2 = createPlatformType("MyPlatform");
        holder.platform = createPlatform(agentToken, "TestPlatform", "Platform1", "Platform1",4);
        holder.platform2 = createPlatform(agentToken, "TestPlatform", "Platform2", "Platform2",4);
        holder.platform3 = createPlatform(agentToken, "MyPlatform", "Platform3", "Platform3",4);
        holder.serverType = createServerType("TestServer", "6.0", new String[] { "TestPlatform" });
        holder.server = createServer(holder.platform, holder.serverType, "Server1");
        holder.serverType2 = createServerType("SomeServer", "6.0", new String[] { "TestPlatform" });
        holder.server2 = createServer(holder.platform, holder.serverType2, "Server2");
        holder.server3 = createServer(holder.platform, holder.serverType2, "Server3");
        holder.server4 = createServer(holder.platform, holder.serverType2, "Server4");
        Set<Server> servers = new HashSet<Server>(3);
        servers.add(holder.server2);
        servers.add(holder.server3);
        servers.add(holder.server4);
        holder.serverGroup = createServerResourceGroup(servers, "ServerGroup");
        holder.serviceType = createServiceType("TestService", holder.serverType);
        holder.service = createService(holder.server.getId(), holder.serviceType, "Service1", "desc",
            "location");
        holder.serviceType2 = createServiceType("TestService2", holder.serverType2);
        holder.service2 = createService(holder.server2.getId(), holder.serviceType2, "Service2", "desc2",
            "location2");
        holder.serviceType3 = createServiceType("TestService3", holder.serverType2);
        holder.service3 = createService(holder.server3.getId(), holder.serviceType3, "Service3", "desc3",
            "location3");

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
