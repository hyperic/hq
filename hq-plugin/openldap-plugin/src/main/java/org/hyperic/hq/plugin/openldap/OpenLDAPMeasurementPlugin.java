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
package org.hyperic.hq.plugin.openldap;

import org.hyperic.hq.product.MeasurementPlugin;
import org.hyperic.hq.product.Metric;
import org.hyperic.hq.product.MetricUnreachableException;
import org.hyperic.hq.product.MetricInvalidException;
import org.hyperic.hq.product.MetricNotFoundException;
import org.hyperic.hq.product.MetricValue;
import org.hyperic.hq.product.PluginException;

import javax.naming.Context;
import javax.naming.directory.Attribute;
import javax.naming.directory.Attributes;
import javax.naming.directory.DirContext;
import javax.naming.directory.InitialDirContext;
import javax.naming.directory.SearchControls;
import javax.naming.directory.SearchResult;
import javax.naming.NamingEnumeration;
import javax.naming.NamingException;

import java.util.Collection;
import java.util.Hashtable;
import java.util.Properties;
import java.util.TreeSet;
import javax.naming.CommunicationException;
import org.apache.commons.logging.Log;
import org.hyperic.hq.product.PluginManager;

public class OpenLDAPMeasurementPlugin
        extends MeasurementPlugin {

    private DirContext ctx = null;
    private boolean hasMonitoringEnabled = false;
    private final Log log = getLog();

    public DirContext getDirContext(Properties props) throws NamingException {
        if (this.ctx == null) {
            synchronized (this) {
                if (this.ctx == null) {
                    log.debug("[getDirContext] creating new connection");
                    Collection rtn = new TreeSet();
                    Hashtable ldapEnv = new Hashtable();
                    String ldapDriver = props.getProperty("ldapDriver"),
                            ldapHostURL = props.getProperty("ldapHostURL"),
                            ldapAuthType = props.getProperty("ldapAuthType"),
                            ldapPasswd = props.getProperty("ldapPasswd"),
                            ldapTreePathToDN = props.getProperty("ldapTreePathToDN");
                    ldapTreePathToDN = (ldapTreePathToDN == null)
                            ? "" : ldapTreePathToDN;
                    ldapPasswd = (ldapPasswd == null) ? "" : ldapPasswd;
                    ldapPasswd = (ldapPasswd.matches("^\\s*$")) ? "" : ldapPasswd;
                    ldapEnv.put(Context.INITIAL_CONTEXT_FACTORY, ldapDriver);
                    ldapEnv.put(Context.PROVIDER_URL, ldapHostURL);
                    ldapEnv.put(Context.SECURITY_AUTHENTICATION, ldapAuthType);
                    ldapEnv.put(Context.SECURITY_PRINCIPAL, ldapTreePathToDN);
                    ldapEnv.put(Context.SECURITY_CREDENTIALS, ldapPasswd);
                    this.ctx = new InitialDirContext(ldapEnv);
                }
            }
        }
        return this.ctx;
    }

    @Override
    public MetricValue getValue(Metric metric)
            throws PluginException,
            MetricUnreachableException,
            MetricInvalidException,
            MetricNotFoundException {
        // will look like "generic:Type=GenericService,option1=option1,option2=option2"
        String objectName = metric.getObjectName();

        // will look like "Availability"
        // -OR-
        // "cn=PDU,cn=Statistics,cn=Monitor:monitorCounter"
        String alias = metric.getAttributeName();

        MetricValue res;
        if (metric.isAvail()) {
            try {
                hasMonitoringEnabled = hasMonitoringEnabled(metric);
                res = new MetricValue(Metric.AVAIL_UP, System.currentTimeMillis());
            } catch (NamingException ex) {
                res = new MetricValue(Metric.AVAIL_DOWN, System.currentTimeMillis());
                hasMonitoringEnabled = false;
                this.ctx = null; // reset connection [HHQ-4986]
                log.debug("[getValue] error:" + ex, ex);
            }
        } else {
            try {
                if (alias.equalsIgnoreCase("connectiontimems")) {
                    res = getConnTimeMetric(metric);
                } else {
                    if (hasMonitoringEnabled) {
                        String[] attrs = alias.split(":");
                        if (attrs[0] == null || attrs[1] == null) {
                            throw new MetricNotFoundException("bad template format");
                        }
                        res = getMetric(metric, attrs[0], attrs[1]);
                    } else {
                        res = new MetricValue(MetricValue.NONE, System.currentTimeMillis());
                    }
                }
            } catch (CommunicationException ex) {
                log.debug("[getValue] error:" + ex, ex);
                this.ctx = null; // reset connection [HHQ-4986]
                throw new MetricNotFoundException(ex.getMessage(), ex);
            } catch (NamingException ex) {
                log.debug("[getValue] error:" + ex, ex);
                throw new MetricNotFoundException("Service " + objectName + ", " + alias + " not found", ex);
            }
        }
        return res;
    }

    private MetricValue getConnTimeMetric(Metric metric)
            throws NamingException {
        long start = System.currentTimeMillis();
        hasMonitoringEnabled(metric);
        long now = System.currentTimeMillis();
        return new MetricValue((now - start), now);
    }

    private MetricValue getMetric(Metric metric, String tree, String attr)
            throws MetricNotFoundException, NamingException {
        NamingEnumeration enumer = null;
        try {
            String[] a = {attr};
            SearchControls cons = new SearchControls();
            cons.setSearchScope(SearchControls.OBJECT_SCOPE);
            cons.setReturningAttributes(a);
            enumer = getDirContext(metric.getProperties()).search(tree,
                    "(&(objectClass=*))", cons);
            while (enumer.hasMore()) {
                SearchResult searchresult = (SearchResult) enumer.next();
                Attributes attrs = searchresult.getAttributes();
                Attribute val;
                if (null != (val = attrs.get(attr))) {
                    return new MetricValue(new Double(val.get().toString()),
                            System.currentTimeMillis());
                }
            }
            throw new MetricNotFoundException("");
        } finally {
            if (enumer != null) {
                enumer.close();
            }
        }
    }

    /**
     * @return true  = monitoring is enabled
     * @return false = monitoring is not enabled
     * @exception NamingException no conection
     */
    private boolean hasMonitoringEnabled(Metric metric)
            throws NamingException {
        NamingEnumeration enumer = null,
                enumerx = null,
                enumery = null;

        boolean res = false;
        try {
            String[] a = {"monitorContext"};
            SearchControls cons = new SearchControls();
            cons.setSearchScope(SearchControls.OBJECT_SCOPE);
            cons.setReturningAttributes(a);
            enumer = getDirContext(metric.getProperties()).search("", "(&(objectClass=*))", cons);
            while (enumer.hasMore() && !res) {
                SearchResult searchresult = (SearchResult) enumer.next();
                Attributes attrs = searchresult.getAttributes();
                enumerx = attrs.getIDs();
                while (enumerx.hasMore()) {
                    String id = (String) enumerx.next();
                    Attribute attr = attrs.get(id);
                    res = true;
                }
            }
        } finally {
            if (enumer != null) {
                enumer.close();
            }
            if (enumerx != null) {
                enumerx.close();
            }
            if (enumery != null) {
                enumery.close();
            }
        }

        log.debug("[hasMonitoringEnabled] res=" + res + " metric:" + metric);
        return res;
    }
}
