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
import java.io.ObjectInput;
import java.io.ObjectOutput;


/**
 * The buffers containing the data stream chunks for a {@link RemoteInputStream}.
 */
public class StreamBuffer implements Externalizable {

    private boolean _isEOS;
    
    private byte[] _buffer;
        
    /**
     * Default constructor for externalization only.
     */
    public StreamBuffer() {}
    
    /**
     * Keep constructor private to force using the static initializer methods 
     * that enforce a consistent object initial state.
     */
    private StreamBuffer(boolean isEOS, byte[] buffer) {
        _isEOS = false;
        _buffer = buffer;
    }
    
    /**
     * Create a new stream buffer instance.
     * 
     * @param buffer The buffered data.
     * @return The stream buffer.
     * @throws NullPointerException if the buffered data is <code>null</code>.
     * @throws IllegalArgumentException if the buffer is empty.
     */
    public static StreamBuffer newInstance(byte[] buffer) {
        if (buffer == null) {
            throw new NullPointerException("buffer is null");
        }
        
        if (buffer.length == 0) {
            throw new IllegalArgumentException("buffer must not be empty");
        }
        
        return new StreamBuffer(false, buffer);
    }
    
    /**
     * Create a new end of stream instance.
     * 
     * @return The stream buffer.
     */
    public static StreamBuffer newEOSInstance() {
        return new StreamBuffer(true, new byte[0]);
    }
    
    /**
     * @return The buffered data.
     */
    public byte[] getBuffer() {
        return _buffer;
    }
    
    /**
     * @return <code>true</code> if this instance signals the end of stream.
     */
    public boolean isEOS() {
        return _isEOS;
    }

    public void readExternal(ObjectInput in) throws IOException, ClassNotFoundException {
        _isEOS = in.readBoolean();
        int length = in.readInt();
        _buffer = new byte[length];
        in.readFully(_buffer);
    }

    public void writeExternal(ObjectOutput out) throws IOException {
        out.writeBoolean(_isEOS);
        out.writeInt(_buffer.length);
        out.write(_buffer);
    }

}
