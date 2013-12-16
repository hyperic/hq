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
package org.hyperic.hq.plugin.websphere;

import com.ibm.websphere.management.AdminClient;
import javax.management.ObjectName;
import javax.management.j2ee.statistics.Stats;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hyperic.hq.product.PluginException;

public class WebappCollector extends WebsphereCollector {

    private static final Log log = LogFactory.getLog(WebappCollector.class.getName());
    private ObjectName sessionStatsObjectName = null;
    private static final String[] sessionStatNames = {
        "CreateCount", "InvalidateCount", "LifeTime",
        "ActiveCount", "LiveCount", "NoRoomForNewSessionCount",
        "CacheDiscardCount", "ExternalReadTime", "ExternalReadSize",
        "ExternalWriteTime", "ExternalWriteSize", "AffinityBreakCount",
        "TimeSinceLastActivated", "TimeoutInvalidationCount",
        "ActivateNonExistSessionCount", "SessionObjectSize"
    };

    protected void init(AdminClient mServer) throws PluginException {
        String module = getModuleName();
        int ix = module.indexOf('#');
        if (ix == -1) {
            throw new PluginException("Malformed webapp name '" + module + "'");
        }
        String app = module.substring(0, ix);
        String war = module.substring(ix + 1);

        ObjectName name = newObjectNamePattern("j2eeType=WebModule,"
                + "J2EEApplication=" + app + ","
                + "name=" + war + ","
                + getProcessAttributes());

        setObjectName(resolve(mServer, name));

        // for session manager
        name = newObjectNamePattern("name="
                + getModuleName() + ","
                + "type=SessionManager,"
                + getProcessAttributes());

        // better catch this resolve, don't want to
        // fail if stats are not found.
        try {
            sessionStatsObjectName = resolve(mServer, name);
        } catch (PluginException e) {
            log.debug("Can't resolve session stats object name. "
                    + "Not collecting session statistics.");
        }
    }

    public void collect(AdminClient mServer) throws PluginException {
        Object servlets = getAttribute(mServer, getObjectName(), "servlets");
        if (servlets == null) {
            setAvailability(false);
        } else {
            setAvailability(true);
            if (sessionStatsObjectName == null) {
                init(mServer);
            }
            Stats stats = (Stats) getStats(mServer, sessionStatsObjectName);
            if (stats != null) {
                for (int i = 0; i < sessionStatNames.length; i++) {
                    setValue(sessionStatNames[i], getStatCount(stats, sessionStatNames[i]));
                }
            } else {
                log.debug("No session manager stats");
            }
        }
    }
}
