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

package org.hyperic.hq.bizapp.client;

import java.io.IOException;
import java.net.ConnectException;
import java.util.HashSet;

import javax.net.ssl.SSLException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hyperic.hq.agent.AgentKeystoreConfig;
import org.hyperic.hq.agent.stats.AgentStatsCollector;
import org.hyperic.hq.bizapp.agent.ProviderInfo;
import org.hyperic.hq.bizapp.shared.lather.CommandInfo;
import org.hyperic.hq.bizapp.shared.lather.SecureAgentLatherValue;
import org.hyperic.lather.LatherRemoteException;
import org.hyperic.lather.LatherValue;
import org.hyperic.lather.client.LatherHTTPClient;

/**
 * Central place for communication back to the server. 
 */
public abstract class AgentCallbackClient {
    private static final int TIMEOUT_CONN = 30 * 1000;
    private static final int TIMEOUT_DATA = 5 * 60 * 1000;

    private static final Log log = LogFactory.getLog(AgentCallbackClient.class);
    private static final String LATHER_CMD = "LATHER_CMD";
    private ProviderFetcher fetcher;        // Storage of provider info
    private HashSet<String> secureCommands; // Secure commands
    private static AgentStatsCollector statsCollector = AgentStatsCollector.getInstance();
    static {
        statsCollector.register(LATHER_CMD);
        for (String cmd : CommandInfo.ALL_COMMANDS) {
            statsCollector.register(LATHER_CMD + "_" + cmd.toUpperCase());
        }
    }

    public AgentCallbackClient(ProviderFetcher fetcher, String[] secureCommands) {
        this.fetcher = fetcher;
        this.resetProvider();
        this.secureCommands = new HashSet<String>();
        for(int i=0; i<secureCommands.length; i++){
            this.secureCommands.add(secureCommands[i]);
        }
    }

    public AgentCallbackClient(ProviderFetcher fetcher){
        this(fetcher, CommandInfo.SECURE_COMMANDS);
    }

    void resetProvider(){
    }

    /**
     * Get the most up-to-date information about what our provider is, 
     * from the storage provider.
     *
     * @return the string provider (such as jnp:stuff or http:otherstuff)
     */
    protected ProviderInfo getProvider()
        throws AgentCallbackClientException
    {
        ProviderInfo val = this.fetcher.getProvider();
        
        if(val == null){
            final String msg = "Unable to communicate with server -- " +
                               "provider not yet setup";
            throw new AgentCallbackClientException(msg);
        }
        return val;
    }

    /**
     * Check to see if a particular provider URL is valid (i.e. something
     * that we know about && can process.
     */
    public static boolean isValidProviderURL(String provider){
        return provider.startsWith("http:") ||
            provider.startsWith("https:");
    }

    /**
     * Generate a provider URL given a host and port.  This routine 
     * adds in the prefix (such as http:, etc.) as well as the URL
     * after the host to identify the server interface (if necessary)
     *
     * @param host Host to generate provider for
     * @param port Port to use for provider.  If it is -1, the default
     *             port will be used.
     */
    public static String getDefaultProviderURL(String host, int port, boolean secure) {
        String proto;

        if(port == -1){
            port = secure ? 7443 : 7080;
        }

        proto = secure ? "https" : "http";

        return proto + "://" + host + ":" + port +
            "/lather";
    }
    
    /**
     * Retrieve the host name from a provider URL.
     * 
     * @param providerURL The provider URL.
     * @return The host name.
     */
    public static String getHostFromProviderURL(String providerURL) {
        int startIndex = providerURL.indexOf(':')+3;
        
        int endIndex = providerURL.indexOf(':', startIndex);
        
        return providerURL.substring(startIndex, endIndex);
    }

    protected LatherValue invokeLatherCall(ProviderInfo provider, String methodName, LatherValue args)
    throws AgentCallbackClientException {
    	return invokeLatherCall(provider, methodName, args, (new AgentKeystoreConfig()).isAcceptUnverifiedCert());
    }
       
    protected LatherValue invokeLatherCall(ProviderInfo provider, String methodName,
                                           LatherValue args, final boolean acceptUnverifiedCertificates)
    throws AgentCallbackClientException {
        LatherHTTPClient client;
        final boolean debug = log.isDebugEnabled();
        String addr = provider.getProviderAddress();
        if (this.secureCommands.contains(methodName)) {
            final String agentToken = provider.getAgentToken();
            ((SecureAgentLatherValue)args).setAgentToken(agentToken);
        }
        try {
        	client = new LatherHTTPClient(addr, TIMEOUT_CONN, TIMEOUT_DATA, acceptUnverifiedCertificates);
            final long start = now();
            LatherValue rtn = client.invoke(methodName, args);
            final long duration = now()-start;
            statsCollector.addStat(duration, LATHER_CMD);
            statsCollector.addStat(duration, LATHER_CMD + "_" + methodName.toUpperCase());
            return rtn;
        } catch(SSLException e) {
        	if (debug) log.debug(e,e);
        	throw new AgentCallbackClientException(e);
        } catch(ConnectException exc) {
            // All exceptions are logged as debug.  If the caller wants to
            // log the exception message, it can.
            final String eMsg = "Unable to contact server @ " + addr + ": " + exc;
            if (debug) log.debug(eMsg);
            
            throw new AgentCallbackClientException(eMsg);
        } catch(IOException exc) {
            String msg = exc.getMessage();

            if (msg != null) {
                String eMsg;
                
                if (msg.indexOf("Service Unavailable") != -1) {
                    eMsg = "Unable to contact server -- it has no more free connections";
                    
                    if (debug) log.debug(eMsg);
                } else {
                    eMsg = "IO error: " + exc.getMessage();
                    
                    if (debug) log.debug(eMsg);
                }
                
                throw new AgentCallbackClientException(eMsg);
            }
            
            if (debug) log.debug("IO error", exc);
            
            throw new AgentCallbackClientException("IO error: " + exc.getMessage());
        } catch(LatherRemoteException exc) {
            String eMsg;

            if (exc.getMessage().indexOf("Unauthorized agent denied") != -1) {
                eMsg = "Unable to invoke '" + methodName + "':  Permission denied";
                if (debug) log.debug(eMsg);
            } else {
                eMsg = "Remote error while invoking '" + methodName + ": " +  exc;
                if (debug) log.debug(eMsg);
            } 

            throw new AgentCallbackClientException(eMsg, exc);
        } catch(IllegalStateException e) {
        	if (debug) log.debug("Could not create the LatherHTTPClient instance", e);
        	
        	throw new AgentCallbackClientException(e);
        }
    }
    private long now() {
        return System.currentTimeMillis();
    }
}
