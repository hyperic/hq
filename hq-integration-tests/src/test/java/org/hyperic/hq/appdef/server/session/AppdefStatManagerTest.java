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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.hyperic.hq.appdef.shared.AppdefEntityConstants;
import org.hyperic.hq.appdef.shared.AppdefEntityID;
import org.hyperic.hq.appdef.shared.AppdefStatManager;
import org.hyperic.hq.bizapp.shared.uibeans.ResourceTreeNode;
import org.hyperic.hq.inventory.domain.ResourceGroup;
import org.hyperic.hq.test.BaseInfrastructureTest;
import org.hyperic.util.data.ITreeNode;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.annotation.DirtiesContext;

@DirtiesContext
public class AppdefStatManagerTest
    extends BaseInfrastructureTest {

    @Autowired
    private AppdefStatManager appdefStatManager;

    private Platform platform;

    private Server server;

    private Service service;

    private Application application;

    private ServerType someServer;
    
    private ResourceGroup serverGroup;

    @Before
    public void setUp() throws Exception {
        String agentToken = "agentToken123";
        createAgent("127.0.0.1", 2144, "authToken", agentToken, "5.0");
        createPlatformType("TestPlatform", "test");
        createPlatformType("MyPlatform", "test");
        platform = createPlatform(agentToken, "TestPlatform", "Platform1", "Platform1");
        createPlatform(agentToken, "TestPlatform", "Platform2", "Platform2");
        createPlatform(agentToken, "MyPlatform", "Platform3", "Platform3");
        ServerType serverType = createServerType("TestServer", "6.0",
            new String[] { "TestPlatform" }, "test", false);
        server = createServer(platform, serverType, "Server1");
        someServer = createServerType("SomeServer", "6.0", new String[] { "TestPlatform" }, "test",
            false);
        Server server2 = createServer(platform, someServer, "Server2");
        Server server3 = createServer(platform, someServer, "Server3");
        Server server4 = createServer(platform, someServer, "Server4");
        Set<Server> servers = new HashSet<Server>(3);
        servers.add(server2);
        servers.add(server3);
        servers.add(server4);
        serverGroup = createServerResourceGroup(servers, "ServerGroup");
        ServiceType serviceType = createServiceType("TestService", "test", serverType);
        service = createService(server, serviceType, "Service1", "desc", "location");
        ServiceType serviceType2 = createServiceType("WebAppService", "test", serverType);
        createService(server, serviceType2, "Service2", "desc", "location");
        List<AppdefEntityID> appServices = new ArrayList<AppdefEntityID>(1);
        appServices.add(service.getEntityId());
        application = createApplication("swf-booking-mvc", "Spring Travel",
            GENERIC_APPLICATION_TYPE, appServices);
        createApplication("demo", "Demo", GENERIC_APPLICATION_TYPE,
            new ArrayList<AppdefEntityID>(0));
        createApplication("manager", "Manages", J2EE_APPLICATION_TYPE,
            new ArrayList<AppdefEntityID>(0));
        flushSession();
        clearSession();
    }

    @Test
    public void testGetPlatformCountsByTypeMap() {
        Map<String, Integer> platformCounts = appdefStatManager
            .getPlatformCountsByTypeMap(authzSubjectManager.getOverlordPojo());
        final Map<String, Integer> expected = new HashMap<String, Integer>();
        expected.put("TestPlatform", 2);
        expected.put("MyPlatform", 1);
        assertEquals(expected, platformCounts);
    }

    @Test
    public void testGetPlatformsCount() {
        assertEquals(3, appdefStatManager.getPlatformsCount(authzSubjectManager.getOverlordPojo()));
    }

    @Test
    public void testGetServerCountsByTypeMap() {
        Map<String, Integer> serverCounts = appdefStatManager
            .getServerCountsByTypeMap(authzSubjectManager.getOverlordPojo());
        final Map<String, Integer> expected = new HashMap<String, Integer>();
        expected.put("TestServer", 1);
        expected.put("SomeServer", 3);
        assertEquals(expected, serverCounts);
    }

    @Test
    public void testGetServersCount() {
        assertEquals(4, appdefStatManager.getServersCount(authzSubjectManager.getOverlordPojo()));
    }

    @Test
    public void testGetServiceCountsByTypeMap() {
        Map<String, Integer> serviceCounts = appdefStatManager
            .getServiceCountsByTypeMap(authzSubjectManager.getOverlordPojo());
        final Map<String, Integer> expected = new HashMap<String, Integer>();
        expected.put("TestService", 1);
        expected.put("WebAppService", 1);
        assertEquals(expected, serviceCounts);
    }

    @Test
    public void testGetServicesCount() {
        assertEquals(2, appdefStatManager.getServicesCount(authzSubjectManager.getOverlordPojo()));
    }

    @Test
    public void testGetApplicationCountsByTypeMap() {
        Map<String, Integer> appCounts = appdefStatManager
            .getApplicationCountsByTypeMap(authzSubjectManager.getOverlordPojo());
        final Map<String, Integer> expected = new HashMap<String, Integer>();
        expected.put("Generic Application", 2);
        expected.put("J2EE Application", 1);
        assertEquals(expected, appCounts);
    }

    @Test
    public void testGetGroupCountsMap() {
        Map<Integer, Integer> groupCounts = appdefStatManager.getGroupCountsMap(authzSubjectManager
            .getOverlordPojo());
        final Map<Integer, Integer> expected = new HashMap<Integer, Integer>();
        expected.put(AppdefEntityConstants.APPDEF_TYPE_GROUP_ADHOC_GRP, 0);
        expected.put(AppdefEntityConstants.APPDEF_TYPE_GROUP_ADHOC_APP, 0);
        expected.put(AppdefEntityConstants.APPDEF_TYPE_GROUP_ADHOC_PSS, 0);
        expected.put(AppdefEntityConstants.APPDEF_TYPE_GROUP_COMPAT_PS, 1);
        expected.put(AppdefEntityConstants.APPDEF_TYPE_GROUP_COMPAT_SVC, 0);
        assertEquals(expected, groupCounts);
    }

    @Test
    public void testGetNavMapDataForPlatform() throws Exception {
        ResourceTreeNode[] navMap = appdefStatManager.getNavMapDataForPlatform(authzSubjectManager
            .getOverlordPojo(), platform.getId());
        assertEquals(1, navMap.length);
        assertEquals("Platform1", navMap[0].getName());
        ITreeNode[] children = navMap[0].getUpChildren();
        assertEquals(4, children.length);
        assertEquals("Server4", children[0].getName());
        assertEquals("Server3", children[1].getName());
        assertEquals("Server2", children[2].getName());
        assertEquals("Server1", children[3].getName());
    }

    @Test
    public void testGetNavMapDataForServer() throws Exception {
        ResourceTreeNode[] navMap = appdefStatManager.getNavMapDataForServer(authzSubjectManager
            .getOverlordPojo(), server.getId());
        assertEquals(1, navMap.length);
        assertEquals("Server1", navMap[0].getName());
        ITreeNode[] upChildren = navMap[0].getUpChildren();
        assertEquals(2, upChildren.length);
        assertEquals("Service2", upChildren[0].getName());
        assertEquals("Service1", upChildren[1].getName());
        ITreeNode[] downChildren = navMap[0].getDownChildren();
        assertEquals(1, downChildren.length);
        assertEquals("Platform1", downChildren[0].getName());
    }

    @Test
    public void testGetNavMapDataForService() throws Exception {
        ResourceTreeNode[] navMap = appdefStatManager.getNavMapDataForService(authzSubjectManager
            .getOverlordPojo(), service.getId());
        assertEquals(1, navMap.length);
        assertEquals("Service1", navMap[0].getName());
        ITreeNode[] upChildren = navMap[0].getUpChildren();
        assertEquals(1, upChildren.length);
        assertEquals("swf-booking-mvc", upChildren[0].getName());
        ITreeNode[] downChildren = navMap[0].getDownChildren();
        assertEquals(1, downChildren.length);
        assertEquals("Server1", downChildren[0].getName());
    }

    @Test
    public void testGetNavMapDataForApplication() throws Exception {
        ResourceTreeNode[] navMap = appdefStatManager.getNavMapDataForApplication(
            authzSubjectManager.getOverlordPojo(), application.getId());
        assertEquals(1, navMap.length);
        assertEquals("swf-booking-mvc",navMap[0].getName());
    }

    @Test
    public void testGetNavMapDataForAutoGroup() throws Exception {
        ResourceTreeNode[] navMap = appdefStatManager
            .getNavMapDataForAutoGroup(authzSubjectManager.getOverlordPojo(),
                new AppdefEntityID[] { platform.getEntityId() }, someServer.getId());
        assertEquals(1, navMap.length);
        assertEquals("SomeServer", navMap[0].getName());
        ITreeNode[] upChildren = navMap[0].getUpChildren();
        assertEquals(1, upChildren.length);
        assertEquals("Platform1", upChildren[0].getName());
        ITreeNode[] downChildren = navMap[0].getDownChildren();
        assertEquals(3, downChildren.length);
        assertEquals("Server2", downChildren[0].getName());
        assertEquals("Server3", downChildren[1].getName());
        assertEquals("Server4", downChildren[2].getName());
    }
    
    @Test
    public void testGetNavMapDataForGroup() throws Exception {
        ResourceTreeNode[] navMap = appdefStatManager.getNavMapDataForGroup(authzSubjectManager.getOverlordPojo(), serverGroup.getId());
        assertEquals(1, navMap.length);
        assertEquals("ServerGroup", navMap[0].getName());
        ITreeNode[] downChildren = navMap[0].getDownChildren();
        assertEquals(3, downChildren.length);
        assertEquals("Server2", downChildren[0].getName());
        assertEquals("Server3", downChildren[1].getName());
        assertEquals("Server4", downChildren[2].getName());
    }
}
