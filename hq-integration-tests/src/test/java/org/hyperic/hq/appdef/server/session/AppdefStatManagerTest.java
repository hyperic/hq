package org.hyperic.hq.appdef.server.session;

import java.util.HashMap;
import java.util.Map;

import org.hyperic.hq.appdef.shared.AppdefStatManager;
import org.hyperic.hq.test.BaseInfrastructureTest;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.annotation.DirtiesContext;
import static org.junit.Assert.assertEquals;

@DirtiesContext
public class AppdefStatManagerTest extends BaseInfrastructureTest {

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
        ServerType serverType = createServerType("TestServer", "6.0", new String[] { "TestPlatform" }, "test",
            false);
        createServer(platform, serverType, "Server1");
        ServerType serverType2 = createServerType("SomeServer", "6.0", new String[] { "TestPlatform" }, "test",
            false);
        createServer(platform, serverType2, "Server2");
        createServer(platform, serverType2, "Server3");
        createServer(platform, serverType2, "Server4");
    }
    
    @Test
    public void testGetPlatformCountsByTypeMap() {
        Map<String,Integer> platformCounts = appdefStatManager.getPlatformCountsByTypeMap(authzSubjectManager.getOverlordPojo());
        final Map<String,Integer> expected = new HashMap<String,Integer>();
        expected.put("TestPlatform", 2);
        expected.put("MyPlatform", 1);
        assertEquals(expected,platformCounts);
    }
    
    @Test
    public void testGetPlatformsCount() {
        assertEquals(3,appdefStatManager.getPlatformsCount(authzSubjectManager.getOverlordPojo()));
    }
    
    @Test
    public void testServerCountsByTypeMap() {
        Map<String,Integer> serverCounts = appdefStatManager.getServerCountsByTypeMap(authzSubjectManager.getOverlordPojo());
        final Map<String,Integer> expected = new HashMap<String,Integer>();
        expected.put("TestServer", 1);
        expected.put("SomeServer", 3);
        assertEquals(expected,serverCounts);
    }
    
    @Test
    public void testGetServersCount() {
        assertEquals(4,appdefStatManager.getServersCount(authzSubjectManager.getOverlordPojo()));
    }
}
