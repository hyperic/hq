/**
 * NOTE: This copyright does *not* cover user programs that use Hyperic
 * program services by normal system calls through the application
 * program interfaces provided as part of the Hyperic Plug-in Development
 * Kit or the Hyperic Client Development Kit - this is merely considered
 * normal use of the program, and does *not* fall under the heading of
 * "derived work".
 *
 *  Copyright (C) [2010], VMware, Inc.
 *  This file is part of Hyperic.
 *
 *  Hyperic is free software; you can redistribute it and/or modify
 *  it under the terms version 2 of the GNU General Public License as
 *  published by the Free Software Foundation. This program is distributed
 *  in the hope that it will be useful, but WITHOUT ANY WARRANTY; without
 *  even the implied warranty of MERCHANTABILITY or FITNESS FOR A
 *  PARTICULAR PURPOSE. See the GNU General Public License for more
 *  details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program; if not, write to the Free Software
 *  Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307
 *  USA.
 *
 */
package org.hyperic.hq.plugin.gfee.log;

import java.io.IOException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.management.MBeanServerConnection;
import javax.management.Notification;
import javax.management.NotificationListener;
import javax.management.ObjectName;

import org.apache.commons.lang.ArrayUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hyperic.hq.plugin.gfee.GFMXConstants;
import org.hyperic.hq.plugin.gfee.mx.GFJmxConnection;
import org.hyperic.hq.product.LogTrackPlugin;
import org.hyperic.hq.product.PluginException;
import org.hyperic.hq.product.TrackEvent;
import org.hyperic.hq.product.TypeInfo;
import org.hyperic.util.config.ConfigOption;
import org.hyperic.util.config.ConfigResponse;

public class GFNotificationTrack extends LogTrackPlugin implements NotificationListener, Runnable {

    /** The Constant log. */
    private static final Log log = 
        LogFactory.getLog(GFNotificationTrack.class);
    
    /** Gemfire alert notification constant. */
    public final static String GF_NOTIFICATION_ALERT = "gemfire.distributedsystem.alert";
    
    /* Force settings */
    public final static String OPT_FORCE_ERROR = "force.error";
    public final static String OPT_FORCE_WARN = "force.warn";
    public final static String OPT_FORCE_INFO = "force.info";

    /** Log level map. */
    private static final String[] LOG_LEVELS = {
        "error,severe",     //Error
        "warning",          //Warning
        "info,config",      //Info
        "fine,finer,finest" //Debug
    };

    /* Patterns for force settings. */
    private Pattern forceErrorPattern = null;
    private Pattern forceWarnPattern = null;
    private Pattern forceInfoPattern = null;
    
    /** Cached connection to mbean server. */
    private MBeanServerConnection mServer = null;

    /** Bean used to check connection status. */
    private ObjectName gemFireAgentMBean = null;
    
    /** Timestamp when connections is last checked. */
    private long lastCheck = 0;

    @Override
    public void configure(ConfigResponse config) throws PluginException {
        if(log.isDebugEnabled()) {
            log.debug("[configure] config=" + config);
        }
        super.configure(config);
        
        try {
            gemFireAgentMBean = new ObjectName(GFMXConstants.AGENT_OBJ_NAME);
        } catch (Exception e) {
            throw new PluginException(e);
        }
        
        String eRegex = config.getValue(OPT_FORCE_ERROR, null);
        this.forceErrorPattern = (StringUtils.isBlank(eRegex) ? null : Pattern.compile(eRegex));
        String wRegex = config.getValue(OPT_FORCE_WARN, null);
        this.forceWarnPattern = (StringUtils.isBlank(wRegex) ? null : Pattern.compile(wRegex));
        String iRegex = config.getValue(OPT_FORCE_INFO, null);
        this.forceInfoPattern = (StringUtils.isBlank(iRegex) ? null : Pattern.compile(iRegex));

        subscribe();
        
        getManager().addRunnableTracker(this);
    }
    
