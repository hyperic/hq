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

package org.hyperic.hq.plugin.weblogic.jmx;

import java.io.File;
import java.util.Collections;
import java.util.Comparator;
import java.util.Hashtable;
import java.util.List;
import java.util.Properties;
import java.util.StringTokenizer;

import javax.management.MBeanServer;
import javax.management.ObjectName;
import javax.management.MalformedObjectNameException;

import org.hyperic.hq.product.TypeBuilder;

import org.hyperic.hq.plugin.weblogic.WeblogicLogFileTrackPlugin;
import org.hyperic.hq.plugin.weblogic.WeblogicMetric;
import org.hyperic.hq.plugin.weblogic.WeblogicProductPlugin;

public class ServerQuery
    extends BaseServerQuery
    implements Comparator {

    private WeblogicDiscover discover;
    private String url;
    private boolean isAdmin = false;
    private boolean isRunning = true; //innocent until proven guilty
    private String jvmType = "JVMRuntime";
    private File cwd;

    public static final String MBEAN_TYPE = "Server";

    public static final String PROTOCOL_T3  = "t3";
    public static final String PROTOCOL_T3S = "t3s";

    public static final String ATTR_ADMIN_PORT =
        "AdministrationPort";

    public static final String ATTR_ADMIN_OVERRIDE_PORT =
        "AdministrationPortAfterOverride";

    public static final String ATTR_ADMIN_PORT_ENABLED =
        "AdministrationPortEnabled";

    public static final String ATTR_JVM_RUNTIME =
        "JVMRuntime";

    //seems MBeanServer.getAttributes stops getting attributes
    //without throwing an exception when it hits an attribute
    //that does not exist.  so the order is important here
    //to support 6.1
    private static final String[] SERVER_ATTRS = {
        //6.1+
        ATTR_LISTEN_ADDR, ATTR_LISTEN_PORT,
        "DefaultProtocol",
        ATTR_NOTES,
        //7.1+
        "ServerVersion",
        ATTR_ADMIN_PORT,
        ATTR_ADMIN_PORT_ENABLED,
        //ATTR_ADMIN_OVERRIDE_PORT, XXX gone in 9.1
    };

    private static final String[] CPROP_ATTRS = {
        "ServerVersion", "JavaVersion", "JavaVendor"
    };

    private static final String[] JVM_RUNTIME_ATTRS = {
        "JavaVersion", "JavaVendor"
    };

    //ListenAddress here is more reliable
    //in the standalone server it is null in the ServerMBean (above)
    private static final String[] SERVER_RUNTIME_ATTRS = {
        //6.1+
        "WeblogicVersion",
        ATTR_LISTEN_ADDR, "CurrentDirectory",
        ATTR_JVM_RUNTIME,
        //7.1+
        ATTR_SSL_LISTEN_PORT, "AdminServer"
    };

    private static final String[] LOG_ATTRS = {
        "FileName"  
    };

    public WeblogicDiscover getDiscover() {
        return this.discover;
    }

    public void setDiscover(WeblogicDiscover discover) {
        this.discover = discover;
    }

    public WeblogicQuery cloneInstance() {
        ServerQuery query = (ServerQuery)super.cloneInstance();
        query.discover = this.discover;
        return query;
    }

    private boolean isAdminPortEnabled() {
        return "true".equals(getAttribute(ATTR_ADMIN_PORT_ENABLED));
    }

    public String getListenPort() {
        if (isAdminPortEnabled()) {
            String oPort = getAttribute(ATTR_ADMIN_OVERRIDE_PORT);
            if ("0".equals(oPort) || (oPort == null)) {
                //no override port configured.
                return getAttribute(ATTR_ADMIN_PORT);
            }
            return oPort;
        }
        if (isAdminSSL()) {
            return getSSLListenPort();
        }
        return super.getListenPort();
    }

    public String getProtocol() {
        return isAdminSSL() ?
            PROTOCOL_T3S : getAttribute("DefaultProtocol");
    }

    public boolean isAdminSSL() {
        return this.discover.isAdminSSL();
    }

    private void configureUrl() {
        String address = getListenAddress();

        int idx = address.indexOf("/");
        if (idx != -1) {
            address = address.substring(idx+1);
            this.attrs.put(ATTR_LISTEN_ADDR, address);
        }

        this.url =
            getProtocol() + "://" + address + ":" +
            getListenPort();
    }

    public String getUrl() {
        return this.url;
    }

    public boolean isValidVersion(String version) {
        if (version == null) {
            return false;
        }

        //e.g. "8.1.0.0"
        //ServerVersion attribute may return "unknown"
        //if server was created but not yet started.
        for (int i=0; i<version.length(); i++) {
            char c = version.charAt(i);
            if (!(Character.isDigit(c) || (c == '.'))) {
                return false;
            }
        }
        
        return true;
    }

    private String getWeblogicVersion() {
        String version = getAttribute("WeblogicVersion");
        if (version == null) {
            return null;
        }
        StringTokenizer tok = new StringTokenizer(version);

        while (tok.hasMoreTokens()) {
            String s = tok.nextToken();
            
            if (isValidVersion(s)) {
                return s;
            }
        }

        return null;
    }

    private ObjectName getServerRuntime() {
        Hashtable attributes = new Hashtable();

        attributes.put("Type", "ServerRuntime");
        attributes.put("Name", getName());
        attributes.put("Location", getName());

        try {
            return new ObjectName(this.discover.getDomain(),
                                  attributes);
        } catch (MalformedObjectNameException e) {
            //wont happen.
            throw new IllegalArgumentException(e.getMessage());
        }
    }

    private ObjectName getJVMRuntime() {
        String jvm = getAttribute(ATTR_JVM_RUNTIME);
        if (jvm == null) {
            return null;
        }
        try {
            return new ObjectName(jvm);
        } catch (MalformedObjectNameException e) {
            //wont happen.
            throw new IllegalArgumentException(e.getMessage());
        }
    }

    private ObjectName getSSL() {
        Hashtable attributes = new Hashtable();

        attributes.put("Type", "SSL");
        attributes.put("Name", getName());
        attributes.put("Server", getName());

        try {
            return new ObjectName(this.discover.getDomain(), 
                                  attributes);
        } catch (MalformedObjectNameException e) {
            //wont happen.
            throw new IllegalArgumentException(e.getMessage());
        }
    }

    private void getSSLAttrs(MBeanServer mServer) {
        try {
            //SSLListenPort attribute only exists in ServerRuntimeMBean
            //so we have to get it for the nodes via the SSLMBean
            Object port = mServer.getAttribute(getSSL(),
                                               ATTR_LISTEN_PORT);
            this.attrs.put(ATTR_SSL_LISTEN_PORT, port.toString());
        } catch (Exception e) {
            //unlikely/ok
        }
    }

    private ObjectName getLogMBean() {
        Hashtable attributes = new Hashtable();
        attributes.put("Name", getName());
        attributes.put("Location", getName());
        attributes.put("ServerConfig", getName());
        attributes.put("Type", "LogConfig");

        try {
            return new ObjectName(this.discover.getDomain(), 
                                  attributes);
        } catch (MalformedObjectNameException e) {
            //wont happen.
            throw new IllegalArgumentException(e.getMessage());
        }
    }
    
    public boolean getAttributes(MBeanServer mServer,
                                 ObjectName name) {

        setName(name.getKeyProperty("Name"));

        if (!super.getAttributes(mServer, name, SERVER_ATTRS)) {
            return false;
        }

        if (isAdminPortEnabled()) {
            //gone in 9.1+
            super.getAttributes(mServer, name,
                                new String[] { ATTR_ADMIN_OVERRIDE_PORT });
        }

        ObjectName runtimeName = getServerRuntime();
        ObjectName logName = getLogMBean();

        boolean isAdminName =
            getName().equals(this.discover.getAdminName());

        if (isAdminName) {
            //this is the admin server instance
            super.getAttributes(mServer,
                                runtimeName,
                                SERVER_RUNTIME_ATTRS);
            super.getAttributes(mServer,
                                getJVMRuntime(),
                                JVM_RUNTIME_ATTRS);
            super.getAttributes(mServer,
                                logName,
                                LOG_ATTRS);
            if (getSSLListenPort() == null) {
                getSSLAttrs(mServer);
            }
            configureUrl();
        }
        else {
            getSSLAttrs(mServer);

            //this is a node server
            configureUrl();

            try {
                MBeanServer nodeServer = 
                    this.discover.getMBeanServer(this.url);
                this.isRunning =
                    super.getAttributes(nodeServer,
                                        runtimeName,
                                        SERVER_RUNTIME_ATTRS);
                if (this.isRunning) {
                    if (getJVMRuntime() == null) {
                        this.isRunning = false;
                    }
                    else {
                        super.getAttributes(nodeServer,
                                            getJVMRuntime(),
                                            JVM_RUNTIME_ATTRS);
                    }
                    super.getAttributes(nodeServer,
                                        logName,
                                        LOG_ATTRS);
                }
                configureUrl(); //attributes may differ now (e.g. ListenAddress)
            } catch (Exception e) {
                //ok; server is not running.
                this.isRunning = false;
            }
        }

        String serverVersion = getAttribute("ServerVersion");

        if (isValidVersion(serverVersion)) {
            this.version = serverVersion.substring(0, 3);
        }
        else if ((serverVersion == null) ||
                 serverVersion.equals("unknown"))
        { 
            //6.1 does not have a ServerVersion attribute.
            //9.1 might be == "unknown"
            this.version = getWeblogicVersion();
        }

        if (!this.isRunning) {
            return true;
        }

        /*
        //6.1 does not have the AdminServer attribute.
        //PeopleSoft may have AdminServer = true for nodes.
        String adminServer = getAttribute("AdminServer");
        if (adminServer != null) {
            this.isAdmin = "true".equals(adminServer);
        }
        */
        this.isAdmin = isAdminName;

        //XXX if node is started by the nodemanager
        //this value is different than when started by hand
        File path = new File(getAttribute("CurrentDirectory"));
        if (path.getName().equals(".")) {
            path = path.getParentFile();
        }
        this.cwd = path;

        return true;
    }

    public String getMBeanAlias() {
        return "Location";
    }

    public String getMBeanType() {
        return MBEAN_TYPE;
    }

    public String getQualifiedName() {
        return this.discover.getDomain() + " " + getName();
    }

    public String getResourceType() {
        String type;

        if (this.isAdmin) {
            type = WeblogicProductPlugin.ADMIN_NAME;
        }
        else {
            type = WeblogicProductPlugin.SERVER_NAME;
        }
        String version = getDiscover().getVersion();
        return TypeBuilder.composeServerTypeName(type, version);
    }

    public String getIdentifier() {
        //domain + serverName is unique in weblogic
        return getResourceFullName();
    }

    public String getInstallPath() {
        return new File(this.cwd, getName()).toString();
    }

    public File getCwd() {
        return this.cwd;
    }

    public void setCwd(File value) {
        this.cwd = value;
    }

    public boolean isAdmin() {
        return this.isAdmin;
    }

    public boolean isRunning() {
        return this.isRunning;
    }

    protected String getControlProgram() {
        String ctl;

        if (!this.isRunning) {
            return "";
        }

        if (this.isAdmin) {
            ctl = new File(this.cwd, "startWebLogic.sh").toString();
        }
        else {
            if (this.cwd.getName().equals("nodemanager")) {
                ctl = "";
            }
            else {
                ctl = new File(this.cwd, "startManagedWebLogic.sh").toString();
            }
        }

        return ctl;
    }

    public void configureAdminProps(Properties props) {
        props.setProperty(WeblogicMetric.PROP_ADMIN_URL,
                          this.discover.getAdminURL());
        props.setProperty(WeblogicMetric.PROP_ADMIN_USERNAME,
                          this.discover.getUsername());
        props.setProperty(WeblogicMetric.PROP_ADMIN_PASSWORD,
                          this.discover.getPassword());
    }

    public void configure(Properties props) {
        configureAdminProps(props);

        if (!this.isAdmin) {
            props.setProperty(WeblogicMetric.PROP_SERVER_URL,
                              this.url);
        }

        props.setProperty(WeblogicMetric.PROP_DOMAIN,
                          this.discover.getDomain());
        props.setProperty(WeblogicMetric.PROP_SERVER,
                          getName());

        String jvmType;
        if (getJVMRuntime() != null) {
            jvmType = getJVMRuntime().getKeyProperty("Type");
        }
        else {
            jvmType = this.jvmType; //server is not running
        }
        props.setProperty(WeblogicMetric.PROP_JVM, jvmType);

        String log = getAttribute("FileName");
        if (log != null) {
            String cur = "." + File.separator;
            if (log.startsWith(cur)) {
                log = log.substring(2);
            }
            log = this.cwd + File.separator + log;
            props.setProperty(WeblogicLogFileTrackPlugin.PROP_FILES_SERVER,
                               log);
        }
    }

    public String[] getCustomPropertiesNames() {
        return CPROP_ATTRS;
    }

    private int getOrder(ServerQuery s) {
        if (s.isAdmin()) {
            return 1;
        }
        if (s.isRunning()) {
            return 2;
        }
        return 3;
    }

    public int compare(Object s1, Object s2) {

        return getOrder((ServerQuery)s1) - 
               getOrder((ServerQuery)s2);
    }

    public void sort(List servers) {
        Collections.sort(servers, this);
    }
}
