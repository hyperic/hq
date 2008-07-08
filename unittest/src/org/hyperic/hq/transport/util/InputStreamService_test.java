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

import junit.framework.TestCase;

/**
 * Tests the InputStreamService_test class.
 */
public class InputStreamService_test extends TestCase {

    private InputStreamServiceImpl _streamService;
    
    public InputStreamService_test(String name) {
        super(name);
    }
    
    public void setUp() throws Exception {
        super.setUp();
        _streamService = InputStreamServiceImpl.getInstance();
    }
    
    public void tearDown() throws Exception {
        super.tearDown();
        
        // age out all the old registered buffers
        _streamService.ageOutOldBuffersFromRegistry(0);
    }
    
    /**
     * Expected a NullPointerException.
     */
    public void testWritingNullBufferToStream() throws Exception {
        RemoteInputStream is = _streamService.getRemoteStream();
        
        try {
            _streamService.writeBufferToRemoteStream(is.getStreamId(), null);
            fail("Expected NPE");
        } catch (NullPointerException e) {
            // expected outcome
        }
        
    }
    
    /**
     * Expected a IllegalArgumentException.
     */
    public void testWritingEmptyBufferToStream() throws Exception {
        RemoteInputStream is = _streamService.getRemoteStream();
        
        try {
            _streamService.writeBufferToRemoteStream(is.getStreamId(), new byte[0]);
            fail("Expected IllegalArgumentException");
        } catch (IllegalArgumentException e) {
            // expected outcome
        }
    }
    
    /**
     * Expected a IOException.
     */
    public void testWritingBufferWithInvalidStreamId() throws Exception {
        String id = null;
        byte[] buffer = {1, 2, 3};
        
        try {
            _streamService.writeBufferToRemoteStream(id, buffer);
            fail("Expected IOException");
        } catch (IOException e) {
            // expected outcome
        }
        
        id = "unknown";
        
        try {
            _streamService.writeBufferToRemoteStream(id, buffer);
            fail("Expected IOException");
        } catch (IOException e) {
            // expected outcome
        }
        
    }
    
    /**
     * Expected a IOException.
     */
    public void testSignalingEndOfStreamWithInvalidStreamId() throws Exception {
        String id = null;
        
        try {
            _streamService.signalEndOfRemoteStream(id);
            fail("Expected IOException");
        } catch (IOException e) {
            // expected outcome
        }
        
        id = "unknown";
        
        try {
            _streamService.signalEndOfRemoteStream(id);
            fail("Expected IOException");
        } catch (IOException e) {
            // expected outcome
        }
    }
    
    /**
     * Expected a IOException.
     */
    public void testGettingNextBufferWithInvalidStreamId() throws Exception {
        String id = null;
        
        try {
            _streamService.getNextBuffer(id);
            fail("Expected IOException");
        } catch (IOException e) {
            // expected outcome
        }
        
        id = "unknown";
        
        try {
            _streamService.getNextBuffer(id);
            fail("Expected IOException");
        } catch (IOException e) {
            // expected outcome
        }
    }
    
    public void testAgingOutOldBuffersInStreamRegistry() throws Exception {
        RemoteInputStream is1 = _streamService.getRemoteStream();
        RemoteInputStream is2 = _streamService.getRemoteStream();
        
        // we should be able to write to these streams
        byte[] buffer = {1,2,3,4,5,127};
        
        _streamService.writeBufferToRemoteStream(is1.getStreamId(), buffer);
        _streamService.writeBufferToRemoteStream(is2.getStreamId(), buffer);
        
        // need to age the buffers a bit
        Thread.sleep(100);

        // now age out all the registered buffers
        int numAged = _streamService.ageOutOldBuffersFromRegistry(0);
        
        assertEquals(2, numAged);
        
        // now any writes to the remote streams should throw an IOException since 
        // there is no registered buffer for the stream ids
        try {
            _streamService.writeBufferToRemoteStream(is1.getStreamId(), buffer);
            fail("Expected IOException");
        } catch (IOException e) {
            // expected outcome
        }
        
        try {
            _streamService.writeBufferToRemoteStream(is2.getStreamId(), buffer);
            fail("Expected IOException");
        } catch (IOException e) {
            // expected outcome
        }        
    }
        
}
