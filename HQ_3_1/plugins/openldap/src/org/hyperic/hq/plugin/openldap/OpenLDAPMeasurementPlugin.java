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

import org.hyperic.util.config.ConfigResponse;
import org.hyperic.util.config.ConfigSchema;
import org.hyperic.util.config.SchemaBuilder;
import org.hyperic.hq.product.MeasurementPlugin;
import org.hyperic.hq.product.Metric;
import org.hyperic.hq.product.MetricUnreachableException;
import org.hyperic.hq.product.MetricInvalidException;
import org.hyperic.hq.product.MetricNotFoundException;
import org.hyperic.hq.product.MetricValue;
import org.hyperic.hq.product.PluginException;
import org.hyperic.hq.product.PluginManager;

import org.hyperic.sigar.Sigar;
import org.hyperic.sigar.SigarException;
import org.hyperic.sigar.ptql.ProcessFinder;

import javax.naming.Context;
import javax.naming.directory.Attribute;
import javax.naming.directory.Attributes;
import javax.naming.directory.DirContext;
import javax.naming.directory.InitialDirContext;
import javax.naming.directory.InvalidSearchControlsException;
import javax.naming.directory.InvalidSearchFilterException;
import javax.naming.directory.SearchControls;
import javax.naming.directory.SearchResult;
import javax.naming.NamingEnumeration;
import javax.naming.NamingException;

import java.util.Collection;
import java.util.Hashtable;
import java.util.Properties;
import java.util.TreeSet;

