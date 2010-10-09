/**
 * NOTE: This copyright does *not* cover user programs that use HQ
 * program services by normal system calls through the application
 * program interfaces provided as part of the Hyperic Plug-in Development
 * Kit or the Hyperic Client Development Kit - this is merely considered
 * normal use of the program, and does *not* fall under the heading of
 *  "derived work".
 *
 *  Copyright (C) [2009-2010], VMware, Inc.
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

package org.hyperic.hq.control.server.session;

import junit.framework.TestCase;

import org.easymock.EasyMock;
import org.hyperic.hq.appdef.server.session.Server;
import org.hyperic.hq.appdef.server.session.Service;
import org.hyperic.hq.appdef.shared.AppdefEntityConstants;
import org.hyperic.hq.appdef.shared.AppdefEntityID;
import org.hyperic.hq.appdef.shared.ConfigManager;
import org.hyperic.hq.appdef.shared.ServerManager;
import org.hyperic.hq.autoinventory.shared.AutoinventoryManager;
import org.hyperic.hq.measurement.shared.TrackerManager;
import org.hyperic.hq.product.ProductPlugin;
import org.hyperic.util.config.ConfigResponse;

/**
 * Unit test of the {@link ServerRestartHandler}
 * @author jhickey
 * 
 */
public class ServerRestartHandlerTest
    extends TestCase
{

    private ServerRestartHandlerImpl serverRestartHandler;

    private AutoinventoryManager autoInvManager;

    private ServerManager serverManager;

    private ConfigManager configManager;

    private TrackerManager trackerManager;

    private static final AppdefEntityID SERVER_ID = new AppdefEntityID(2, 3);

    private void replay() {
        EasyMock.replay(autoInvManager, serverManager, configManager, trackerManager);
    }

    public void setUp() throws Exception {
        super.setUp();
        this.autoInvManager = EasyMock.createMock(AutoinventoryManager.class);
        this.serverManager = EasyMock.createMock(ServerManager.class);
        this.configManager = EasyMock.createMock(ConfigManager.class);
        this.trackerManager = EasyMock.createMock(TrackerManager.class);
        this.serverRestartHandler = new ServerRestartHandlerImpl(serverManager,
                                                             configManager,
                                                             autoInvManager,
                                                             trackerManager);
        this.serverRestartHandler.setStartDelay(0);
    }

    /**
     * Verifies successful behavior of restart handler
     * @throws Exception
     */
    public void testRestartServer() throws Exception {
        Server server = new Server();
        Service service = new Service();
        Integer serviceId = Integer.valueOf(889);
        service.setId(serviceId);
        AppdefEntityID serviceEntityId = new AppdefEntityID(AppdefEntityConstants.APPDEF_TYPE_SERVICE, serviceId);
        server.addService(service);
        EasyMock.expect(serverManager.findServerById(SERVER_ID.getId())).andReturn(server);
        autoInvManager.toggleRuntimeScan(null, SERVER_ID, true);
        ConfigResponse response = new ConfigResponse();
        EasyMock.expect(configManager.getMergedConfigResponse(null, ProductPlugin.TYPE_MEASUREMENT, SERVER_ID, true))
                .andReturn(response);

        trackerManager.enableTrackers(null, SERVER_ID, response);
        EasyMock.expect(configManager.getMergedConfigResponse(null,
                                                              ProductPlugin.TYPE_MEASUREMENT,
                                                              serviceEntityId,
                                                              true)).andReturn(response);

        trackerManager.enableTrackers(null, serviceEntityId, response);
        replay();
        serverRestartHandler.serverRestarted(SERVER_ID);
        verify();
    }

    /**
     * Verifies an Exception is thrown and trackers still enabled if failure
     * occurs during auto-discovery
     * @throws Exception
     */
    public void testRestartServerFailedAutoDiscovery() throws Exception {
        Server server = new Server();
        Service service = new Service();
        Integer serviceId = Integer.valueOf(889);
        service.setId(serviceId);
        AppdefEntityID serviceEntityId = new AppdefEntityID(AppdefEntityConstants.APPDEF_TYPE_SERVICE, serviceId);
        server.addService(service);
        EasyMock.expect(serverManager.findServerById(SERVER_ID.getId())).andReturn(server);
        autoInvManager.toggleRuntimeScan(null, SERVER_ID, true);
        EasyMock.expectLastCall().andThrow(new RuntimeException("No!"));
        ConfigResponse response = new ConfigResponse();
        EasyMock.expect(configManager.getMergedConfigResponse(null, ProductPlugin.TYPE_MEASUREMENT, SERVER_ID, true))
                .andReturn(response);

        trackerManager.enableTrackers(null, SERVER_ID, response);
        EasyMock.expect(configManager.getMergedConfigResponse(null,
                                                              ProductPlugin.TYPE_MEASUREMENT,
                                                              serviceEntityId,
                                                              true)).andReturn(response);

        trackerManager.enableTrackers(null, serviceEntityId, response);
        replay();
        try {
            serverRestartHandler.serverRestarted(SERVER_ID);
            fail("Expected an Exception to be thrown on failed auto-discovery");
        } catch (Exception e) {
            verify();
        }
    }

    /**
     * Verifies that nothing blows up and service trackers are still enabled if
     * failure occurs enabling server trackers
     * @throws Exception
     */
    public void testRestartServerFailedServerConfigLogTrackEnable() throws Exception {
        Server server = new Server();
        Service service = new Service();
        Integer serviceId = Integer.valueOf(889);
        service.setId(serviceId);
        AppdefEntityID serviceEntityId = new AppdefEntityID(AppdefEntityConstants.APPDEF_TYPE_SERVICE, serviceId);
        server.addService(service);
        ConfigResponse response = new ConfigResponse();
        EasyMock.expect(serverManager.findServerById(SERVER_ID.getId())).andReturn(server);
        autoInvManager.toggleRuntimeScan(null, SERVER_ID, true);
        EasyMock.expect(configManager.getMergedConfigResponse(null, ProductPlugin.TYPE_MEASUREMENT, SERVER_ID, true))
                .andThrow(new RuntimeException("No"));
        EasyMock.expect(configManager.getMergedConfigResponse(null,
                                                              ProductPlugin.TYPE_MEASUREMENT,
                                                              serviceEntityId,
                                                              true)).andReturn(response);

        trackerManager.enableTrackers(null, serviceEntityId, response);
        replay();

        serverRestartHandler.serverRestarted(SERVER_ID);
        verify();

    }

    /**
     * Verifies that nothing blows up if failure occurs enabling service
     * trackers
     * @throws Exception
     */
    public void testRestartServerFailedServiceConfigLogTrackEnable() throws Exception {
        Server server = new Server();
        Service service = new Service();
        Integer serviceId = Integer.valueOf(889);
        service.setId(serviceId);
        AppdefEntityID serviceEntityId = new AppdefEntityID(AppdefEntityConstants.APPDEF_TYPE_SERVICE, serviceId);
        server.addService(service);
        EasyMock.expect(serverManager.findServerById(SERVER_ID.getId())).andReturn(server);
        autoInvManager.toggleRuntimeScan(null, SERVER_ID, true);

        ConfigResponse response = new ConfigResponse();
        EasyMock.expect(configManager.getMergedConfigResponse(null, ProductPlugin.TYPE_MEASUREMENT, SERVER_ID, true))
                .andReturn(response);

        trackerManager.enableTrackers(null, SERVER_ID, response);
        EasyMock.expect(configManager.getMergedConfigResponse(null,
                                                              ProductPlugin.TYPE_MEASUREMENT,
                                                              serviceEntityId,
                                                              true)).andThrow(new RuntimeException("No"));
        replay();
        serverRestartHandler.serverRestarted(SERVER_ID);
        verify();

    }

    private void verify() {
        EasyMock.verify(autoInvManager, serverManager, configManager, trackerManager);
    }

}
