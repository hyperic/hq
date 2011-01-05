package org.hyperic.hq.inventory.domain;

import java.util.HashSet;
import java.util.Set;

import org.hyperic.hq.appdef.server.session.Platform;
import org.hyperic.hq.appdef.server.session.PlatformType;
import org.hyperic.hq.appdef.server.session.Server;
import org.hyperic.hq.appdef.server.session.ServerType;
import org.hyperic.hq.appdef.server.session.Service;
import org.hyperic.hq.appdef.server.session.ServiceType;
import org.hyperic.hq.common.ApplicationException;
import org.hyperic.hq.common.NotFoundException;
import org.hyperic.hq.test.BaseInfrastructureTest;
import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.assertEquals;

public class ResourceIntegrationTest extends BaseInfrastructureTest {

    private Server testServer;
    private Service testService;
    private Platform testPlatform;
    private Service platformService;

    @Before
    public void initializeTestData() throws ApplicationException, NotFoundException {
        String agentToken = "agentToken123";
        createAgent("127.0.0.1", 2144, "authToken", agentToken, "4.5");
        flushSession();
        PlatformType testPlatformType = createPlatformType("Linux");
        testPlatform = createPlatform(agentToken, "Linux", "Test Platform Linux",
            "Test Platform Linux",2);
        // Create ServerType
        ServerType testServerType = createServerType("Tomcat", "6.0", new String[] { "Linux" });
        // Create test server
        testServer = createServer(testPlatform, testServerType, "My Tomcat");
        // Create ServiceType
        ServiceType serviceType = createServiceType("Spring JDBC Template", testServerType);
        ServiceType platServiceType = createServiceType("HTTP Check",testPlatformType);
        // Create test service
        testService = createService(testServer.getId(), serviceType, "jdbcTemplate",
            "Spring JDBC Template", "my computer");
        platformService =  createService(testPlatform.getId(), platServiceType, "httpCheck1",
            "HTTP Check", "my computer");
    }
    
    @Test
    public void testGetChildrenRecursive() {
        Set<Resource> children = resourceManager.findRootResource().getChildren(true);
        final Set<Resource> expected =  new HashSet<Resource>();
        expected.add(testPlatform.getResource());
        expected.add(testServer.getResource());
        expected.add(testService.getResource());
        expected.add(platformService.getResource());
        assertEquals(expected,children);
    }
    
    @Test
    public void testGetChildren() {
        Set<Resource> children = resourceManager.findRootResource().getChildren(false);
        final Set<Resource> expected =  new HashSet<Resource>();
        expected.add(testPlatform.getResource());
        assertEquals(expected,children);
    }
}
