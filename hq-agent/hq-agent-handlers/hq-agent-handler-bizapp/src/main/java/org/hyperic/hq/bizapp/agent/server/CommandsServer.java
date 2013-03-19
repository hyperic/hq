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


import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.Socket;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.util.Properties;

import javax.net.ssl.KeyManager;
import javax.net.ssl.KeyManagerFactory;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hyperic.hq.agent.AgentAPIInfo;
import org.hyperic.hq.agent.AgentConfig;
import org.hyperic.hq.agent.AgentConfigException;
import org.hyperic.hq.agent.AgentKeystoreConfig;
import org.hyperic.hq.agent.AgentRemoteException;
import org.hyperic.hq.agent.AgentRemoteValue;
import org.hyperic.hq.agent.AgentStartupCallback;
import org.hyperic.hq.agent.server.AgentDaemon;
import org.hyperic.hq.agent.server.AgentNotificationHandler;
import org.hyperic.hq.agent.server.AgentServerHandler;
import org.hyperic.hq.agent.server.AgentStartException;
import org.hyperic.hq.agent.server.AgentStorageException;
import org.hyperic.hq.agent.server.AgentStorageProvider;
import org.hyperic.hq.bizapp.agent.CommandsAPIInfo;
import org.hyperic.hq.bizapp.agent.ProviderInfo;
import org.hyperic.hq.bizapp.agent.TokenData;
import org.hyperic.hq.bizapp.agent.TokenManager;
import org.hyperic.hq.bizapp.agent.TokenStorer;
import org.hyperic.hq.bizapp.agent.commands.CreateToken_args;
import org.hyperic.hq.bizapp.agent.commands.CreateToken_result;
import org.hyperic.hq.bizapp.agent.commands.GetServer_args;
import org.hyperic.hq.bizapp.agent.commands.GetServer_result;
import org.hyperic.hq.bizapp.agent.commands.SetServer_args;
import org.hyperic.hq.bizapp.agent.commands.SetServer_result;
import org.hyperic.util.security.KeystoreConfig;
import org.hyperic.util.security.KeystoreManager;
import org.hyperic.util.security.SecurityUtil;

