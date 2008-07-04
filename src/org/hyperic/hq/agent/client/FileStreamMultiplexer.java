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

package org.hyperic.hq.agent.client;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import org.hyperic.hq.agent.AgentRemoteException;
import org.hyperic.hq.agent.FileData;
import org.hyperic.hq.agent.FileDataResult;
import org.hyperic.util.math.MathUtil;

/**
 * A utility combining one or more file input streams into a single output 
 * stream.
 */
public class FileStreamMultiplexer {
    
    /**
     * The default buffer size (8kb).
     */
    public static final int DEFAULT_BUFFER_SIZE = 8*1024;
    
    private final int _bufferSize;
    
    /**
     * Creates an instance using the default max output stream buffer size.
     */
    public FileStreamMultiplexer() {
        this(DEFAULT_BUFFER_SIZE);
    }
    
    /**
     * Creates an instance where the max output stream buffer size is specified.
     *
     * @param bufferSize The buffer size.
     * @throws IllegalArgumentException if the buffer size is not greater than zero.
     */
    public FileStreamMultiplexer(int bufferSize) {
        if (bufferSize <= 0) {
            throw new IllegalArgumentException("buffer size must be greater than zero: "+bufferSize);
        }
        
        _bufferSize = bufferSize;
    }
    
    /**
     * Muliplex the input streams into a single output stream.
     * 
     * @param outStream The output stream where the combined streams are written.
     * @param destFiles The files representing each input stream. 
     * @param streams The file input streams.
     * @return The file data result.
     */
    public FileDataResult[] sendData(OutputStream outStream,
                                     FileData[] destFiles,
                                     InputStream[] streams) 
        throws IOException, AgentRemoteException {
        
        FileDataResult[] res = new FileDataResult[destFiles.length];
        byte[] sendBuf = new byte[_bufferSize];

        for(int i=0; i<destFiles.length; i++){
            long startTime = System.currentTimeMillis();
            long toSend = destFiles[i].getSize();

            while(toSend > 0){
                int nToRead, nBytes;

                if((nToRead = streams[i].available()) == 0){
                    throw new AgentRemoteException("No available bytes to " +
                            "read for '" + 
                            destFiles[i].getDestFile() +
                            "' - needed " + toSend +
                            " more bytes to complete");
                }

                nToRead = MathUtil.clamp(nToRead, 1, sendBuf.length);
                nBytes  = streams[i].read(sendBuf, 0, nToRead);
                if(nBytes == -1){
                    throw new AgentRemoteException("Error reading from for '" +
                            destFiles[i].getDestFile() +
                            "' - read returned -1");
                }

                outStream.write(sendBuf, 0, nBytes);
                toSend -= nBytes;
            }

            long sendTime = System.currentTimeMillis() - startTime;
            res[i] = new FileDataResult(destFiles[i].getDestFile(),
                    destFiles[i].getSize(), sendTime);
        }

        return res;
    }

}
