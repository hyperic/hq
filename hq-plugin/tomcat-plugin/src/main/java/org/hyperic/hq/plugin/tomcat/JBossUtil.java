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

package org.hyperic.hq.plugin.tomcat;

import java.io.IOException;
import java.rmi.RemoteException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Properties;

import javax.management.Attribute;
import javax.management.AttributeNotFoundException;
import javax.management.InstanceNotFoundException;
import javax.management.IntrospectionException;
import javax.management.InvalidAttributeValueException;
import javax.management.MBeanInfo;
import javax.management.MalformedObjectNameException;
import javax.management.MBeanException;
import javax.management.MBeanServerConnection;
import javax.management.ObjectName;
import javax.management.ReflectionException;
import javax.management.RuntimeMBeanException;
import javax.management.j2ee.statistics.CountStatistic;
import javax.management.j2ee.statistics.RangeStatistic;
import javax.management.j2ee.statistics.Stats;
import javax.management.j2ee.statistics.Statistic;
import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;

//import org.hyperic.hq.plugin.jboss.jmx.ServerQuery;
import org.hyperic.hq.product.ControlPlugin;
import org.hyperic.hq.product.Metric;
import org.hyperic.hq.product.MetricInvalidException;
import org.hyperic.hq.product.MetricNotFoundException;
import org.hyperic.hq.product.MetricUnreachableException;
import org.hyperic.hq.product.PluginException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

public class JBossUtil {

    private static final String PROP_NAMING_CONNECTOR =
        "connector.jndi.name";

    private static final String JNDI_FACTORY =
        "org.jboss.security.jndi.JndiLoginInitialContextFactory";

    private static final String PROP_JNP_TIMEOUT =
        "jnp.timeout";

    private static final String PROP_JNP_SOTIMEOUT =
        "jnp.sotimeout";

    private static final String DEFAULT_JNP_TIMEOUT =
        System.getProperty(PROP_JNP_TIMEOUT,
                           String.valueOf(30 * 1000));

    private static final String DEFAULT_JNP_SOTIMEOUT =
        System.getProperty(PROP_JNP_SOTIMEOUT,
                           DEFAULT_JNP_TIMEOUT);

    //http://wiki.jboss.org/wiki/Wiki.jsp?page=NamingContextFactory
    private static final String[][] NAMING_PROPS = {
        {
            PROP_NAMING_CONNECTOR,
            "jmx/rmi/RMIAdaptor"
        },
        {
            Context.INITIAL_CONTEXT_FACTORY,
            "org.jnp.interfaces.NamingContextFactory"
        },
        {
            Context.URL_PKG_PREFIXES,
            "org.jboss.naming:org.jnp.interfaces"
        },
        {
            "jnp.disableDiscovery",
            "true"
        },
        {
            PROP_JNP_TIMEOUT,
            DEFAULT_JNP_TIMEOUT
        },
        {
            PROP_JNP_SOTIMEOUT,
            DEFAULT_JNP_SOTIMEOUT
        }
    };

    private static final String[] STAT_PROVIDER   = { "StatisticsProvider", "Stats" };
    private static final String[] STAT_PROVIDER_4 = { "statisticsProvider", "stats" };

    private static Log log = LogFactory.getLog("JBossUtil");

    public static MBeanServerConnection getMBeanServerConnection(Properties config)
        throws NamingException, RemoteException {
        MBeanServerConnection adaptor;

        Properties props = new Properties();

        for (int i=0; i<NAMING_PROPS.length; i++) {
            props.setProperty(NAMING_PROPS[i][0],
                              NAMING_PROPS[i][1]);
        }

        props.putAll(config);
        props.put("java.naming.provider.url", config.get("jmx.url"));

        if (props.getProperty(Context.SECURITY_PRINCIPAL) != null) {
            props.setProperty(Context.INITIAL_CONTEXT_FACTORY,
                              JNDI_FACTORY);
        }

        InitialContext ctx = new InitialContext(props);

        try {
            Object o=ctx.lookup(props.getProperty(PROP_NAMING_CONNECTOR));
            log.debug("=> "+Arrays.asList(o.getClass().getInterfaces()));
            adaptor = (MBeanServerConnection)o;
        } finally {
            ctx.close();
        }

        return adaptor;
    }
    
