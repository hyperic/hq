/*
 * NOTE: This copyright does *not* cover user programs that use HQ
 * program services by normal system calls through the application
 * program interfaces provided as part of the Hyperic Plug-in Development
 * Kit or the Hyperic Client Development Kit - this is merely considered
 * normal use of the program, and does *not* fall under the heading of
 * "derived work".
 * 
 * Copyright (C) [2004-2010], Hyperic, Inc.
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

package org.hyperic.hq.product;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.util.StringTokenizer;
import java.util.Properties;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hyperic.hq.measurement.MeasurementConstants;
import org.hyperic.util.config.ConfigResponse;
import org.hyperic.util.config.ConfigSchema;
import org.hyperic.util.StringUtil;

/**
 * this class parses Metrics in the form of:
 * jmx-domain:jmx-properties:jmx-attribute:metric-properties
 * For example:
 * jboss.system:type=ServerInfo,lang=eng:FreeMemory:naming=jnp://localhost:1099,password=xxx
 * where:
 * jmx-domain     = jboss.system
 * jmx-properties = type=ServerInfo,lang=eng
 * jmx-attribute  = FreeMemory
 * metric-properties = naming=jnp://localhost:1099,password=xxx
 */

public class Metric {
    
    private static final Log log = LogFactory.getLog(Metric.class);

    public static final String ATTR_AVAIL = "Availability";
    
    public static final double AVAIL_UNKNOWN = MeasurementConstants.AVAIL_UNKNOWN;
    public static final double AVAIL_UP      = MeasurementConstants.AVAIL_UP;
    public static final double AVAIL_DOWN    = MeasurementConstants.AVAIL_DOWN;
    public static final double AVAIL_WARN    = MeasurementConstants.AVAIL_WARN;
    public static final double AVAIL_PAUSED  = MeasurementConstants.AVAIL_PAUSED;
    public static final double AVAIL_POWERED_OFF = MeasurementConstants.AVAIL_POWERED_OFF;

    private static HashMap cache = new HashMap();
   
    private static final MetricProperties NO_PROPERTIES =
        new MetricProperties();
    
    private final Object lock = new Object();

    private String template   = null;
    private String domainName = null;
    private String objectName = null;
    private String objectPropString = null;
    private MetricProperties objectProperties = null;
    private String attributeName = null;

    private String propString = null;
    private MetricProperties props = null;
    private String id = null; //for tie-in to logging
    private String category = null;
    private long interval;

    private Metric() {
        synchronized (lock) {
            interval = -1;
        }
    }

    //we only need to encode these three
    private static String getEncoding(char c) {
        //poor-mans java.net.URLEncoder
        switch (c) {
          case ':':
            return "%3A";
          case '=':
            return "%3D";
          case ',':
            return "%2C";
          default:
            return null;
        }
    }

    private static char getDecoding(String s, int i) {
        char c1 = s.charAt(i+1);
        char c2 = s.charAt(i+2);

        if ((c1 == '2') && (c2 == 'C')) {
            return ',';
        }
        else if (c1 == '3') {
            if (c2 == 'A') {
                return ':';
            }
            else if (c2 == 'D') {
                return '=';
            }
        }

        return '0';
    }

    /**
     * HHQ-3246: Some characters need to be escaped with a double backlash 
     * to preserve their value during the decoding process.
     * 
     * For example, the equals sign = will normally be encoded
     * to %3D and decoded back to =
     * 
     * However, if %3D is the desired string, it needs
     * to be escaped with the double backslash \\%3D so that %3D
     * is the outputted string during the decoding process.
     */
    private static String getUnescapedDecoding(String s, int i) {
        String escapeIndicator = "\\\\";
        String[] specialVals = new String[] {"%3D", "%3A", "%2C"};
        String unescapedDecoding = null;
        
        try {
            String encodedString = s.substring(i, i+5);           
            
            for (int j=0; j<specialVals.length; j++) {
                String escapedString = escapeIndicator + specialVals[j];
                if (escapedString.equals(encodedString)) {
                    unescapedDecoding = specialVals[j];
                    break;
                }
            }
        } catch (IndexOutOfBoundsException iob) {
            //
        }

        return unescapedDecoding;
    }

   

