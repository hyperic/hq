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
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.hyperic.hq.appdef.shared.AppdefEntityID;
import org.hyperic.hq.appdef.shared.AppdefEntityTypeID;
import org.hyperic.hq.appdef.shared.AppdefManager;
import org.hyperic.hq.authz.server.session.Role;
import org.hyperic.hq.authz.server.session.RoleDAO;
import org.hyperic.hq.test.BaseInfrastructureTest;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.annotation.DirtiesContext;
/**
 * Integration test of the {@link AppdefManagerImpl}
 * @author jhickey
 *
 */
@DirtiesContext
public class AppdefManagerTest
    extends BaseInfrastructureTest {

    @Autowired
    private AppdefManager appdefManager;

    @Autowired
    private RoleDAO roleDAO;

    private PlatformType platformType;

    private ServerType serverType;

    private ServiceType serviceType;

    @Before
    public void setUp() throws Exception {
        String agentToken = "agentToken123";
        createAgent("127.0.0.1", 2144, "authToken", agentToken, "5.0");
        platformType = createPlatformType("TestPlatform", "test");
        Platform platform = createPlatform(agentToken, "TestPlatform", "Platform1", "Platform1");
        // getControllablePlatformTypes requires control config to be not null
        platform.getConfigResponse().setControlResponse(new byte[0]);
        Set<Platform> platforms = new HashSet<Platform>();
        platforms.add(platform);
        Role superUser = roleDAO.findByName("Super User Role");
        List<Role> roles = new ArrayList<Role>();
        roles.add(superUser);
        createPlatformResourceGroup(platforms, "MyPlatforms", roles);
        serverType = createServerType("TestServer", "6.0", new String[] { "TestPlatform" }, "test",
            false);
        Server server = createServer(platform, serverType, "Server1");
        server.getConfigResponse().setControlResponse(new byte[0]);
        Set<Server> servers = new HashSet<Server>(1);
        servers.add(server);
        createServerResourceGroup(servers, "MyServers", roles);
        serviceType = createServiceType("TestService", "test", serverType);
        Service service = createService(server, serviceType, "Service1", "desc", "location");
        service.getConfigResponse().setControlResponse(new byte[0]);
        Set<Service> services = new HashSet<Service>(1);
        services.add(service);
        createServiceResourceGroup(services, "MyServices");
        flushSession();
    }

    @Test
    public void testGetControllablePlatformTypes() throws Exception {
        Map<String, AppdefEntityID> platformTypes = appdefManager
            .getControllablePlatformTypes(authzSubjectManager.getOverlordPojo());
        assertEquals(1, platformTypes.size());
        assertEquals("TestPlatform", platformTypes.keySet().iterator().next());
    }

    @Test
    public void testGetControllablePlatformNames() throws Exception {
        Map<String, AppdefEntityID> platformNames = appdefManager.getControllablePlatformNames(
            authzSubjectManager.getOverlordPojo(), platformType.getId());
        assertEquals(1, platformNames.size());
        assertEquals("Platform1", platformNames.keySet().iterator().next());
    }

    @Test
    public void testGetControllableServerTypes() throws Exception {
        Map<String, AppdefEntityTypeID> serverTypes = appdefManager
            .getControllableServerTypes(authzSubjectManager.getOverlordPojo());
        assertEquals(1, serverTypes.size());
        assertEquals("TestServer", serverTypes.keySet().iterator().next());
    }

    @Test
    public void testGetControllableServerNames() throws Exception {
        Map<String, AppdefEntityID> serverNames = appdefManager.getControllableServerNames(
            authzSubjectManager.getOverlordPojo(), serverType.getId());
        assertEquals(1, serverNames.size());
        assertEquals("Server1", serverNames.keySet().iterator().next());
    }

    @Test
    public void testGetControllableServiceTypes() throws Exception {
        Map<String, AppdefEntityTypeID> serviceTypes = appdefManager
            .getControllableServiceTypes(authzSubjectManager.getOverlordPojo());
        assertEquals(1, serviceTypes.size());
        assertEquals("TestService", serviceTypes.keySet().iterator().next());
    }

    @Test
    public void testGetControllableServiceNames() throws Exception {
        Map<String, AppdefEntityID> serviceNames = appdefManager.getControllableServiceNames(
            authzSubjectManager.getOverlordPojo(), serviceType.getId());
        assertEquals(1, serviceNames.size());
        assertEquals("Service1", serviceNames.keySet().iterator().next());
    }
}