    /*
     * @deprecated
     */
    public static MBeanServerConnection getMBeanServerConnection(String url)
        throws NamingException, RemoteException {

        Properties config = new Properties();
        config.setProperty(Context.PROVIDER_URL, url);
        return getMBeanServerConnection(config);
    }

    private static String getServerURL(Metric metric) {
        return metric.getProperties().getProperty(Context.PROVIDER_URL);
    }

    public static MBeanServerConnection getMBeanServerConnection(Metric metric)
        throws NamingException, RemoteException {
                    
        return getMBeanServerConnection(metric.getProperties());
    }

    //we only cache RemoteMBeanServerConnection handles for measurements.
    //jndi lookup won't have much impact for control or discovery
    private static HashMap serverCache = new HashMap();
    // Maps attrNames to their lowercase equivalents.  After static initialization,
    // get-only access.
    private static HashMap jsr77LowerCase = new HashMap();
    // Maps urls to a Boolean indicating whether or not the jsr77 attrNames
    // from the url are lowercase or not.  Needs synchronization.
    private static HashMap lowerCaseURLMappings = new HashMap();

//    //thanks JBoss, for changing the attribute case in 3.2.8
//    static {
//        String[] attrs = {
//            JBossMeasurementPlugin.ATTR_STATE_MANAGEABLE,
//        };
//        for (int i=0; i<attrs.length; i++) {
//            String attr = attrs[i];
//            String lcAttr =
//                Character.toLowerCase(attr.charAt(0)) +
//                attr.substring(1);
//            jsr77LowerCase.put(attr, lcAttr);
//        }
//    }

    private static MetricInvalidException invalid(Metric metric,
                                                  Exception e) {
        String msg =
            "Malformed ObjectName [" + metric.getObjectName() + "]";
        return new MetricInvalidException(msg, e);
    }

    private static MetricUnreachableException unreachable(Metric metric,
                                                          Exception e) {
        String msg =
            "Can't connect to MBeanServerConnection [" +
            metric.toDebugString() + "]: " + e;
        return new MetricUnreachableException(msg, e);
    }

    private static MetricNotFoundException notfound(Metric metric,
                                                    Exception e) {
        String msg =
            "Metric not found [" + metric.toDebugString() + "]: " + e;
        return new MetricNotFoundException(msg, e);
    }

    private static PluginException error(Metric metric,
                                         Exception e) {
        String msg =
            "Invocation error [" + metric.toDebugString() + "]: " + e;
        return new PluginException(msg, e);
    }

    private static PluginException error(Metric metric,
                                         Exception e,
                                         String method) {
        String msg =
            "Method '" + method +
            "' invocation error [" + metric.toDebugString() + "]: " + e;
        return new PluginException(msg, e);
    }

    //dealing with the attribute case change had been done with the HQ server
    //types JBoss 3.2 and JBoss 4.0, but then 3.2.8 threw that off by making
    //the same changes as 4.0
//    static void determineJSR77Case(String url, MBeanServerConnection mServer) {
//        try {
//            ObjectName server =
//                new ObjectName(ServerQuery.SERVER_NAME);
//            String version =
//                (String)mServer.getAttribute(server, ServerQuery.ATTR_VERSION);
//            if (version.length() < 5) {
//                return;
//            }
//            boolean lc;
//            int majorVersion = Character.getNumericValue(version.charAt(0));
//            if (majorVersion >= 4) {
//                //4.x, 5.x
//                lc = true;
//            }
//            else if (Character.getNumericValue(version.charAt(4)) >= 8) {
//                //3.2.8
//                lc = true;
//            }
//            else {
//                //<= 3.2.7
//                lc = false;
//            }
//
//            synchronized (lowerCaseURLMappings) {
//            	lowerCaseURLMappings.put(url, lc ? Boolean.TRUE : Boolean.FALSE);
//            }
//
//            if (log.isDebugEnabled()) {
//                log.debug(url + " " + version + " jsr77LowerCase=" + lc);
//            }
//        } catch (Exception e) {
//            //unlikely, but in this case, leave it to the server type to determine the case
//        }
//    }