    //we only encode/decode property values
    //which are input by a user or auto inventory
    public static String encode(String val) {
        StringBuffer buf = new StringBuffer(val.length());
        boolean changed = false;

        for (int i=0; i<val.length(); i++) {
            char c = val.charAt(i);

            String enc = getEncoding(c);
            if (enc == null) {
                buf.append(c);
            }
            else {
                buf.append(enc);
                changed = true;
            }
        }

        return changed ? buf.toString() : val;
    }

    //java.net.URLDecoder is not forgiving enough.
    public static String decode(String val) {
        StringBuffer buf = new StringBuffer(val.length());
        boolean changed = false;
        int len = val.length();

        for (int i=0; i<len; i++) {
            char c = val.charAt(i);
            
            if ((c == '\\') && ((i+4) < len)) {
                String unesc = getUnescapedDecoding(val, i);
                
                if (unesc == null) {
                    buf.append(c);
                } else {
                    i += 4;
                    buf.append(unesc);
                    changed = true;
                }
            } else {
                if ((c == '%') && ((i+2) < len)) {
                    char dc = getDecoding(val, i);

                    if (dc != '0') {
                        i += 2;
                        c = dc;
                        changed = true;
                    }
                }
                
                buf.append(c);
            }
        }

        return changed ? buf.toString() : val;
    }

    /**
     * The domain name - corresponding the the ObjectName domain.
     */ 
    public String getDomainName() {
        return this.domainName;
    }
    
    public void setDomainName(String domain) {
        this.domainName = domain;
    }

    /** The full JMX object name - domain : objectName 
     */ 
    public String getObjectName() {
	// do not return the decoded version of the object name
	// at this point, since sigar will decode it successively
	// causing things to break
        return this.objectName;
    }

    /** Set the JMX object name.
     */
    public void setObjectName(String objectName) {
        this.objectName = objectName;
        // Clear out the object properties to force them to be
        // recreated on subsequent calls to getObjectProperties()
        this.objectProperties = null;
    }

    /** The attribute name
     */ 
    public String getAttributeName() {
        return this.attributeName;
    }

    public String toString() {
        return this.template;
    }
    
  

 

    static String mask(String val) {
        if (val == null) {
            return "";
        }
        StringBuffer sb = new StringBuffer();
        for (int i=0; i<val.length(); i++) {
            sb.append('*');
        }
        return sb.toString();
    }

    private String toDebugString(String orig, Properties props) {
        if (props == null && orig == null) {
            return orig;    
        }
        StringBuffer ds = new StringBuffer();
        StringTokenizer tok = new StringTokenizer(orig, ",");
        while (tok.hasMoreTokens()) {
            String pair = tok.nextToken();
            int ix = pair.indexOf('=');
            if (ix == -1) {
                ds.append(pair);
                continue;
            }
            String key = pair.substring(0, ix);
            String origVal = pair.substring(ix-1, pair.length());
            if (ConfigSchema.isSecret(key)) {
                String val = (props == null) ? origVal : props.getProperty(key);
                ds.append(key).append('=');
                ds.append(mask(val));
            }
            else {
                ds.append(pair);
            }
            if (tok.hasMoreTokens()) {
                ds.append(',');
            }
        }
        return ds.toString();
    }

    public String toDebugString() {
        StringBuffer dm = new StringBuffer();
        dm.append(this.domainName).append(':');
        dm.append(toDebugString(this.objectPropString, this.objectProperties));
        dm.append(':').append(this.attributeName);
        if (this.propString != null) {
            dm.append(':');
            dm.append(toDebugString(this.propString, this.props));
        }
        return dm.toString();
    }

