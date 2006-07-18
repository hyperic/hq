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

package org.hyperic.hq.product.servlet.client;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.StringTokenizer;
import java.util.jar.Attributes;
import java.util.jar.Manifest;

import org.hyperic.hq.product.Metric;
import org.hyperic.hq.product.MetricInvalidException;
import org.hyperic.hq.product.MetricUnreachableException;
import org.hyperic.hq.product.PluginException;

import org.hyperic.util.StringUtil;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * Remote proxy for JMX. One instance per managed server. It will cache values
 * to avoid making too many requests.
 * 
 * It also include all the constants related with the connection to the remote
 * side. 
 */
public class JMXRemote
{
    private static HashMap cache = new HashMap();
    private static Log log = LogFactory.getLog(JMXRemote.class);

    private static final String PATH               = "/hyperic-hq/";

    public static final String PROP_JMX_URL        = "jmxUrl";
    public static final String PROP_JMX_USER       = "jmxUser";
    public static final String PROP_JMX_PASS       = "jmxPass";

    // Hardcoded tomcat connector type.  For these types we must use an
    // URL availability check rather than look up an MBean attribute
    public static final String PROP_CONNECTOR_TYPE = "GlobalRequestProcessor";
        
    // Params for templates for services    
    public static final String PROP_SERVLET        = "servlet-name";
    public static final String PROP_HOST           = "host";
    public static final String PROP_CONTEXT        = "context";

    // JMX Domain exposed on the other side (web servlet)
    public static final String DEFAULT_DOMAIN   = "hyperic-hq";

    // Virtual domain for availability
    public static final String AVAIL_DOMAIN = "servlet.avail";
    
    protected String  host;
    protected int     port;
    
    // Path to the hyperic-hq webapp. Can be overriden in jmxUrl 
    protected String appPath = PATH;
    
    // Original full url
    protected String  jmxUrl;
    protected String  protocol;
    private String password;
    private String user;

    protected JMXProtocolRequest requestor;

    public final static Integer ZERO =new Integer(0);
    public final static Integer ONE  =new Integer(1);

    long lastAccess;
    Manifest mf = null;

    // How long to cache the jmx-attributes Manifest
    private long cacheTimeout = 30000;

    private int status;
    
    public static JMXRemote getInstance(Properties props)
        throws PluginException {

        String jmxUrl = props.getProperty(JMXRemote.PROP_JMX_URL);
        
        JMXRemote remote = (JMXRemote)cache.get(jmxUrl);

        if (remote == null) {
            remote = new JMXRemote();
            remote.setJmxUrl(jmxUrl);
            remote.setUser(props.getProperty(JMXRemote.PROP_JMX_USER));
            remote.setPassword(props.getProperty(JMXRemote.PROP_JMX_PASS));
            remote.init();
            cache.put(jmxUrl, remote);
        }
        
        return remote;
    }
    
    // Getters and setters

    public String getHost() {
        return host;
    }

    public void setHost(String host) {
        this.host = host;
    }

    public int getPort() {
        return port;
    }