    static Object getRemoteMBeanValue(Metric metric)
        throws MetricNotFoundException,
               MetricInvalidException,
               MetricUnreachableException,
               PluginException {

        MBeanServerConnection mServer = null;
        boolean cached = true;
        String url = getServerURL(metric);

        synchronized (serverCache) {
            mServer = (MBeanServerConnection)serverCache.get(url);
        }

        if (mServer == null) {
            cached = false;
            try {
                mServer = getMBeanServerConnection(metric); //jndi lookup
            } catch (NamingException e) {
                throw unreachable(metric, e);
            } catch (RemoteException e) {
                throw unreachable(metric, e);
            }

//            determineJSR77Case(url, mServer);
            synchronized (serverCache) {
                serverCache.put(url, mServer);
            }
        }

        String attrName = metric.getAttributeName();
        boolean lc;

        Boolean jsr77Case;
        synchronized (lowerCaseURLMappings) {
        	jsr77Case = (Boolean) lowerCaseURLMappings.get(url);
		}

        if (jsr77Case != null) {
            lc = jsr77Case.booleanValue();
        }
        else {
            lc = Character.isLowerCase(attrName.charAt(0));
        }

        String lcAttr;
        //another 3.2.8 hack
        if (lc && ((lcAttr = (String)jsr77LowerCase.get(attrName)) != null)) {
            attrName = lcAttr;
        }

        try {
            ObjectName objName = new ObjectName(metric.getObjectName());

            if (attrName.substring(1).startsWith(/*S*/"tatistic")) {
                return getJSR77Statistic(mServer, objName, metric, lc);
            }
            else if (attrName.equals("__INSTANCE__")) {
                //cheap hack for an avail metric for MBeans that dont
                //have anything better we can use, e.g. Hibernate.
                try {
                    mServer.getObjectInstance(objName);
                    return Boolean.TRUE;
                } catch (Exception e) {
                    return Boolean.FALSE;
                }
            }
            else {
                return mServer.getAttribute(objName, attrName);
            }
        } catch (MalformedObjectNameException e) {
            throw invalid(metric, e);
        } catch (InstanceNotFoundException e) {
            throw notfound(metric, e);
        } catch (AttributeNotFoundException e) {
            //XXX not all MBeans have a reasonable attribute to
            //determine availability, so just assume if we get this far
            //the MBean exists and is alive.
            if (attrName.equals(Metric.ATTR_AVAIL)) {
                return new Double(Metric.AVAIL_UP);
            }
            throw notfound(metric, e);
        } catch (ReflectionException e) {
            throw error(metric, e);
        } catch (MBeanException e) {
            throw error(metric, e);
        } catch (RuntimeMBeanException e) {
            throw error(metric, e);
        } catch (Exception e) {
            //CommunicationException, NamingException, RemoteException, etc.
            if (cached) {
                //retry once, in the event the cached connection was stale
            	synchronized (serverCache) {
            		serverCache.remove(url);
            	}
                log.debug("MBeanServerConnection cache cleared for " + url);
                return getRemoteMBeanValue(metric);
            }
            else {
                throw unreachable(metric, e);
            }
        }
    }
//
    static Double getJSR77Statistic(MBeanServerConnection mServer,
                                    ObjectName objName,
                                    Metric metric, boolean lc)
        throws MetricNotFoundException,
               MetricInvalidException,
               MetricUnreachableException,
               PluginException {

        //jboss changed attribute case in version 4.0
        String[] attrs;
        if (lc) {
            attrs = STAT_PROVIDER_4;
        }
        else {
            attrs = STAT_PROVIDER;
        }

        Stats stats;
        try {
            Boolean provider =
                (Boolean) mServer.getAttribute(objName, attrs[0]);
            if ((provider == null) || !provider.booleanValue()) {
                String msg =
                    "MBeanServerConnection does not provide statistics";
                throw new PluginException(msg);
            }

            stats = (Stats)mServer.getAttribute(objName, attrs[1]);
        } catch (RemoteException e) {
            throw unreachable(metric, e);
        } catch (InstanceNotFoundException e) {
            throw notfound(metric, e);
        } catch (AttributeNotFoundException e) {
            throw notfound(metric, e);
        } catch (ReflectionException e) {
            throw error(metric, e);
        } catch (MBeanException e) {
            throw error(metric, e);
        } catch (IOException e) {
            throw error(metric, e);
        }

        if (stats == null) {
            throw new PluginException("MBeanServerConnection has no stats");
        }

        String statName = metric.getAttributeName().substring(9);
        Statistic stat = stats.getStatistic(statName);
        if (stat == null) {
            String msg =
                "Statistic '" + statName + "' not found [" + metric + "]";
            throw new MetricNotFoundException(msg);
        }

        long value;
        if (stat instanceof CountStatistic) {
            value = ((CountStatistic)stat).getCount();
        }
        else if (stat instanceof RangeStatistic) {
            value = ((RangeStatistic)stat).getCurrent();
        }
        else {
            String msg =
                "Unsupported statistic type [" +
                statName.getClass().getName() +
                " for [" + metric + "]";
            throw new MetricInvalidException(msg);
        }

        return new Double(value);
    }
//
//    public static Object invoke(Metric metric, String method)
//        throws MetricUnreachableException,
//               MetricNotFoundException,
//               PluginException {
//        return invoke(metric, method, new Object[0], new String[0]);
//    }
//
//    private static Object setAttribute(MBeanServerConnection mServer, ObjectName obj,
//                                       String name, Object value)
//        throws MetricUnreachableException,
//               MetricNotFoundException,
//               PluginException,
//               ReflectionException,
//               InstanceNotFoundException,
//               MBeanException,
//               IOException {
//
//        if (name.startsWith("set")) {
//            name = name.substring(3);
//        }
//
//        Attribute attr = new Attribute(name, value);
//
//        try {
//            mServer.setAttribute(obj, attr);
//        } catch (AttributeNotFoundException e) {
//            throw new MetricNotFoundException(e.getMessage(), e);
//        } catch (InvalidAttributeValueException e) {
//            throw new ReflectionException(e);
//        }
//
//        return null;
//    }
//
//    public static Object invoke(Metric metric, String method,
//                                Object[] args, String[] sig)
//        throws MetricUnreachableException,
//               MetricNotFoundException,
//               PluginException {
//        try {
//            MBeanServerConnection mServer = getMBeanServerConnection(metric);
//            ObjectName obj = new ObjectName(metric.getObjectName());
//            MBeanInfo info = mServer.getMBeanInfo(obj);
//
//            if (sig.length == 0) {
//                MBeanUtil.OperationParams params =
//                    MBeanUtil.getOperationParams(info, method, args);
//                if (params.isAttribute) {
//                    return setAttribute(mServer, obj,
//                                        method, params.arguments[0]);
//                }
//                sig  = params.signature;
//                args = params.arguments;
//            }
//
//            return mServer.invoke(obj, method, args, sig);
//        } catch (NamingException e) {
//            throw unreachable(metric, e);
//        } catch (RemoteException e) {
//            throw unreachable(metric, e);
//        } catch (MalformedObjectNameException e) {
//            throw invalid(metric, e);
//        } catch (InstanceNotFoundException e) {
//            throw notfound(metric, e);
//        } catch (ReflectionException e) {
//            throw error(metric, e, method);
//        } catch (IntrospectionException e) {
//            throw error(metric, e);
//        } catch (MBeanException e) {
//            throw error(metric, e, method);
//        } catch (IOException e) {
//            throw error(metric, e);
//        }
//    }
//
//    public static Metric configureMetric(ControlPlugin plugin, String template) {
//        log.debug("[configureMetric] template = '"+template+"'");
//        template = JBoss5MeasurementPlugin.translateMetic(template, plugin.getConfig());
//        log.debug("[configureMetric] template = '"+template+"'");
//        String metric = Metric.translate(template, plugin.getConfig());
//
//        try {
//            return Metric.parse(metric); //parsing will be cached
//        } catch (Exception e) {
//            e.printStackTrace(); //XXX; but aint gonna happen
//            return null;
//        }
//    }
}
