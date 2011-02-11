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
package com.vmware.springsource.hyperic.plugin.gemfire;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.management.MBeanServerConnection;
import javax.management.Notification;
import javax.management.NotificationListener;
import javax.management.ObjectName;
import org.apache.commons.logging.Log;
import org.hyperic.hq.product.LogTrackPlugin;

import org.hyperic.hq.product.PluginException;
import org.hyperic.hq.product.jmx.MxUtil;
import org.hyperic.util.config.ConfigResponse;

public class AlertsPlugin extends LogTrackPlugin implements NotificationListener {

    Log log = getLog();
    Pattern msgPatt = Pattern.compile("\\[(\\w*) *(\\d{4}/\\d{2}/\\d{2} \\d{2}:\\d{2}:\\d{2}.\\d{3} \\w*) *(\\w*) *([^\\]]*)] *(.*)");
    SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss.SSS z");

    @Override
    public void configure(ConfigResponse config) throws PluginException {
        log.debug("[configure] config=" + config);
        super.configure(config);
        MBeanServerConnection mServer;

        try {
            mServer = MxUtil.getMBeanServer(config.toProperties());
            ObjectName obj = new ObjectName("GemFire:type=MemberInfoWithStatsMBean");
            mServer.addNotificationListener(obj, this, null, null);
            log.debug("[configure] listener OK");
        } catch (Exception e) {
            throw new PluginException(e.getMessage(), e);
        }
    }

    public void handleNotification(Notification notification, Object handback) {
        if (log.isDebugEnabled()) {
            log.debug("[handleNotification] notification.getType() => " + notification.getType());
        }
        if ("gemfire.distributedsystem.alert".equals(notification.getType())) {
            if (log.isDebugEnabled()) {
                log.debug("[handleNotification] notification.getMessage() => " + notification.getMessage().trim());
            }
            Matcher m = msgPatt.matcher(notification.getMessage().trim());
            if (m.find()) {
                try {
                    String level = m.group(1);
                    Date date = dateFormat.parse(m.group(2));
                    String menberID = m.group(4);
                    String msg = m.group(5);
                    reportEvent(date.getTime(), LOGLEVEL_ERROR, menberID, msg);
                } catch (Exception ex) {
                    log.debug("[handleNotification] BAD FORMAT!!!! " + ex.getMessage(), ex);
                }
            } else {
                log.debug("[handleNotification] BAD FORMAT!!!!");
            }
        }
    }
}
