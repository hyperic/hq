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
package org.hyperic.hq.plugin.gfee.util;

import org.hyperic.hq.plugin.gfee.GFMXConstants;

public class GFUtils {

    /**
     * Region name from path.
     *
     * @param path the path
     * @return the string
     */
    public static String regionNameFromPath(String path) {
        int lDelim = path.lastIndexOf('/');
        return lDelim < 0 ? path : path.substring(lDelim+1);  
    }

    /**
     * Returns string describing bit mask flags.
     * 
     * @param mask bitmask
     * @return Debug string for bitmask status
     */
    public static String roleMaskToDebugString(int mask) {
        StringBuilder buf = new StringBuilder();
        buf.append("bitmask flags for ");
        buf.append(mask);
        buf.append(" - peer:");
        buf.append(((mask & GFMXConstants.MEMBER_ROLE_APPLICATIONPEER) == GFMXConstants.MEMBER_ROLE_APPLICATIONPEER));
        buf.append(" / ");
        buf.append("hub:");
        buf.append(((mask & GFMXConstants.MEMBER_ROLE_GATEWAYHUB) == GFMXConstants.MEMBER_ROLE_GATEWAYHUB));
        buf.append(" / ");
        buf.append("cs:");
        buf.append(((mask & GFMXConstants.MEMBER_ROLE_CACHESERVER) == GFMXConstants.MEMBER_ROLE_CACHESERVER));
        return buf.toString();
    }

}
