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

import java.io.Externalizable;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInput;
import java.io.ObjectOutput;

import org.jboss.remoting.InvokerLocator;
import org.jboss.remoting.transporter.TransporterClient;

/**
 * An input stream that maintains a local byte buffer replenished via calls to a 
 * remote source. The stream id is used to maintain the stream state on the 
 * remote source. This class is not thread safe.
 *  
 *  The invoker locator to the remote source must be set before reading from this 
 *  stream.
 */
public class RemoteInputStream 
    extends InputStream implements Externalizable {
    
    private InvokerLocator _sourceInvokerLocator;
    
    private String _streamId;
    
    private byte[] _currentBuffer;
    
    private int _currentBufferIdx;
    
    private InputStreamService _streamService;
    
    private boolean _closed;
    
    private boolean _isEOS;
    
    /**
     * Default constructor for externalization only.
     */
    public RemoteInputStream() {}
    
    /**
     * Creates an instance on the remote source to be serialized and sent to 
     * the remote client reading from this stream.
     *
     * @param streamId The stream id that uniquely identifies this 
     *                 stream on the remote source.
     * @throws NullPointerException if the stream id is <code>null</code>.                
     */
    public RemoteInputStream(String streamId) {
        if (streamId == null) {
            throw new NullPointerException("stream id is null");
        }
        
        _streamId = streamId;
    }
    
    /**
     * Set the invoker locator to the remote source.
     * 
     * @param invokerLocator The invoker locator to the remote source.
     * @throws NullPointerException if the invoker locator is <code>null</code>.
     */
    public void setRemoteSourceInvokerLocator(InvokerLocator invokerLocator) {
        if (invokerLocator == null) {
            throw new NullPointerException("invoker locator is null");
        }
        
        _sourceInvokerLocator = invokerLocator;
    }
    
    /**
     * @see java.io.InputStream#available()
     */
    public int available() throws IOException {
        if (_closed) {
            throw new IOException("stream is closed");
        }
        
        if (_currentBuffer == null) {
            return 0;
        }
        
        return _currentBuffer.length-_currentBufferIdx;
    }
    
    /**
     * @see java.io.InputStream#read(byte[], int, int)
     */
    public int read(byte b[], int off, int len) throws IOException {
        if (_closed) {
            throw new IOException("stream is closed");
        }
        
        _isEOS = retrieveNextBuffer();
        
        if (_isEOS) {
            return -1;
        }
                
        int avail = available();
        int maxLen = len;
        
        if (avail > 0) {
            // Only retrieve the next buffer if the current one is not depleted.
            maxLen = Math.min(avail, len);
        }
        
        return super.read(b, off, maxLen);            
    }
    
    /**
     * @see java.io.InputStream#read()
     */
    public int read() throws IOException {
        if (_closed) {
            throw new IOException("stream is closed");
        }
                
        _isEOS = retrieveNextBuffer();
        
        if (_isEOS) {
            return -1;
        }
        
        int c = 0xff & _currentBuffer[_currentBufferIdx++];
        
        if (_currentBufferIdx == _currentBuffer.length) {
            _currentBuffer = null;
        }
        
        return c;
    }
    
    private boolean retrieveNextBuffer() throws IOException {
        if (_isEOS){
            return true;
        }
        
        if (_currentBuffer == null) {
            StreamBuffer nextBuffer = getInputStreamService().getNextBuffer(_streamId);
            
            if (nextBuffer.isEOS()) {
                return true;
            }
            
            _currentBuffer = nextBuffer.getBuffer();
            _currentBufferIdx = 0;
        }
        
        return false;
    }
    
    /**
     * @see java.io.InputStream#close()
     */
    public void close() throws IOException {
        if (!_closed) {
            _closed = true;

            if (_streamService != null) {
                TransporterClient.destroyTransporterClient(_streamService);
                _streamService = null;
            }
            
            _currentBuffer = null;
        }
    }
    
    /**
     * @return The uniquely identifying stream id.
     */
    public String getStreamId() {
        return _streamId;
    }

    public void readExternal(ObjectInput in) throws IOException, ClassNotFoundException {
        _streamId = in.readUTF();
    }

    public void writeExternal(ObjectOutput out) throws IOException {
        out.writeUTF(_streamId);
    }
    
    private InputStreamService getInputStreamService() throws IOException {        
        if (_streamService == null) {
            if (_sourceInvokerLocator == null) {
                throw new IOException("remote source invoker locator was not set");
            }
            
            try {
                _streamService = (InputStreamService)TransporterClient.
                    createTransporterClient(_sourceInvokerLocator, InputStreamService.class);
            } catch (Exception e) {
                throw new IOException("Failed to connect to input stream " +
                		              "service on remote source: "+e);
            }
        }

        return _streamService;
    }

}
