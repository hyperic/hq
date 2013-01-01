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
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.hibernate.NonUniqueObjectException;
import org.hyperic.hq.appdef.Agent;
import org.hyperic.hq.appdef.Ip;
import org.hyperic.hq.appdef.shared.AIIpValue;
import org.hyperic.hq.appdef.shared.AIPlatformValue;
import org.hyperic.hq.appdef.shared.AppdefDuplicateFQDNException;
import org.hyperic.hq.appdef.shared.AppdefDuplicateNameException;
import org.hyperic.hq.appdef.shared.AppdefEntityID;
import org.hyperic.hq.appdef.shared.AppdefEntityNotFoundException;
import org.hyperic.hq.appdef.shared.IpValue;
import org.hyperic.hq.appdef.shared.PlatformNotFoundException;
import org.hyperic.hq.appdef.shared.PlatformValue;
import org.hyperic.hq.authz.server.session.Resource;
import org.hyperic.hq.authz.shared.AuthzConstants;
import org.hyperic.hq.common.ApplicationException;
import org.hyperic.hq.common.NotFoundException;
import org.hyperic.hq.common.SystemException;
import org.hyperic.hq.common.VetoException;
import org.hyperic.hq.product.PlatformTypeInfo;
import org.hyperic.hq.test.BaseInfrastructureTest;
import org.hyperic.util.pager.PageControl;
import org.hyperic.util.pager.PageList;
import org.junit.Before;
import org.junit.Test;
import org.springframework.test.annotation.DirtiesContext;

/**
 * Integration test of the {@link PlatformManagerImpl}
 * @author iperumal
 * 
 */
