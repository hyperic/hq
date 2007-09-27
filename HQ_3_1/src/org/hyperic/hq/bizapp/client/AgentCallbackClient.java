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
import java.net.URL;
import java.util.HashSet;

import org.hyperic.lather.LatherRemoteException;
import org.hyperic.lather.LatherValue;
import org.hyperic.lather.client.LatherHTTPClient;
import org.hyperic.hq.bizapp.agent.ProviderInfo;
import org.hyperic.hq.bizapp.shared.lather.CommandInfo;
import org.hyperic.hq.bizapp.shared.lather.SecureAgentLatherValue;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * Central place for communication back to the server. 
 */
public abstract class AgentCallbackClient {
    private static final int TIMEOUT_CONN = 30 * 1000;
    private static final int TIMEOUT_DATA = 5 * 60 * 1000;

    private static final Log log = 
        LogFactory.getLog(AgentCallbackClient.class.getName());

    private ProviderFetcher fetcher;        // Storage of provider info
    private HashSet         secureCommands; // Secure commands

    public AgentCallbackClient(ProviderFetcher fetcher,
                               String[] secureCommands) {

        this.fetcher         = fetcher;
        this.resetProvider();

        this.secureCommands = new HashSet();
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
    public static String getDefaultProviderURL(String host, int port,
                                               boolean secure)
    {
        String proto;

        if(port == -1){
            port = secure ? 7443 : 7080;
        }

        proto = secure ? "https" : "http";

        return proto + "://" + host + ":" + port +
            "/jboss-lather/JBossLather";
    }

    protected LatherValue invokeLatherCall(ProviderInfo provider,
                                           String methodName,
                                           LatherValue args)
        throws AgentCallbackClientException
    {
        LatherHTTPClient client;
        String addr;

        addr = provider.getProviderAddress();

        if(this.secureCommands.contains(methodName)){
            final String agentToken = provider.getAgentToken();

            ((SecureAgentLatherValue)args).setAgentToken(agentToken);
        }

        client = new LatherHTTPClient(addr, TIMEOUT_CONN, TIMEOUT_DATA);
        try {
            return client.invoke(methodName, args);
        } catch(ConnectException exc){
            // All exceptions are logged as debug.  If the caller wants to
            // log the exception message, it can.
            final String eMsg = "Unable to contact server @ " + addr + ": " +
                exc.getMessage();
        
            this.log.debug(eMsg);
            throw new AgentCallbackClientException(eMsg);
        } catch(IOException exc){
            String msg = exc.getMessage();

            if(msg != null){
                String eMsg;

                if(msg.indexOf("is still starting") != -1){
                    eMsg = "Unable to contact server -- it is still starting";
                    this.log.debug(eMsg);
                } else if(msg.indexOf("Service Unavailable") != -1){
                    eMsg = "Unable to contact server -- it has no more " +
                        "free connections";
                    this.log.debug(eMsg);
                } else {
                    eMsg = "IO error: " + exc.getMessage();
                    this.log.debug(eMsg);
                }
                throw new AgentCallbackClientException(eMsg);
            }
            this.log.debug("IO error", exc);
            throw new AgentCallbackClientException("IO error: " +
                                                   exc.getMessage());
        } catch(LatherRemoteException exc){
            String eMsg;

            if(exc.getMessage().indexOf("Unauthorized agent denied") != -1){
                eMsg = "Unable to invoke '" + methodName + 
                    "':  Permission denied";
                this.log.debug(eMsg);
            } else {
                eMsg = "Remote error while invoking '" + methodName + ": " + 
                    exc.getMessage();
                this.log.debug(eMsg);
            } 

            throw new AgentCallbackClientException(eMsg, exc);
        }
    }
}
