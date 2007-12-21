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

package org.hyperic.hq.product;

import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.HashMap;
import java.util.Map;
import java.util.Iterator;

import org.apache.commons.logging.Log;

import org.hyperic.snmp.MIBLookupException;
import org.hyperic.snmp.MIBTree;
import org.hyperic.snmp.SNMPClient;
import org.hyperic.snmp.SNMPException;
import org.hyperic.snmp.SNMPSession;
import org.hyperic.snmp.SNMPValue;

import org.hyperic.util.StringUtil;
import org.hyperic.util.timer.StopWatch;

public class SNMPMeasurementPlugin
    extends MeasurementPlugin {

    public static final String DOMAIN = "snmp";

    public static final String PROP_INDEX_NAME  = "snmpIndexName";
    public static final String PROP_INDEX_VALUE = "snmpIndexValue";

    private static final String PROP_OID         = "snmpOID";
    private static final String PROP_VARTYPE     = "snmpVarType";

    private static final int VARTYPE_SINGLE = 0;
    private static final int VARTYPE_NEXT   = 1;
    private static final int VARTYPE_COLUMN = 2;
    private static final int VARTYPE_INDEX  = 3;
    private static final int VARTYPE_OID    = 4;

    private static HashMap VARTYPES = new HashMap();
    private SNMPClient client = new SNMPClient();
    private static Map ixCache = new HashMap();
    private static Object ixLock = new Object();
    private static long ixTimestamp = 0;
    private static long ixExpire = (60 * 1000) * 60; //1 hour

    static {
        VARTYPES.put("single", new Integer(VARTYPE_SINGLE));
        VARTYPES.put("column", new Integer(VARTYPE_COLUMN));
        VARTYPES.put("index", new Integer(VARTYPE_INDEX));
        VARTYPES.put("oid", new Integer(VARTYPE_OID));
        VARTYPES.put("next", new Integer(VARTYPE_NEXT));
    }
    
    private Log log;
    
    /**
     * @return The MIB names that should be loaded for this plugin.
     */
    protected String[] getMIBs() {
        String prop = getProperty("MIBS");
        if (prop == null) {
            return new String[0];
        }
        else {
            List mibs = StringUtil.explode(prop, ",");
            return (String[])mibs.toArray(new String[0]);
        }
    }

    private static int convertVarType(Properties props) {
        String var = props.getProperty(PROP_VARTYPE);
        if (var == null) {
            if (props.getProperty(PROP_INDEX_NAME) != null) {
                var = "index";
            }
            else if (props.getProperty(PROP_OID) != null) {
                var = "oid";
            }
            else {
                var = "single"; //default
            }
        }
        Integer type = (Integer)VARTYPES.get(var);
        if (type == null) {
            String msg =
                "Unsupported " + PROP_VARTYPE + ": '" + var + "'";
            throw new IllegalArgumentException(msg);
        }
        return type.intValue();
    }

    /**
     * @see org.hyperic.hq.product.GenericPlugin#init
     */
    public void init(PluginManager manager)
        throws PluginException {

        super.init(manager);
        this.log = getLog();
        
        String prop = "snmp.indexCacheExpire";

        String expire =
            manager.getProperty(prop);

        if (expire != null) {
            ixExpire = Integer.parseInt(expire) * 1000;
        }

        final String pdkDir =
            manager.getProperty(ProductPluginManager.PROP_PDK_DIR);

        if (pdkDir == null) {
            return; //dont load MIBs in the server
        }

        MIBTree.setMibDir(pdkDir + "/mibs");

        try {
            if (this.client.init(manager.getProperties())) {
                if (this.data != null) { //null in the case of proxies
                    String jar = this.data.getFile();
                    String[] mibs = getMIBs();
                    
                    if (jar.endsWith(".xml")) {
                        //MIB files on local disk
                        this.client.addMIBs(mibs);
                    }
                    else {
                        //MIB files embedded within the jar
                        this.client.addMIBs(jar, mibs);
                    }
                }
            }
        } catch (SNMPException e) {
            throw new PluginException(e.getMessage(), e);
        }
    }

    private double getDoubleValue(SNMPValue snmpValue)
        throws PluginException {

        final String invalidType = 
            "SNMP query returned a string which could not be handled: ";

        //if toLong or toFloat throw an exception only if
        //SNMPException.E_VARIABLE_IS_NOT_NUMERIC
        //we swallow those exceptions because we've already checked the type
        switch (snmpValue.getType()) {
          case SNMPValue.TYPE_LONG:
          case SNMPValue.TYPE_LONG_CONVERTABLE:
            try {
                return (double)snmpValue.toLong();
            } catch (SNMPException e) {}

          case SNMPValue.TYPE_STRING:
            String value = snmpValue.toString();

            //e.g. iplanet iwsInstanceLoad1MinuteAverage
            //on anything but solaris
            if ("".equals(value)) {
                this.log.debug("string value is empty, returning -1");
                return -1;
            }

            //in general we should not be dealing with strings at all.
            //however, iplanet for example stores cpu usage as a string.
            //in which case we can convert to double.
            try {
                double val = Double.parseDouble(value);
                this.log.debug("converted using Double.parseDouble");
                return val;
            } catch (NumberFormatException e) {}

            //snmpValue.toLong() when value is TYPE_STRING
            //converts to a date stamp, which is our last attempt.
            try {
                double val = (double)snmpValue.toLong();
                this.log.debug("converted using snmpValue.toLong");
                return val;
            } catch (SNMPException e) {}

          default:
            throw new PluginException(invalidType + snmpValue.toString());
        }
    }

    private static boolean listIsEmpty(List list) {
        if (list == null) {
            return true;
        }
        return list.isEmpty();
    }

    //XXX unless we change snmplib to throw a more informative message
    //SNMPSession_v1.getNextValue throws SNMPException with the
    //message "SNMPException #101"
    //only seems to happen if it cannot talk to the snmp agent.
    //otherwise getColumn and getNextValue just return a null List
    private MetricUnreachableException snmpConnectException(Metric metric,
                                                            SNMPException e) {
        Properties props = metric.getObjectProperties();
        String cfg =
            props.getProperty(SNMPClient.PROP_IP) +
            ":" +
            props.getProperty(SNMPClient.PROP_PORT) +
            " " +
            props.getProperty(SNMPClient.PROP_VERSION) + "," +
            props.getProperty(SNMPClient.PROP_COMMUNITY);
        String msg = "Unable to connect to SNMP Agent (" + cfg + ")";
        return new MetricUnreachableException(msg, e);
    }

    /**
     * @see org.hyperic.hq.product.MeasurementPlugin#getValue
     */
    public MetricValue getValue (Metric metric) 
        throws MetricUnreachableException,
               MetricNotFoundException,
               PluginException {

        boolean isDebug = this.log.isDebugEnabled();
        SNMPSession session = getSession(metric);
        if (session == null) {
            throw new PluginException("SNMPSession was null!");
        }
        Properties props = metric.getProperties();

        double value = 0;
        String varName = metric.getAttributeName();

        if ((varName == null) || varName.equals("%oid%")) {
            //special case for optional netservices.SNMP.OID Value metric
            return MetricValue.NONE;
        }

        String varOID = getProperty(varName);
        if (varOID != null) {
            if (isDebug) {
                log.debug(getName() + " defined " +
                          varName + " to " + varOID);
            }
            varName = varOID;
        }
        int varType = convertVarType(props);
        List columnOfValues;
        int size;
        StopWatch timer = null;

        if (isDebug) {
            timer = new StopWatch();
        }

        if ((varType == VARTYPE_SINGLE) || (varType == VARTYPE_NEXT)) {
            SNMPValue snmpValue;
            try {
                if (varType == VARTYPE_SINGLE) {
                    snmpValue = session.getSingleValue(varName);
                }
                else {
                    snmpValue = session.getNextValue(varName);
                }
            } catch (MIBLookupException e) {
                throw new MetricInvalidException(e.getMessage());
            } catch (SNMPException e) {
                throw snmpConnectException(metric, e);
            } finally {
                if (timer != null) {
                    this.log.debug("getValue took: " + timer);
                }
            }
            value = getDoubleValue(snmpValue);
        }
        else {
            try {
                switch (varType) {
                  case VARTYPE_INDEX:
                    columnOfValues = session.getBulk(varName);
                
                    if (listIsEmpty(columnOfValues)) {
                        String msg = "Column data not found: " + varName;
                        throw new MetricNotFoundException(msg);
                    }

                    size = columnOfValues.size();

                    int index = getIndex(props, session);
                    if (index >= columnOfValues.size()) {
                        String ix =
                            props.getProperty(PROP_INDEX_NAME);
                        String val =
                            props.getProperty(PROP_INDEX_VALUE);
                        String msg = "No value found for SNMP index: " +
                            ix + "." + val;
                        throw new MetricNotFoundException(msg);
                    }
                    value = getDoubleValue((SNMPValue)columnOfValues.get(index));
                    break;
                  case VARTYPE_OID:
                    int idx = -1;
                    if (props.getProperty(PROP_INDEX_NAME) != null) {
                        //lookup index if given and include in the oid match
                        idx = getIndex(props, session);
                    }
                    String oid = props.getProperty(PROP_OID);
                    boolean found = false;

                    SNMPValue snmpValue = session.getTableValue(varName, idx+1, oid);
                    if (snmpValue != null) {
                        value = getDoubleValue(snmpValue);
                        found = true;
                    }

                    if (!found) {
                        String msg = "OID not found: " + oid;
                        throw new MetricNotFoundException(msg);
                    }
                    break;
                  case VARTYPE_COLUMN:
                    columnOfValues = session.getBulk(varName);
                
                    if (listIsEmpty(columnOfValues)) {
                        String msg = "Column data not found: " + varName;
                        throw new MetricNotFoundException(msg);
                    }

                    size = columnOfValues.size();

                    //XXX should support OID matching here too
                    for (int i=0; i<size; i++) {
                        value += getDoubleValue((SNMPValue)columnOfValues.get(i));
                    }
                  default:
                    throw new MetricNotFoundException("Invalid vartype");
                }
            } catch (SNMPException e) {
                throw snmpConnectException(metric, e);
            } finally {
                if (timer != null) {
                    this.log.debug("getValue took: " + timer +
                                   " (type=" + varType + ")");
                }
            }
        }

        //e.g. Airport ifOperStatus == 0 == AVAIL_DOWN
        if ("true".equals(metric.getObjectProperty("Avail"))) {
            if (value <= 0) {
                value = Metric.AVAIL_DOWN;
            }
            else {
                value = Metric.AVAIL_UP;
            }
        }

        return new MetricValue(value);
    }

    private int getIndex(Properties props, SNMPSession session)
        throws MetricUnreachableException,
        MetricNotFoundException {

        String indexName  = props.getProperty(PROP_INDEX_NAME);
        String indexValue = props.getProperty(PROP_INDEX_VALUE);

        if (indexName == null) {
            throw new MetricInvalidException("missing indexName");
        }
        if (indexValue == null) {
            throw new MetricInvalidException("missing indexValue");
        }

        synchronized (ixLock) {
            return getIndex(indexName, indexValue, session);
        }
    }

    private int getIndex(String indexName, String indexValue,
                         SNMPSession session)
        throws MetricUnreachableException,
        MetricNotFoundException {

        long timeNow = System.currentTimeMillis();

        Integer ix;
        boolean expired = false;

        if ((timeNow - ixTimestamp) > ixExpire) {
            if (ixTimestamp == 0) {
                this.log.debug("initializing index cache");
            }
            else {
                this.log.debug("clearing index cache");
            }

            ixCache.clear();
            ixTimestamp = timeNow;
            expired = true;
        }
        else {
            if ((ix = (Integer)ixCache.get(indexValue)) != null) {
                return ix.intValue();
            }
        }

        //for multiple indices we iterate through indexNames and
        //combine the values which we later attempt to match against
        //indexValue
        List indexNames = StringUtil.explode(indexName, "->");
        ArrayList data = new ArrayList();

        //XXX this can be optimized, esp. if indexNames.size() == 1
        for (int i=0; i<indexNames.size(); i++) {
            String name = (String)indexNames.get(i);

            List values = null;

            try {
                values = session.getBulk(name);

                for (int j=0; j<values.size(); j++) {
                    String value = values.get(j).toString();
                    StringBuffer buf = null;

                    if (data.size()-1 >= j) {
                        buf = (StringBuffer)data.get(j);
                        buf.append("->").append(value);
                    }
                    else {
                        buf = new StringBuffer(value);
                        data.add(buf);
                    }
                }
            } catch (SNMPException e) {
                throw new MetricInvalidException(e);
            }
        }

        //we go in reverse in the case of apache having
        //two servername->80 entries, the first being the default (unused)
        //second being the vhost on 80 which is actually handling the requests
        //XXX we could/should? enforce uniqueness here.
        for (int i=data.size()-1; i>=0; i--) {
            StringBuffer buf = (StringBuffer)data.get(i);
            String cur = buf.toString();

            //since we fetched all the data might as well build
            //up the index cache for future reference.
            Integer index = new Integer(i);
            ixCache.put(cur, index);
            //only seen w/ microsoft snmp server
            //where interface name has a trailing null byte
            ixCache.put(cur.trim(), index);
        }

        if (this.log.isDebugEnabled()) {
            if (expired) {
                this.log.debug("built index cache:");
                for (Iterator it = ixCache.entrySet().iterator();
                     it.hasNext();) {
                    Map.Entry ent = (Map.Entry)it.next();
                    this.log.debug("   " + ent.getKey() +
                                   "=>" + ent.getValue());
                }
            }
            else {
                this.log.debug("forced to rebuild index cache " +
                               " looking for: " + indexValue);
            }
        }

        if ((ix = (Integer)ixCache.get(indexValue)) != null) {
            return ix.intValue();
        }

        String possibleValues = ", possible values=";

        if (listIsEmpty(data)) {
            possibleValues += "[NONE FOUND]";
        }
        else {
            possibleValues += data.toString();
        }

        throw new MetricNotFoundException("could not find value '" +
                                            indexValue + "' in column '" +
                                            indexName + "'" + possibleValues);
    }

    private SNMPSession getSession(Metric metric)
        throws PluginException {

        Properties props = metric.getObjectProperties();
        if (props.get(SNMPClient.PROP_IP) == null) {
            //backcompat: shitty decision to make ip address the domain name
            props.put(SNMPClient.PROP_IP, metric.getDomainName());
        }

        try {
            return this.client.getSession(props);
        } catch (SNMPException e) {
            throw new PluginException(e.getMessage(), e);
        }
    }
}
