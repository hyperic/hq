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

package org.hyperic.hq.agent.handler.bizapp;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hyperic.hq.agent.*;
import org.hyperic.hq.agent.bizapp.agent.*;
import org.hyperic.hq.agent.bizapp.agent.commands.*;
import org.hyperic.hq.agent.server.*;
import org.hyperic.util.exec.Execute;
import org.hyperic.util.exec.ExecuteWatchdog;
import org.hyperic.util.exec.PumpStreamHandler;
import org.hyperic.util.security.SecurityUtil;
import org.springframework.stereotype.Component;

import javax.net.ssl.KeyManager;
import javax.net.ssl.KeyManagerFactory;
import java.io.*;
import java.net.InetAddress;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
import java.util.Properties;

@Component
public class CommandsServer implements AgentServerHandler, TokenStorer, AgentNotificationHandler {
    private static final String KEYSTORE_PW = "storePW";

    private static final long TOKEN_TIMEOUT = 20 * 1000;   // 20 seconds

    private CommandsAPIInfo verAPI = new CommandsAPIInfo();
    private Log log = LogFactory.getLog(CommandsServer.class);
    private TokenManager tokenManager;
    private AgentStorageProvider storage;
    private String tokenFile;
    private String keystoreFile;
    private String keyAlg;
    private AgentStartupCallback agentStartupCallback;

    private AgentService agentService;

    public AgentAPIInfo getAPIInfo() {
        return this.verAPI;
    }

    public String[] getCommandSet() {
        return CommandsAPIInfo.commandSet;
    }

    public AgentRemoteValue dispatchCommand(String cmd, AgentRemoteValue args,
                                            InputStream in, OutputStream out) throws AgentRemoteException {
        if (cmd.equals(CommandsAPIInfo.command_createToken)) {
            return this.createToken(new CreateToken_args(args));
        } else if (cmd.equals(CommandsAPIInfo.command_getServer)) {
            return this.getServer(new GetServer_args(args));
        } else if (cmd.equals(CommandsAPIInfo.command_setServer)) { 
            return this.setServer(agentService, new SetServer_args(args));
        } else {
            throw new AgentRemoteException("Unknown command: " + cmd);
        }
    }

    private String getTokenFile() {
        return this.tokenFile;
    }

    public OutputStream getTokenStoreStream()
            throws IOException {
        return new FileOutputStream(getTokenFile());
    }

    private void storeTokens()
            throws IOException {
        try {
            this.tokenManager.store();
        } catch (IOException exc) {
            this.log.error("Failed to store token data to '" +
                    getTokenFile() +
                    "': " + exc.getMessage());
            throw exc;
        }
    }

    private CreateToken_result createToken(CreateToken_args args) {
        CreateToken_result res;
        TokenData newToken;

        newToken = new TokenData(SecurityUtil.generateRandomToken(),
                System.currentTimeMillis(), false);
        res = new CreateToken_result();
        res.setToken(newToken.getToken());
        this.tokenManager.addToken(newToken);
        try {
            this.storeTokens();
        } catch (IOException exc) {
            // ignore, since there is nothing we can do
        }
        return res;
    }

    private SetServer_result setServer(AgentService agentService, SetServer_args args) {
        SetServer_result res;
        ProviderInfo provider;

        provider = args.getProvider();

        String providerMsg = "Setting the HQ server to: " + provider.getProviderAddress();

        if (provider.isNewTransport()) {
            providerMsg += "; unidirectional=" + provider.isUnidirectional();
        }

        this.log.info(providerMsg);

        CommandsAPIInfo.setProvider(this.storage, provider);
        try {
            this.storage.flush();
        } catch (AgentStorageException exc) {
            this.log.error("Error flushing storage", exc);
        }
        res = new SetServer_result();

        agentService.sendNotification(CommandsAPIInfo.NOTIFY_SERVER_SET, provider.getProviderAddress());
        
        return res;
    }

    private GetServer_result getServer(GetServer_args args) {
        GetServer_result res = new GetServer_result();
        res.setProvider(CommandsAPIInfo.getProvider(this.storage));
 
        return res;
    }

    private KeyStore getKeyStoreInstance()
            throws AgentStartException {
        final String KEYSTORE_TYPE = "JKS";

        try {
            return KeyStore.getInstance(KEYSTORE_TYPE);
        } catch (KeyStoreException exc) {
            throw new AgentStartException("Unable to get keystore instance " +
                    "for " + KEYSTORE_TYPE);
        }
    }

    private String genAgentName() {
        return "AGENT-" + SecurityUtil.generateRandomToken();
    }

