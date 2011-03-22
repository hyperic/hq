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

import static org.junit.Assert.assertNotNull;

import java.util.List;

import org.hyperic.hq.appdef.server.session.Platform;
import org.hyperic.hq.appdef.server.session.Server;
import org.hyperic.hq.appdef.server.session.ServerType;
import org.hyperic.hq.appdef.shared.ServerManager;
import org.hyperic.hq.measurement.shared.MeasurementManager;
import org.hyperic.hq.test.BaseInfrastructureTest;
import org.hyperic.util.config.ConfigResponse;
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

    @Autowired
    private ServerManager serverManager;
    
    private Integer measId;

    @Before
    public void setUp() throws Exception {
        String agentToken = "agentToken123";
        createAgent("127.0.0.1", 2144, "authToken", agentToken, "5.0");
        // Using the automatically deployed test plugin
        Platform platform = createPlatform(agentToken, "PluginTestPlatform","Platform1",
            "Platform1",3);
        ServerType serverType = serverManager.findServerTypeByName("PluginTestServer 1.0");
        Server server = createServer(platform, serverType, "Server1");
        Server server2 = createServer(platform, serverType, "Server2");
        List<Measurement> defaultMeasurements = measurementManager.createDefaultMeasurements(authzSubjectManager.getOverlordPojo(), server
            .getEntityId(), "PluginTestServer 1.0", new ConfigResponse());
        measId = defaultMeasurements.get(0).getId();
        measurementManager.createDefaultMeasurements(authzSubjectManager.getOverlordPojo(), server2
            .getEntityId(), "PluginTestServer 1.0", new ConfigResponse());
        flush();
    }
    
    @Test
    public void testGetMeasurement() {
        Measurement m = measurementManager.getMeasurement(measId);
        assertNotNull(m);
    }

}
