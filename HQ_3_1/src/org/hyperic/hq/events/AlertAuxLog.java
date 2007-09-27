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

package org.hyperic.hq.events;

import java.util.List;

/**
 * Contains auxillary information about why an alert fired.  
 */
public interface AlertAuxLog {
    /**
     * Get a text descritption of the log.
     */
    String getDescription();
    
    /**
     * Provide an optional URL which can be used to see more information
     * about the log.
     * 
     * This method should return a path after the regular web prefix 
     * (i.e. it will be prepended with http://localhost:7080/ or whatever
     * the context is)
     */
    String getURL();
    
    /**
     * Returns a list of {@link AlertAuxLog}s which are children of this
     * log.
     */
    List getChildren();
    
    void addChild(AlertAuxLog child);
    
    AlertAuxLogProvider getProvider();
    
    long getTimestamp();
}
