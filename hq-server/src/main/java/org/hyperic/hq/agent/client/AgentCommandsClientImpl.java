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
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.hyperic.hq.agent.AgentConnectionException;
import org.hyperic.hq.agent.AgentRemoteException;
import org.hyperic.hq.agent.FileData;
import org.hyperic.hq.agent.FileDataResult;
import org.hyperic.hq.appdef.Agent;
import org.hyperic.hq.transport.AgentProxyFactory;
import org.hyperic.hq.transport.util.InputStreamServiceImpl;
import org.hyperic.hq.transport.util.RemoteInputStream;

/**
 * The Agent Commands client that uses the new transport.
 */
public class AgentCommandsClientImpl 
    extends AbstractCommandsClient implements AgentCommandsClient {
    
    private final boolean _agentRegistrationClient;

    public AgentCommandsClientImpl(Agent agent, AgentProxyFactory factory) {
        super(agent, factory);
        _agentRegistrationClient = false;
    }
    
    /**
     * This constructor should only be used during agent registration where 
     * the agent doesn't yet know its agent token and the Agent pojo has not 
     * yet been persisted on the server.
     */
    public AgentCommandsClientImpl(AgentProxyFactory factory, 
                                   String agentAddress, 
                                   int agentPort, 
                                   boolean unidirectional) {
        super(createAgent(agentAddress, agentPort, unidirectional), factory);
        
        _agentRegistrationClient = true;
    }
    
    private static Agent createAgent(String agentAddress, 
                                     int agentPort, 
                                     boolean unidirectional) {
        Agent agent = new Agent();
        agent.setAddress(agentAddress);
        agent.setPort(agentPort);
        agent.setUnidirectional(unidirectional);
        return agent;
    }
    
    /**
     * @see org.hyperic.hq.agent.client.AgentCommandsClient#agentSendFileData(org.hyperic.hq.agent.FileData[], java.io.InputStream[])
     */
    public FileDataResult[] agentSendFileData(FileData[] destFiles,
                                              InputStream[] streams)
            throws AgentRemoteException, AgentConnectionException {
        
        assertOnlyPingsAllowedForAgentRegistration();
        
        if(destFiles.length != streams.length){
            throw new IllegalArgumentException("Streams and dest files " +
                                               "arrays must be the same " +
                                               "length");
        }
        
        InputStreamServiceImpl streamService = InputStreamServiceImpl.getInstance();
        
        RemoteInputStream remoteIs = streamService.getRemoteStream();
        
        AgentCommandsClient proxy = null;
        
        try {
            // must be an async proxy so the current thread is not blocked from 
            // sending the file stream bytes to the remote client
            proxy = (AgentCommandsClient)getAsynchronousProxy(AgentCommandsClient.class, false);
            
            proxy.agentSendFileData(destFiles, new InputStream[]{remoteIs});        
        } finally {
            safeDestroyService(proxy);
        }
        
        OutputStream outStream = null;
        
        try {
            outStream = new OutputStreamWrapper(streamService, remoteIs.getStreamId());
            
            // Send the file to the agent in 32kb chunks
            FileStreamMultiplexer muxer = new FileStreamMultiplexer(32*1024);
            
            return muxer.sendData(outStream, destFiles, streams);
        } catch(IOException exc){
            throw new AgentRemoteException("IO Exception while sending " +
                                           "file data: " + exc.getMessage(), exc);
        } finally {
            if (outStream != null) {
                try {
                    outStream.close();
                } catch (IOException e) {
                 // swallow
                }                 
            }
        }
    }
    
    private static class OutputStreamWrapper extends OutputStream {
        
        private final InputStreamServiceImpl _streamService;
        private final String _streamId;
        
        public OutputStreamWrapper(InputStreamServiceImpl streamService, String streamId) {
            _streamService = streamService;
            _streamId = streamId;
        }
        
        public void write(byte b[], int off, int len) throws IOException {
            // Always copy the buffer since the invoking thread may be 
            // reusing this buffer.
            byte[] buffer = new byte[len-off];
            System.arraycopy(b, off, buffer, 0, buffer.length);            
            
            try {
                _streamService.writeBufferToRemoteStream(_streamId, buffer);
            } catch (InterruptedException e) {
                throw new IOException("buffer write interrupted");
            }
        }

        public void write(int b) throws IOException {
            throw new UnsupportedOperationException("single byte writes not supported");
        }
        
        public void close() throws IOException {
            boolean eosSignaled = false;
            int i = 0;
            
            while (!eosSignaled && i++ < 10) {
                try {
                    _streamService.signalEndOfRemoteStream(_streamId);
                    eosSignaled = true;
                } catch (InterruptedException e) {
                    
                }                
            }
        }
        
    }    
    
    /**
     * @see org.hyperic.hq.agent.client.AgentCommandsClient#die()
     */
    public void die() throws AgentRemoteException, AgentConnectionException {
        assertOnlyPingsAllowedForAgentRegistration();
        
        AgentCommandsClient proxy = null;
        
        try {
            proxy = (AgentCommandsClient)getAsynchronousProxy(AgentCommandsClient.class, false);
            proxy.die();        
        } finally {
            safeDestroyService(proxy);
        }
    }

    /**
     * @see org.hyperic.hq.agent.client.AgentCommandsClient#ping()
     */
    public long ping() throws AgentRemoteException, AgentConnectionException {
        if (_agentRegistrationClient && getAgent().isUnidirectional()) {
            // The unidirectional client does not work yet since the agent 
            // is not aware of its agent token at this time
            return 0;
        }
        
        AgentCommandsClient proxy = null;
        
        try {
            proxy = (AgentCommandsClient)getSynchronousProxy(AgentCommandsClient.class);
            
            long sendTime = System.currentTimeMillis();
            proxy.ping();
            long recvTime = System.currentTimeMillis();
            return (recvTime-sendTime);
        } finally {
            safeDestroyService(proxy);
        }
    }
    
    /**
     * @see org.hyperic.hq.agent.client.AgentCommandsClient#getCurrentAgentBundle()
     */
    public String getCurrentAgentBundle() 
        throws AgentRemoteException, AgentConnectionException {

        AgentCommandsClient proxy = null;
        
        try {
            proxy = (AgentCommandsClient)getSynchronousProxy(AgentCommandsClient.class);
            return proxy.getCurrentAgentBundle();           
        } finally {
            safeDestroyService(proxy);
        }
    }  
    
    /**
     * @see org.hyperic.hq.agent.client.AgentCommandsClient#upgrade(java.lang.String, java.lang.String)
     */
    public Map upgrade(String tarFile, String destination) 
        throws AgentRemoteException, AgentConnectionException {
        
        AgentCommandsClient proxy = null;
        Map result = new HashMap();
        
        try {
            proxy = (AgentCommandsClient)getSynchronousProxy(AgentCommandsClient.class);
            result = proxy.upgrade(tarFile, destination);           
        } finally {
            safeDestroyService(proxy);
        }
        
        return result;
    }

    /**
     * @see org.hyperic.hq.agent.client.AgentCommandsClient#restart()
     */
    public void restart() throws AgentRemoteException, AgentConnectionException {
        assertOnlyPingsAllowedForAgentRegistration();
        
        AgentCommandsClient proxy = null;
        
        try {
            proxy = (AgentCommandsClient)getAsynchronousProxy(AgentCommandsClient.class, false);
            proxy.restart();           
        } finally {
            safeDestroyService(proxy);
        }
    }
    
    private void assertOnlyPingsAllowedForAgentRegistration() 
        throws AgentConnectionException {
        
        if (_agentRegistrationClient) {
            throw new AgentConnectionException("Only client ping is allowed");
        }        
    }

    public Map<String, Boolean> agentRemoveFile(Collection<String> files)
    throws AgentRemoteException, AgentConnectionException {
        AgentCommandsClient proxy = null;
        try {
            proxy = (AgentCommandsClient)getAsynchronousProxy(AgentCommandsClient.class, false);
            Map<String, Boolean> rtn = proxy.agentRemoveFile(files);
            if (rtn == null) {
                _log.error("error removing files from agent=" + getAgent() + " files=" + files);
                rtn = new HashMap<String, Boolean>(files.size());
                for (String file : files) {
                    rtn.put(file, false);
                }
            }
            return rtn;
        } finally {
            safeDestroyService(proxy);
        }
    }

}
