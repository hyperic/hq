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

import java.util.StringTokenizer;

import org.hyperic.hq.product.LogTrackPlugin;
import org.hyperic.hq.product.PluginException;
import org.hyperic.util.config.ConfigResponse;

public class MxNotificationPlugin
    extends LogTrackPlugin {

    public static final String PROP_NOTIFICATION_LISTENER_NAME =
        "NOTIFICATION_LISTENER_NAME";

    private MxNotificationListener listener = null;

    public void configure(ConfigResponse config)
        throws PluginException {

        super.configure(config);

        //no concept of log levels in JMX notifications
        setLogLevel(LOGLEVEL_INFO);

        this.listener =
            MxNotificationListener.getInstance(this);

        this.listener.add();
    }

    public String[] getMBeans() {
        String listenerName =
            getTypeProperty(PROP_NOTIFICATION_LISTENER_NAME);

        if (listenerName != null) {
            StringTokenizer tok =
                new StringTokenizer(listenerName, "\r\n");
            String[] mbeans = new String[tok.countTokens()];
            int i=0;
            while (tok.hasMoreTokens()) {
                mbeans[i++] = tok.nextToken().trim();
            }
            return mbeans;
        }

        String objectName = 
            getTypeProperty(MxQuery.PROP_OBJECT_NAME);
        if (objectName == null){
            return new String[0];
        }
        else {
            return new String[] { objectName };
        }
    }

    public void shutdown() throws PluginException {
        if (this.listener != null) {
            this.listener.remove();
            this.listener = null;
        }

        super.shutdown();
    }

    protected boolean supportsLogLevels() {
        return false;
    }
}
