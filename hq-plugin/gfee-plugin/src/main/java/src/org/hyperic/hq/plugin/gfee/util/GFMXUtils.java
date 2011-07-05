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

import javax.management.MalformedObjectNameException;
import javax.management.ObjectName;

/**
 * The Class GFMXUtils.
 */
public class GFMXUtils {

    /** The Constant EMPTY_ARGS. */
    public final static Object[] EMPTY_ARGS = {}; 

    /** The Constant EMPTY_DEF. */
    public final static String[] EMPTY_DEF = {};

    public final static String KEY_ID = "id";

    public static String getId(ObjectName obj){
        return obj.getKeyProperty(KEY_ID);
    }

    public static String getField(ObjectName obj, String fName){
        return obj.getKeyProperty(fName);
    }


    public static ObjectName combine(ObjectName obj,String key, String value){
        StringBuilder b = new StringBuilder();
        b.append(obj.getCanonicalName());
        b.append(',');
        b.append(key);
        b.append('=');
        b.append(value);
        try {
            return new ObjectName(b.toString());
        } catch (MalformedObjectNameException e) {
            e.printStackTrace();
        } catch (NullPointerException e) {
            e.printStackTrace();
        }
        return null;
    }

}