public class OpenLDAPMeasurementPlugin
    extends MeasurementPlugin
{
    private Sigar mySigar = new Sigar();
    private DirContext ctx = null;
    private Boolean hasMonitoringEnabled = null;
    private Object myLockObj = new Object();

    public DirContext getDirContext(Metric metric)
        throws NamingException
    {
        if (this.ctx != null)
            return this.ctx;
        synchronized(this)
        {
            if (this.ctx != null)
                return this.ctx;
            Properties props = metric.getProperties();
            Collection rtn = new TreeSet();
            Hashtable ldapEnv = new Hashtable();
            String ldapDriver   = props.getProperty("ldapDriver"),
                   ldapHostURL  = props.getProperty("ldapHostURL"),
                   ldapAuthType = props.getProperty("ldapAuthType"),
                   ldapPasswd   = props.getProperty("ldapPasswd"),
                   ldapTreePathToDN = props.getProperty("ldapTreePathToDN");
            ldapTreePathToDN = (ldapTreePathToDN == null) ?
                                                "" : ldapTreePathToDN;
            ldapPasswd = (ldapPasswd == null) ?  "" : ldapPasswd;
            ldapPasswd = (ldapPasswd.matches("^\\s*$")) ?  "" : ldapPasswd;
            ldapEnv.put(Context.INITIAL_CONTEXT_FACTORY, ldapDriver);
            ldapEnv.put(Context.PROVIDER_URL, ldapHostURL);
            ldapEnv.put(Context.SECURITY_AUTHENTICATION, ldapAuthType);
            ldapEnv.put(Context.SECURITY_PRINCIPAL, ldapTreePathToDN);
            ldapEnv.put(Context.SECURITY_CREDENTIALS, ldapPasswd);
            this.ctx = new InitialDirContext(ldapEnv);
            return this.ctx;
        }
    }

    public MetricValue getValue(Metric metric)
        throws PluginException,
               MetricUnreachableException,
               MetricInvalidException,
               MetricNotFoundException
    {
        // will look like "generic:Type=GenericService,option1=option1,option2=option2"
        String objectName = metric.getObjectName();
        String option1 = metric.getObjectProperty("option1"),
               option2 = metric.getObjectProperty("option2");

        // will look like "Availability"
        // -OR-
        // "cn=PDU,cn=Statistics,cn=Monitor:monitorCounter"
        String alias = metric.getAttributeName();

        try
        {
            if (objectName.indexOf("Server") != -1)
            {
                if (hasMonitoringEnabled == null)
                {
                    synchronized(myLockObj)
                    {
                        hasMonitoringEnabled = (hasMonitoringEnabled == null) ?
                                                 hasMonitoringEnabled(metric) :
                                                 hasMonitoringEnabled;
                    }
                }
                if (alias.equalsIgnoreCase("availability"))
                    return getAvail(metric);
                else if (alias.equalsIgnoreCase("connectiontimems"))
                    return getConnTimeMetric(metric);
                else if (hasMonitoringEnabled == null || !hasMonitoringEnabled.booleanValue())
                    return MetricValue.NONE;

                String[] attrs = alias.split(":");
                if (attrs[0] == null || attrs[1] == null)
                    throw new MetricNotFoundException("");

                return getMetric(metric, attrs[0], attrs[1]);
            }
            throw new MetricNotFoundException("");
        }
        catch (NamingException e) {
            throw new MetricNotFoundException("Service "+objectName+", "+alias+" not found", e);
        }
        catch (MetricNotFoundException e) {
            throw new MetricNotFoundException("Service "+objectName+", "+alias+" not found", e);
        }
    }

    private MetricValue getConnTimeMetric(Metric metric)
        throws NamingException
    {
        long start = System.currentTimeMillis();
        hasMonitoringEnabled(metric);
        long now = System.currentTimeMillis();
        return new MetricValue((now - start), now);
    }

    private MetricValue getMetric(Metric metric, String tree, String attr)
        throws MetricNotFoundException, NamingException
    {
        NamingEnumeration enumer = null;
        try
        {
            String[] a = {attr};
            SearchControls cons = new SearchControls();
            cons.setSearchScope(SearchControls.OBJECT_SCOPE);
            cons.setReturningAttributes(a);
            enumer = getDirContext(metric).search(tree,
                "(&(objectClass=*))", cons);
            while (enumer.hasMore())
            {
                SearchResult searchresult = (SearchResult)enumer.next();
                Attributes attrs = searchresult.getAttributes();
                Attribute val;
                if (null != (val = attrs.get(attr))) {
                    return new MetricValue(new Double(val.get().toString()),
                                           System.currentTimeMillis());
                }
            }
            throw new MetricNotFoundException("");
        }
        finally {
            if (enumer != null) enumer.close();
        }
    }

    private MetricValue getAvail(Metric metric)
        throws NamingException
    {
        return (null == hasMonitoringEnabled(metric)) ?
            new MetricValue(Metric.AVAIL_DOWN, System.currentTimeMillis()) :
            new MetricValue(Metric.AVAIL_UP, System.currentTimeMillis());
    }

    /**
     * @return true  = monitoring is enabled
     * @return false = monitoring is not enabled
     * @return null  = not available
     */
    private Boolean hasMonitoringEnabled(Metric metric)
        throws NamingException
    {
        NamingEnumeration enumer = null,
                          enumerx = null,
                          enumery = null;
        try
        {
            String[] a = {"monitorContext"};
            SearchControls cons = new SearchControls();
            cons.setSearchScope(SearchControls.OBJECT_SCOPE);
            cons.setReturningAttributes(a);
            enumer = getDirContext(metric).search("", "(&(objectClass=*))", cons);
            while (enumer.hasMore())
            {
                SearchResult searchresult = (SearchResult)enumer.next();
                Attributes attrs = searchresult.getAttributes();
                enumerx = attrs.getIDs();
                while (enumerx.hasMore())
                {
                    String id = (String)enumerx.next();
                    Attribute attr = attrs.get(id);
                    return Boolean.TRUE;
                }
            }
            return Boolean.FALSE;
        }
        catch (InvalidSearchFilterException e) {
        }
        catch (InvalidSearchControlsException e) {
        }
        catch (NamingException e) {
        }
        finally
        {
            if (enumer != null) enumer.close();
            if (enumerx != null) enumerx.close();
            if (enumery != null) enumery.close();
        }
        return null;
    }
}
