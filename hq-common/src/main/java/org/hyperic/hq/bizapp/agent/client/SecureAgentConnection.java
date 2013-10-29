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

import java.io.DataOutputStream;
import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;

import javax.net.ssl.SSLSocket;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.http.conn.ssl.SSLSocketFactory;
import org.hyperic.hq.agent.client.AgentConnection;
import org.hyperic.util.security.DefaultSSLProviderImpl;
import org.hyperic.util.security.KeystoreConfig;
import org.hyperic.util.security.SSLProvider;

/**
 * An object which wraps an AgentConnection object, so as to provide
 * SSL capabilities (associated with the CAMServerHandler's handleConnection
 * method)
 */
public class SecureAgentConnection 
    extends AgentConnection
{
    private static final Log log = LogFactory.getLog(SecureAgentConnection.class);

    private static final String PROP_READ_TIMEOUT = "agent.readTimeOut";
    private static final String PROP_POST_HANDSHAKE_TIMEOUT = "agent.postHandshakeTimeOut";
    private static final int READ_TIMEOUT = 60000;
    private static final int POST_HANDSHAKE_TIMEOUT = 5 * 60 * 1000;

    private String agentAddress;
    private int    agentPort;
    private String authToken;
    private boolean acceptUnverifiedCertificate = false;

    private KeystoreConfig keystoreConfig;

    private SecureAgentConnection(KeystoreConfig keystoreConfig, String agentAddress, int agentPort,
                                  String authToken) {
        super(agentAddress, agentPort);
        this.agentAddress = agentAddress;
        this.agentPort    = agentPort;
        this.authToken    = authToken;
        this.keystoreConfig = keystoreConfig;
    }
    
    public SecureAgentConnection(String agentAddress, int agentPort, String authToken,
                                 KeystoreConfig keystoreConfig,
                                 boolean acceptUnverifiedCertificate) {
    	this(keystoreConfig, agentAddress, agentPort, authToken);
    	this.acceptUnverifiedCertificate = acceptUnverifiedCertificate;
    }
    
    @Override
    protected Socket getSocket()
        throws IOException
    {
        SSLSocket socket;

        log.debug("Creating secure socket");

        try {
            // Check for configured agent read timeout from System properties
            int readTimeout;
           
            try {
                readTimeout = Integer.parseInt(System.getProperty(PROP_READ_TIMEOUT));
            } catch (NumberFormatException e) {
                readTimeout = READ_TIMEOUT;
            }
            
            // Check for configured agent post handshake timeout
            // from System properties
            int postHandshakeTimeout;
            try {
                postHandshakeTimeout =
                    Integer.parseInt(System.getProperty(PROP_POST_HANDSHAKE_TIMEOUT));
            } catch (NumberFormatException e) {
                postHandshakeTimeout = POST_HANDSHAKE_TIMEOUT;
            }
            
            SSLProvider sslProvider = new DefaultSSLProviderImpl(keystoreConfig, acceptUnverifiedCertificate);
            
            SSLSocketFactory factory = sslProvider.getSSLSocketFactory();

        	// See the following links...
        	// http://www.apache.org/dist/httpcomponents/httpcore/RELEASE_NOTES-4.1.x.txt
        	// http://www-128.ibm.com/developerworks/forums/dw_thread.jsp?message=13695343&cat=10&thread=73546&treeDisplayType=threadmode1&forum=178#13695343
        	// In any case, it would seem as though the bug has since been fixed in IBM's JRE, no need to work around it anymore...
            socket = (SSLSocket) factory.createSocket();
            
            // Make sure the InetAddress used to initialize the socket has a non-null hostname (empty string).
            // This prevents slow and unnecessary reverse DNS querying when the connection is opened.
            InetAddress withoutHost = InetAddress.getByName(this.agentAddress);
            InetAddress withHost = InetAddress.getByAddress("", withoutHost.getAddress());
            InetSocketAddress address = new InetSocketAddress( withHost, this.agentPort);
            
            socket.connect(address, readTimeout);

            // Set the socket timeout during the initial handshake to detect
            // connection issues with the agent.  
            socket.setSoTimeout(readTimeout);
            
            log.debug("Secure socket is connected to " + address + " - starting handshake.");

            socket.startHandshake();

            log.debug("SSL handshake complete");
            
            // [HHQ-3694] The timeout is set to a post handshake value.
            socket.setSoTimeout(postHandshakeTimeout);
            
        } catch(IOException exc){
            IOException toThrow = new IOException("Unable to connect to " +
                                  this.agentAddress + ":" +
                                  this.agentPort + ": " +
                                  exc.getMessage());
            // call initCause instead of constructor to be java 1.5 compat
            toThrow.initCause(exc);
            throw toThrow;
        }

        // Write our security settings
        try {
            DataOutputStream dOs;

            dOs = new DataOutputStream(socket.getOutputStream());
            dOs.writeUTF(this.authToken);
        } catch(IOException exc){
            IOException toThrow = new IOException("Unable to write auth params to server");
            // call initCause instead of constructor to be java 1.5 compat
            toThrow.initCause(exc);
            throw toThrow;
        }

        return socket;
    }

    public String toString(){
        return this.agentAddress + ":" + this.agentPort;
    }
}
