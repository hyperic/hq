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

package org.hyperic.hq.common.server.session;

import java.util.Properties;

/**
 *
 * This class helps to keep the properties in memory, rather than looking up
 * the EJBs all the time.  This is meant for only the configs that have
 * NULL as the prefix, which appears to be all of the configs at this point.
 */
public class ServerConfigCache {
    private Properties cache = null;
    
    private static ServerConfigCache singleton = new ServerConfigCache();
    
    private ServerConfigCache() { }     // Privatize the constructor
    
    public static ServerConfigCache getInstance() {
        return singleton;
    }
    
    public void setProperties(Properties props) {
        cache = props;
    }
    
    public Properties getProperties() {
        if (cache == null)
            throw new IllegalStateException(
                "Cannot return properties before server config cache has " +
                "been initialized");

        return cache;
    }
    
    public void clearProperties() {
        cache = null;
    }
}
