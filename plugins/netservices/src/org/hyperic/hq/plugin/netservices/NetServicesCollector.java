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
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.util.Properties;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocket;
import javax.net.ssl.X509TrustManager;

import org.hyperic.hq.product.Collector;
import org.hyperic.hq.product.PluginException;
import org.hyperic.sigar.NetConnection;
import org.hyperic.sigar.NetStat;
import org.hyperic.sigar.Sigar;
import org.hyperic.sigar.SigarException;
import org.hyperic.util.security.BogusTrustManager;

public abstract class NetServicesCollector extends Collector {

    private static final String PROP_NETSTAT = "netservices.netstat";

    private int port = -1;
    private int defaultPort, defaultSSLPort;
    private boolean isSSL, enableNetstat;
    private String sslProtcol;

    private InetSocketAddress sockaddr;

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

        byte[] address =
            getSocketAddress().getAddress().getAddress();
        int port = getSocketAddress().getPort();
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
        this.enableNetstat = 
            !"false".equals(getPlugin().getManagerProperty(PROP_NETSTAT));
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
                throw new PluginException("Missing " + PROP_PASSWORD);
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
        if (this.sockaddr == null) {
            String host = getHostname();
            int port = getPort();
            this.sockaddr =
                new InetSocketAddress(host, port);
            if (getSource() == null) {
                setSource(host + ":" + port);
            }
        }
        return this.sockaddr;
    }
    
    public String getHostAddress() {
        return getSocketAddress().getAddress().getHostAddress();
    }

    protected Socket createSSLSocket()
        throws IOException {

        SSLContext context;
        String proto = getSSLProtocol();

        try {
            context = SSLContext.getInstance(proto);
        } catch(NoSuchAlgorithmException e) {
            throw new IOException("Unable to get SSL context (" +
                                  proto + "): " +
                                  e.getMessage());
        }

        try {
            X509TrustManager[] managers =
                new X509TrustManager[] { new BogusTrustManager() };

            context.init(null, managers, null);
        } catch(KeyManagementException e) {
            throw new IOException("Unable to initialize trust manager: " +
                                   e.getMessage());
        }

        return context.getSocketFactory().createSocket();
    }

    protected void connect(Socket socket)
        throws IOException {

        try {
            socket.connect(getSocketAddress(), getTimeoutMillis());
            socket.setSoTimeout(getTimeoutMillis());
            setMessage("OK");
        } catch (IOException e) {
            setMessage("connect " + getSocketAddress(), e);
            throw e;
        }        
    }
    
    public Socket getSocket()
        throws IOException {
        
        Socket socket;
        boolean isSSL = isSSL();
        
        if (isSSL) {
            try {
                socket = createSSLSocket();
            } catch (IOException e) {
                setMessage("Failed to create SSL socket", e);
                throw e;
            }
        }
        else {
            socket = new Socket();
        }

        connect(socket);

        if (isSSL) {
            try {
                ((SSLSocket)socket).startHandshake();
            } catch (IOException e) {
                setMessage("SSL start handshake failed", e);
                throw e;
            }
        }

        return socket;
    }
    
    public SocketWrapper getSocketWrapper()
        throws IOException {

        return new SocketWrapper(getSocket());
    }
}
