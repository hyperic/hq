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

import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;

import junit.framework.TestCase;

import org.hyperic.hq.transport.util.AsynchronousInvocationHandler;
import org.jmock.core.Verifiable;
import org.jmock.expectation.ExpectationCounter;

import edu.emory.mathcs.backport.java.util.concurrent.CountDownLatch;
import edu.emory.mathcs.backport.java.util.concurrent.RejectedExecutionException;
import edu.emory.mathcs.backport.java.util.concurrent.TimeUnit;

/**
 * Tests the AsynchronousInvoker class.
 */
public class AsynchronousInvoker_test extends TestCase {

    public AsynchronousInvoker_test(String name) {
        super(name);
    }
    
    public void testIllegalThreadPoolSize() {
        try {
             new AsynchronousInvoker(-1);
        } catch (IllegalArgumentException e) {
            // expected outcome
        } catch (Exception e) {
            fail("Expected IllegalArgumentException instead of: "+e);
        }
        
        try {
            new AsynchronousInvoker(0);
       } catch (IllegalArgumentException e) {
           // expected outcome
       } catch (Exception e) {
           fail("Expected IllegalArgumentException instead of: "+e);
       }
    }
    
    /**
     * Test an invocation that is not guaranteed.
     */
    public void testNonGuaranteedInvocation() throws InterruptedException {
        AsynchronousInvoker invoker = new AsynchronousInvoker(2);
        invoker.start();
        
        MockNonGuaranteedInvocationHandler handler = 
            new MockNonGuaranteedInvocationHandler();
        
        handler.setExpectedInvocation();
        
        invoker.invoke(handler);
        
        handler.waitForInvocation();
        
        invoker.stop();
        
        handler.verify();
    }
    
    /**
     * Test an attempt at a non-guranteed invocation after the invoker is 
     * stopped. Expect a RejectedExecutionException.
     */
    public void testNonGuranteedInvocationAfterStoppingInvoker() {
        AsynchronousInvoker invoker = new AsynchronousInvoker(2);
        invoker.start();
        
        MockNonGuaranteedInvocationHandler handler = 
            new MockNonGuaranteedInvocationHandler();
        
        handler.setExpectedNoInvocation();

        invoker.stop();
        
        try {
            invoker.invoke(handler);    
        } catch (RejectedExecutionException e) {
            // expected outcome
        } catch (Exception e) {
            fail("Expected RejectedExecutionException instead of: "+e);
        } finally {
            handler.verify();                    
        }
                        
    }
    
    private class MockNonGuaranteedInvocationHandler 
        extends AsynchronousInvocationHandler implements Verifiable {

        private final CountDownLatch _latch = new CountDownLatch(1);
        
        private final ExpectationCounter _numInvocations = 
            new ExpectationCounter("num invocations"); 
        
        public MockNonGuaranteedInvocationHandler() {
            super(false);
        }
        
        public void setExpectedInvocation() {
            _numInvocations.setExpected(1);
        }
        
        public void setExpectedNoInvocation() {
            _numInvocations.setExpectNothing();
        }

        public void handleInvocation() throws Exception {
            _numInvocations.inc();
            _latch.countDown();
        }
        
        public void waitForInvocation() throws InterruptedException {
            _latch.await(60, TimeUnit.SECONDS);
        }

        public void readExternal(ObjectInput in) throws IOException,
                ClassNotFoundException {
            
        }

        public void writeExternal(ObjectOutput out) throws IOException {
            
        }

        public void verify() {
            _numInvocations.verify();
        }
        
    }
    
}
