package org.hyperic.hq.bizapp.server.session;

import static org.junit.Assert.assertEquals;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.regex.PatternSyntaxException;

import org.hyperic.hq.appdef.server.session.Platform;
import org.hyperic.hq.appdef.server.session.PlatformType;
import org.hyperic.hq.appdef.server.session.Server;
import org.hyperic.hq.appdef.server.session.ServerType;
import org.hyperic.hq.appdef.server.session.Service;
import org.hyperic.hq.appdef.server.session.ServiceType;
import org.hyperic.hq.appdef.shared.AppdefDuplicateNameException;
import org.hyperic.hq.appdef.shared.AppdefEntityConstants;
import org.hyperic.hq.appdef.shared.AppdefEntityID;
import org.hyperic.hq.appdef.shared.AppdefEntityTypeID;
import org.hyperic.hq.appdef.shared.AppdefGroupNotFoundException;
import org.hyperic.hq.appdef.shared.AppdefResourceValue;
import org.hyperic.hq.appdef.shared.ApplicationNotFoundException;
import org.hyperic.hq.appdef.shared.ServerValue;
import org.hyperic.hq.appdef.shared.ServiceTypeValue;
import org.hyperic.hq.appdef.shared.ServiceValue;
import org.hyperic.hq.appdef.shared.ValidationException;
import org.hyperic.hq.auth.shared.SessionException;
import org.hyperic.hq.auth.shared.SessionManager;
import org.hyperic.hq.authz.shared.PermissionException;
import org.hyperic.hq.bizapp.shared.AppdefBoss;
import org.hyperic.hq.common.ApplicationException;
import org.hyperic.hq.common.NotFoundException;
import org.hyperic.hq.inventory.domain.PropertyType;
import org.hyperic.hq.inventory.domain.ResourceType;
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
    private ServiceType testServiceType;
    private PlatformType testPlatformType;
    private ServerType testServerType;
    private String agentToken;
    @Autowired
    private AppdefBoss appdefBoss;
    @Autowired
    private SessionManager sessionManager;
    private int sessionId;
    private ResourceType testPlatformResType;

    @Before
    public void initializeTestData() throws ApplicationException, NotFoundException {
        sessionId = sessionManager.put(authzSubjectManager.getOverlordPojo());
        agentToken = "agentToken123";
        createAgent("127.0.0.1", 2144, "authToken", agentToken, "4.5");
        flushSession();
        testPlatformType = createPlatformType("Linux");
        testPlatformResType = resourceManager.findResourceTypeByName("Linux");
        testPlatformResType.addPropertyType(new PropertyType("BiosUUID","The UUID of BIOS"));
        testServerType = createServerType("Tomcat", "6.0", new String[] { "Linux" });
        ResourceType testServerResType = resourceManager.findResourceTypeByName("Tomcat");
        testServerResType.addPropertyType(new PropertyType("PermGen","Perm gen"));
        platServiceType = createServiceType("HTTP Check", testPlatformType);
        ResourceType platformResType = resourceManager.findResourceTypeByName("HTTP Check");
        platformResType.addPropertyType(new PropertyType("Something","Something"));
        testServiceType = createServiceType("Servlet",testServerType);
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
    
    @Test
    public void testFindViewablePlatformServiceTypes() throws ApplicationException, NotFoundException {
        Platform testPlatform = createPlatform(agentToken, "Linux", "MyPlat", "MyPlat", 2);
        Server testServer = createServer(testPlatform, testServerType, "Server1" );
        createService(testPlatform.getId(), platServiceType, "Service1", "Service1","my computer");
        createService(testServer.getId(), testServiceType, "Service2", "Service2","my computer");
        PageList<ServiceTypeValue> serviceTypes = appdefBoss.findViewablePlatformServiceTypes(sessionId, testPlatform.getId());
        assertEquals(1,serviceTypes.size());
        assertEquals(platServiceType.getName(),serviceTypes.get(0).getName());
    }
    
    @Test
    public void testCreateAndUpdateServer() throws ApplicationException, NotFoundException {
        Platform testPlatform = createPlatform(agentToken, "Linux", "MyPlat", "MyPlat", 2);
        Map<String,String> props =  new HashMap<String,String>();
        props.put("PermGen", "192");
        ServerValue serverValue = new ServerValue();
        serverValue.setName("Server1");
        serverValue.setInstallPath("/some/place");
        ServerValue createdServer = appdefBoss.createServer(sessionId, serverValue, testPlatform.getId(), testServerType.getId(), props);
        assertEquals(props,resourceManager.findResourceByName("Server1").getProperties(false));
        Map<String,String> newProps =  new HashMap<String,String>();
        newProps.put("PermGen", "256");
        appdefBoss.updateServer(sessionId, createdServer,newProps);
        assertEquals(newProps,resourceManager.findResourceByName("Server1").getProperties(false));
    }
    
    @Test
    public void testCreateAndUpdateService() throws ApplicationException, NotFoundException {
        Platform testPlatform = createPlatform(agentToken, "Linux", "MyPlat", "MyPlat", 2);
        createServer(testPlatform, testServerType, "Server1" );
        Map<String,String> props =  new HashMap<String,String>();
        props.put("Something", "Else");
        ServiceValue serviceValue = new ServiceValue();
        serviceValue.setName("Service1");
        Service createdService = appdefBoss.createService(authzSubjectManager.getOverlordPojo(),serviceValue,platServiceType.getId(),testPlatform.getId(),props);
        assertEquals(props,resourceManager.findResourceByName("Service1").getProperties(false));
        Map<String,String> newProps =  new HashMap<String,String>();
        newProps.put("Something", "Other");
        appdefBoss.updateService(sessionId,createdService.getServiceValue(),newProps);
        assertEquals(newProps,resourceManager.findResourceByName("Service1").getProperties(false));
    }
    
    @Test
    public void testSetCpropValue() throws ApplicationException {
        Platform testPlatform = createPlatform(agentToken, "Linux", "MyPlat", "MyPlat", 2);
        appdefBoss.setCPropValue(sessionId, testPlatform.getEntityId(), "BiosUUID", "586998283");
        assertEquals("586998283",resourceManager.findResourceByName("MyPlat").getProperty("BiosUUID"));
    }
    
    @Test
    public void testGetCPropDescEntries() throws ApplicationException {
        Platform testPlatform = createPlatform(agentToken, "Linux", "MyPlat", "MyPlat", 2);
        appdefBoss.setCPropValue(sessionId, testPlatform.getEntityId(), "BiosUUID", "586998283");
        Map<String,String> expected = new HashMap<String,String>();
        expected.put("The UUID of BIOS","586998283");
        assertEquals(expected,appdefBoss.getCPropDescEntries(sessionId, testPlatform.getEntityId()));
    }
    
    @Test
    public void testGetCPropKeys() throws ApplicationException {
        Platform testPlatform = createPlatform(agentToken, "Linux", "MyPlat", "MyPlat", 2);
        Set<PropertyType> expected = new HashSet<PropertyType>();
        expected.add(testPlatformResType.getPropertyType("BiosUUID"));
        assertEquals(expected,new HashSet<PropertyType>(appdefBoss.getCPropKeys(sessionId, testPlatform.getEntityId())));
    }

}
