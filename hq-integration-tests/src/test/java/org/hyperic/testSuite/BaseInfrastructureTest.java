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
package org.hyperic.testSuite;


import java.util.concurrent.TimeUnit;
import org.hyperic.hq.context.IntegrationTestContextLoader;
import org.junit.*;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.beans.factory.annotation.Autowired;
import org.junit.runner.RunWith;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.transaction.annotation.Transactional;


/**
 * BaseInfrastructureTest
 * 
 * @author Helena Edelson
 */
@Transactional
@DirtiesContext
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = "classpath*:META-INF/spring/*-context.xml", loader = IntegrationTestContextLoader.class)
abstract public class BaseInfrastructureTest {
    
    protected Log logger = LogFactory.getLog(this.getClass());
    protected long startTime;
    protected long endTime;

    @BeforeClass
    public static void initialize(){

    }

    
    @Before
    public void before(){
        startTime = System.nanoTime();
        logger.debug("****** Test starting ******");
    }

    @After
    public void after(){
        endTime = System.nanoTime();
        logger.debug(buildMessage());
    }

    @AfterClass
    public static void tearDown(){

    }

    /**
     *
     * @return
     */
    private String buildMessage(){
        return new StringBuilder().append("****** Test executed in ").append(endTime).append(" nanoseconds or ").
                append(TimeUnit.SECONDS.convert(endTime, TimeUnit.NANOSECONDS)).append(" seconds").toString();
    }

}

