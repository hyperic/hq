/*
 * NOTE: This copyright does *not* cover user programs that use HQ
 * program services by normal system calls through the application
 * program interfaces provided as part of the Hyperic Plug-in Development
 * Kit or the Hyperic Client Development Kit - this is merely considered
 * normal use of the program, and does *not* fall under the heading of
 * "derived work".
 * 
 * Copyright (C) [2004 - 2008], Hyperic, Inc.
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
package org.hyperic.hq.ui.util;

import org.hyperic.hq.appdef.shared.AppdefEntityConstants;
import org.hyperic.hq.appdef.shared.AppdefResourceValue;
import org.hyperic.hq.ui.CSSConstants;

/**
 * Util Class for css related operations
 *
 */
public class CSSUtils {

    public static String getStyleClassByResourceType(AppdefResourceValue r) {
        switch (r.getAppdefResourceTypeValue().getAppdefType()) {
            case AppdefEntityConstants.APPDEF_TYPE_PLATFORM:
                return CSSConstants.PLATFORM;
            case AppdefEntityConstants.APPDEF_TYPE_SERVER:
                return CSSConstants.SERVER;
            case AppdefEntityConstants.APPDEF_TYPE_SERVICE:
                return CSSConstants.SERVICE;
            case AppdefEntityConstants.APPDEF_TYPE_GROUP:
                return CSSConstants.GROUP;
            case AppdefEntityConstants.APPDEF_TYPE_APPLICATION:
                return CSSConstants.APPLICATION;
            default:
                return CSSConstants.PLATFORM;
        }
    }
    
}
