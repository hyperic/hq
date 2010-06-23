package org.hyperic.hq.appdef.server.session;

import static org.junit.Assert.assertEquals;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.hyperic.hq.appdef.shared.AppdefEntityConstants;
import org.hyperic.hq.appdef.shared.AppdefEntityID;
import org.hyperic.hq.appdef.shared.AppdefStatManager;
import org.hyperic.hq.test.BaseInfrastructureTest;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.annotation.DirtiesContext;

@DirtiesContext
public class AppdefStatManagerTest
    extends BaseInfrastructureTest {

    @Autowired
    private AppdefStatManager appdefStatManager;

    @Before
    public void setUp() throws Exception {
        String agentToken = "agentToken123";
        createAgent("127.0.0.1", 2144, "authToken", agentToken, "5.0");
        createPlatformType("TestPlatform", "test");
        createPlatformType("MyPlatform", "test");
        Platform platform = createPlatform(agentToken, "TestPlatform", "Platform1", "Platform1");
        createPlatform(agentToken, "TestPlatform", "Platform2", "Platform2");
        createPlatform(agentToken, "MyPlatform", "Platform3", "Platform3");
        ServerType serverType = createServerType("TestServer", "6.0",
            new String[] { "TestPlatform" }, "test", false);
        Server server = createServer(platform, serverType, "Server1");
        ServerType serverType2 = createServerType("SomeServer", "6.0",
            new String[] { "TestPlatform" }, "test", false);
        Server server2 = createServer(platform, serverType2, "Server2");
        Server server3 = createServer(platform, serverType2, "Server3");
        Server server4 = createServer(platform, serverType2, "Server4");
        Set<Server> servers = new HashSet<Server>(3);
        servers.add(server2);
        servers.add(server3);
        servers.add(server4);
        createServerResourceGroup(servers, "ServerGroup");
        ServiceType serviceType = createServiceType("TestService", "test", serverType);
        createService(server, serviceType, "Service1", "desc", "location");
        ServiceType serviceType2 = createServiceType("WebAppService", "test", serverType);
        createService(server, serviceType2, "Service2", "desc", "location");
        createApplication("swf-booking-mvc", "Spring Travel", GENERIC_APPLICATION_TYPE, new ArrayList<AppdefEntityID>(0));
        createApplication("demo", "Demo", GENERIC_APPLICATION_TYPE, new ArrayList<AppdefEntityID>(0));
        createApplication("manager", "Manages", J2EE_APPLICATION_TYPE, new ArrayList<AppdefEntityID>(0));
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
        Map<Integer,Integer> groupCounts = appdefStatManager.getGroupCountsMap(authzSubjectManager.getOverlordPojo());
        final Map<Integer,Integer> expected = new HashMap<Integer,Integer>();
        expected.put(AppdefEntityConstants.APPDEF_TYPE_GROUP_ADHOC_GRP, 1);
        expected.put(AppdefEntityConstants.APPDEF_TYPE_GROUP_ADHOC_APP, 0);
        expected.put(AppdefEntityConstants.APPDEF_TYPE_GROUP_ADHOC_PSS, 0);
        expected.put(AppdefEntityConstants.APPDEF_TYPE_GROUP_COMPAT_PS, 0);
        expected.put(AppdefEntityConstants.APPDEF_TYPE_GROUP_COMPAT_SVC, 0);
        assertEquals(expected,groupCounts);
    }
}
