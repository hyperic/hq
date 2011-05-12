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
 */

package org.hyperic.hq.operation.rabbit;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;

/**
 * In progress
 * @author Helena Edelson
 */
public class AbstractRabbitTest {

    protected final Log logger = LogFactory.getLog(this.getClass());

    private static final String install_home = "";

    private static final String agent_home = install_home + "/agent-4.6.0.BUILD-SNAPSHOT";

    private static final String agent_bundle_home = agent_home + "/bundles/agent-4.6.0.BUILD-SNAPSHOT";

    private static final String sigar_home = "/resources/generated-test-resources";

    private static AnnotationConfigApplicationContext ctx;


    @BeforeClass
    public static void prepare() {
        ctx = new AnnotationConfigApplicationContext("org.hyperic.hq.operation.rabbit");
        ctx.registerShutdownHook(); 
       
        //System.setProperty("org.hyperic.sigar.path", sigar_home);
        System.setProperty("agent.install.home", agent_home);
        System.setProperty("agent.bundle.home", agent_bundle_home);
    }

    @AfterClass
    public static void destroy() {
        ctx.destroy();
    }
}
