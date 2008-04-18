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

package org.hyperic.hq.bizapp.agent.client;

import org.hyperic.hq.agent.AgentConnectionException;
import org.hyperic.hq.agent.client.AgentConnection;
import org.hyperic.hq.appdef.Agent;
import org.hyperic.hq.bizapp.agent.CommonSSL;
import org.hyperic.util.JDK;
import org.hyperic.util.security.BogusTrustManager;

import java.io.DataOutputStream;
import java.io.IOException;

import java.net.InetSocketAddress;
import java.net.Socket;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;

import javax.net.ssl.SSLContext;
import javax.net.ssl.X509TrustManager;

/**
 * An object which wraps an AgentConnection object, so as to provide
 * SSL capabilities (associated with the CAMServerHandler's handleConnection
 * method)
 */
public class SecureAgentConnection 
    extends AgentConnection
{
    private static final String PROP_READ_TIMEOUT = "agent.readTimeOut";
    private static final int READ_TIMEOUT = 60000;

    private String agentAddress;
    private int    agentPort;
    private String authToken;

    public SecureAgentConnection(Agent acv){
        super(acv.getAddress(), acv.getPort().intValue());
        this.agentAddress = acv.getAddress();
        this.agentPort    = acv.getPort().intValue();
        this.authToken    = acv.getAuthToken();
    }

    public SecureAgentConnection(String agentAddress, int agentPort,
                              String authToken)
    {
        super(agentAddress, agentPort);
        this.agentAddress = agentAddress;
        this.agentPort    = agentPort;
        this.authToken    = authToken;
    }

    private SSLSocket getSSLSocket(SSLSocketFactory factory,
                                   String host, 
                                   int port, int timeout)
        throws IOException {

        SSLSocket socket;

        //this approach breaks in the IBM 1.4 JRE
        //http://www-128.ibm.com/developerworks/forums/dw_thread.jsp?
        //message=13695343&cat=10&thread=73546&treeDisplayType=threadmode1&forum=178#13695343
        //XXX we could check if the jre is at the required patch level
        if (!JDK.IS_IBM) {
            socket = (SSLSocket)factory.createSocket();
            socket.connect(new InetSocketAddress(host, port), timeout);
        }
        else {
            socket = (SSLSocket)factory.createSocket(host, port);
        }

        // Set the socket timeout during the initial handshake to detect
        // connection issues with the agent.  The timeout is reset to 0
        // (unlimited) post handshake, preserving the behavior for 3.1 and
        // prior.
        socket.setSoTimeout(timeout);
        socket.startHandshake();
        socket.setSoTimeout(0);
        
        return socket;
    }
    
    protected Socket getSocket()
        throws AgentConnectionException
    {
        SSLSocketFactory factory;
        SSLContext context;
        SSLSocket sock;

        try {
            context = CommonSSL.getSSLContext();
        } catch(NoSuchAlgorithmException exc){
            throw new AgentConnectionException("Unable to get SSL context: " +
                                               exc.getMessage());
        }

        // Initialize the SSL context with the bogus trust manager so that
        // we can connect to servers with a self-signed certificate
        //
        // YYY -- Note, that this implies that we do NOT validate the
        //        server's authenticity, and this is therefore subject to
        //        a man-in-the-middle attack.
        try {
            BogusTrustManager trustMan;

            trustMan = new BogusTrustManager();
            context.init(null, new X509TrustManager[] { trustMan }, null);
        } catch(KeyManagementException exc){
            throw new AgentConnectionException("Unable to initialize trust " +
                                               "manager: " + exc.getMessage());
        }

        try {
            // Check for configured agent read timeout from System properties
            int timeout;
            String readTimeout = System.getProperty(PROP_READ_TIMEOUT);
            try {
                timeout = Integer.parseInt(readTimeout);
            } catch (NumberFormatException e) {
                timeout = READ_TIMEOUT;
            }

            factory = context.getSocketFactory();
            sock = getSSLSocket(factory, this.agentAddress,
                                this.agentPort, timeout);
        } catch(IOException exc){
            throw new AgentConnectionException("Unable to connect to " +
                                               this.agentAddress + ":" +
                                               this.agentPort + ": " +
                                               exc.getMessage());
        }

        // Write our security settings
        try {
            DataOutputStream dOs;
            
            dOs = new DataOutputStream(sock.getOutputStream());
            dOs.writeUTF(this.authToken);
        } catch(IOException exc){
            throw new AgentConnectionException("Unable to write auth params " +
                                               "to server");
        }

        return sock;
    }

    public String toString(){
        return this.agentAddress + ":" + this.agentPort;
    }
}