    private void keyStoreLoadStream(KeyStore keystore, InputStream loadData)
            throws AgentStartException {
        try {
            keystore.load(loadData, KEYSTORE_PW.toCharArray());
        } catch (IOException exc) {
            throw new AgentStartException("Unable to create new keystore: " +
                    exc.getMessage());
        } catch (NoSuchAlgorithmException exc) {
            throw new AgentStartException("Unable to find algorithm to " +
                    "create keystore: " +
                    exc.getMessage());
        } catch (CertificateException exc) {
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
                        "O=" + "hyperic.net" +
                        ", " +
                        "C=US";
    }

    private void createKeyStore()
            throws AgentStartException {
        ByteArrayOutputStream output = new ByteArrayOutputStream();

        String errmsg =
                "Failed to generate " + this.keystoreFile + ": ";

        String javaHome = System.getProperty("java.home");
        String keytool =
                javaHome + File.separator + "bin" + File.separator + "keytool";

        String[] args = {
                keytool,
                "-genkey",
                "-dname", getDname(),
                "-alias", "HQ",
                "-keystore", this.keystoreFile,
                "-storepass", KEYSTORE_PW,
                "-keypass", KEYSTORE_PW,
                "-keyalg", this.keyAlg
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
            throws AgentStartException {
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
                try {
                    is.close();
                } catch (IOException e) {
                }
            }
        }

        return keyStore;
    }

    private KeyManager[] getKeyManagers(KeyStore useStore)
            throws AgentStartException {
        KeyManagerFactory res;
        String alg;

        alg = KeyManagerFactory.getDefaultAlgorithm();
        try {
            res = KeyManagerFactory.getInstance(alg);
            res.init(useStore, KEYSTORE_PW.toCharArray());
        } catch (Exception exc) {
            throw new AgentStartException("Unable to get default key " +
                    "manager: " + exc.getMessage());
        }

        return res.getKeyManagers();
    }

    private TokenData generateLocalToken(AgentConfig cfg)
            throws AgentStartException {
        InetAddress addr;

        try {
            if ((addr = cfg.getListenIpAsAddr()) == null) {
                //XXX InetAddress.getByAddress is only in jdk 1.4+
                addr = InetAddress.getByName("127.0.0.1");
            }
        } catch (Exception exc) {
            throw new AgentStartException("Unable to get listen IP as addr: " +
                    exc.getMessage());
        }

        return new TokenData(SecurityUtil.generateRandomToken(),
                System.currentTimeMillis(), true);
    }

    private void setupTokenManager(AgentConfig cfg)
            throws AgentStartException {
        String tokenFile = getTokenFile();

        this.tokenManager = new TokenManager(TOKEN_TIMEOUT, this);
        try {
            this.tokenManager.load(new FileInputStream(tokenFile));
        } catch (IOException exc) {
            this.log.warn("Unable to load " + tokenFile + ": generating " +
                    "new file: " + exc.getMessage());
            this.tokenManager.addToken(this.generateLocalToken(cfg));
            try {
                this.storeTokens();
            } catch (IOException eExc) {
                final String msg = "Unable to store " + tokenFile + ": " +
                        eExc.getMessage();

                throw new AgentStartException(msg);
            }
        }
    }

    public void handleNotification(String msgClass, String msg) {
        this.log.debug("handling notification: " + msgClass + ":" + msg);

        if (this.agentStartupCallback != null) {
            this.log.debug("calling back to agent");
            if (msgClass.equals(NotificationConstants.AGENT_UP)) {
                this.agentStartupCallback.onAgentStartup(true);
            } else if (msgClass.equals(NotificationConstants.AGENT_FAILED_START)) {
                this.agentStartupCallback.onAgentStartup(false);
            }
        }

    }

    public void startup(AgentService agentService) throws AgentStartException {
        this.agentService = agentService;
        
        try {

            this.agentStartupCallback = new AgentStartupCallback(agentService.getBootConfig());
            agentService.registerNotifyHandler(this, agentService.getNotifyAgentUp());
            agentService.registerNotifyHandler(this, agentService.getNotifyAgentFailedStart());
        }
        catch (AgentConfigException e) {
            this.log.warn("Failure to find startup " +
                    "reporting port in sys properties");
        }
        catch (IOException e) {
            throw new AgentStartException("Wrapped Exception", e);
        }

        try {
            SSLConnectionListener listener;
            KeyManager[] keyManagers;
            AgentConfig cfg;
            Properties bootConfig;
            KeyStore keystore;

            cfg = agentService.getBootConfig();
            this.tokenFile = cfg.getTokenFile();
            this.setupTokenManager(cfg);
            bootConfig = cfg.getBootProperties();
            this.storage = agentService.getStorageProvider();
            this.keystoreFile =
                    bootConfig.getProperty(AgentConfig.PROP_KEYSTORE[0]);
            this.keyAlg =
                    bootConfig.getProperty("agent.keyalg", "RSA");
            keystore = this.loadKeyStore();
            keyManagers = this.getKeyManagers(keystore);
            listener = new SSLConnectionListener(cfg, keyManagers,
                    this.tokenManager);
            agentService.setConnectionListener(listener);
        } catch (AgentRunningException exc) {
            throw new AgentStartException("Unable to initialize SSL: " +
                    exc.getMessage());
        }
        this.log.info("Commands Server started up");
    }

    public void shutdown() {
        this.log.info("Commands Server shut down");
    }
}
