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

package org.hyperic.hq.product.jmx;

import java.util.Properties;

import javax.management.AttributeChangeNotification;
import javax.management.ListenerNotFoundException;
import javax.management.MBeanServerConnection;
import javax.management.MalformedObjectNameException;
import javax.management.Notification;
import javax.management.NotificationFilter;
import javax.management.NotificationListener;
import javax.management.ObjectName;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hyperic.hq.product.LogTrackPlugin;
import org.hyperic.hq.product.PluginException;
import org.hyperic.hq.product.TrackEvent;

public class MxNotificationListener implements NotificationListener {

    private LogTrackPlugin plugin;
    private Properties props;
    private String[] mbeans;
    private boolean isLogTrackEnabled = true;
    private boolean isConfigTrackEnabled = true;

    protected static Log log =
        LogFactory.getLog(MxNotificationListener.class.getName());

    private MxNotificationListener() { }
    
    public MxNotificationListener(LogTrackPlugin plugin) {
        this.plugin = plugin;
        this.props = plugin.getConfig().toProperties();
    }

    public static MxNotificationListener getInstance(MxNotificationPlugin plugin) {
        MxNotificationListener listener =
            new MxNotificationListener(plugin);

        listener.mbeans = plugin.getMBeans();

        return listener;
    }

    public NotificationFilter getFilter() {
        return null;
    }

    public Object getHandback() {
        return null;
    }

    private String[] translate(String[] mbeans) {
        String[] translated = new String[mbeans.length];

        for (int i=0; i<mbeans.length; i++) {
            translated[i] =
                MxUtil.expandObjectName(mbeans[i], this.props);
        }

        return translated;
    }

    public void add()
        throws PluginException {

        MBeanServerConnection mServer;

        try {
            mServer =
                MxUtil.getMBeanServer(this.props);
        } catch (Exception e) {
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
                log.debug("Added listener for: " + mbeans[i]);
            } catch (Exception e) {
                throw new PluginException("addNotificationListener(" +
                                          mbeans[i] + "): " +
                                          e.getMessage(), e);
            }
        }
    }

    public void remove()
        throws PluginException {

        MBeanServerConnection mServer;

        try {
            mServer =
                MxUtil.getMBeanServer(this.props);
        } catch (Exception e) {
            throw new PluginException("getMBeanServer(" + this.props + "): " +
                                      e.getMessage(), e);
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
            } catch (ListenerNotFoundException e) {
                log.warn(mbeans[i] + ": " + e.getMessage());
            } catch (Exception e) {
                throw new PluginException("removeNotificationListener(" +
                                          mbeans[i] + "): " +
                                          e.getMessage(), e);
            }
        }
    }

    public synchronized void handleNotification(Notification notification,
                                                Object handback) {

        String msg;
        boolean isAttrChange = notification instanceof AttributeChangeNotification;

        if (log.isDebugEnabled()) {
            log.debug(this.plugin.getName() +
                      " received notification: " + notification);
        }

        if (isAttrChange && this.isConfigTrackEnabled) {
            AttributeChangeNotification change = 
                (AttributeChangeNotification)notification;

            msg =
                "Attribute: " + change.getAttributeName() +
                " changed from " +
                change.getOldValue() + " to " + change.getNewValue();
        }
        else if (this.isLogTrackEnabled){
            msg = notification.getMessage();
        }
        else {
            return;
        }

        if (msg == null) {
            Object data = notification.getUserData();
            if (data != null) {
                msg = data.toString();
            }
            else {
                msg = notification.getType();
            }
        }

        long time     = notification.getTimeStamp();

        // Default level to INFO
        int level     = LogTrackPlugin.LOGLEVEL_INFO;

        // Check notification.getType() for Error, Warn, Info, Debug (case insensitive)
        String typeString = notification.getType();
        if(typeString != null) {
            if(typeString.equalsIgnoreCase(LogTrackPlugin.LOGLEVEL_ERROR_LABEL)) {
                level = LogTrackPlugin.LOGLEVEL_ERROR;
            } else if(typeString.equalsIgnoreCase(LogTrackPlugin.LOGLEVEL_WARN_LABEL)) {
                level = LogTrackPlugin.LOGLEVEL_WARN;
            } else if(typeString.equalsIgnoreCase(LogTrackPlugin.LOGLEVEL_DEBUG_LABEL)) {
                level = LogTrackPlugin.LOGLEVEL_DEBUG;
            }
        }

        String source = notification.getSource().toString();

        if (isAttrChange) {
            TrackEvent event = 
                new TrackEvent(this.plugin.getName(),
                               time,
                               level,
                               source,
                               msg);
            this.plugin.getManager().reportEvent(event);
        }
        else {
            //apply filters to msg
            this.plugin.reportEvent(time, level, source, msg);
        }
    }
}