public class CommandsServer 
    implements AgentServerHandler, TokenStorer, AgentNotificationHandler
{
    private static final long   TOKEN_TIMEOUT = 20 * 1000;   // 20 seconds

    private CommandsAPIInfo      verAPI;
    private Log                  log;
    private TokenManager         tokenManager;
    private AgentStorageProvider storage;
    private AgentDaemon          agent;
    private Socket               startupSock;
    private String               tokenFile;
    private String               keyAlg;
    private AgentStartupCallback agentStartupCallback;

    public CommandsServer(){
        this.verAPI       = new CommandsAPIInfo();
        this.log          = LogFactory.getLog(CommandsServer.class);
        this.tokenManager = null;
        this.storage      = null;
        this.agent        = null;
        this.startupSock  = null;
        this.keyAlg       = null;
        this.agentStartupCallback = null;
    }

    public AgentAPIInfo getAPIInfo(){
        return this.verAPI;
    }

    public String[] getCommandSet(){
        return CommandsAPIInfo.commandSet;
    }

    public AgentRemoteValue dispatchCommand(String cmd, AgentRemoteValue args,
                                            InputStream in, OutputStream out)
        throws AgentRemoteException 
    {
        if(cmd.equals(CommandsAPIInfo.command_createToken)){
            return this.createToken(new CreateToken_args(args));
        } else if(cmd.equals(CommandsAPIInfo.command_getServer)){
            return this.getServer(new GetServer_args(args));
        } else if(cmd.equals(CommandsAPIInfo.command_setServer)){
            return this.setServer(new SetServer_args(args));
        } else {
            throw new AgentRemoteException("Unknown command: " + cmd);
        }
    }

    private String getTokenFile() {
        return this.tokenFile;
    }

    public OutputStream getTokenStoreStream()
        throws IOException
    {
        return new FileOutputStream(getTokenFile());
    }

    private void storeTokens()
        throws IOException
    {
        try {
            this.tokenManager.store();
        } catch(IOException exc){
            this.log.error("Failed to store token data to '" + 
                           getTokenFile() + 
                           "': " + exc.getMessage());
            throw exc;
        }
    }

    private CreateToken_result createToken(CreateToken_args args){
        CreateToken_result res;
        TokenData newToken;

        newToken = new TokenData(SecurityUtil.generateRandomToken(), 
                                 System.currentTimeMillis(), false);
        res = new CreateToken_result();
        res.setToken(newToken.getToken());
        this.tokenManager.addToken(newToken);
        try {
            this.storeTokens();
        } catch(IOException exc){
            // ignore, since there is nothing we can do
        }
        return res;
    }

    private SetServer_result setServer(SetServer_args args){
        SetServer_result res;
        ProviderInfo provider;

        provider = args.getProvider();
        
        String providerMsg = "Setting the HQ server to: "+provider.getProviderAddress();
                
        if (provider.isNewTransport()) {
            providerMsg+="; unidirectional="+provider.isUnidirectional();
        }
        
        this.log.info(providerMsg);
                
        CommandsAPIInfo.setProvider(this.storage, provider);
        try {
            this.storage.flush();
        } catch(AgentStorageException exc){
            this.log.error("Error flushing storage", exc);
        }
        res = new SetServer_result();

        this.agent.sendNotification(CommandsAPIInfo.NOTIFY_SERVER_SET,
                                    provider.getProviderAddress());
        return res;
    }

    private GetServer_result getServer(GetServer_args args){
        GetServer_result res;
        ProviderInfo provider;

        res = new GetServer_result();
        res.setProvider(CommandsAPIInfo.getProvider(this.storage));
        return res;
    }

    private KeyStore getKeyStoreInstance()
        throws AgentStartException
    {
        final String KEYSTORE_TYPE = "JKS";

        try {
            return KeyStore.getInstance(KEYSTORE_TYPE);
        } catch(KeyStoreException exc){
            throw new AgentStartException("Unable to get keystore instance " +
                                          "for " + KEYSTORE_TYPE);
        }
    }
    
    private KeyManager[] getKeyManagers(KeyStore useStore,String filePass)
        throws AgentStartException
    {
        KeyManagerFactory res;
        String alg;

        alg = KeyManagerFactory.getDefaultAlgorithm();
        try {
            res = KeyManagerFactory.getInstance(alg);
            res.init(useStore, filePass.toCharArray());
        } catch(Exception exc){
            throw new AgentStartException("Unable to get default key " +
                                          "manager: " + exc.getMessage());
        }
        return res.getKeyManagers();
    }

    private TokenData generateLocalToken(AgentConfig cfg)
        throws AgentStartException
    {
        InetAddress addr;

        try {
            if((addr = cfg.getListenIpAsAddr()) == null){
                //XXX InetAddress.getByAddress is only in jdk 1.4+
                addr = InetAddress.getByName("127.0.0.1");
            }
        } catch(Exception exc){
            throw new AgentStartException("Unable to get listen IP as addr: " +
                                          exc.getMessage());
        }
        
        return new TokenData(SecurityUtil.generateRandomToken(), 
                             System.currentTimeMillis(), true);
    }

    private void setupTokenManager(AgentConfig cfg)
        throws AgentStartException
    {
        String tokenFile = getTokenFile();

        this.tokenManager = new TokenManager(TOKEN_TIMEOUT, this);
        try {
            this.tokenManager.load(new FileInputStream(tokenFile));
        } catch(IOException exc){
            this.log.warn("Unable to load " + tokenFile + ": generating " +
                          "new file: " + exc.getMessage());
            this.tokenManager.addToken(this.generateLocalToken(cfg));
            try {
                this.storeTokens();
            } catch(IOException eExc){
                final String msg = "Unable to store " + tokenFile + ": " + 
                    eExc.getMessage();

                throw new AgentStartException(msg);
            }
        }
    }

    public void handleNotification(String msgClass, String msg){
        this.log.debug("handling notification: "+msgClass+":"+ msg);
        
        if (this.agentStartupCallback != null) {
            this.log.debug("calling back to agent");
            if(msgClass.equals(AgentDaemon.NOTIFY_AGENT_UP)){
                this.agentStartupCallback.onAgentStartup(true);
            } else if (msgClass.equals(AgentDaemon.NOTIFY_AGENT_FAILED_START)) {
                this.agentStartupCallback.onAgentStartup(false);
            }            
        }
        
    }
    
    public void startup(AgentDaemon agent)
        throws AgentStartException 
    {
        try {
            this.agentStartupCallback = new AgentStartupCallback(agent.getBootConfig());
            agent.registerNotifyHandler(this, AgentDaemon.NOTIFY_AGENT_UP);
            agent.registerNotifyHandler(this, AgentDaemon.NOTIFY_AGENT_FAILED_START);
        }
        catch (AgentConfigException e) {
            log.warn("Failure to find startup reporting port in sys properties: " + e);
            log.debug(e,e);
        }
        catch (IOException e) {
            throw new AgentStartException("Wrapped Exception", e);
        }
        
        this.agent = agent;

        try {
            SSLConnectionListener listener;
            KeyManager[] keyManagers;
            AgentConfig cfg;
            Properties bootConfig;
            KeyStore keystore;

            cfg          = agent.getBootConfig();
            this.tokenFile = cfg.getTokenFile();
            this.setupTokenManager(cfg);
            bootConfig   = cfg.getBootProperties();
            this.storage = agent.getStorageProvider();
            this.keyAlg =
                bootConfig.getProperty("agent.keyalg", "RSA");
            
            KeystoreConfig  keystoreConfig = new AgentKeystoreConfig();
            keystore     = KeystoreManager.getKeystoreManager().getKeyStore(keystoreConfig );
            keyManagers  = this.getKeyManagers(keystore,keystoreConfig.getFilePassword());
            listener     = new SSLConnectionListener(cfg, this.tokenManager);
            agent.setConnectionListener(listener);
        } catch(Exception exc){
            //This catch is intended to catch AgentRunningException, KeyStoreException, IOException
            throw new AgentStartException("Unable to initialize SSL: " + exc.getMessage(), exc);
        }
        this.log.info("Commands Server started up");
    }

    public final void postInitActions() throws AgentStartException { /*do nothing*/ }//EOM
    
    public void shutdown(){
        this.log.info("Commands Server shut down");
    }
}
