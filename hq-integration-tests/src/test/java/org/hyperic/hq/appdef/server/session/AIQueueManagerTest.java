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
package org.hyperic.hq.appdef.server.session;

import static org.junit.Assert.assertEquals;

import org.hyperic.hq.appdef.shared.AIPlatformValue;
import org.hyperic.hq.appdef.shared.AIQueueManager;
import org.hyperic.hq.appdef.shared.AppdefEntityNotFoundException;
import org.hyperic.hq.appdef.shared.AppdefEntityValue;
import org.hyperic.hq.appdef.shared.CPropKeyNotFoundException;
import org.hyperic.hq.appdef.shared.CPropManager;
import org.hyperic.hq.authz.shared.PermissionException;
import org.hyperic.hq.test.BaseInfrastructureTest;
import org.hyperic.util.config.ConfigResponse;
import org.hyperic.util.config.EncodingException;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Integration test of {@link AIQueueManagerImpl}
 * @author jhickey
 * 
 */

public class AIQueueManagerTest
    extends BaseInfrastructureTest {

    @Autowired
    private AIQueueManager aiQueueManager;
    @Autowired
    private CPropManager cPropManager;
    private AIPlatformValue queuedAIPlatform;
    private Platform platform;

    @Before
    public void setUp() throws Exception {
        createAgent("127.0.0.1", 2144, "hqadmin", "agentToken", "4.5");
        PlatformType platformType = createPlatformType("JenOS", "testit");
        cPropManager.addKey(platformType, "numCpus", "Number of CPUs");

        // Add the platform to AI Queue with custom prop value of 4 CPUs
        AIPlatformValue aiPlatform = new AIPlatformValue();
        aiPlatform.setFqdn("Platform1");
        aiPlatform.setAgentToken("agentToken");
        ConfigResponse platformConfig = new ConfigResponse();
        platformConfig.setValue("numCpus", 4);
        aiPlatform.setCustomProperties(platformConfig.encode());
        this.queuedAIPlatform = aiQueueManager.queue(authzSubjectManager.getOverlordPojo(),
            aiPlatform, false, false, false);

        // Add the platform to inventory with custom prop value of 2 CPUs
        this.platform = createPlatform("agentToken", "JenOS", "Platform1", "Platform1");
        cPropManager.setValue(platform.getEntityId(), platformType.getId(), "numCpus", "2");
    }

    /**
     * Verifies HHQ-4185 behavior - testing "side effect" setting of custom
     * properties when the findAIPlatformById method is called. TODO get rid of
     * this behavior and make find read-only
     * @throws EncodingException
     * @throws CPropKeyNotFoundException
     * @throws AppdefEntityNotFoundException
     * @throws PermissionException
     */
    @Test
    public void testFindAIPlatformByIdUpdatesCProps() throws EncodingException,
        CPropKeyNotFoundException, AppdefEntityNotFoundException, PermissionException {
        // Do a find, which for some reason triggers updating the actual
        // platform's cprop value with the one from the AI Queue
        aiQueueManager.findAIPlatformById(authzSubjectManager.getOverlordPojo(), queuedAIPlatform
            .getId());
        AppdefEntityValue appdefVal = new AppdefEntityValue(platform.getEntityId(),
            authzSubjectManager.getOverlordPojo());
        assertEquals("4", cPropManager.getValue(appdefVal, "numCpus"));
    }

    /**
     * Verifies HHQ-4185 behavior - testing "side effect" setting of custom
     * properties when the findAIPlatformByFqdn method is called. TODO get rid of
     * this behavior and make find read-only
     * @throws EncodingException
     * @throws CPropKeyNotFoundException
     * @throws AppdefEntityNotFoundException
     * @throws PermissionException
     */
    @Test
    public void testFindAIPlatformByFqdnUpdatesCProps() throws CPropKeyNotFoundException,
        AppdefEntityNotFoundException, PermissionException {
        // Do a find, which for some reason triggers updating the actual
        // platform's cprop value with the one from the AI Queue
        aiQueueManager.findAIPlatformByFqdn(authzSubjectManager.getOverlordPojo(), queuedAIPlatform
            .getFqdn());
        AppdefEntityValue appdefVal = new AppdefEntityValue(platform.getEntityId(),
            authzSubjectManager.getOverlordPojo());
        assertEquals("4", cPropManager.getValue(appdefVal, "numCpus"));
    }
}
