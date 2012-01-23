/**
 * NOTE: This copyright does *not* cover user programs that use Hyperic
 * program services by normal system calls through the application
 * program interfaces provided as part of the Hyperic Plug-in Development
 * Kit or the Hyperic Client Development Kit - this is merely considered
 * normal use of the program, and does *not* fall under the heading of
 *  "derived work".
 *
 *  Copyright (C) [2010], VMware, Inc.
 *  This file is part of Hyperic.
 *
 *  Hyperic is free software; you can redistribute it and/or modify
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
package org.hyperic.hq.bizapp.server.session;

import org.hyperic.hq.appdef.Agent;
import org.hyperic.hq.appdef.server.session.Platform;
import org.hyperic.hq.appdef.server.session.PlatformType;
import org.hyperic.hq.appdef.server.session.Server;
import org.hyperic.hq.appdef.server.session.ServerType;
import org.hyperic.hq.appdef.server.session.Service;
import org.hyperic.hq.appdef.server.session.ServiceType;
import org.hyperic.hq.appdef.shared.AgentManager;
import org.hyperic.hq.appdef.shared.AppdefDuplicateNameException;
import org.hyperic.hq.appdef.shared.PlatformManager;
import org.hyperic.hq.appdef.shared.PlatformNotFoundException;
import org.hyperic.hq.appdef.shared.PlatformValue;
import org.hyperic.hq.appdef.shared.ServerManager;
import org.hyperic.hq.appdef.shared.ServerValue;
import org.hyperic.hq.appdef.shared.ServiceManager;
import org.hyperic.hq.appdef.shared.ValidationException;
import org.hyperic.hq.authz.shared.AuthzSubjectManager;
import org.hyperic.hq.authz.shared.PermissionException;
import org.hyperic.hq.common.ApplicationException;
import org.hyperic.hq.common.NotFoundException;
import org.hyperic.hq.context.IntegrationTestContextLoader;
import org.hyperic.hq.context.IntegrationTestSpringJUnit4ClassRunner;
import org.hyperic.hq.measurement.shared.MeasurementManager;
import org.hyperic.util.config.ConfigResponse;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;

/**
 * Adds a specified number of FileService services to a DB for subsequent
 * performance testing. Need to run against a DB after a clean server startup
 * has registered all the plugins (so we can create services of type FileServer)
 * @author jhickey
 * 
 */
@RunWith(IntegrationTestSpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = "classpath*:META-INF/spring/*-context.xml", loader = IntegrationTestContextLoader.class)
@DirtiesContext
public class PerformanceTestPopulator {

    @Autowired
    private PlatformManager platformManager;

    @Autowired
    private AgentManager agentManager;

    @Autowired
    private AuthzSubjectManager authzSubjectManager;

    @Autowired
    private ServiceManager serviceManager;
    @Autowired
    private ServerManager serverManager;

    @Autowired
    private MeasurementManager measurementManager;

    private static final int SERVICE_COUNT = 3000;

    @Test
    public void setUp() throws Exception {
        agentManager.createLegacyAgent("127.0.0.1", 2144, "hqadmin", "agentToken", "4.5");
        PlatformType platformType = platformManager.findPlatformTypeByName("MacOSX");
        Platform platform = createPlatform(platformType, "Platform1", "Platform1", "10.16.17.232",
            agentManager.getAgent("agentToken"));

        ServerType serverType = serverManager.findServerTypeByName("FileServer");
        Server server = createServer(platform, serverType, "FileServer1", "FileServer1", "/foo");
        ServiceType serviceType = serviceManager.findServiceTypeByName("FileServer File");
        for (int i = 1; i <= SERVICE_COUNT; i++) {
            Service service = serviceManager.createService(authzSubjectManager.getOverlordPojo(),
                server.getId(), serviceType.getId(), "FileService" + i, "Something", "my computer");
            measurementManager.createDefaultMeasurements(authzSubjectManager.getOverlordPojo(),
                service.getEntityId(), "FileServer File", new ConfigResponse());
        }
        // above creates 110 for platform + (SERVICE_COUNT * 8) entries in
        // EAM_MEASUREMENT
    }

    private Platform createPlatform(PlatformType platformType, String fqdn, String name,
                                    String remoteIp, Agent agent) throws ApplicationException {

        PlatformValue platform = new PlatformValue();
        platform.setCpuCount(2);
        platform.setPlatformType(platformType.getPlatformTypeValue());
        platform.setAgent(agent);
        platform.setFqdn(fqdn);
        platform.setName(name);

        Platform newPlatform = platformManager.createPlatform(
            authzSubjectManager.getOverlordPojo(), platformType.getId(), platform, agent.getId());

        // always add loopback
        platformManager.addIp(newPlatform, "127.0.0.1", "255.0.0.0", "00:00:00:00:00:00");
        platformManager.addIp(newPlatform, remoteIp, "255.0.0.0", "00:00:00:00:00:00");

        return newPlatform;
    }

    private Server createServer(Platform platform, ServerType serverType, String name,
                                String autoInvId, String installpath)
        throws PlatformNotFoundException, AppdefDuplicateNameException, ValidationException,
        PermissionException, NotFoundException {
        ServerValue server = new ServerValue();
        server.setName(name);
        server.setAutoinventoryIdentifier(autoInvId);
        server.setInstallPath(installpath);
        return serverManager.createServer(authzSubjectManager.getOverlordPojo(), platform.getId(),
            serverType.getId(), server);
    }

    
}
