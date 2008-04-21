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

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.Socket;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
import java.util.Properties;

import org.hyperic.hq.agent.AgentAPIInfo;
import org.hyperic.hq.agent.AgentConfig;
import org.hyperic.hq.agent.AgentRemoteException;
import org.hyperic.hq.agent.AgentRemoteValue;
import org.hyperic.hq.agent.server.AgentDaemon;
import org.hyperic.hq.agent.server.AgentNotificationHandler;
import org.hyperic.hq.agent.server.AgentRunningException;
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
import org.hyperic.hq.transport.AgentTransport;
import org.hyperic.util.exec.Execute;
import org.hyperic.util.exec.ExecuteWatchdog;
import org.hyperic.util.exec.PumpStreamHandler;
import org.hyperic.util.security.SecurityUtil;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import javax.net.ssl.KeyManager;
import javax.net.ssl.KeyManagerFactory;

public class CommandsServer 
    implements AgentServerHandler, TokenStorer, AgentNotificationHandler
{
    private static final String KEYSTORE_PW = "storePW";
    private static final String KEYSTORE_FILE = "data/agent.keystore";

    private static final long   TOKEN_TIMEOUT = 20 * 1000;   // 20 seconds

    private CommandsAPIInfo      verAPI;
    private Log                  log;
    private TokenManager         tokenManager;
    private AgentStorageProvider storage;
    private AgentDaemon          agent;
    private Socket               startupSock;
    private String               tokenFile;
    private String               keystoreFile;
    private String               keyAlg;

    public CommandsServer(){
        this.verAPI       = new CommandsAPIInfo();
        this.log          = LogFactory.getLog(CommandsServer.class);
        this.tokenManager = null;
        this.storage      = null;
        this.agent        = null;
        this.startupSock  = null;
        this.keystoreFile = null;
        this.keyAlg       = null;
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
        this.log.info("Setting the HQ server to: " + 
                      provider.getProviderAddress());
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

    private String genAgentName(){
        return "AGENT-" + SecurityUtil.generateRandomToken();
    }

    private void keyStoreLoadStream(KeyStore keystore, InputStream loadData)
        throws AgentStartException
    {
        try {
            keystore.load(loadData, KEYSTORE_PW.toCharArray());
        } catch(IOException exc){
            throw new AgentStartException("Unable to create new keystore: " +
                                          exc.getMessage());
        } catch(NoSuchAlgorithmException exc){
            throw new AgentStartException("Unable to find algorithm to " +
                                          "create keystore: " + 
                                          exc.getMessage());
        } catch(CertificateException exc){
            throw new AgentStartException("Unable to load certificate from " +
                                          "keystore: " + exc.getMessage());
        }
    }

    private String getDname() {
        return
        "CN=" + genAgentName() +
        ", " +
        "OU=" + "HQ" +
        ", " +
        "O="  + "hyperic.net" +
        ", " +
        "C=US";
    }

    private void createKeyStore()
        throws AgentStartException
    {
        ByteArrayOutputStream output = new ByteArrayOutputStream();

        String errmsg =
            "Failed to generate " + this.keystoreFile + ": ";

        String javaHome = System.getProperty("java.home");
        String keytool =
            javaHome + File.separator + "bin" + File.separator + "keytool";
        
        String[] args = {
            keytool,
            "-genkey",
            "-dname",     getDname(),
            "-alias",     "HQ",
            "-keystore",  this.keystoreFile,
            "-storepass", KEYSTORE_PW,
            "-keypass",   KEYSTORE_PW,
            "-keyalg",    this.keyAlg
        };

        int timeout = 5 * 60 * 1000; //5min
        ExecuteWatchdog wdog = new ExecuteWatchdog(timeout);
        Execute exec =
            new Execute(new PumpStreamHandler(output), wdog);
        exec.setCommandline(args);

        log.debug("Generating keystore: " +
                  exec.getCommandLineString());

        int rc;
        try {
            rc = exec.execute();
        } catch (Exception e) {
            rc = -1;
            log.error(e);
        }
        
        if (rc != 0) {
            String msg = output.toString().trim();
            if (msg.length() == 0) {
                msg = "timeout after " + timeout + "ms";
            }
            throw new AgentStartException(errmsg + "[" +
                                          exec.getCommandLineString() + "] " +
                                          msg);
        }
    }

    private KeyStore loadKeyStore()
        throws AgentStartException
    {
        KeyStore keyStore;
        FileInputStream is = null;

        File ks = new File(this.keystoreFile);

        if (!ks.exists()) {
            createKeyStore();
            this.log.warn("Agent certificate not found --" +
                          " generating a new one");
        }
        
        try {
            keyStore = getKeyStoreInstance();
            is = new FileInputStream(this.keystoreFile);
            keyStoreLoadStream(keyStore, is);
        } catch (Exception e) {
            String msg =
                "Loading keystore file " + this.keystoreFile +
                ": " + e.getMessage();
            throw new AgentStartException(msg, e);
        } finally {
            if (is != null) {
                try { is.close(); } catch (IOException e) {}
            }
        }

        return keyStore;
    }

    private KeyManager[] getKeyManagers(KeyStore useStore)
        throws AgentStartException
    {
        KeyManagerFactory res;
        String alg;

        alg = KeyManagerFactory.getDefaultAlgorithm();
        try {
            res = KeyManagerFactory.getInstance(alg);
            res.init(useStore, KEYSTORE_PW.toCharArray());
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
        if(msgClass.equals(AgentDaemon.NOTIFY_AGENT_UP)){
            try {
                DataOutputStream dOs;

                dOs = new DataOutputStream(this.startupSock.getOutputStream());
                dOs.writeInt(1);
            } catch(IOException exc){
                this.log.error("Error writing startup state to startup port: "+
                               exc.getMessage());
            } finally {
                try {this.startupSock.close();} catch(IOException iexc){}
            }
        }
    }

    public void startup(AgentDaemon agent, AgentTransport agentTransport)
        throws AgentStartException 
    {
        String startupPort;
        int sPort;

        startupPort = System.getProperty(CommandsAPIInfo.PROP_UP_PORT);
        if(startupPort == null){
            this.log.warn("Failure to find startup " +
                          "reporting port in sys properties");
        } else {
            sPort = Integer.parseInt(startupPort);
            try {
                this.startupSock = new Socket("127.0.0.1", sPort);
            } catch(IOException exc){
                throw new AgentStartException("Failed to connect to startup " +
                                              "port (" + sPort + "): " +
                                              exc.getMessage(), exc);
            }
            agent.registerNotifyHandler(this, AgentDaemon.NOTIFY_AGENT_UP);
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
            this.keystoreFile =
                bootConfig.getProperty("agent.keystore", KEYSTORE_FILE);
            this.keyAlg =
                bootConfig.getProperty("agent.keyalg", "RSA");
            keystore     = this.loadKeyStore();
            keyManagers  = this.getKeyManagers(keystore);
            listener     = new SSLConnectionListener(cfg, keyManagers,
                                                     this.tokenManager);
            agent.setConnectionListener(listener);
        } catch(AgentRunningException exc){
            throw new AgentStartException("Unable to initialize SSL: " +
                                          exc.getMessage());
        }
        this.log.info("Commands Server started up");
    }

    public void shutdown(){
        this.log.info("Commands Server shut down");
    }
}
