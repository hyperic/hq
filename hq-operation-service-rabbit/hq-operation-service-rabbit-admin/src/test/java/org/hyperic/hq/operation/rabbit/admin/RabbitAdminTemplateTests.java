/*
 * NOTE: This copyright does *not* cover user programs that use HQ
 * program services by normal system calls through the application
 * program interfaces provided as part of the Hyperic Plug-in Development
 * Kit or the Hyperic Client Development Kit - this is merely considered
 * normal use of the program, and does *not* fall under the heading of
 * "derived work".
 *
 * Copyright (C) [2009-2010], VMware, Inc.
 * This file is part of HQ.
 *
 * HQ is free software; you can redistribute it and/or modify
 * it under the terms version 2 of the GNU General Public License as
 * published by the Free Software Foundation. This program is distributed
 * in the hope that it will be useful, but WITHOUT ANY WARRANTY; without
 * even the implied warranty of MERCHANTABILITY or FITNESS FOR A
 * PARTICULAR PURPOSE. See the GNU General Public License for more
 * details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307
 * USA.
 */

package org.hyperic.hq.operation.rabbit.admin;

import org.hyperic.hq.operation.rabbit.admin.erlang.BrokerStatus;
import org.hyperic.hq.operation.rabbit.admin.erlang.Node;
import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;

import java.util.List;

/**
 * @author Helena Edelson
 */
@Ignore
public class RabbitAdminTemplateTests {

    private RabbitAdminTemplate adminTemplate = new RabbitAdminTemplate();
 
    @Test
    public void testBrokerStatus() {
        BrokerStatus status = adminTemplate.getBrokerStatus();
        Assert.assertNotNull(status);
        Assert.assertTrue(status.getRunningNodes().size() > 0);
    }

    @Test
    public void testNodeStatus() {
        Node node = adminTemplate.getNodeStatus();
        Assert.assertNotNull(node);
        Assert.assertTrue(node.getPeersInCluster().size() > 0);
    }

    @Test
    public void testUsers() {
        List<String> users = adminTemplate.listUsers();
        Assert.assertTrue(users.size() > 0);
    }
}
