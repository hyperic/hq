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
import java.io.EOFException;
import java.io.IOException;
import java.io.InterruptedIOException;
import java.net.InetAddress;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLServerSocket;
import javax.net.ssl.SSLServerSocketFactory;
import javax.net.ssl.SSLSocket;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hyperic.hq.agent.AgentConfig;
import org.hyperic.hq.agent.AgentConnectionException;
import org.hyperic.hq.agent.AgentKeystoreConfig;
import org.hyperic.hq.agent.server.AgentConnectionListener;
import org.hyperic.hq.agent.server.AgentServerConnection;
import org.hyperic.hq.agent.server.AgentStartException;
import org.hyperic.hq.bizapp.agent.TokenData;
import org.hyperic.hq.bizapp.agent.TokenManager;
import org.hyperic.hq.bizapp.agent.TokenNotFoundException;
import org.hyperic.util.security.DefaultSSLProviderImpl;
import org.hyperic.util.security.SSLProvider;

class SSLConnectionListener
    extends AgentConnectionListener
{
    
    private static final String PROP_READ_TIMEOUT = "agent.readTimeOut";
    private static int READ_TIMEOUT = 120000;
    static {
        try {
            READ_TIMEOUT = Integer.parseInt(System.getProperty(PROP_READ_TIMEOUT));
        } catch (NumberFormatException e) {
        }
    }
    private SSLServerSocket listenSock;
    private Log             log;
    private TokenManager    tokenManager;

    public SSLConnectionListener(AgentConfig cfg, TokenManager tokenManager)
    {
        super(cfg);
        this.listenSock   = null;
        this.log          = LogFactory.getLog(SSLConnectionListener.class);
        this.tokenManager = tokenManager;
    }

    private SSLServerConnection handleNewConn(SSLSocket sock)
    throws AgentConnectionException, SocketTimeoutException {
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
            
            this.log.debug("Starting to read authToken for SSL connection");
            authToken = dIs.readUTF();
            this.log.debug("Finished reading authToken for SSL connection");
        } catch(SocketTimeoutException exc) {
            throw exc;
        } catch(IOException exc){
            throw new AgentConnectionException("Error negotiating auth: " +
                                               exc.getMessage(), exc);
        }

        // Set the token from pending to locked, if need be
        doSave = false;
        try {
            token = this.tokenManager.getToken(authToken);
        } catch(TokenNotFoundException exc){
            this.log.error("Rejecting client from " + remoteAddr + 
                           ": Passed an invalid auth token (" +
                           authToken + ")", exc);
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
                log.debug(iExc, iExc);
            } catch (EOFException e) {
                log.debug(e, e);
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
        this.log.debug("Done connecting SSL");
        return res;
    }

    public AgentServerConnection getNewConnection()
    throws AgentConnectionException, InterruptedIOException {
        AgentServerConnection res;
        SSLSocket inConn = null;
        boolean success = false;
        try {
            inConn  = (SSLSocket)this.listenSock.accept();
            inConn.setSoTimeout(READ_TIMEOUT);
        } catch(InterruptedIOException exc){
            throw exc;
        } catch(IOException exc){
            throw new AgentConnectionException(exc.getMessage(), exc);
        }
        try {
            res     = handleNewConn(inConn);
            success = true;
        } catch (SocketTimeoutException e) {
            InterruptedIOException toThrow = new InterruptedIOException();
            toThrow.initCause(e);
            log.warn("socket timed out while handling a command from the server: " + e);
            log.debug(e,e);
            throw toThrow;
        } finally {
            if(!success) {
                close(inConn);
            }
        }
        return res;
    }

    private void close(SSLSocket socket) {
        if (socket != null){
            try {
                socket.close();
            } catch(IOException exc){
                log.debug(exc,exc);
            }
        }
    }

    public void setup(int timeout) throws AgentStartException {
        AgentConfig cfg = this.getConfig();
        AgentKeystoreConfig keystoreConfig = new AgentKeystoreConfig();
    	SSLProvider provider = new DefaultSSLProviderImpl(keystoreConfig,keystoreConfig.isAcceptUnverifiedCert());
        SSLContext context = provider.getSSLContext();
    	SSLServerSocketFactory sFactory = context.getServerSocketFactory();
        
        InetAddress addr;
        
        try {
            addr = cfg.getListenIpAsAddr();
        } catch(UnknownHostException exc){
            throw new AgentStartException("Failed to setup listen socket on '" + cfg.getListenIp() + "': unknown host");
        }

        int port = cfg.getListenPort();
        // Better to retry until this succeeds rather than give up and not allowing
        // the agent to start
        while (true) {
            try {
            	listenSock = (SSLServerSocket)sFactory.createServerSocket(port, 50, addr);
            	listenSock.setEnabledCipherSuites(getSupportedAndEnabledCiphers(cfg.getEnabledCipherList(), sFactory));
                listenSock.setSoTimeout(timeout);
                break;
            } catch(IOException exc){
                if (listenSock != null) {
                    try {
                        listenSock.close();
                    } catch (IOException e1) {
                        log.debug(e1,e1);
                    }
                }
                log.warn("Failed to listen at " + cfg.getListenIp() +
                         ":" + port + ": " + exc.getMessage() + ".  Will retry until up.");
                log.debug(exc,exc);
                try {
                    Thread.sleep(30000);
                } catch (InterruptedException e) {
                    log.debug(e,e);
                }
            }
        }
    }
    
    /**
     * Find the intersection between enabled and supported ciphers.
     * 
     * If a cipher is enabled but not supported, log it in the agent's log.
     * 
     * @param enabledCiphers
     * @param sFactory
     * @return
     */
    private String[] getSupportedAndEnabledCiphers(List<String> enabledCiphers, SSLServerSocketFactory sFactory) {
    	Set<String> supportedCiphers = new HashSet<String>(Arrays.asList(sFactory.getSupportedCipherSuites()));
    	List<String> unsupportedCiphers = new ArrayList<String>();
    	for (String cipher : enabledCiphers) {
    		if (!supportedCiphers.contains(cipher)) {
    			unsupportedCiphers.add(cipher);
    			log.warn("Cipher " + cipher + " is not supported, removing from list of negotiable ciphers.");
    		}
    	}
    	enabledCiphers.removeAll(unsupportedCiphers);
    	return enabledCiphers.toArray(new String[]{});
    }

    public void cleanup(){
        if(this.listenSock != null){
            log.info("closing listener socket " + listenSock.getInetAddress() + ":" + listenSock.getLocalPort());
            try {this.listenSock.close();} catch(IOException exc){}
            this.listenSock = null;
        }
    }
}
