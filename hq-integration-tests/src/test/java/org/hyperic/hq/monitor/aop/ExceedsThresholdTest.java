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
package org.hyperic.hq.monitor.aop;

import static junit.framework.Assert.assertFalse;
import static junit.framework.Assert.assertNotNull;

import org.hyperic.hq.monitor.MockService;
import org.hyperic.testSuite.BaseInfrastructureTest;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.util.StopWatch;


/**
 * ExceedsThresholdTests
 * 
 * We need to define:
 * a) what is the threshold (i.e. too long)
 * b) what is the course of action if true
 * 
 * @author Helena Edelson
 * @see <a href="http://jira.hyperic.com/browse/HE-356">Add aspect to track @Transactional/@Service method runtime and log if exceeded threshold</a>
 */
@ContextConfiguration
@Ignore("This test requires manual verification that the aspect logged a message")
public class ExceedsThresholdTest extends BaseInfrastructureTest {

    @Autowired private MockService mockServiceImpl;
   
 
    private long unAcceptableDuration = 300000; 

    @Before
    public void before() {
        assertNotNull("mockServiceImpl should not be null", mockServiceImpl);
    }

    /**
     * Test: set aop-context.xml maximumDuration to
     * the same variable as unAcceptableDuration
     * 
     */
    @Test
    public void monitorControlPerformance() {
        StopWatch sw = new StopWatch();
        sw.start("test");

        mockServiceImpl.foo(unAcceptableDuration);

        sw.stop(); 
        assertFalse("method exceeded acceptable duration: " + sw.getTotalTimeMillis(), sw.getTotalTimeMillis() < unAcceptableDuration);
    }



}
