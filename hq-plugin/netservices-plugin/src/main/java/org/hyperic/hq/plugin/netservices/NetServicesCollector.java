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

package org.hyperic.hq.plugin.netservices;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.Properties;

import javax.net.ssl.SSLSocket;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.http.conn.ssl.SSLSocketFactory;
import org.hyperic.hq.agent.AgentKeystoreConfig;
import org.hyperic.hq.product.Collector;
import org.hyperic.hq.product.PluginException;
import org.hyperic.sigar.NetConnection;
import org.hyperic.sigar.NetStat;
import org.hyperic.sigar.Sigar;
import org.hyperic.sigar.SigarException;
import org.hyperic.util.security.DefaultSSLProviderImpl;
import org.hyperic.util.security.SSLProvider;

public abstract class NetServicesCollector extends Collector {

    private static Log log = LogFactory.getLog(NetServicesCollector.class);
    
    private int port = -1;
    private int defaultPort, defaultSSLPort;
    private boolean isSSL, enableNetstat;
    private String sslProtcol;
    private AgentKeystoreConfig keystoreConfig;
    private String user, pass;

    private boolean hasCredentials;

    //XXX should just have a NetStat thread that is always
    //collecting, for these services and the platform to share.
    private static Sigar sigar = null;
    
    protected int getDefaultTimeout() {
        return 10; //10 seconds
    }

    protected String getPropTimeout() {
        return "sotimeout";
    }

    protected void netstat() {
        if (!this.enableNetstat) {
            return;
        }
        
        InetSocketAddress saddr = getSocketAddress();
        byte[] address = saddr.getAddress().getAddress();
        int port = saddr.getPort();
        NetStat netstat;
        synchronized (sigar) {
            try {
                netstat =
                    sigar.getNetStat(address, port);
            } catch (SigarException e) {
                return;
            }
        }

        setValue("InboundConnections", netstat.getTcpInboundTotal());
        setValue("OutboundConnections", netstat.getTcpOutboundTotal());
        setValue("AllInboundConnections", netstat.getAllInboundTotal());
        setValue("AllOutboundConnections", netstat.getAllOutboundTotal());

        int[] states = netstat.getTcpStates();
        for (int i=0; i<states.length; i++) {
            setValue("State" + NetConnection.getStateString(i),
                     states[i]);
        }        
    }

    protected void init() throws PluginException {
        Properties props = getProperties();
        
        this.keystoreConfig = new AgentKeystoreConfig();
        this.enableNetstat = getPlugin().isNetStatEnabled();
        
        if (this.enableNetstat) {
            if (sigar == null) {
                sigar = new Sigar();
            }
        }

        try {
            getHostAddress();
        } catch (Exception e) {
            throw new PluginException("Invalid " + PROP_HOSTNAME + "=" +
                                      getHostname(), e);
        }

        this.user = props.getProperty(PROP_USERNAME);
        this.pass = props.getProperty(PROP_PASSWORD);

        this.hasCredentials =
            (this.user != null) || (this.pass != null);

        if (this.hasCredentials) {
            if (this.user == null) {
                throw new PluginException("Missing " + PROP_USERNAME);
            }
            if (this.pass == null) {
                if (log.isDebugEnabled()){
                    log.debug("Password was null, setting to empty string.");
                }
                this.pass = "";
            }
        }
        
        this.defaultPort =
            getIntegerTypeProperty(PROP_PORT);

        this.defaultSSLPort =
            getIntegerTypeProperty(PROP_SSLPORT);

        this.isSSL =
            "true".equals(props.getProperty(PROP_SSL)) || //ssl=true, default to TLS
            PROTOCOL_HTTPS.equals(props.getProperty(PROP_PROTOCOL)); //back-compat

        this.sslProtcol =
            props.getProperty(PROP_SSL_PROTOCOL);
        if (this.sslProtcol != null) {
            //using sslprotocol=SSL|TLS instead of ssl=true
            this.isSSL =
                !this.sslProtcol.equalsIgnoreCase("none");
        }
        else {
            this.sslProtcol = "TLS";
        }
    }

    public abstract void collect();

    public boolean isPoolable() {
        return true;
    }

    public String getHostname() {
        return getProperties().getProperty(PROP_HOSTNAME, DEFAULT_HOSTNAME);
    }

    protected int getIntegerTypeProperty(String key) {
        String val = getPlugin().getTypeProperty(key);
        if (val != null) {
            return Integer.parseInt(val);
        }
        else {
            return -1;
        }
    }
    
    protected int getDefaultPort() {
        return isSSL() ?
               this.defaultSSLPort : this.defaultPort;
    }
    
    public int getPort() {
        if (this.port == -1) {
            this.port =
                getIntegerProperty(PROP_PORT,
                                   getDefaultPort());    
        }
        
        return this.port;
    }

    public String getUsername() {
        return this.user;
    }

    /**
     * @return The password specified. If no password was specified in the 
     * properties, an empty string is returned.
     */
    public String getPassword() {
        return this.pass;
    }

    public boolean hasCredentials() {
        return this.hasCredentials;
    }
    
    public boolean isSSL() {
        return this.isSSL;
    }

    public String getSSLProtocol() {
        return this.sslProtcol;
    }

    public boolean isFollow() {
        return "true".equals(getProperties().getProperty(PROP_FOLLOW));
    }
    
    public String getPath() {
        return getProperties().getProperty(PROP_PATH, "/");
    }

    public InetSocketAddress getSocketAddress() {
        String host = getHostname();
        int port = getPort();
        if (getSource() == null) {
            setSource(host + ":" + port);
        }
        InetSocketAddress saddr =
            new InetSocketAddress(host, port);
        return saddr;
    }
    
    public String getHostAddress() {
        return getSocketAddress().getAddress().getHostAddress();
    }
    
    public SocketWrapper getSocketWrapper() throws IOException {
    	return getSocketWrapper(false);
    }
    
    protected void connect(Socket socket)
                            throws IOException {
        InetSocketAddress saddr = getSocketAddress();
        try {
            socket.connect(saddr, getTimeoutMillis());
            socket.setSoTimeout(getTimeoutMillis());
            setMessage("OK");
        } catch (IOException e) {
            setMessage("connect " + saddr, e);
            throw e;
        }        
    }
    
    public SocketWrapper getSocketWrapper(boolean acceptUnverifiedCertificatesOverride) throws IOException {
        if (isSSL()) { 
            // Sometimes we may want to override what's set in the keystore config...mostly for init purposes...
            boolean accept = acceptUnverifiedCertificatesOverride ? true : keystoreConfig.isAcceptUnverifiedCert();
            SSLProvider sslProvider = new DefaultSSLProviderImpl(keystoreConfig, accept);
            SSLSocketFactory factory = sslProvider.getSSLSocketFactory();
            Socket socket = factory.createSocket();

            socket.connect(getSocketAddress(), getTimeoutMillis());
            socket.setSoTimeout(getTimeoutMillis());
            ((SSLSocket) socket).startHandshake();       

            return new SocketWrapper(socket);
        } else {
            Socket socket = new Socket();
            connect(socket);
            return new SocketWrapper(socket);
        }
    }
}