@DirtiesContext
public class PlatformManagerTest
    extends BaseInfrastructureTest {

    private Agent testAgent;

    private List<PlatformType> testPlatformTypes;

    private PlatformType testPlatformType;

    private List<Platform> testPlatforms;

    private Platform testPlatform;

    private Server testServer;

    private Service testService;

    private List<Platform> createPlatforms(String agentToken) throws ApplicationException {
        List<Platform> platforms = new ArrayList<Platform>(10);
        for (int i = 1; i < 10; i++) {
            platforms.add(i - 1, createPlatform(agentToken, "pType" + i, "TestPlatform" + i,
                "TestPlatform" + i));
        }
        // Create on Linux platform (supported platform)
        platforms.add(9, createPlatform(agentToken, "Linux", "Test Platform Linux",
            "Test Platform Linux"));
        return platforms;
    }

    private List<PlatformType> createTestPlatformTypes() throws NotFoundException {
        List<PlatformType> pTypes = new ArrayList<PlatformType>(10);
        String platformType;
        for (int i = 1; i < 10; i++) {
            platformType = "pType" + i;
            pTypes.add(i - 1, createPlatformType(platformType, "Test Plugin" + i));
        }
        pTypes.add(9, createPlatformType("Linux", "Test Plugin"));
        return pTypes;
    }

    @Before
    public void initializeTestData() throws ApplicationException, NotFoundException {
        String agentToken = "agentToken123";
        testAgent = createAgent("127.0.0.1", 2144, "authToken", agentToken, "4.5");

        testPlatformTypes = createTestPlatformTypes();
        // Get Linux platform type
        testPlatformType = testPlatformTypes.get(9);
        testPlatforms = createPlatforms(agentToken);
        // Get Linux platform
        testPlatform = testPlatforms.get(9);
        // Create ServerType
        ServerType testServerType = createServerType("Tomcat", "6.0", new String[] { "Linux" },
            "Test Server Plugin", false);
        // Create test server
        testServer = createServer(testPlatform, testServerType, "My Tomcat");
        // Create ServiceType
        ServiceType serviceType = createServiceType("Spring JDBC Template", "Test Server Plugin",
            testServerType);
        // Create test service
        testService = createService(testServer, serviceType, "platformService jdbcTemplate",
            "Spring JDBC Template", "my computer");
        Set<Platform> testPlatforms = new HashSet<Platform>(1);
        testPlatforms.add(testPlatform);
        createPlatformResourceGroup(testPlatforms, "AllPlatformGroup");
    }

    @Test
    public void testFindPlatformType() {
        PlatformType pType = platformManager.findPlatformType(testPlatformTypes.get(0).getId());
        assertEquals("Incorrect PlatformType Found ById", pType, testPlatformTypes.get(0));
    }

    @Test
    public void testFindPlatformTypeByName() throws PlatformNotFoundException {
        PlatformType pType = platformManager.findPlatformTypeByName("pType1");
        assertEquals("Incorrect PlatformType found ByName", pType, testPlatformTypes.get(0));
    }

    @Test(expected = PlatformNotFoundException.class)
    public void testFindPlatformTypeByNameNotFound() throws PlatformNotFoundException {
        platformManager.findPlatformTypeByName("Test");
    }

    @Test
    public void testFindAllPlatformTypes() {
        List<PlatformType> allPTypes = (List<PlatformType>) platformManager.findAllPlatformTypes();
        assertTrue("Not all platform types fetched", allPTypes.containsAll(testPlatformTypes));
    }

    @Test
    public void testFindSupportedPlatformTypes() {
        List<PlatformType> supported = (List<PlatformType>) platformManager
            .findSupportedPlatformTypes();
        assertEquals("Support platform doesn't exist", supported.get(0), testPlatformType);
    }

    @Test
    public void testFindUnsupportedPlatformTypes() {
        List<PlatformType> unSupported = (List<PlatformType>) platformManager
            .findUnsupportedPlatformTypes();
        List<PlatformType> unsupportedPlatformTypes = new ArrayList<PlatformType>();
        List<PlatformType> expectedPlatformTypes = new ArrayList<PlatformType>();
        for (PlatformType platformType : unSupported) {
            if(platformType.getName().startsWith("pType")) {
                unsupportedPlatformTypes.add(platformType);
            }
        }
        for (PlatformType platformType : testPlatformTypes) {
            if(platformType.getName().startsWith("pType")) {
                expectedPlatformTypes.add(platformType);
            }
        }

        java.util.Collections.sort(unsupportedPlatformTypes) ; 
        java.util.Collections.sort(expectedPlatformTypes) ; 
        assertEquals("Unsupported platform doesn't exist", unsupportedPlatformTypes,
            expectedPlatformTypes);
    }

    @Test
    public void testFindResource() {
        // Get supported platform
        PlatformType pType = testPlatformType;
        // Find the resource
        Resource res = platformManager.findResource(pType);
        assertEquals("Resource name incorrect", res.getName(), pType.getName());
    }

    public void testGetAllPlatformTypes() {
        fail("Not yet implemented");
    }

    public void testGetViewablePlatformTypes() {
        fail("Not yet implemented");
    }

    @Test
    public void testGetPlatformPluginNameForPlatform() throws AppdefEntityNotFoundException {
        String platformPluginName = platformManager.getPlatformPluginName(testPlatform
            .getEntityId());
        assertEquals("PlatformPluginName incorrect for platform", "Linux", platformPluginName);
    }

    @Test
    public void testGetPlatformPluginNameForServer() throws AppdefEntityNotFoundException {
        String ppNameOfServer = platformManager.getPlatformPluginName(testServer.getEntityId());
        assertEquals("PlatformPluginName incorrect for server", "Tomcat Linux", ppNameOfServer);
    }

    @Test
    public void testGetPlatformPluginNameForService() throws AppdefEntityNotFoundException {
        String ppNameOfService = platformManager.getPlatformPluginName(testService.getEntityId());
        assertEquals("PlatformPluginName incorrect for service", "Spring JDBC Template Linux",
            ppNameOfService);
    }

    /*
     * @Test public void testGetPlatformPluginNameForGroup() throws
     * AppdefEntityNotFoundException { String ppNameOfGroup =
     * platformManager.getPlatformPluginName(AppdefEntityID
     * .newGroupID(testGroup.getId()));
     * assertEquals("PlatformPluginName incorrect for service",
     * "Spring JDBC Template Linux", ppNameOfGroup); }
     */

    /*
     * @Test public void testRemovePlatform() throws PermissionException,
     * VetoException, PlatformNotFoundException {
     * platformManager.removePlatform(authzSubjectManager.getOverlordPojo(),
     * testPlatform);
     * assertNull(platformManager.findPlatformById(testPlatform.getId())); }
     */

    /*
     * public void testHandleResourceDelete() { fail("Not yet implemented"); }
     */

    @Test
    public void testCreatePlatformByAIPlatformValues() throws ApplicationException {
        String agentToken = "agentToken123";

        Platform platform = createPlatform(agentToken, testPlatformType.getName(),
            "Test Platform CreationByAIValues", "Test PlatformByAIValues");
        assertNotNull(platform.getResource());
        assertEquals(platform.getName(), "Test PlatformByAIValues");
        assertEquals(platform.getAgent().getAgentToken(), agentToken);
        assertEquals(platform.getCpuCount(), new Integer(2));
        assertEquals(platform.getPlatformType().getName(), testPlatformType.getName());
        assertEquals(platform.getFqdn(), "Test Platform CreationByAIValues");
    }

    @Test
    public void testCreatePlatformIncorrectPlatformType() throws ApplicationException {
        String agentToken = "agentToken123";

        try {
            // Provide a non-existent platform type
            createPlatform(agentToken, "abcd", "Test Platform Creation", "Test Platform");
        } catch (SystemException e) {
            assertEquals(e.getMessage(), "Unable to find PlatformType [abcd]");
            return;
        }
        fail("Expected SystemException is not thrown");
    }

    @Test(expected = NonUniqueObjectException.class)
    public void testCreatePlatformDuplicate() throws ApplicationException {
        String agentToken = "agentToken123";
        AIPlatformValue aiPlatform = new AIPlatformValue();
        aiPlatform.setCpuCount(2);
        aiPlatform.setName("Test Platform");
        aiPlatform.setPlatformTypeName(testPlatformType.getName());
        aiPlatform.setAgentToken(agentToken);
        aiPlatform.setFqdn("Test Platform Creation");
        platformManager.createPlatform(authzSubjectManager.getOverlordPojo(), aiPlatform);
        platformManager.createPlatform(authzSubjectManager.getOverlordPojo(), aiPlatform);
    }

    @Test
    public void testCreatePlatformIncorrectAgentToken() throws ApplicationException {
        // Add an invalid agent token
        String agentToken = "agentToken";
        AIPlatformValue aiPlatform = new AIPlatformValue();
        aiPlatform.setCpuCount(2);
        aiPlatform.setPlatformTypeName(testPlatformType.getName());
        aiPlatform.setAgentToken(agentToken);
        aiPlatform.setFqdn("Test Platform Creation");
        try {
            platformManager.createPlatform(authzSubjectManager.getOverlordPojo(), aiPlatform);
        } catch (ApplicationException e) {
            assertEquals(e.getMessage(), "Unable to find agent: " + aiPlatform.getAgentToken());
            return;
        }
        fail("Expected ApplicationException for invalid agent token is not thrown");
    }

    @Test
    public void testCreatePlatformByPlatformType() throws ApplicationException {
        PlatformValue pValue = new PlatformValue();
        pValue.setCpuCount(2);
        pValue.setName("Test Platform ByPlatformType");
        pValue.setFqdn("Test Platform CreationByPlatformType");
        Platform platform = platformManager.createPlatform(authzSubjectManager.getOverlordPojo(),
            testPlatformType.getId(), pValue, testAgent.getId());
        assertNotNull(platform.getResource());
        assertEquals(platform.getName(), "Test Platform ByPlatformType");
        assertEquals(platform.getFqdn(), "Test Platform CreationByPlatformType");
        assertEquals(platform.getCpuCount(), new Integer(2));
    }

    @Test(expected = AppdefDuplicateNameException.class)
    public void testCreatePlatformDuplicateName() throws ApplicationException {
        PlatformValue pValue = new PlatformValue();
        pValue.setCpuCount(2);
        pValue.setName("Test Platform ByPlatformType");
        pValue.setFqdn("Test Platform CreationByPlatformType");
        platformManager.createPlatform(authzSubjectManager.getOverlordPojo(), testPlatformType
            .getId(), pValue, testAgent.getId());
        platformManager.createPlatform(authzSubjectManager.getOverlordPojo(), testPlatformType
            .getId(), pValue, testAgent.getId());
    }

    @Test(expected = AppdefDuplicateFQDNException.class)
    public void testCreatePlatformDuplicateFQDN() throws ApplicationException {
        PlatformValue pValue = new PlatformValue();
        pValue.setCpuCount(2);
        pValue.setName("Test Platform ByPlatformType");
        pValue.setFqdn("Test Platform CreationByPlatformType");
        platformManager.createPlatform(authzSubjectManager.getOverlordPojo(), testPlatformType
            .getId(), pValue, testAgent.getId());
        pValue.setName("Test Platform ByPlatformType1");
        platformManager.createPlatform(authzSubjectManager.getOverlordPojo(), testPlatformType
            .getId(), pValue, testAgent.getId());
    }

    @Test
    public void testCreatePlatformAddVirtualServers() throws ApplicationException,
        NotFoundException {
        // Create virtual server types for test platform type
        ServerType vServerType = createServerType("CPU Server", "1.0",
            new String[] { testPlatformType.getName() }, "Test virtual Server Plugin", true);
        PlatformValue pValue = new PlatformValue();
        pValue.setCpuCount(2);
        pValue.setName("Test Platform ByPlatformType");
        pValue.setFqdn("Test Platform CreationByPlatformType");
        Platform platform = platformManager.createPlatform(authzSubjectManager.getOverlordPojo(),
            testPlatformType.getId(), pValue, testAgent.getId());
        // Check if the virtual server is fetched with the expected virtual
        // server name
        Server server = serverManager.getServerByName(platform, pValue.getName() + " " +
                                                                vServerType.getName());
        assertNotNull(server);
        assertNotNull(platform.getResource());
        assertEquals(platform.getName(), "Test Platform ByPlatformType");
        assertEquals(platform.getFqdn(), "Test Platform CreationByPlatformType");
        assertEquals(platform.getCpuCount(), new Integer(2));
    }

    @Test
    public void testGetAllPlatforms() throws ApplicationException, NotFoundException {
        PageList<PlatformValue> pValues = platformManager.getAllPlatforms(authzSubjectManager
            .getOverlordPojo(), null);
        assertEquals(testPlatforms.size(), pValues.size());
    }

    @Test
    public void testGetRecentPlatforms() throws ApplicationException, NotFoundException {
        long setTime = System.currentTimeMillis();
        int i = 1;
        for (Platform p : testPlatforms) {
            // Set Platforms creation time 20 minutes before the current time.
            p.setCreationTime(setTime - 20 * 60000l);
            i++;
        }
        // Change two of the platform's creation time to recent
        testPlatforms.get(0).setCreationTime(setTime);
        testPlatforms.get(1).setCreationTime(setTime - 2 * 60000l);
        testPlatforms.get(2).setCreationTime(setTime - 3 * 60000l);
        PageList<PlatformValue> pValues = platformManager.getRecentPlatforms(authzSubjectManager
            .getOverlordPojo(), 5 * 60000l, 10);
        assertEquals(3, pValues.size());
    }

    @Test
    public void testGetPlatformById() throws ApplicationException, NotFoundException {
        Platform platform = platformManager.getPlatformById(authzSubjectManager.getOverlordPojo(),
            testPlatform.getId());
        assertEquals("Correct Platform is not fetched", platform, testPlatform);
    }

    @Test(expected = PlatformNotFoundException.class)
    public void testGetPlatformByInvalidId() throws ApplicationException, NotFoundException {
        platformManager.getPlatformById(authzSubjectManager.getOverlordPojo(), -2);
    }

    @Test
    public void testFindPlatformById() throws ApplicationException {
        Platform platform = platformManager.findPlatformById(testPlatform.getId());
        assertEquals("Correct Platform is not found by Id", platform, testPlatform);
    }

    @Test(expected = PlatformNotFoundException.class)
    public void testFindPlatformByInvalidId() throws ApplicationException {
        platformManager.findPlatformById(-2);
    }

    @Test
    public void testGetPlatformByAIPlatformFQDN() throws ApplicationException {
        AIPlatformValue aiPlatform = new AIPlatformValue();
        aiPlatform.setFqdn(testPlatform.getFqdn());
        // Platform platform =
        // platformManager.createPlatform(authzSubjectManager.getOverlordPojo(),
        // aiPlatform);
        // Following method will fetch the platform based on the FQDN
        Platform fetchedPlatform = platformManager.getPlatformByAIPlatform(authzSubjectManager
            .getOverlordPojo(), aiPlatform);
        assertNotNull(fetchedPlatform);
        assertEquals(testPlatform, fetchedPlatform);
    }

    // @Test TODO: Find out why getPhysPlatformByAgentToken() doesn't return a
    // valid platform by agent token
    // public void testGetPlatformByAIPlatformAgentToken() throws
    // ApplicationException {
    // AIPlatformValue aiPlatform = new AIPlatformValue();
    // //First set AIPlatformValue to invalid FQDN
    // aiPlatform.setFqdn("abcd");
    // aiPlatform.setAgentToken(testPlatform.getAgent().getAgentToken());
    // //Following method will fetch the platform based on agent token
    // Platform fetchedPlatform =
    // platformManager.getPlatformByAIPlatform(authzSubjectManager.getOverlordPojo(),
    // aiPlatform);
    // assertNotNull(fetchedPlatform);
    // assertEquals(testPlatform, fetchedPlatform);
    // }

    // @Test
    // public void testGetPlatformByNameAndAuth() throws ApplicationException {
    // PlatformValue fetchedPlatform =
    // platformManager.getPlatformByName(authzSubjectManager.getOverlordPojo(),
    // testPlatform.getName());
    // assertNotNull(fetchedPlatform);
    // assertEquals(testPlatform.getPlatformValue(), fetchedPlatform);
    // }

    @Test
    public void testGetPlatformByName() {
        Platform fetchedPlatform = platformManager.getPlatformByName(testPlatform.getName());
        assertNotNull(fetchedPlatform);
        assertEquals(testPlatform, fetchedPlatform);
    }

    @Test
    public void testFindPlatformByFqdn() throws ApplicationException {
        Platform fetchedPlatform = platformManager.findPlatformByFqdn(authzSubjectManager
            .getOverlordPojo(), testPlatform.getFqdn());
        assertNotNull(fetchedPlatform);
        assertEquals(testPlatform, fetchedPlatform);
    }

    @Test
    public void testFindPlatformByInvalidFqdn() throws ApplicationException {
        try {
            platformManager.findPlatformByFqdn(authzSubjectManager.getOverlordPojo(), "abcd");
        } catch (PlatformNotFoundException e) {
            assertEquals(e.getMessage(), "Platform with fqdn abcd not found");
            return;
        }
        fail("PlatformNotFoundException is not thrown for an invalid FQDN");
    }

    @Test
    public void testGetPlatformByIpAddr() throws ApplicationException {
        PlatformValue pValue = new PlatformValue();
        pValue.setCpuCount(2);
        pValue.setName("Test Platform ByPlatformType");
        pValue.setFqdn("Test Platform CreationByPlatformType");
        IpValue ipValue = new IpValue();
        ipValue.setAddress("127.0.0.1");
        ipValue.setMACAddress("12:34:G0:93:58:96");
        ipValue.setNetmask("255:255:255:0");
        pValue.addIpValue(ipValue);
        Platform platform = platformManager.createPlatform(authzSubjectManager.getOverlordPojo(),
            testPlatformType.getId(), pValue, testAgent.getId());
        Collection<Platform> getPlatforms = platformManager.getPlatformByIpAddr(authzSubjectManager
            .getOverlordPojo(), "127.0.0.1");
        assertNotNull(getPlatforms);
        assertEquals(platform, getPlatforms.iterator().next());
    }

    @Test
    public void testGetPlatformPksByAgentToken() throws ApplicationException {
        List<Integer> platformPKs = (List<Integer>) platformManager.getPlatformPksByAgentToken(
            authzSubjectManager.getOverlordPojo(), "agentToken123");
        List<Integer> testPlatformPKs = new ArrayList<Integer>();
        for (Platform platform : testPlatforms) {
            testPlatformPKs.add(platform.getId());
        }

        java.util.Collections.sort(testPlatformPKs) ; 
        java.util.Collections.sort(platformPKs) ; 
        assertEquals(testPlatformPKs, platformPKs);
    }

    @Test
    public void testGetPlatformPksByInvalidAgentToken() throws ApplicationException {
        try {
            platformManager.getPlatformPksByAgentToken(authzSubjectManager.getOverlordPojo(),
                "agentTokenInvalid");
        } catch (PlatformNotFoundException e) {
            assertEquals(e.getMessage(), "Platform with agent token agentTokenInvalid not found");
            return;
        }
        fail("PlatformNotFoundException is not thrown for an invalid agent token");
    }

    @Test
    public void testGetPlatformByService() throws ApplicationException {
        PlatformValue pValue = platformManager.getPlatformByService(authzSubjectManager
            .getOverlordPojo(), testService.getId());
        assertEquals(testPlatform.getPlatformValue(), pValue);
    }

    @Test
    public void testGetPlatformByInvalidService() throws ApplicationException {
        Integer invalidId = testService.getId() + 12345;
        try {
            platformManager.getPlatformByService(authzSubjectManager.getOverlordPojo(), invalidId);
        } catch (PlatformNotFoundException e) {
            assertEquals(e.getMessage(), "platform for service " + invalidId + " not found");
            return;
        }
        fail("PlatformNotFoundException is not thrown for invalid service");
    }

    @Test
    public void testGetPlatformIdByService() throws ApplicationException {
        Integer platformId = platformManager.getPlatformIdByService(testService.getId());
        assertEquals(testPlatform.getId(), platformId);
    }

    @Test
    public void testGetPlatformIdByInvalidService() throws ApplicationException {
        Integer invalidId = testService.getId() + 12345;
        try {
            platformManager.getPlatformIdByService(invalidId);
        } catch (PlatformNotFoundException e) {
            assertEquals(e.getMessage(), "platform for service " + invalidId + " not found");
            return;
        }
        fail("PlatformNotFoundException is not thrown for invalid service");
    }

    @Test
    public void testGetPlatformByServer() throws ApplicationException {
        PlatformValue pValue = platformManager.getPlatformByServer(authzSubjectManager
            .getOverlordPojo(), testServer.getId());
        assertEquals(testPlatform.getPlatformValue(), pValue);
    }

    @Test
    public void testGetPlatformByInvalidServer() throws ApplicationException {
        Integer invalidId = testServer.getId() + 12345;
        try {
            platformManager.getPlatformByServer(authzSubjectManager.getOverlordPojo(), invalidId);
        } catch (PlatformNotFoundException e) {
            assertEquals(e.getMessage(), "platform for server " + invalidId + " not found");
            return;
        }
        fail("PlatformNotFoundException is not thrown for invalid server");
    }

    @Test
    public void testGetPlatformIdByServer() throws ApplicationException {
        Integer platformId = platformManager.getPlatformIdByServer(testServer.getId());
        assertEquals(testPlatform.getId(), platformId);
    }

    @Test
    public void testGetPlatformIdByInvalidServer() throws ApplicationException {
        Integer invalidId = testServer.getId() + 12345;
        try {
            platformManager.getPlatformIdByServer(invalidId);
        } catch (PlatformNotFoundException e) {
            assertEquals(e.getMessage(), "platform for server " + invalidId + " not found");
            return;
        }
        fail("PlatformNotFoundException is not thrown for invalid server");
    }

    @Test
    public void testGetPlatformsByServers() throws ApplicationException {
        List<AppdefEntityID> serverIds = new ArrayList<AppdefEntityID>();
        serverIds.add(testServer.getEntityId());
        PageList<PlatformValue> pValues = platformManager.getPlatformsByServers(authzSubjectManager
            .getOverlordPojo(), serverIds);
        assertEquals(testPlatform.getPlatformValue(), pValues.get(0));
    }

    @Test
    public void testGetPlatformsByApplication() throws ApplicationException, NotFoundException {
        AppdefEntityID serviceId= testService.getEntityId();
        List<AppdefEntityID> services = new ArrayList<AppdefEntityID>();
        services.add(serviceId);
        Application app = createApplication("Test Application", "testing", GENERIC_APPLICATION_TYPE, services);
        flushSession();
        //clear the session to update the bi-directional app to app service relationship
        clearSession();
        PageControl pc = new PageControl();
        PageList<PlatformValue> pValues = platformManager.getPlatformsByApplication(
            authzSubjectManager.getOverlordPojo(), app.getId(), pc);
        assertEquals(testPlatform.getPlatformValue(), pValues.get(0));
    }

    @Test
    public void testGetPlatformIdsByType() throws ApplicationException {
        Integer[] platformIds = platformManager.getPlatformIds(authzSubjectManager
            .getOverlordPojo(), testPlatform.getPlatformType().getId());
        assertEquals(platformIds[0], testPlatform.getId());
    }

    @Test
    public void testGetPlatformsByType() throws ApplicationException {
        List<Platform> platforms = platformManager.getPlatformsByType(authzSubjectManager
            .getOverlordPojo(), testPlatform.getPlatformType().getName());
        assertEquals(platforms.get(0), testPlatform);
    }

    @Test
    public void testFindPlatformsByIpAddr() throws ApplicationException {
        PlatformValue pValue = new PlatformValue();
        pValue.setCpuCount(2);
        pValue.setName("Test Platform ByPlatformType");
        pValue.setFqdn("Test Platform CreationByPlatformType");
        IpValue ipValue = new IpValue();
        ipValue.setAddress("127.0.0.1");
        ipValue.setMACAddress("12:34:G0:93:58:96");
        ipValue.setNetmask("255:255:255:0");
        pValue.addIpValue(ipValue);
        Platform platform = platformManager.createPlatform(authzSubjectManager.getOverlordPojo(),
            testPlatformType.getId(), pValue, testAgent.getId());
        PageControl pc = new PageControl();
        PageList<PlatformValue> foundPlatformValue = platformManager.findPlatformsByIpAddr(
            authzSubjectManager.getOverlordPojo(), "127.0.0.1", pc);
        assertEquals(platform.getPlatformValue(), foundPlatformValue.get(0));
        foundPlatformValue = platformManager.findPlatformsByIpAddr(authzSubjectManager
            .getOverlordPojo(), "127.0.0.2", pc);
        assertTrue(foundPlatformValue.isEmpty());
    }

    @Test
    public void testFindPlatformPojosByTypeAndName() throws ApplicationException {
        List<Platform> platforms = new ArrayList<Platform>(10);
        for (int i = 1; i <= 10; i++) {
            AIPlatformValue aiPlatform = new AIPlatformValue();
            aiPlatform.setName("RegexTestPlatform" + i);
            aiPlatform.setCpuCount(2);
            aiPlatform.setPlatformTypeName(testPlatformTypes.get(0).getName());
            aiPlatform.setAgentToken("agentToken123");
            aiPlatform.setFqdn("RegexTestPlatform" + i);
            platforms.add(i - 1, platformManager.createPlatform(authzSubjectManager
                .getOverlordPojo(), aiPlatform));
        }
        List<Platform> foundPlatforms = platformManager.findPlatformPojosByTypeAndName(
            authzSubjectManager.getOverlordPojo(), testPlatformTypes.get(0).getId(),
            "RegexTestPlatform");

        assertEqualsList(platforms,foundPlatforms) ;
        //assertEquals(platforms, foundPlatforms);
    }

    private final void assertEqualsList(final List expected, final List actual) { 
        boolean areEqual = true ; 

        if(expected == null)  areEqual = (actual == null) ; 
        else if(actual == null)  areEqual = false ; 
        else if(expected.size() != actual.size()) areEqual = false ; 
        else { 
            for(Object o : expected) { 
                if(!actual.contains(o)) { 
                    areEqual = false ; 
                    break ; 
                }//EO if not the same 
            }//EO while there are more items      
        }//EO else if not null or different sizes 

        if(!areEqual) fail("expected same:<"+expected+"> was not:<"+actual+">") ;
    }//EOM 

    // TODO
    public void testFindParentPlatformPojosByNetworkRelation() {
        fail("Not yet implemented");
    }

    // TODO
    public void testFindPlatformPojosByNoNetworkRelation() {
        fail("Not yet implemented");
    }

    @Test
    public void testFindPlatformPojosByIpAddr() throws ApplicationException {
        PlatformValue pValue = new PlatformValue();
        pValue.setCpuCount(2);
        pValue.setName("Test Platform ByPlatformType");
        pValue.setFqdn("Test Platform CreationByPlatformType");
        IpValue ipValue = new IpValue();
        ipValue.setAddress("127.0.0.1");
        ipValue.setMACAddress("12:34:G0:93:58:96");
        ipValue.setNetmask("255:255:255:0");
        pValue.addIpValue(ipValue);
        Platform platform = platformManager.createPlatform(authzSubjectManager.getOverlordPojo(),
            testPlatformType.getId(), pValue, testAgent.getId());
        Collection<Platform> foundPlatform = platformManager.findPlatformPojosByIpAddr("127.0.0.1");
        assertEquals(platform, foundPlatform.iterator().next());
    }

    // TODO
    public void testFindDeletedPlatforms() {
        fail("Not yet implemented");
    }

    @Test
    public void testUpdatePlatformImpl() throws ApplicationException {
        PlatformValue pv = new PlatformValue();
        pv.setCpuCount(2);
        pv.setName("Test Platform ByPlatformType");
        pv.setFqdn("Test Platform CreationByPlatformType");
        Platform platform = platformManager.createPlatform(authzSubjectManager.getOverlordPojo(),
            testPlatformType.getId(), pv, testAgent.getId());
        pv.setCpuCount(null);
        pv.setName("Updated Platform");
        pv.setFqdn("UpdatedPlatform");
        pv.setId(platform.getId());
        Platform updatedPlatform = platformManager.updatePlatformImpl(authzSubjectManager
            .getOverlordPojo(), pv);
        assertEquals(updatedPlatform.getCpuCount().intValue(), 2);
        assertEquals(updatedPlatform.getName(), "Updated Platform");
        assertEquals(updatedPlatform.getFqdn(), "UpdatedPlatform");
    }

    @Test(expected = AppdefDuplicateNameException.class)
    public void testUpdatePlatformImplDupName() throws ApplicationException {
        PlatformValue pv = new PlatformValue();
        pv.setCpuCount(2);
        pv.setName("Test Platform ByPlatformType");
        pv.setFqdn("Test Platform CreationByPlatformType");
        Platform platform = platformManager.createPlatform(authzSubjectManager.getOverlordPojo(),
            testPlatformType.getId(), pv, testAgent.getId());
        pv.setCpuCount(null);
        // Update the name to an existing platform name (testPlatform)
        pv.setName(testPlatform.getName());
        pv.setFqdn("UpdatedPlatform");
        pv.setId(platform.getId());
        platformManager.updatePlatformImpl(authzSubjectManager.getOverlordPojo(), pv);
    }

    @Test(expected = AppdefDuplicateFQDNException.class)
    public void testUpdatePlatformImplDupFqdn() throws ApplicationException {
        PlatformValue pv = new PlatformValue();
        pv.setCpuCount(2);
        pv.setName("Test Platform ByPlatformType");
        pv.setFqdn("Test Platform CreationByPlatformType");
        Platform platform = platformManager.createPlatform(authzSubjectManager.getOverlordPojo(),
            testPlatformType.getId(), pv, testAgent.getId());
        pv.setCpuCount(null);
        // Update the name to an existing platform name (testPlatform)
        pv.setName("Updated Platform");
        pv.setFqdn(testPlatform.getFqdn());
        pv.setId(platform.getId());
        platformManager.updatePlatformImpl(authzSubjectManager.getOverlordPojo(), pv);
    }

    // TODO:
    /*
     * Yet to confirm how does the method updatePlatformTypes updates the
     * platformType name
     */
    public void testUpdatePlatformTypes() throws NotFoundException, VetoException {
        PlatformTypeInfo[] pInfos = new PlatformTypeInfo[] { new PlatformTypeInfo("Linux") };
        platformManager.updatePlatformTypes("Test Plugin", pInfos);
    }

    @Test
    public void testCreatePlatformType() throws NotFoundException {
        String platformTypeName = "platformType";
        String plugin = "Test PlatformType Plugin";
        PlatformType pType = platformManager.createPlatformType(platformTypeName, plugin);
        assertEquals(pType.getName(), platformTypeName);
        assertEquals(pType.getPlugin(), plugin);
        assertNotNull(platformManager.findResource(pType));
        assertEquals(platformManager.findResource(pType).getName(), platformTypeName);
        assertEquals(platformManager.findResource(pType).getResourceType(), resourceManager
            .findResourceTypeByName(AuthzConstants.platformPrototypeTypeName));
    }

    @Test
    public void testUpdateWithAI() throws ApplicationException {
        AIPlatformValue aiPlatform = new AIPlatformValue();
        // Set AIPlatformValue for the testPlatform
        aiPlatform.setPlatformTypeName(testPlatform.getPlatformType().getName());
        aiPlatform.setAgentToken(testPlatform.getAgent().getAgentToken());
        aiPlatform.setFqdn(testPlatform.getFqdn());
        // Now set the name & CPU count of the platform
        aiPlatform.setName("Updated PlatformName");
        aiPlatform.setCpuCount(4);
        platformManager.updateWithAI(aiPlatform, authzSubjectManager.getOverlordPojo());
        assertEquals(testPlatform.getName(), "Updated PlatformName");
        assertEquals(testPlatform.getResource().getName(), "Updated PlatformName");
        assertEquals(testPlatform.getCpuCount().intValue(), 4);
    }

    // TODO
    // Currently the method updateWithAI -> getPlatformByAIPlatform() ->
    // getPhysPlatformByAgentToken(agentToken) has some issues
    // Once this is fixed, we can add the usecase to update FQDN
    public void testUpdateAIForFQDNChange() {
    }

    // TODO
    public void testUpdateAIIpValues() throws ApplicationException {
        AIPlatformValue aiPlatform = new AIPlatformValue();
        // Set AIPlatformValue for the testPlatform
        aiPlatform.setPlatformTypeName(testPlatform.getPlatformType().getName());
        aiPlatform.setAgentToken(testPlatform.getAgent().getAgentToken());
        aiPlatform.setFqdn(testPlatform.getFqdn());
        AIIpValue aiIpVal = new AIIpValue();
        aiIpVal.setAddress("192.168.1.2");
        aiIpVal.setMACAddress("12:34:G0:93:58:96");
        aiIpVal.setNetmask("255:255:255:0");
        // Queue status to AIQueueConstants.Q_STATUS_REMOVED
        aiIpVal.setQueueStatus(3);
        aiPlatform.addAIIpValue(aiIpVal);
        // Now perform updateAI
        // TODO: why update of AgentIP happens only if the queue status removed?
        platformManager.updateWithAI(aiPlatform, authzSubjectManager.getOverlordPojo());
    }

    @Test
    public void testAddIp() {
        platformManager.addIp(testPlatform, "127.0.0.1", "255:255:255:0", "12:34:G0:93:58:96");
        platformManager.addIp(testPlatform, "192.168.1.2", "255:255:0:0", "91:34:45:93:67:96");
        Collection<Ip> ips = testPlatform.getIps();
        assertEquals(ips.size(), 2);
        for (Ip ip : ips) {
            if (ip.getAddress().equals("192.168.1.2")) {
                assertEquals(ip.getMacAddress(), "91:34:45:93:67:96");
                assertEquals(ip.getNetmask(), "255:255:0:0");
            } else {
                assertEquals(ip.getAddress(), "127.0.0.1");
                assertEquals(ip.getMacAddress(), "12:34:G0:93:58:96");
                assertEquals(ip.getNetmask(), "255:255:255:0");
            }
        }
    }

    @Test
    public void testUpdateIp() {
        platformManager.addIp(testPlatform, "127.0.0.1", "255:255:255:0", "12:34:G0:93:58:96");
        Collection<Ip> ips = testPlatform.getIps();
        for (Ip ip : ips) {
            assertEquals(ip.getAddress(), "127.0.0.1");
            assertEquals(ip.getMacAddress(), "12:34:G0:93:58:96");
            assertEquals(ip.getNetmask(), "255:255:255:0");
        }
        platformManager.updateIp(testPlatform, "127.0.0.1", "255:255:0:0", "91:34:45:93:67:96");
        ips = testPlatform.getIps();
        for (Ip ip : ips) {
            assertEquals(ip.getAddress(), "127.0.0.1");
            assertEquals(ip.getMacAddress(), "91:34:45:93:67:96");
            assertEquals(ip.getNetmask(), "255:255:0:0");
        }
    }

    @Test
    public void testRemoveIp() {
        platformManager.addIp(testPlatform, "127.0.0.1", "255:255:255:0", "12:34:G0:93:58:96");
        platformManager.addIp(testPlatform, "192.168.1.2", "255:255:0:0", "91:34:45:93:67:96");
        platformManager.removeIp(testPlatform, "192.168.1.2", "255:255:0:0", "91:34:45:93:67:96");
        Collection<Ip> ips = testPlatform.getIps();
        assertEquals(ips.size(), 1);
        for (Ip ip : ips) {
            assertEquals(ip.getAddress(), "127.0.0.1");
            assertEquals(ip.getMacAddress(), "12:34:G0:93:58:96");
            assertEquals(ip.getNetmask(), "255:255:255:0");
        }
    }

    @Test
    public void testGetPlatformTypeCounts() {

        List<Object[]> counts = platformManager.getPlatformTypeCounts();
        List<Object[]> actuals = new ArrayList<Object[]>(10);
        // Add the Linux testPlatformType as the result is sorted
        actuals.add(0, new Object[] { testPlatformTypes.get(9).getName(), Long.valueOf("1") });
        for (int i = 1; i <= 9; i++) {
            // Add platform Type name and count (here count is always 1)
            actuals.add(i,
                new Object[] { testPlatformTypes.get(i - 1).getName(), Long.valueOf("1") });
        }
        for (int i = 0; i <= 9; i++) {
            assertEquals((String) counts.get(i)[0], ((String) actuals.get(i)[0]));
            assertEquals((Long) counts.get(i)[1], ((Long) actuals.get(i)[1]));
        }
    }

    @Test
    public void testGetPlatformCount() {
        // we have added 10 test platforms during initial setup
        assertEquals(platformManager.getPlatformCount().intValue(), 10);
    }

    @Test
    public void testGetCpuCount() {
        // 10 test platforms with each having 2 CPUs
        assertEquals(platformManager.getCpuCount().intValue(), 20);
    }
}