    public String[] getLogLevelAliases() {
        return LOG_LEVELS;
    }
    
    protected ConfigOption getFilesOption(TypeInfo info, ConfigResponse config) {
        return null;
    }
    
    /*
     * (non-Javadoc)
     * @see javax.management.NotificationListener#handleNotification(javax.management.Notification, java.lang.Object)
     */
    public void handleNotification(Notification notification, Object handback) {
        String message = notification.getMessage();
        if (log.isDebugEnabled()) {
            log.debug("[handleNotification] notification.getType() => " + notification.getType() + 
                      " / notification.getMessage() => " + notification.getMessage() +
                      " / notification.getSource() => " + notification.getSource() +
                      " / notification.getSequenceNumber() => " + notification.getSequenceNumber());
        }

        if (GF_NOTIFICATION_ALERT.equals(notification.getType())) {
            String level = parseLogLevel(message);
            
            TrackEvent event =  newTrackEvent(notification.getTimeStamp(),
                                            forceLevel(message, level != null ? level : "info"),
                                            notification.getSource().toString(),
                                            message);
            if (event != null) {
                getManager().reportEvent(event);
            }            
        }
        
    }
    
    /**
     * Parsing log level from gemfire log message.
     * 
     * @param levelString Message to parse
     * @return Found log level, null if method failed to find anything or level
     *         is unknown or unsupported.
     */
    private String parseLogLevel(String levelString) {
        String[] fields = levelString.split(" ");
        
        if(fields.length == 0 || fields[0].length() < 2)
            return null;
        
        String level = fields[0].substring(1);

        if(log.isDebugEnabled())
            log.debug("parseLogLevel:"+levelString+"/"+level);
        
        if(ArrayUtils.contains(LOG_LEVELS, level)) {
            return level;
        } else {
            return null;
        }        
    }

    /**
     * This function checks if we need to force a change of
     * log level. Sometimes we only want to track error level
     * but actual error message is tagged with info for example.
     * 
     * @param message Message to check against force regex
     * @param level Original log level
     * @return New log level if matched against the rules. Returns
     *         original if match failed. 
     */
    private String forceLevel(String message, String level) {
        // we match in this order: error,warn,info

        if(forceErrorPattern != null) {
            Matcher m = forceErrorPattern.matcher(message);
            if(m.find())
                return "error";
        }

        if(forceWarnPattern != null) {
            Matcher m = forceWarnPattern.matcher(message);
            if(m.find())
                return "warning";
        }

        if(forceInfoPattern != null) {
            Matcher m = forceInfoPattern.matcher(message);
            if(m.find())
                return "info";
        }

        return level;
    }

    /**
     * Subscribes to alert notifications.
     * 
     * This method also caches mbean server connection what is
     * later used to check if mbean server is still available.
     */
    private void subscribe() {
        GFJmxConnection gf = new GFJmxConnection(getConfig());
        // just in case, unsubscribe first
        gf.removeAlertNotificationListener(this);
        mServer = gf.addAlertNotificationListener(this);
        if(mServer == null)
            log.info("Failed to subscribe to Gemfire alert notifications.");
        else
            log.debug("Successfully subscribed to Gemfire alert notifications.");

    }

    @Override
    public void run() {
        long now = System.currentTimeMillis();
        
        // check status once per minute
        if((lastCheck + 60000) > now)
            return;

        // if connection is null, try to subscribe
        if(mServer == null)
            subscribe();
        try {
            // if we don't get exception from this method,
            // we assume mbean server connection is ok.
            if(mServer != null)
                mServer.isRegistered(gemFireAgentMBean);
        } catch (IOException e) {
            // mbean server is not there...
            // set to null and wait next check cycle
            // to retry.
            mServer = null;
        }
        lastCheck = now;
    }

    @Override
    public void shutdown() throws PluginException {
        // we're done, remove runnable
        getManager().removeRunnableTracker(this);
        super.shutdown();
    }

    

}
