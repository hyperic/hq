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
package org.hyperic.hq.measurement.server.session;

import static org.easymock.EasyMock.*;
import static org.junit.Assert.*;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import org.easymock.Capture;
import org.hyperic.hq.appdef.Agent;
import org.hyperic.hq.appdef.server.session.Platform;
import org.hyperic.hq.appdef.server.session.ResourceCreatedZevent;
import org.hyperic.hq.appdef.server.session.ResourceRefreshZevent;
import org.hyperic.hq.appdef.server.session.ResourceUpdatedZevent;
import org.hyperic.hq.appdef.server.session.ResourceZevent;
import org.hyperic.hq.appdef.server.session.Server;
import org.hyperic.hq.appdef.server.session.ServerType;
import org.hyperic.hq.appdef.shared.AppdefEntityID;
import org.hyperic.hq.appdef.shared.PlatformManager;
import org.hyperic.hq.appdef.shared.ServerManager;
import org.hyperic.hq.authz.shared.PermissionException;
import org.hyperic.hq.authz.shared.ResourceManager;
import org.hyperic.hq.measurement.shared.MeasurementManager;
import org.hyperic.hq.measurement.shared.SRNManager;
import org.hyperic.hq.test.BaseInfrastructureTest;
import org.hyperic.util.config.ConfigResponse;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.annotation.DirtiesContext;

/**
 * Integration test of {@link MeasurementManagerImpl}
 * @author jhickey
 * 
 */
@DirtiesContext
public class MeasurementManagerTest
    extends BaseInfrastructureTest {

    @Autowired
    private MeasurementManager measurementManager;

    private Agent agent;

    private AppdefEntityID[] servers;

    @Autowired
    private ResourceManager resourceManager;

    @Autowired
    private ServerManager serverManager;
    
    @Autowired
    private PlatformManager platformManager;

    @Before
    public void setUp() throws Exception {
        String agentToken = "agentToken123";
        agent = createAgent("127.0.0.1", 2144, "authToken", agentToken, "5.0");
        // Using the automatically deployed test plugin
        Platform platform = createPlatform(agentToken, "PluginTestPlatform", "Platform1",
            "Platform1");
        ServerType serverType = serverManager.findServerTypeByName("PluginTestServer 1.0");
        Server server = createServer(platform, serverType, "Server1");
        Server server2 = createServer(platform, serverType, "Server2");
        servers = new AppdefEntityID[] { server.getEntityId(), server2.getEntityId() };
        measurementManager.createDefaultMeasurements(authzSubjectManager.getOverlordPojo(), server
            .getEntityId(), "PluginTestServer 1.0", new ConfigResponse());
        measurementManager.createDefaultMeasurements(authzSubjectManager.getOverlordPojo(), server2
            .getEntityId(), "PluginTestServer 1.0", new ConfigResponse());
        flushSession();
    }

    @Test
    public void testDisableMeasurementsForDeletion() throws PermissionException {
        measurementManager.disableMeasurementsForDeletion(authzSubjectManager.getOverlordPojo(),
            agent, servers);
        flushSession();
        clearSession();
        List<Measurement> server1Measurements = measurementManager.findMeasurements(
            authzSubjectManager.getOverlordPojo(), resourceManager.findResource(servers[0]));
        // The test is not valid unless the test plugin has defined at least 1
        // measurement for the server type
        assertTrue(server1Measurements.size() > 0);
        List<Measurement> server2Measurements = measurementManager.findMeasurements(
            authzSubjectManager.getOverlordPojo(), resourceManager.findResource(servers[1]));
        // The test is not valid unless the test plugin has defined at least 1
        // measurement for the server type
        assertTrue(server2Measurements.size() > 0);
        // verify all metrics were disabled
        assertTrue(measurementManager.findEnabledMeasurements(Arrays.asList(servers)).isEmpty());
    }
    
    @Test
    public void testFindDesignatedMeasurements(){
    	List<Measurement> measurements = measurementManager.findDesignatedMeasurements(servers[0]);
        assertTrue(measurements.size() > 0);
        for (Measurement m: measurements){
        	assertTrue(m.getTemplate().isDesignate());
        }
    }
    
    @Test
    public void testHandleCreateRefreshEvents() throws Exception {
    	SRNManager srnManager = createMock(SRNManager.class);
    	measurementManager.setSrnManager(srnManager);
    	//EasyMock.replay(zeventManagerMock);

        Platform p = platformManager.getPlatformByName("Platform1");
        ServerType serverType = serverManager.findServerTypeByName("PluginTestServer 1.0");
        Server server = createServer(p, serverType, "Server2");
        flushSession();

        ResourceZevent event = new ResourceCreatedZevent(authzSubjectManager.getOverlordPojo(), server.getEntityId());
        ResourceZevent eventRef = new ResourceRefreshZevent(authzSubjectManager.getOverlordPojo(), server.getEntityId());
    	ResourceZevent eventUpd = new ResourceUpdatedZevent(authzSubjectManager.getOverlordPojo(), server.getEntityId());

    	// set up mock for method returning void and capturing
    	Capture<Collection<AppdefEntityID>> capture = new Capture<Collection<AppdefEntityID>>();
    	srnManager.scheduleInBackground(and(capture(capture), isA(Collection.class)), anyBoolean(), anyBoolean());
    	expectLastCall().times(3);
    	replay(srnManager);

    	measurementManager.handleCreateRefreshEvents(Arrays.asList(event));    	    	
    	measurementManager.handleCreateRefreshEvents(Arrays.asList(eventRef));
    	assertTrue(capture.hasCaptured());

/*    	EasyMock.expectLastCall().times(1);
    	EasyMock.replay(zeventManagerMock);
*/    	List<Measurement> measurements = measurementManager.findDesignatedMeasurements(server.getEntityId());
        for (Measurement m: measurements){
        	//m.getTem
        }

    	measurementManager.handleCreateRefreshEvents(Arrays.asList(eventUpd));
    	Assert.assertTrue(capture.hasCaptured());
    }
    
    
}
