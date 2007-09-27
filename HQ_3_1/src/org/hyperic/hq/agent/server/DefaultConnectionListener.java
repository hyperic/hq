/*
 * NOTE: This copyright does *not* cover user programs that use HQ
 * program services by normal system calls through the application
 * program interfaces provided as part of the Hyperic Plug-in Development
 * Kit or the Hyperic Client Development Kit - this is merely considered
 * normal use of the program, and does *not* fall under the heading of
 * "derived work".
 * 
 * Copyright (C) [2004, 2005, 2006], Hyperic, Inc.
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

package org.hyperic.hq.agent.server;

import org.hyperic.hq.agent.AgentConnectionException;
import org.hyperic.hq.agent.AgentConfig;

import java.io.IOException;
import java.io.InterruptedIOException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.UnknownHostException;

class DefaultConnectionListener
    extends AgentConnectionListener
{
    private ServerSocket listenSock;

    DefaultConnectionListener(AgentConfig cfg){
        super(cfg);
        this.listenSock = null;
    }

    public void setup(int connTimeout)
        throws AgentStartException
    {
        InetAddress addr;
        String listenIp;
        int listenPort;

        listenPort = this.getConfig().getListenPort();
        
        try {
            addr = this.getConfig().getListenIpAsAddr();
        } catch(UnknownHostException exc){
            throw new AgentStartException("Failed to get IP for '" +
                                          this.getConfig().getListenIp() +"'");
        }

        try {
            this.listenSock = new ServerSocket(listenPort, 50, addr);
            this.listenSock.setSoTimeout(connTimeout);
        } catch(IOException exc){
            throw new AgentStartException("Failed to setup listen socket: " +
                                          exc.getMessage());
        }
    }

    public void cleanup(){
        if(this.listenSock != null){
            try {this.listenSock.close();} catch(IOException exc){}
            this.listenSock = null;
        }
    }

    public AgentServerConnection getNewConnection()
        throws AgentConnectionException, InterruptedIOException
    {
        Socket conn;

        try {
            conn = this.listenSock.accept();
        } catch(InterruptedIOException exc){
            throw exc;
        } catch(IOException exc){
            throw new AgentConnectionException("Error accepting socket: " +
                                               exc.getMessage());
        }

        return new DefaultServerConnection(conn);
    }
}