    public Properties getProperties() {
        if (this.props == null) {
            if (this.propString == null) {
                this.props = NO_PROPERTIES;
            }
            else {
                this.props = parseProperties(this.propString);
            }
        }
        return this.props;
    }

    public void setPropString(String propString) {
        this.propString = propString;
    }

    public String getPropString() {
        return this.propString;
    }

    public String getObjectProperty(String property)
    {
        return getObjectProperties().getProperty(property);
    }
    
    /** Properties in the local part of the ObjectName.
     */ 
    public Properties getObjectProperties() {
        if (this.objectProperties == null) {
            this.objectProperties = parseProperties(this.objectPropString);
            this.objectProperties.setDefaults(this.props);
        }

        return this.objectProperties;
    }

    /** The local part of the ObjectName.
     */ 
    public String getObjectPropString() {
        return this.objectPropString;
    }

    private static MetricProperties parseProperties(String config) {
        MetricProperties props;

        //common for templates to have the same properties
        synchronized (cache) {
            props = (MetricProperties)cache.get(config);
            if (props != null) {
                return props;
            }
            props = new MetricProperties();
            cache.put(config, props);
        }

        //e.g. PluginLinter parses but does not replace properties
        //such as %process.query%
        if (config.indexOf(",") == -1) {
            if (config.startsWith("%") && config.endsWith("%")) {
                return props;
            }
        }
        StringTokenizer st = new StringTokenizer(config, ",");

        while (st.hasMoreTokens()) {
            String attr = st.nextToken(); 

            if (attr.equals("*")) {
                //e.g. used in MBeanServer.queryMBeans
                continue;
            }

            int ix = attr.indexOf('=');

            if (ix == -1) {
                continue;
            }

            String key = attr.substring(0, ix);
            String val = attr.substring(key.length()+1);

            if (val.length() == 0) {
                continue;
            }
            
            ix = val.length()-1;
            
            if ((val.charAt(0) == '%') &&
                (val.charAt(ix) == '%') &&
                val.substring(1, ix).equals(key))
            {
                //value was not replaced
                continue;
            }

            props.setProperty(key, decode(val));
        }

        return props;
    }

    public String getId() {
        return this.id;
    }

    public void setId(int type, int id) {
        this.id = type + ":" + id;
    }

