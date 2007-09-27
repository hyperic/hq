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

package org.hyperic.hq.appdef.shared;

import org.hyperic.hq.product.PlatformDetector;

/**
 * Shared base class for the AIPlatform/AIIP/AIServer
 */
public abstract class AIAppdefResourceValue {

    public abstract Integer getId();

    public abstract int getQueueStatus();
    
    public abstract boolean getIgnored();
    
    /**
     * return a String form of the QueueStatus
     * 
     * @return String
     */
    public String getQueueStatusStr()
    {
        return AIQueueConstants.getQueueStatusString(getQueueStatus());
    }
    
    public boolean isPlatformDevice() {
        if (!(this instanceof AIPlatformValue)) {
            return false;
        }
        AIPlatformValue platform = (AIPlatformValue)this;
        //XXX add device flag to appdef schema, this works fine for now.
        return !PlatformDetector.isSupportedPlatform(platform.getPlatformTypeName());
    }
}
