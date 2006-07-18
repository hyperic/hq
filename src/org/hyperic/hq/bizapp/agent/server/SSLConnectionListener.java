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

package org.hyperic.hq.bizapp.agent.server;

import java.io.DataInputStream;
import java.io.IOException;
import java.io.InterruptedIOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Iterator;
import java.util.List;

import javax.net.ssl.KeyManager;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLServerSocket;
import javax.net.ssl.SSLServerSocketFactory;
import javax.net.ssl.SSLSocket;

import org.hyperic.hq.agent.AgentConfig;
import org.hyperic.hq.agent.AgentConnectionException;
import org.hyperic.hq.agent.server.AgentConnectionListener;
import org.hyperic.hq.agent.server.AgentServerConnection;
import org.hyperic.hq.agent.server.AgentStartException;
import org.hyperic.hq.bizapp.agent.CommonSSL;
import org.hyperic.hq.bizapp.agent.TokenData;
import org.hyperic.hq.bizapp.agent.TokenManager;
import org.hyperic.hq.bizapp.agent.TokenNotFoundException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

class SSLConnectionListener
    extends AgentConnectionListener
{
    private SSLServerSocket listenSock;
    private Log             log;
    private KeyManager[]    kManagers;
    private TokenManager    tokenManager;

    public SSLConnectionListener(AgentConfig cfg, 
                                 KeyManager[] kManagers,
                                 TokenManager tokenManager)
    {
        super(cfg);
        this.listenSock   = null;
        this.log          = LogFactory.getLog(SSLConnectionListener.class);
        this.kManagers    = kManagers;
        this.tokenManager = tokenManager;
    }

    private SSLServerConnection handleNewConn(SSLSocket sock)
        throws AgentConnectionException
    {
        SSLServerConnection res;
        InetAddress remoteAddr;
        TokenData token;
        String authToken;
        boolean doSave;

        remoteAddr = sock.getInetAddress();
        this.log.debug("Handling SSL connection from " + remoteAddr);
        res = new SSLServerConnection(sock);
        
        // Validate the actual auth token which is sent
        try {
            DataInputStream dIs;

            dIs = new DataInputStream(sock.getInputStream());
            authToken = dIs.readUTF();
        } catch(IOException exc){
            throw new AgentConnectionException("Error negotiating auth: " +
                                               exc.getMessage());
        }

        // Set the token from pending to locked, if need be
        doSave = false;
        try {
            token = this.tokenManager.getToken(authToken);
        } catch(TokenNotFoundException exc){
            this.log.error("Rejecting client from " + remoteAddr + 
                           ": Passed an invalid auth token (" +
                           authToken + ")");
            // Due to 20 second expiration, the tokens in the manager
            // may not match what is in the tokendata.
            List l = this.tokenManager.getTokens();
            for(Iterator i=l.iterator(); i.hasNext();) {
                TokenData data = (TokenData)i.next();
                this.log.debug("Token: " + data.getToken() + ":" +
                               data.getCreateTime() + ":" +
                               (data.isLocked() ? "locked" : "pending"));
            }

            try {
                res.readCommand();
                res.sendErrorResponse("Unauthorized");
            } catch(AgentConnectionException iExc){
            }

            throw new AgentConnectionException("Client from " + remoteAddr +
                                               " unauthorized");
        }

        if(!token.isLocked()){
            try {
                this.log.info("Locking auth token");
                this.tokenManager.setTokenLocked(token, true);
                doSave = true;
            } catch(TokenNotFoundException exc){
                // This should never occur
                this.log.error("Error setting token '" + token + "' to " +
                               "locked state -- it no longer exists");
            }
        }

        // If set the token, re-store the data.
        if(doSave){
            try {
                this.tokenManager.store();
            } catch(IOException exc){
                this.log.error("Error storing token data: " + 
                               exc.getMessage());
            }
        }
        return res;
    }

    public AgentServerConnection getNewConnection()
        throws AgentConnectionException, InterruptedIOException
    {
        AgentServerConnection res;
        SSLSocket inConn = null;
        boolean success = false;

        try {
            inConn  = (SSLSocket)this.listenSock.accept();
            res     = this.handleNewConn(inConn);
            success = true;
        } catch(InterruptedIOException exc){
            throw exc;
        } catch(IOException exc){
            throw new AgentConnectionException(exc.getMessage());
        } finally {
            if(success == false && inConn != null){
                try {
                    inConn.close();
                } catch(IOException exc){
                }
            }
        }

        return res;
    }

    public void setup(int timeout)
        throws AgentStartException
    {
        SSLServerSocketFactory sFactory;
        AgentConfig cfg;
        SSLContext context;
        InetAddress addr;
        int port;
            
        try {
            context = CommonSSL.getSSLContext();
            context.init(this.kManagers, null, null);
        } catch(Exception exc){
            throw new AgentStartException("Unable to setup SSL context: " + 
                                          exc.getMessage());
        }

        sFactory = context.getServerSocketFactory();
        
        cfg = this.getConfig();
        try {
            addr = cfg.getListenIpAsAddr();
        } catch(UnknownHostException exc){
            throw new AgentStartException("Failed to setup listen socket " +
                                          " on '" + cfg.getListenIp() + "': "+
                                          "unknown host");
        }

        port = cfg.getListenPort();
        try {
            this.listenSock = 
                (SSLServerSocket)sFactory.createServerSocket(port, 50, addr);
            this.listenSock.setSoTimeout(timeout);
        } catch(IOException exc){
            throw new AgentStartException("Failed to listen at " +
                                          cfg.getListenIp() + ":" + port +
                                          ": " + exc.getMessage());
        }
    }

    public void cleanup(){
        if(this.listenSock != null){
            try {this.listenSock.close();} catch(IOException exc){}
            this.listenSock = null;
        }
    }
}
