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

package org.hyperic.hq.plugin.weblogic.config;

import java.util.Properties;

import javax.management.AttributeChangeNotification;
import javax.management.InstanceNotFoundException;
import javax.management.ListenerNotFoundException;
import javax.management.MBeanServer;
import javax.management.MalformedObjectNameException;
import javax.management.Notification;
import javax.management.NotificationFilter;
import javax.management.ObjectName;

import org.hyperic.hq.product.LogTrackPlugin;
import org.hyperic.hq.product.Metric;
import org.hyperic.hq.product.MetricNotFoundException;
import org.hyperic.hq.product.MetricUnreachableException;
import org.hyperic.hq.product.PluginException;
import org.hyperic.hq.product.TrackEvent;
import org.hyperic.hq.product.TypeInfo;
import org.hyperic.hq.plugin.weblogic.WeblogicConfigTrackPlugin;
import org.hyperic.hq.plugin.weblogic.WeblogicProductPlugin;
import org.hyperic.hq.plugin.weblogic.WeblogicUtil;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import weblogic.management.RemoteNotificationListener;

public class WeblogicAttributeChangeListener
    implements RemoteNotificationListener {

    private static final String[] SERVER_MBEANS = {
        "%domain%:Location=%server%,Name=%server%,ServerConfig=%server%,Type=SSLConfig",
        "%domain%:Location=%server%,Name=%server%,Type=ServerConfig",
    };
    
    private static final String[] JDBC_MBEANS = {
        "%domain%:Name=%jdbc.conn%,Type=JDBCConnectionPool",      
    };

    private WeblogicConfigTrackPlugin plugin;
    private Properties props;
    private String[] mbeans;

    protected static Log log =
        LogFactory.getLog(WeblogicAttributeChangeListener.class.getName());

    private WeblogicAttributeChangeListener() { }
    
    public WeblogicAttributeChangeListener(WeblogicConfigTrackPlugin plugin) {
        this.plugin = plugin;
        this.props = plugin.getConfig().toProperties();
    }

    public static WeblogicAttributeChangeListener getInstance(WeblogicConfigTrackPlugin plugin) {
        WeblogicAttributeChangeListener listener =
            new WeblogicAttributeChangeListener(plugin);

        TypeInfo type = plugin.getTypeInfo();

        if (type.isServer(WeblogicProductPlugin.ADMIN_NAME) ||
            type.isServer(WeblogicProductPlugin.SERVER_NAME))
        {
            listener.mbeans = SERVER_MBEANS;
        }
        else if (type.isService(WeblogicProductPlugin.JDBC_CONN_NAME)) {
            listener.mbeans = JDBC_MBEANS;
        }
        else {
            throw new IllegalArgumentException(type.getName());
        }

        return listener;
    }

    public NotificationFilter getFilter() {
        return null;
    }

    public Object getHandback() {
        return null;
    }

    public void add()
        throws PluginException {

        MBeanServer mServer;

        try {
            mServer =
                WeblogicUtil.getMBeanServer(this.props);
        } catch (MetricUnreachableException e) {
            throw new PluginException(e.getMessage(), e);
        } catch (MetricNotFoundException e) {
            throw new PluginException(e.getMessage(), e);
        }            

        String[] mbeans = translate(this.mbeans);

        for (int i=0; i<mbeans.length; i++) {
            ObjectName obj;
            try {
                obj = new ObjectName(mbeans[i]);
            } catch (MalformedObjectNameException e) {
                //programmer error.
                throw new IllegalArgumentException(e.getMessage());
            }

            try {
                mServer.addNotificationListener(obj, this,
                                                getFilter(),
                                                getHandback());
                log.info("Added listener for: " + mbeans[i]);
            } catch (InstanceNotFoundException e) {
                throw new PluginException("InstanceNotFound: '" +
                                          mbeans[i] + "'", e);
            }
        }
    }

    public void remove()
        throws PluginException {

        MBeanServer mServer;

        try {
            mServer =
                WeblogicUtil.getMBeanServer(this.props);
        } catch (MetricUnreachableException e) {
            throw new PluginException(e.getMessage(), e);
        } catch (MetricNotFoundException e) {
            throw new PluginException(e.getMessage(), e);
        }            

        String[] mbeans = translate(this.mbeans);

        for (int i=0; i<mbeans.length; i++) {
            ObjectName obj;
            try {
                obj = new ObjectName(mbeans[i]);
            } catch (MalformedObjectNameException e) {
                //programmer error.
                throw new IllegalArgumentException(e.getMessage());
            }

            try {
                mServer.removeNotificationListener(obj, this);
                log.info("Removed listener for: " + mbeans[i]);
            } catch (InstanceNotFoundException e) {
                throw new PluginException(mbeans[i] + ": " +
                                                 e.getMessage(), e);
            } catch (ListenerNotFoundException e) {
                log.warn(mbeans[i] + ": " + e.getMessage());
            }
        }
    }

    public String[] translate(String[] mbeans) {
        String[] translated = new String[mbeans.length];

        for (int i=0; i<mbeans.length; i++) {
            translated[i] = Metric.translate(mbeans[i], this.props);
        }

        return translated;
    }

    public synchronized void handleNotification(Notification notification,
                                                Object handback) {

        if (!(notification instanceof AttributeChangeNotification)) {
            return;
        }

        AttributeChangeNotification change = 
            (AttributeChangeNotification)notification;

        String msg =
            change.getAttributeName() + " changed from " +
            change.getOldValue() + " to " + change.getNewValue();

        log.info(msg);

        TrackEvent event = 
            new TrackEvent(this.plugin.getName(),
                           System.currentTimeMillis(),
                           LogTrackPlugin.LOGLEVEL_INFO,
                           change.getSource().toString(),
                           msg);
                                              
        this.plugin.getManager().reportEvent(event);
    }
}
