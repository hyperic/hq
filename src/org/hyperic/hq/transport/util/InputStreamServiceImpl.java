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
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.hyperic.util.timer.Clock;
import org.hyperic.util.timer.ClockFactory;
import org.jboss.util.id.UID;

import edu.emory.mathcs.backport.java.util.concurrent.ArrayBlockingQueue;
import edu.emory.mathcs.backport.java.util.concurrent.TimeUnit;


/**
 * The input stream service implementation that resides on the remote source 
 * from which a {@link RemoteInputStream} reads its data.
 * 
 * It is expected that the remote source will retrieve a remote stream instance, 
 * send it to the remote client, and then, within a single thread, the remote 
 * source write the buffers to the remote stream until the end of stream is signaled.
 * 
 * To prevent an excessive amount of buffered data from residing in main memory 
 * (on the source or client), buffer writes and reads are serialized such that 
 * only one buffer of data may be stored at any time in the remote source and 
 * the remote client.
 */
public class InputStreamServiceImpl implements InputStreamService {
    
    /**
     * The system property key for the remote input stream timeout (in seconds). 
     * This timeout corresponds to the maximum time that a thread writing a 
     * buffer to a remote stream will be blocked waiting for the previously 
     * written buffer to be read. It also corresponds to the maximum time that 
     * the remote client retrieving the next buffer will wait for a buffer to 
     * be written to the remote stream.
     */
    public static final String INPUTSTREAM_TIMEOUT = "org.hq.transport.inputstream.timeout";
    
    /**
     * The default remote input stream timeout (60 seconds).
     */
    public static final int DEFAULT_INPUTSTREAM_TIMEOUT = 60;
    
    private static final InputStreamServiceImpl INSTANCE = new InputStreamServiceImpl();
    
    private final Map _streamBufferRegistry;
    private final Clock _clock;
    private final long _timeout;
    
    /**
     * Private constructor for singleton.
     */
    private InputStreamServiceImpl() {
        _streamBufferRegistry = Collections.synchronizedMap(new HashMap());
        _clock = ClockFactory.getInstance().getClock();
        
        _timeout = Long.getLong(INPUTSTREAM_TIMEOUT, DEFAULT_INPUTSTREAM_TIMEOUT).longValue();
    }
    
    /**
     * @return The singleton instance. of this service.
     */
    public static InputStreamServiceImpl getInstance() {
        return INSTANCE;
    }

    /**
     * @see org.hyperic.hq.transport.util.InputStreamService#getNextBuffer(java.lang.String)
     */
    public StreamBuffer getNextBuffer(String streamId) throws IOException {
        RegisteredStreamBuffer registeredBuffer = getRegisteredStreamBuffer(streamId);
        
        StreamBuffer streamBuffer;
        try {
            streamBuffer = registeredBuffer.getNextBuffer();
        } catch (InterruptedException e) {
            throw new IOException("thread interrupted");
        }
        
        if (streamBuffer.isEOS()) {
            _streamBufferRegistry.remove(streamId);
        }
        
        return streamBuffer;
    }

    /**
     * Retrieve the remote stream to send to the remote client.
     * 
     * @return The remote input stream.
     */
    public RemoteInputStream getRemoteStream() {
        cleanOldBuffersFromRegistry();
              
        String streamId = UID.asString();
        _streamBufferRegistry.put(streamId, new RegisteredStreamBuffer(_clock, _timeout));
        return new RemoteInputStream(streamId);
    }
    
    /**
     * Iterate through the stream buffer registry and remove any registered 
     * buffers that have not been touched by the remote client within the 
     * last 30 minutes. This prevents a memory leak in the case that the 
     * remote client connection is dropped in the middle of reading from the 
     * remote stream.
     */
    private void cleanOldBuffersFromRegistry() {
        long oldestBufferRetrieveTime = _clock.currentTimeMillis() - 30*60000;
        
        synchronized (_streamBufferRegistry) {            
            for (Iterator iter = _streamBufferRegistry.values().iterator(); 
                 iter.hasNext();) {
                RegisteredStreamBuffer registeredBuffer = (RegisteredStreamBuffer) iter.next();
                
                if (registeredBuffer.lastBufferRetrieveTime() < oldestBufferRetrieveTime) {
                    iter.remove();
                }
            }
        }
    }
    
    /**
     * Write a buffer to the remote stream. This method blocks until the previously 
     * written buffer has been read by the remote client.
     * 
     * @param streamId The remote input stream id.
     * @param buffer The buffer to write to the remote stream.
     * @throws IOException if there is no remote stream registered for the given stream id 
     *                     or if the {@link #DEFAULT_INPUTSTREAM_TIMEOUT} is reached.
     * @throws NullPointerException if the buffer is <code>null</code>.
     * @throws IllegalArgumentException if the buffer is empty.
     */
    public void writeBufferToRemoteStream(String streamId, byte[] buffer) 
        throws InterruptedException, IOException {
        
        RegisteredStreamBuffer registeredBuffer = getRegisteredStreamBuffer(streamId);
        
        registeredBuffer.addBuffer(StreamBuffer.newInstance(buffer));
    }
    
    /**
     * Signal that the end of stream has been reached for a given remote stream.
     * This signal unregisters the remote stream so that no more buffers may 
     * be written to it. This method blocks until the previously written buffer 
     * has been read by the remote client.
     * 
     * @param streamId The remote input stream id.
     * @throws IOException if there is no remote stream registered for the given stream id 
     *                     or if the {@link #DEFAULT_INPUTSTREAM_TIMEOUT} is reached. 
     */
    public void signalEndOfRemoteStream(String streamId) 
        throws InterruptedException, IOException {
        
        RegisteredStreamBuffer registeredBuffer = getRegisteredStreamBuffer(streamId);
        
        registeredBuffer.addBuffer(StreamBuffer.newEOSInstance());
    }

    private RegisteredStreamBuffer getRegisteredStreamBuffer(String streamId) throws IOException {
        RegisteredStreamBuffer registeredBuffer = 
            (RegisteredStreamBuffer)_streamBufferRegistry.get(streamId);
        
        if (registeredBuffer == null) {
            throw new IOException("no input stream is registered for stream id="+streamId);
        }
        return registeredBuffer;
    }
        
    private static class RegisteredStreamBuffer {
        
        private final Clock _clock;
        private final ArrayBlockingQueue _slot;
        private final long _timeout;
        private long _lastBufferRetrievalTime;
        
        public RegisteredStreamBuffer(Clock clock, long timeout) {
            _clock = clock;
            _slot = new ArrayBlockingQueue(1);
            _timeout = timeout;
            _lastBufferRetrievalTime = _clock.currentTimeMillis();
        }
        
        public void addBuffer(StreamBuffer buffer) throws InterruptedException, IOException {            
            boolean offered = _slot.offer(buffer, _timeout, TimeUnit.SECONDS);
            
            if (!offered) {
                throw new IOException("timeout offering stream buffer");
            }
        }
        
        public StreamBuffer getNextBuffer() throws InterruptedException, IOException {
            _lastBufferRetrievalTime = _clock.currentTimeMillis();

            StreamBuffer buffer = (StreamBuffer)_slot.poll(_timeout, TimeUnit.SECONDS);
            
            if (buffer == null) {
                throw new IOException("timeout polling for the next stream buffer");
            }
            
            return buffer;
        }
        
        public long lastBufferRetrieveTime() {
            return _lastBufferRetrievalTime;
        }
        
    }
        
}
