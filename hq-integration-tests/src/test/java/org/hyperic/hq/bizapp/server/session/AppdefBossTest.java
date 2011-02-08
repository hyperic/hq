package org.hyperic.hq.bizapp.server.session;

import static org.junit.Assert.assertEquals;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;
import java.util.regex.PatternSyntaxException;

import org.hyperic.hq.appdef.server.session.Platform;
import org.hyperic.hq.appdef.server.session.PlatformType;
import org.hyperic.hq.appdef.server.session.ServerType;
import org.hyperic.hq.appdef.server.session.ServiceType;
import org.hyperic.hq.appdef.shared.AppdefDuplicateNameException;
import org.hyperic.hq.appdef.shared.AppdefEntityConstants;
import org.hyperic.hq.appdef.shared.AppdefEntityID;
import org.hyperic.hq.appdef.shared.AppdefEntityTypeID;
import org.hyperic.hq.appdef.shared.AppdefGroupNotFoundException;
import org.hyperic.hq.appdef.shared.AppdefResourceValue;
import org.hyperic.hq.appdef.shared.ApplicationNotFoundException;
import org.hyperic.hq.appdef.shared.ValidationException;
import org.hyperic.hq.auth.shared.SessionException;
import org.hyperic.hq.auth.shared.SessionManager;
import org.hyperic.hq.authz.shared.PermissionException;
import org.hyperic.hq.bizapp.shared.AppdefBoss;
import org.hyperic.hq.common.ApplicationException;
import org.hyperic.hq.common.NotFoundException;
import org.hyperic.hq.test.BaseInfrastructureTest;
import org.hyperic.util.pager.PageControl;
import org.hyperic.util.pager.PageList;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.annotation.DirtiesContext;

