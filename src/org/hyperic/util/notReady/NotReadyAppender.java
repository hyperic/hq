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

package org.hyperic.util.notReady;

import org.apache.log4j.AppenderSkeleton;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.apache.log4j.Priority;
import org.apache.log4j.spi.LoggingEvent;

/**
 * Log4j appender to watch the startup process and report
 * progress through the NotReadyValve.
 *
 * Could extend this to display errors to the user.
 */
public class NotReadyAppender extends AppenderSkeleton {

    private String status    = "Booting HQ server...";
    private String lastError = "None";
    private int percent      = 0; 
    private int lastIndex    = -1;

    static final String ELIPSES = "...";
    static final ProgressIndicator[] INDICATORS
        = { new ProgressIndicator("Initializing Coyote", 3, 
                                  "Starting web server"),
            // RMI adaptor starts just before the lather sar.
            new ProgressIndicator("Started jboss.jmx:alias=jmx/rmi/RMIAdaptor", 
                                  8, "Starting HQ Agent listener"),
            new ProgressIndicator("Init J2EE application", 14, 
                                  "Deploying entity beans"),
            new ProgressIndicator("Deploying ControlManager", 18, 
                                  "Deploying session beans"),
            new ProgressIndicator("Mail Service bound to java:/SpiderMail", 21,
                                  "Starting mail service"),
            new ProgressIndicator("Started hyperic.jmx:type=Service," +
                                  "name=ProductPluginDeployerInit J2EE " +
                                  "application", 24,
                                  "Starting HQ plugin deployer"),
            new ProgressIndicator("license/license.xml", 31, 
                                  "Verifying HQ license"),
            new ProgressIndicator("Bound to JNDI name: topic/eventsTopic",
                                  36, "Starting event message queue"),
            new ProgressIndicator("Started jboss.j2ee:jndiName=LocalPlugin," +
                                  "plugin=pool,service=EJB", 39,
                                  "Starting entity beans"),
            new ProgressIndicator("Started jboss.j2ee:" +
                                  "jndiName=LocalControlManager," +
                                  "plugin=pool,service=EJB", 42, 
                                  "Starting session beans"),
            new ProgressIndicator("HQ plugin deployer ready", 45, 
                                  "Deploying HQ plugins")
        };

    public NotReadyAppender() {}

    protected void append(LoggingEvent event) {

        String message = event.getMessage().toString();
        Priority level = event.level;

        if (level.isGreaterOrEqual(Level.ERROR)) {
            this.lastError = message;
        }

        boolean foundStatus = false;
        for (int i=lastIndex+1; i<INDICATORS.length && i<lastIndex+6; i++) {
            if (message.indexOf(INDICATORS[i].match) != -1) {
                this.percent = INDICATORS[i].percent;
                this.status  = INDICATORS[i].status + ELIPSES;
                foundStatus = true;
                lastIndex = i;
                break;
            }
        }
        if (!foundStatus) {
            if (message.startsWith("HQ plugin") &&
                message.endsWith("deployed")) {
                // We currently deploy 27 plugins.. increment 2% for each.
                this.status  = message + ELIPSES;
                this.percent += 2;
                // Sanity check, will only happen if more plugins are added
                // without updating the startup monitor.
                if (this.percent >= 98) {
                    this.percent = 98;
                }
            } else if (message.indexOf("Started in") != -1) {
                this.status  = "Startup Complete";
                this.percent = 100;
            }
        }
    }

    public void close() {
    }

    public boolean requiresLayout() {
        return false;
    }

    public String getStatus() {
        return this.status;
    }

    public String getLastError() {
        return this.lastError;
    }

    public int getPercent() {
        return this.percent;
    }

    static class ProgressIndicator {
        public String match;
        public int percent;
        public String status;
        public ProgressIndicator (String match, int percent, String status) {
            this.match   = match;
            this.percent = percent;
            this.status  = status;
        }
    }
}
