/*
 * NOTE: This copyright does *not* cover user programs that use HQ
 * program services by normal system calls through the application
 * program interfaces provided as part of the Hyperic Plug-in Development
 * Kit or the Hyperic Client Development Kit - this is merely considered
 * normal use of the program, and does *not* fall under the heading of
 * "derived work".
 *
 * Copyright (C) [2004-2008], Hyperic, Inc.
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
package org.hyperic.hq.test;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import net.sf.ehcache.CacheManager;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hibernate.SessionFactory;
import org.hyperic.hq.appdef.Agent;
import org.hyperic.hq.appdef.server.session.Application;
import org.hyperic.hq.appdef.server.session.Platform;
import org.hyperic.hq.appdef.server.session.PlatformType;
import org.hyperic.hq.appdef.server.session.Server;
import org.hyperic.hq.appdef.server.session.ServerType;
import org.hyperic.hq.appdef.server.session.Service;
import org.hyperic.hq.appdef.server.session.ServiceType;
import org.hyperic.hq.appdef.shared.AIPlatformValue;
import org.hyperic.hq.appdef.shared.AgentCreateException;
import org.hyperic.hq.appdef.shared.AgentManager;
import org.hyperic.hq.appdef.shared.AppdefDuplicateNameException;
import org.hyperic.hq.appdef.shared.AppdefEntityConstants;
import org.hyperic.hq.appdef.shared.AppdefEntityID;
import org.hyperic.hq.appdef.shared.AppdefEntityTypeID;
import org.hyperic.hq.appdef.shared.AppdefGroupNotFoundException;
import org.hyperic.hq.appdef.shared.ApplicationManager;
import org.hyperic.hq.appdef.shared.ApplicationNotFoundException;
import org.hyperic.hq.appdef.shared.ApplicationValue;
import org.hyperic.hq.appdef.shared.PlatformManager;
import org.hyperic.hq.appdef.shared.PlatformNotFoundException;
import org.hyperic.hq.appdef.shared.ServerManager;
import org.hyperic.hq.appdef.shared.ServerNotFoundException;
import org.hyperic.hq.appdef.shared.ServerValue;
import org.hyperic.hq.appdef.shared.ServiceManager;
import org.hyperic.hq.appdef.shared.ValidationException;
import org.hyperic.hq.authz.server.session.Resource;
import org.hyperic.hq.authz.server.session.ResourceGroup;
import org.hyperic.hq.authz.server.session.ResourceGroup.ResourceGroupCreateInfo;
import org.hyperic.hq.authz.server.session.Role;
import org.hyperic.hq.authz.shared.AuthzSubjectManager;
import org.hyperic.hq.authz.shared.GroupCreationException;
import org.hyperic.hq.authz.shared.PermissionException;
import org.hyperic.hq.authz.shared.ResourceGroupManager;
import org.hyperic.hq.authz.shared.ResourceManager;
import org.hyperic.hq.common.ApplicationException;
import org.hyperic.hq.common.NotFoundException;
import org.hyperic.hq.context.IntegrationTestContextLoader;
import org.hyperic.hq.context.IntegrationTestSpringJUnit4ClassRunner;
import org.hyperic.hq.grouping.shared.GroupDuplicateNameException;
import org.hyperic.hq.product.ServerTypeInfo;
import org.hyperic.hq.product.ServiceTypeInfo;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.transaction.annotation.Transactional;

/**
 * BaseInfrastructureTest
 * 
 * @author Helena Edelson
 * @author Jennifer Hickey
 */
@Transactional
@RunWith(IntegrationTestSpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = "classpath*:META-INF/spring/*-context.xml", loader = IntegrationTestContextLoader.class)
abstract public class BaseInfrastructureTest extends TestHelper{

    protected Log logger = LogFactory.getLog(this.getClass());
    protected long startTime;
    protected long endTime;

    protected static final int GENERIC_APPLICATION_TYPE = 1;

    protected static final int J2EE_APPLICATION_TYPE = 2;

    @BeforeClass
    public static void initialize() {

    }

    @Before
    public void before() {
        startTime = System.nanoTime();
        logger.debug("****** Test starting ******");
    }

    @After
    public void after() {
        //Clear the query cache
        sessionFactory.evictQueries();
        //Clear the 2nd level cache including regions with queries
        CacheManager.getInstance().clearAll();
        endTime = System.nanoTime();
        logger.debug(buildMessage());
    }

    @AfterClass
    public static void tearDown() {

    }

    /**
     * 
     * @return
     */
    private String buildMessage() {
        return new StringBuilder().append("****** Test executed in ").append(endTime).append(
            " nanoseconds or ").append(TimeUnit.SECONDS.convert(endTime, TimeUnit.NANOSECONDS))
            .append(" seconds").toString();
    }

    protected void flushSession() {
        sessionFactory.getCurrentSession().flush();
    }

    protected void clearSession() {
        sessionFactory.getCurrentSession().clear();
    }

}
