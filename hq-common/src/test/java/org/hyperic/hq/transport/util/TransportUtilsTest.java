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

package org.hyperic.hq.transport.util;

import java.util.HashMap;
import java.util.Map;

import junit.framework.TestCase;

import org.jboss.remoting.InvocationRequest;

/**
 * Tests the TransportUtils class.
 */
public class TransportUtilsTest
    extends TestCase {
    public TransportUtilsTest(String name) {
        super(name);
    }

    /**
     * Test setting a one-way invocation where there is a preexisting request
     * payload.
     */
    public void testSetOneWayInvocationWithPreexistingRequestPayload() {
        Map requestPayload = new HashMap();

        InvocationRequest invocation = new InvocationRequest("sessionId", "subsystem", null, requestPayload, null, null);

        // shouldn't be a one-way invocation yet
        assertFalse(TransportUtils.isOneWayInvocation(invocation));

        TransportUtils.setOneWayInvocation(invocation);

        // check if it is now a one-way invocation
        assertTrue(TransportUtils.isOneWayInvocation(invocation));
    }

    /**
     * Test setting a one-way invocation where there is not a preexisting
     * request payload.
     */
    public void testSetOneWayInvocationWithoutPreexistingRequestPayload() {
        Map requestPayload = null;

        InvocationRequest invocation = new InvocationRequest("sessionId", "subsystem", null, requestPayload, null, null);

        // shouldn't be a one-way invocation yet
        assertFalse(TransportUtils.isOneWayInvocation(invocation));

        TransportUtils.setOneWayInvocation(invocation);

        // check if it is now a one-way invocation
        assertTrue(TransportUtils.isOneWayInvocation(invocation));
    }

    public void testThrowsExpectedException() {
        // this should work
        TransportUtils.assertOperationsThrowException(GoodInterface.class, BaseException.class);

        try {
            TransportUtils.assertOperationsThrowException(BadInterface1.class, BaseException.class);
            fail("Expected IllegalArgumentException");
        } catch (IllegalArgumentException e) {
            // expected outcome
        }

        try {
            TransportUtils.assertOperationsThrowException(BadInterface2.class, BaseException.class);
            fail("Expected IllegalArgumentException");
        } catch (IllegalArgumentException e) {
            // expected outcome
        }
    }

    public interface BadInterface1 {
        void foo();
    }

    public interface BadInterface2 {
        void foo() throws BaseException;

        // throwing a subclass of the expected exception is not allowed
        void bar() throws SubclassException;
    }

    public interface GoodInterface {
        void foo() throws BaseException;

        void bar() throws BaseException;
    }

    public class BaseException
        extends Exception {
        public BaseException(String msg) {
            super(msg);
        }
    }

    public class SubclassException
        extends BaseException {
        public SubclassException(String msg) {
            super(msg);
        }
    }

}
