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

package org.hyperic.hq.grouping.util;

import org.hyperic.hq.authz.shared.ResourceGroupValue;
import org.hyperic.hq.appdef.shared.PlatformValue;
import org.hyperic.hq.appdef.shared.ServerValue;
import org.hyperic.hq.appdef.shared.ServiceValue;

public class CompatUtil {
        public static final int COMPAT_CLASS_GROUP     = 0;
        public static final int COMPAT_CLASS_PLATFORM  = 1;
        public static final int COMPAT_CLASS_SERVER    = 2;
        public static final int COMPAT_CLASS_SERVICE   = 3;

    /** Determines type of compatible valueobject and sets
     *  accordingly.
     *  @param valueobject
     *  @return type designator or -1 if not compatible.
     * */
    public static int determineCompatType (Object testable) {
        int type = -1;
        if ( testable instanceof ResourceGroupValue )
            type = COMPAT_CLASS_GROUP;
        else if ( testable instanceof PlatformValue )
            type = COMPAT_CLASS_PLATFORM;
        else if ( testable instanceof ServerValue )
            type = COMPAT_CLASS_SERVER;
        else if ( testable instanceof ServiceValue )
            type = COMPAT_CLASS_SERVICE;
        return type;
    }
    private CompatUtil() {}
}