    public String getCategory() {
        return this.category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public long getInterval() {
        synchronized (lock) {
            return this.interval;            
        }
    }

    public void setInterval(long interval) {
        synchronized (lock) {
            this.interval = interval;            
        }
    }

    public boolean isAvail() {
        return MeasurementConstants.CAT_AVAILABILITY.equals(getCategory()) ||
               getAttributeName().equals(ATTR_AVAIL);
    }

    /**
     * @param template The metric string to be parsed.
     * @return A Metric that can be used by the plugins.
     * @exception MetricInvalidException If the metric string is malformed.
     */
    public static Metric parse(String template)
        throws MetricInvalidException {

        if ((template == null) || 
            (template.length() == 0)) {
            throw new MetricInvalidException();
        }

        Metric metric;

        synchronized (cache) {
            metric = (Metric)cache.get(template);
        }

        if (metric != null) {
            return metric;
        }

        metric = new Metric();
        metric.template = template;
        
        //e.g. jboss.system:type=ServerInfo:FreeMemory
        StringTokenizer st = new StringTokenizer(template, ":");

        try {
            metric.domainName = st.nextToken(); //e.g. jboss.system
            metric.objectPropString = st.nextToken(); //e.g. type=ServerInfo

            //XXX workaround for hqagent "camAgent:availability" templates
            if (!st.hasMoreTokens()) {
                if (template.endsWith(":")) {
                    metric.attributeName = ""; //e.g. optional snmp %oid%
                }
                else {
                    metric.attributeName = metric.objectPropString;
                    metric.objectPropString = "DummyKey=DummyVal";
                    metric.objectName =
                        metric.domainName + ":" + metric.objectPropString;
                }
            }
            else {
                metric.objectName =
                    template.substring(0,
                                       metric.domainName.length() +
                                       1 +
                                       metric.objectPropString.length());
                metric.attributeName = st.nextToken(); //e.g. FreeMemory
            }

            if (st.hasMoreTokens()) {
                //parse the metric properties
                int offset = metric.objectName.length() +
                             metric.attributeName.length() + 2;

                //e.g. admin.url=t3://localhost:7001,admin.username=system
                metric.propString = template.substring(offset);
            }
        } catch (Exception e) {
            throw new MetricInvalidException(template,e );
        }

        synchronized (cache) {
            cache.put(template, metric);
        }

        metric.attributeName = decode(metric.attributeName);
        return metric;
    }

    private static String replace(String template,
                                  String key, String val) {
        if (val == null) {
            return template;
        }

        return StringUtil.replace(template,
                                  "%" + key + "%",
                                   encode(val));
    }
    
    public static String translate(String template,
                                   ConfigResponse config) {

        Iterator iter = config.getKeys().iterator();

        while (iter.hasNext()) {
            String key = (String)iter.next();
            String val = config.getValue(key);
            template = replace(template, key, val);
        }

        return template;
    }

    public static String translate(String template,
                                   Properties props) {

        Iterator iter = props.entrySet().iterator();

        while (iter.hasNext()) {
            Map.Entry entry = (Map.Entry)iter.next();
            String key = (String)entry.getKey();
            String val = (String)entry.getValue();
            template = replace(template, key, val);
        }

        return template;
    }

    public static String configTemplate(String[] props) {
        String template = "";

        for (int i=0; i<props.length; i++) {
            template += props[i] + "=" + "%" + props[i] + "%";
            if (i+1 < props.length) {
                template += ",";
            }
        }

        return template;
    }

    private static void list(Metric metric) {
        System.out.println("DomainName: '" +
                           metric.getDomainName() + "'");

        System.out.println("ObjectName: '" +
                           metric.getObjectName() + "'");

        System.out.println("AttributeName: '" +
                           metric.getAttributeName() + "'");

        System.out.println("Object Properties: '" +
                           metric.getObjectPropString() + "'" +
                           " (" + metric.getObjectProperties().size() + ")");

        if (metric.propString == null) {
            return;
        }
        
        System.out.println("Connection Properties: '" +
                           metric.getPropString() + "'" +
                           " (" + metric.getProperties().size() + ")");
    }

    private static void list(File file) throws Exception {
        BufferedReader reader = null;

        try {
            reader = new BufferedReader(new FileReader(file));
            String line;
            while ((line = reader.readLine()) != null) {
                line = line.trim();
                if ((line.length() == 0) ||
                     line.startsWith("#"))
                {
                    continue;
                }
                System.out.println("Template: " + line);
                list(Metric.parse(line));
                System.out.println("----------------------------");
            }
        } finally {
            reader.close();
        }        
    }

    public static void main(String[] args) throws Exception {
        long memStart = Runtime.getRuntime().freeMemory();
        for (int i=0; i<args.length; i++) {
            String template = args[i];
            File file = new File(template);
            if (file.exists()) {
                list(file);
            }
            else {
                list(Metric.parse(template));
            }
        }

        long memEnd = Runtime.getRuntime().freeMemory();
        System.out.println("mem diff=" + (memStart-memEnd));
        int nMetrics=0, nProps=0;
        for (Iterator it=cache.values().iterator(); it.hasNext();) {
            Object o = it.next();
            if (o instanceof Metric) {
                nMetrics++;
            }
            else {
                nProps++;
            }
        }
        System.out.println("cache entries=" + cache.size() +
                           ", metrics=" + nMetrics +
                           ", props=" + nProps);
    }
}
