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

package org.hyperic.hq.plugin.netdevice;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.hyperic.hq.product.Collector;
import org.hyperic.hq.product.Metric;
import org.hyperic.hq.product.PluginException;
import org.hyperic.hq.product.SNMPMeasurementPlugin;
import org.hyperic.snmp.SNMPClient;
import org.hyperic.snmp.SNMPException;
import org.hyperic.snmp.SNMPSession;
import org.hyperic.snmp.SNMPValue;

public class SNMPCollector extends Collector {
    protected static final String PROP_COLUMN = "snmpColumn";
    protected Properties _props;
    protected SNMPClient _client;

    private static final String[][] DEFAULT_PROPS = {
        { SNMPClient.PROP_IP, SNMPClient.DEFAULT_IP },
        { SNMPClient.PROP_PORT, SNMPClient.DEFAULT_PORT_STRING },
        { SNMPClient.PROP_VERSION, SNMPClient.VALID_VERSIONS[1] },
        { SNMPClient.PROP_COMMUNITY, SNMPClient.DEFAULT_COMMUNITY }
    };

    protected Log getLog() {
        return LogFactory.getLog(getClass().getName());
    }
    
    protected String getInfo() {
        String info =
            _props.getProperty(SNMPClient.PROP_IP) +
            ":" +
            _props.getProperty(SNMPClient.PROP_PORT) +
            " " +
            _props.getProperty(SNMPClient.PROP_VERSION)
            + "," +
            _props.getProperty(SNMPClient.PROP_COMMUNITY);
        return info;
    }

    protected boolean isEmpty(List list, String name) {
        boolean isEmpty;
        if (list == null) {
            isEmpty = true;
        }
        else {
            isEmpty = list.isEmpty();
        }
        if (isEmpty) {
            setErrorMessage("No data returned for: " + name);
        }
        return isEmpty;
    }

    protected boolean isEmpty(Map map, String name) {
        boolean isEmpty;
        if (map == null) {
            isEmpty = true;
        }
        else {
            isEmpty = map.isEmpty();
        }
        if (isEmpty) {
            setErrorMessage("No data returned for: " + name);
        }
        return isEmpty;
    }
    
    protected boolean isAvailability() {
        String availName =
            getPlugin().getTypeProperty(Metric.ATTR_AVAIL);
        if (availName == null) {
            return false;
        }
        return getCounterName().equals(availName);
    }

    protected String getColumnName() {
        return _props.getProperty(PROP_COLUMN);
    }

    protected String getCounterName() {
        return _props.getProperty(PROP_COLUMN);
    }

    protected String getIndexName() {
        return _props.getProperty(SNMPMeasurementPlugin.PROP_INDEX_NAME);
    }

    protected String getColumnIndex(SNMPValue val) {
        String oid = val.getOID();
        int last = oid.lastIndexOf('.');
        return oid.substring(last+1);        
    }

    protected Map getIndexedColumn(SNMPSession session, String name)
        throws SNMPException {

        return getIndexedColumn(session, name, true);
    }

    protected Map getIndexedColumn(SNMPSession session, String name,
                                   boolean asString)
        throws SNMPException {
        //XXX expose walk() in SNMPClient
        List column;
        Map values = new HashMap();
        try {
            column = session.getBulk(name);
            if (isEmpty(column, name)) {
                return values;
            }
            for (int i=0; i<column.size(); i++) {
                SNMPValue val = (SNMPValue)column.get(i);
                String ix = getColumnIndex(val);
                Object obj;
                if (asString) {
                    obj = val.toString().trim();
                }
                else {
                    obj = val;
                }
                values.put(ix, obj);
            }
        } catch (Exception e) {
            getLog().error("getBulk(" + name + "): " + e);
            return null;
        }
        return values;
    }

    public interface ColumnValueConverter {
        public double convert(String index, SNMPValue value)
            throws Exception;
    }

    public static class GenericColumnValueConverter
        implements ColumnValueConverter {

        public double convert(String index, SNMPValue value)
            throws Exception {
            return value.toLong();
        }
    }

    protected void collectIndexedColumn() {
        collectIndexedColumn(new GenericColumnValueConverter());
    }

    protected SNMPSession getSession() throws Exception {
        try {
            return _client.getSession(_props);
        } catch (Exception e) {
            setErrorMessage(e.getMessage());
            throw e;
        }
    }

    protected void collectIndexedColumn(ColumnValueConverter converter) {
        String indexName = getIndexName();
        String columnName = getColumnName();
        String counterName = getCounterName();
        boolean isAvail = isAvailability();
        SNMPSession session;
        List column;
        Map indexNames;

        try {
            session = getSession();
            indexNames = getIndexedColumn(session, indexName);
            column = session.getBulk(columnName);
        } catch (Exception e) {
            return;
        }
        if (isEmpty(column, columnName) ||
            isEmpty(indexNames, indexName))
        {
            return;
        }

        for (int i=0; i<column.size(); i++) {
            SNMPValue val = (SNMPValue)column.get(i);
            String ix = getColumnIndex(val);
            String name = (String)indexNames.get(ix);
            try {
                setValue(name + "." + counterName, converter.convert(ix, val));
            } catch (Exception e) {
                getLog().warn(columnName + ".convert failed for " +
                              name + "@" + getInfo());
            }
            if (isAvail) {
                setValue(name + "." + Metric.ATTR_AVAIL, Metric.AVAIL_UP);
            }
        }        
    }

    public void init() throws PluginException {
        _client = new SNMPClient();
        _props = getProperties();
        for (int i=0; i<DEFAULT_PROPS.length; i++) {
            String key = DEFAULT_PROPS[i][0];
            String val = DEFAULT_PROPS[i][1];
            if (_props.getProperty(key) == null) {
                _props.setProperty(key, val);
            }
        }
        setSource(getInfo());
        try {
            init(_client.getSession(_props));
        } catch (SNMPException e) {
            throw new PluginException(getInfo() + ": " + e.getMessage());
        }
    }

    protected void init(SNMPSession session) throws PluginException {
        
    }

    public void collect() {

    }
}