    public void setPort(int port) {
        this.port = port;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getUser() {
        return user;
    }

    public void setUser(String user) {
        this.user = user;
    }

    public String getJmxUrl() {
        return this.jmxUrl;
    }

    public void setCacheTimeout(long timeout) {
        this.cacheTimeout = timeout;
    }

    public long  getCacheTimeout() {
        return this.cacheTimeout;
    }

    /** 
     * Return the (URL) path to the /hyperic-hq/ web application.
     * Defaults to /hyperic-hq/ if the URL doesn't include a path
     */ 
    public String getJmxWebappPath() {
        return appPath;
    }
    
    public String getJmxAttributeServletPath() {
        return getJmxWebappPath() + "jmx-attributes";
    }

    /** 
     * Sets the URL for the remote JMX. Protocol can be a HTTP or AJP.
     * We require the full URL - with port. Path defaults to 
     * covalent-eam/jmx-attributes (for backward compat).
     * 
     * @param jmxUrl
     */ 
    public void setJmxUrl(String jmxUrl) {
        this.jmxUrl = jmxUrl;
    }
    
    Properties jmxProperties;
    
    public String getProtocol() {
        return protocol;
    }

    public void setProtocol(String protocol) {
        this.protocol = protocol;
    }

    public void init() throws PluginException {
        
        if (jmxUrl == null || jmxUrl.equals("%jmxUrl%")) {
            throw new PluginException("No JMX URL configured");
        }

        StringTokenizer st = new StringTokenizer(jmxUrl, ":/");

        // Protocol is required
        if (st.hasMoreTokens()) {
            protocol = st.nextToken();
        } else {
            throw new PluginException("No protocol specified " +
                                      "in JMX URL: " + jmxUrl);
        }

        // Host is required
        if (st.hasMoreTokens()) {
            host = st.nextToken();
        } else {
            throw new PluginException("No host specified in " +
                                      "JMX URL: " + jmxUrl);
        }
        
        // Port is optional, default to 80.  (Could make this 7080)
        if (st.hasMoreTokens()) {
            String portString = st.nextToken();
            try {
                port = new Integer(portString).intValue();
            } catch (NumberFormatException e) {
                throw new PluginException("Invalid JMX URL " +
                                          "port: " + portString);
            }
        } else {
            port = 80;
        }

        // App path is also optional (defaults to /hyperic-hq/)
        if (st.hasMoreTokens()) {
            appPath = "/" + st.nextToken() + "/";
        }

        if (log.isDebugEnabled()) {
            this.log.debug("JMXRemote.init() " + 
                           protocol + "://" + host + ":" + 
                           port + "/" + appPath);
        }

        if (protocol.equalsIgnoreCase("ajp") ||
            protocol.equalsIgnoreCase("jk")) {
            requestor = new JMXProtocolAjp();
        } else if (protocol.equalsIgnoreCase("http")) {
            requestor = new JMXProtocolHttp();
        } else if (protocol.equalsIgnoreCase("https")) {
            requestor = new JMXProtocolHttps();
        } else {
            // Could check this param as we parse out the protocol
            throw new PluginException("Unknown protocol: " +
                                             protocol);
        }
    }
    
    public void shutdown() throws PluginException {
        requestor.shutdown();
    }

    /**
     * Get availability of a connector
     */
    public boolean getURLAvailability(Metric jdsn)
    {    
        Properties props = jdsn.getObjectProperties();
        // XXX: we should pull these from the TomcatMeasurementPlugin
        String scheme = props.getProperty("connector");
        String path = props.getProperty("connectorPath");
        if (path == null) {
            path = "/"; // Default to /
        }
        int port;
        try {
            port = Integer.parseInt(props.getProperty("connectorPort"));
        } catch (NumberFormatException e) {
            log.debug("Invalid connector port: " + 
                      props.getProperty("connectorPort"));
            return false;
        }
                                    
        log.debug("Avail check: " + scheme + host + port);

        JMXProtocolRequest request;

        if (scheme.equalsIgnoreCase("http"))
            request = new JMXProtocolHttp();
        else if (scheme.equalsIgnoreCase("https")) {
            request = new JMXProtocolHttps();
        } else if (scheme.equalsIgnoreCase("ajp") ||
                   scheme.equalsIgnoreCase("jk"))
            request = new JMXProtocolAjp();
        else {
            this.log.info("Unknown protocol: " + scheme);
            return false;
        }

        try {
            InputStream reader = request.openStream(host, port,
                                                    user, password,
                                                    path, null);
            reader.close();
            return true;
        } catch (ServerError se) {
            if (se.isSuccess() || se.isRedirect()) {
                return true;
            }
            log.debug("Error checking availability " + se.toString());
        } catch (javax.net.ssl.SSLHandshakeException e) {
            //XXX: This happens in SSL mode, for now assume available
            return true;
        } catch (IOException e) {
            // When we are checking the connector availability, it does
            // not matter if we get an IO error (Such as a 500 error)  In
            // that case, the connector is working.  If the connector
            // is truely down, we will fail with a MetricUnreachableException
            // long before we get here.
            return true;
        } catch (Exception e) {                                      
            log.debug("Error checking availability " + e.toString());
        } finally {
            request.shutdown();
        }

        return false;
    }            

    /**
     * Get availability of a servlet or webapp
     */
    public boolean getAvailability(String path) {
        try {
            InputStream reader=requestor.openStream(host, port, 
                                                    user, password,
                                                    path, null);
            reader.close();
            log.debug("Available: " + host + ":" + port + path + " " + true);
            return true;
        } catch (ServerError se) {
            if (se.isSuccess() || se.isRedirect()) {
                return true;
            }
            log.debug("Error checking availability " + se.toString());
        } catch (Exception e) {
            log.debug("Error checking availability " + e.toString());
        }
        return false;
    }
    
    /** 
     * Like a remote value, but any error will result in returning ZERO.
     */ 
    public Object getAvailability(Metric jdsn)
         throws MetricInvalidException 
    {
        // If we are doing availability on the connector, we must use a
        // URL rather than an mbean lookup
        if (jdsn.getObjectProperties().getProperty("type", "").
            equals(PROP_CONNECTOR_TYPE)) {
            if (getURLAvailability(jdsn))
                return ONE;
            else
                return ZERO;
        }

        try {
            return getValue(jdsn.getObjectName(), jdsn.getAttributeName());
        } catch( Throwable t ) {
            // if we can't even get the availability - it's not available
            return ZERO;
        }
    }

    public Object getRemoteMBeanValue(Metric jdsn)
        throws MetricInvalidException, MetricUnreachableException
    {
        if (jdsn.getAttributeName().toLowerCase().startsWith("availab")) {
            return getAvailability(jdsn);
        }
        
        try {
            if( log.isDebugEnabled())
                log.debug("Getting mbean " + host + " " + appPath + " " + 
                          jdsn.getObjectName() + ":" + jdsn.getAttributeName());

            return getValue(jdsn.getObjectName(), jdsn.getAttributeName());

            // This should be DSNValueNotFound
        } catch( MetricInvalidException ex ) {  // XXX: Should be DSNValueNotFound
            // PR 5571. If the service is configured, we should expect it
            // to be there.
            if (jdsn.getObjectName().indexOf( "type=Servlet") >= 0)  {
                // this is a servlet - maybe it wasn't called, we don't register
                // the mbean until the servlet is executed at least once.
                if( ex.getRemoteMessage() != null && 
                    ex.getRemoteMessage().
                    indexOf("InstanceNotFoundException") >= 0) {
                    return ZERO;                     
                }
            }
            
            // XXX: HAAAACK
            // Work around tomcat bug where it lists the https
            // GlobalRequestProcessors as http.
            
            String objectName = jdsn.getObjectName();
            if ((objectName.indexOf("name=https") != -1) &&
                !jdsn.getAttributeName().toLowerCase().
                startsWith("availab")) {

                // Rewrite to http
                String newObjectName = StringUtil.replace(objectName,
                                                          "https",
                                                          "http");
                String err = "Rewriting objectName to " + newObjectName;
                log.debug(err);
                jdsn.setObjectName(newObjectName);
                return new Double(Double.NaN); //XXX better luck next time?
            }

            throw ex;
        } catch (MetricUnreachableException ex) {
            throw ex;
        } catch( Exception ex ) {
            throw new MetricInvalidException(ex);
        }
    }
    
    public Object invoke( String oname, String operation ) 
        throws Exception
    {
        StringBuffer qry = new StringBuffer();
        qry.append("invoke:").append( oname );
        qry.append(":" + operation);

        Object obj=ZERO;
        obj = this.getRemoteMBeanValue(qry.toString());
        return obj;
    }
    
    /** 
     * Perform JMX query.
     * 
     * XXX: clearly we could optimize and secure this protocol
     *      proof-of-concept will do for the moment
     * 
     * @deprecated This will get one value at a time, inefficient 
     */
    public Object getRemoteMBeanValue(String qry)
        throws Exception
    {
        if( log.isDebugEnabled() )
            log.debug("Connecting to " + host + " " + port + " " + 
                    appPath + " " + qry);
        InputStream in =requestor.openStream(host, port, 
                user, password, getJmxAttributeServletPath(), qry);
        return requestor.parseResponse(in, qry);
    }

    public InputStream openUrl(String path)
        throws Exception
    {
        if(log.isDebugEnabled())
            log.debug("Connecting to " + host + " " + port + " " + 
                      path);
        InputStream in = requestor.openStream(host, port, user, 
                                              password, path, null);
        return in;
    }

    /** 
     * Return the names of the remote mbeans
     */ 
    public Set getRemoteMbeans() 
        throws MetricInvalidException, MetricUnreachableException
    {
        Manifest mf = getRemoteInfo();

        if (mf == null) { 
            throw new MetricInvalidException("Can't access remote server");
        }
        return mf.getEntries().keySet();
    }
    
    public String getValue( String oname, String att )
        throws MetricInvalidException, MetricUnreachableException
    {
        Manifest mf = getRemoteInfo();

        if(mf == null) { 
            throw new MetricInvalidException("Can't access remote server");
        }

        // More backward compatibility for old covalent webapp.
        if (getJmxWebappPath().indexOf("covalent-cam") != -1) {
            oname = StringUtil.replace(oname, "hyperic-hq", "covalent-cam");
        }

        // Get the cached value
        Attributes atts = mf.getAttributes(oname);
        if(atts == null) {
            // No object found
            MetricInvalidException ex = 
                new MetricInvalidException("Instance not found " + oname );
            ex.setRemoteMessage("InstanceNotFoundException");
            throw ex;
        }
        
        return atts.getValue(att);
    }

    /** 
     * Get data for a remote host 
     */ 
    public Manifest getRemoteInfo() 
        throws MetricInvalidException, MetricUnreachableException
    {
        String id = host + port;
        long current = System.currentTimeMillis();
        
        if(mf == null || current - lastAccess > cacheTimeout) {
            refresh();
            lastAccess=current;
        }

        return mf;
    }

    public int getStatus() {
        return this.status;
    }
    
    /** 
     * Refresh the view of the remote system 
     */ 
    public void refresh() 
        throws MetricInvalidException, MetricUnreachableException 
    {
        // Update
        InputStream is = null;
        try {
            is =requestor.openStream(host, port, user, password, 
                                     getJmxAttributeServletPath(), 
                                     "qry=*");
            this.status = HttpURLConnection.HTTP_OK;
        } catch (java.net.ConnectException e) {
            mf = null;
            String err = "Error connecting with monitored server, make " +
                "sure it is started and check http://" + host + ":" +
                port + "/" + "?* Message=" + e.getMessage();
            this.status = HttpURLConnection.HTTP_BAD_GATEWAY;
            throw new MetricUnreachableException(err, e);
        } catch (FileNotFoundException e) {
            mf = null;
            String err =
                getJmxAttributeServletPath() + " Not Found at " +
                "http://" + host + ":" + port;
            this.status = HttpURLConnection.HTTP_NOT_FOUND;
            throw new MetricUnreachableException(err, e);
        } catch (Exception e) {
            mf = null;
            String err = "Error connecting with monitored server, make " +
                "sure it is started and check http://" + host + ":" +
                port + "/" + "?* Message=" + e.getMessage();

            throw new MetricInvalidException(err, e);
        }
        
        try {
            long start = System.currentTimeMillis();
            mf = new Manifest(is);
            long duration = System.currentTimeMillis() - start;
            this.log.debug("Manifest parse took " + duration + "ms");
        } catch(IOException ex) {
            throw new MetricInvalidException("Incorrect response from " +
                                          "monitored server", ex );
        }
    }

    public static void main(String[] args) throws Exception {
        if (args.length > 0 &&
            args[0].startsWith("file:")) {
            // Do a test parse of the manifest file (e.g. file:/path/to/jmx.txt)
            System.out.println("Parsing " + args[0]);
            Manifest mf = new Manifest(new URL(args[0]).openStream());
            System.out.println("Done.");
        } else {
            // http://localhost
            JMXRemote remote = new JMXRemote();
            remote.setJmxUrl(args[0]);
            remote.init();
            
            Map beans = remote.getRemoteInfo().getEntries();
            
            for (Iterator it=beans.entrySet().iterator(); it.hasNext();) {
                Map.Entry entry = (Map.Entry)it.next();
                String name = (String)entry.getKey();
                Attributes attrs = (Attributes)entry.getValue();
                System.out.println(name + "=" + attrs.entrySet());
            }
        }
    }
}