@DirtiesContext
public class AppdefBossTest
    extends BaseInfrastructureTest {

    private ServiceType platServiceType;
    private PlatformType testPlatformType;
    private ServerType testServerType;
    private String agentToken;
    @Autowired
    private AppdefBoss appdefBoss;
    @Autowired
    private SessionManager sessionManager;
    private int sessionId;

    @Before
    public void initializeTestData() throws ApplicationException, NotFoundException {
        sessionId = sessionManager.put(authzSubjectManager.getOverlordPojo());
        agentToken = "agentToken123";
        createAgent("127.0.0.1", 2144, "authToken", agentToken, "4.5");
        flushSession();
        testPlatformType = createPlatformType("Linux");
        testServerType = createServerType("Tomcat", "6.0", new String[] { "Linux" });
        platServiceType = createServiceType("HTTP Check", testPlatformType);
    }

    @Test
    public void testSearchAllPlatforms() throws PatternSyntaxException, ApplicationException {
        for (int i = 1; i <= 8; i++) {
            createPlatform(agentToken, "Linux", "Platform" + 1, "Platform" + i, 2);
        }
        PageControl pc = new PageControl(0, 7);
        PageList<AppdefResourceValue> appdefs = appdefBoss.search(sessionId,
            AppdefEntityConstants.APPDEF_TYPE_PLATFORM, null, null, null, null, false, false,
            false, pc);
        assertEquals(7, appdefs.size());
        assertEquals(8, appdefs.getTotalSize());
        for (int i = 1; i < 8; i++) {
            assertEquals("Platform" + i, appdefs.get(i - 1).getName());
        }
    }

    @Test
    public void testSearchAllServers() throws PatternSyntaxException, ApplicationException,
        NotFoundException {
        Platform testPlatform = createPlatform(agentToken, "Linux", "MyPlat", "MyPlat", 2);
        for (int i = 1; i <= 8; i++) {
            createServer(testPlatform, testServerType, "Server" + i);
        }
        PageControl pc = new PageControl(0, 7);
        PageList<AppdefResourceValue> appdefs = appdefBoss.search(sessionId,
            AppdefEntityConstants.APPDEF_TYPE_SERVER, null, null, null, null, false, false, false,
            pc);
        assertEquals(7, appdefs.size());
        assertEquals(8, appdefs.getTotalSize());
        for (int i = 1; i < 8; i++) {
            assertEquals("Server" + i, appdefs.get(i - 1).getName());
        }
    }

    @Test
    public void testSearchAllServices() throws PatternSyntaxException, ApplicationException,
        NotFoundException {
        Platform testPlatform = createPlatform(agentToken, "Linux", "MyPlat", "MyPlat", 2);
        for (int i = 1; i <= 8; i++) {
            createService(testPlatform.getId(), platServiceType, "Service" + i, "Service" + i,
                "my computer");
        }
        PageControl pc = new PageControl(0, 7);
        PageList<AppdefResourceValue> appdefs = appdefBoss.search(sessionId,
            AppdefEntityConstants.APPDEF_TYPE_SERVICE, null, null, null, null, false, false, false,
            pc);
        assertEquals(7, appdefs.size());
        assertEquals(8, appdefs.getTotalSize());
        for (int i = 1; i < 8; i++) {
            assertEquals("Service" + i, appdefs.get(i - 1).getName());
        }
    }

    @Test
    public void testSearchAllApplications() throws ApplicationNotFoundException,
        AppdefGroupNotFoundException, AppdefDuplicateNameException, ValidationException,
        PermissionException, NotFoundException, SessionException, PatternSyntaxException {
        for (int i = 1; i <= 8; i++) {
            createApplication("App" + i, "App" + i, new ArrayList<AppdefEntityID>(0));
        }
        PageControl pc = new PageControl(0, 7);
        PageList<AppdefResourceValue> appdefs = appdefBoss.search(sessionId,
            AppdefEntityConstants.APPDEF_TYPE_APPLICATION, null, null, null, null, false, false,
            false, pc);
        assertEquals(7, appdefs.size());
        assertEquals(8, appdefs.getTotalSize());
        for (int i = 1; i < 8; i++) {
            assertEquals("App" + i, appdefs.get(i - 1).getName());
        }
    }

    @Test
    public void testSearchPlatformsOfType() throws PatternSyntaxException, ApplicationException,
        NotFoundException {
        createPlatformType("Windows");
        createPlatform(agentToken, "Windows", "APlat", "APlat", 2);
        for (int i = 1; i <= 8; i++) {
            createPlatform(agentToken, "Linux", "Platform" + 1, "Platform" + i, 2);
        }
        PageControl pc = new PageControl(0, 7);
        PageList<AppdefResourceValue> appdefs = appdefBoss.search(sessionId,
            AppdefEntityConstants.APPDEF_TYPE_PLATFORM, null, new AppdefEntityTypeID(1,
                testPlatformType.getId()), null, null, false, false, false, pc);
        assertEquals(7, appdefs.size());
        assertEquals(8, appdefs.getTotalSize());
        for (int i = 1; i < 8; i++) {
            assertEquals("Platform" + i, appdefs.get(i - 1).getName());
        }
    }

    @Test
    public void testSearchServersOfType() throws PatternSyntaxException, ApplicationException,
        NotFoundException {
        ServerType testServerType2 = createServerType("Apache", "6.0", new String[] { "Linux" });
        Platform testPlatform = createPlatform(agentToken, "Linux", "MyPlat", "MyPlat", 2);
        createServer(testPlatform, testServerType2, "Apache1");
        for (int i = 1; i <= 8; i++) {
            createServer(testPlatform, testServerType, "Server" + i);
        }
        PageControl pc = new PageControl(0, 7);
        PageList<AppdefResourceValue> appdefs = appdefBoss.search(sessionId,
            AppdefEntityConstants.APPDEF_TYPE_SERVER, null, new AppdefEntityTypeID(2,
                testServerType.getId()), null, null, false, false, false, pc);
        assertEquals(7, appdefs.size());
        assertEquals(8, appdefs.getTotalSize());
        for (int i = 1; i < 8; i++) {
            assertEquals("Server" + i, appdefs.get(i - 1).getName());
        }
    }

    @Test
    public void testSearchAllServicesOfType() throws PatternSyntaxException, ApplicationException,
        NotFoundException {
        Platform testPlatform = createPlatform(agentToken, "Linux", "MyPlat", "MyPlat", 2);
        ServiceType type2 = createServiceType("ANewType", testPlatformType);
        createService(testPlatform.getId(), type2, "ASvcType", "ASvcType", "my computer");
        for (int i = 1; i <= 8; i++) {
            createService(testPlatform.getId(), platServiceType, "Service" + i, "Service" + i,
                "my computer");
        }
        PageControl pc = new PageControl(0, 7);
        PageList<AppdefResourceValue> appdefs = appdefBoss.search(sessionId,
            AppdefEntityConstants.APPDEF_TYPE_SERVICE, null, new AppdefEntityTypeID(3,
                platServiceType.getId()), null, null, false, false, false, pc);
        assertEquals(7, appdefs.size());
        assertEquals(8, appdefs.getTotalSize());
        for (int i = 1; i < 8; i++) {
            assertEquals("Service" + i, appdefs.get(i - 1).getName());
        }
    }
    
    @Test
    public void testSearchAllCompatibleGroups() throws ApplicationException {
        Platform testPlatform = createPlatform(agentToken, "Linux", "MyPlat", "MyPlat", 2);
        Set<Platform> testPlatforms = new HashSet<Platform>(1);
        testPlatforms.add(testPlatform);
        createPlatformResourceGroup(testPlatforms, "AllPlatformGroup");
        PageControl pc = new PageControl(0, 7);
        PageList<AppdefResourceValue> appdefs = appdefBoss.search(sessionId,
            AppdefEntityConstants.APPDEF_TYPE_GROUP, null, null, null,new int[] {14,15},false, false, false, pc);
        assertEquals(1, appdefs.size());
        assertEquals(1, appdefs.getTotalSize());
        assertEquals("AllPlatformGroup",appdefs.get(0).getName());
    }
    
    @Test
    public void testSearchAllMixedGroups() throws ApplicationException {
        createMixedGroup("SomeGroup", AppdefEntityConstants.APPDEF_TYPE_GROUP_ADHOC_PSS);
        createMixedGroup("AnotherGroup", AppdefEntityConstants.APPDEF_TYPE_GROUP_ADHOC_GRP);
        createMixedGroup("MyGroup", AppdefEntityConstants.APPDEF_TYPE_GROUP_ADHOC_APP);
        Platform testPlatform = createPlatform(agentToken, "Linux", "MyPlat", "MyPlat", 2);
        Set<Platform> testPlatforms = new HashSet<Platform>(1);
        testPlatforms.add(testPlatform);
        createPlatformResourceGroup(testPlatforms, "AllPlatformGroup");
        PageControl pc = new PageControl(0, 7);
        PageList<AppdefResourceValue> appdefs = appdefBoss.search(sessionId,
            AppdefEntityConstants.APPDEF_TYPE_GROUP, null, null, null,new int[] {13,12,11},false, false, false, pc);
        assertEquals(3, appdefs.size());
        assertEquals(3, appdefs.getTotalSize());
        assertEquals("AnotherGroup",appdefs.get(0).getName());
    }
    
    @Test
    public void testSearchSpecificGroupType() throws ApplicationException {
        createMixedGroup("SomeGroup", AppdefEntityConstants.APPDEF_TYPE_GROUP_ADHOC_PSS);
        createMixedGroup("AnotherGroup", AppdefEntityConstants.APPDEF_TYPE_GROUP_ADHOC_GRP);
        createMixedGroup("MyGroup", AppdefEntityConstants.APPDEF_TYPE_GROUP_ADHOC_APP);
        Platform testPlatform = createPlatform(agentToken, "Linux", "MyPlat", "MyPlat", 2);
        Set<Platform> testPlatforms = new HashSet<Platform>(1);
        testPlatforms.add(testPlatform);
        createPlatformResourceGroup(testPlatforms, "AllPlatformGroup");
        PageControl pc = new PageControl(0, 7);
        PageList<AppdefResourceValue> appdefs = appdefBoss.search(sessionId,
            AppdefEntityConstants.APPDEF_TYPE_GROUP, null, null, null,new int[] {12},false, false, false, pc);
        assertEquals(1, appdefs.size());
        assertEquals(1, appdefs.getTotalSize());
        assertEquals("AnotherGroup",appdefs.get(0).getName());
    }
    
    @Test
    public void testSearchCompatibleGroupContainingType() throws ApplicationException, NotFoundException  {
        createPlatformType("Windows");
        Platform test2=createPlatform(agentToken, "Windows", "APlat", "APlat", 2);
        Platform testPlatform = createPlatform(agentToken, "Linux", "MyPlat", "MyPlat", 2);
        Set<Platform> testPlatforms = new HashSet<Platform>(1);
        testPlatforms.add(testPlatform);
        createPlatformResourceGroup(testPlatforms, "AllPlatformGroup");
        
        Set<Platform> testPlatforms2 = new HashSet<Platform>(1);
        testPlatforms2.add(test2);
        createPlatformResourceGroup(testPlatforms2, "AnotherPlatformGroup");
        PageControl pc = new PageControl(0, 7);
        PageList<AppdefResourceValue> appdefs = appdefBoss.search(sessionId,
            AppdefEntityConstants.APPDEF_TYPE_GROUP, null, new AppdefEntityTypeID(1,testPlatformType.getId()), null,new int[] {14,15},false, false, false, pc);
        assertEquals(1, appdefs.size());
        assertEquals(1, appdefs.getTotalSize());
        assertEquals("AllPlatformGroup",appdefs.get(0).getName());
    }

}
