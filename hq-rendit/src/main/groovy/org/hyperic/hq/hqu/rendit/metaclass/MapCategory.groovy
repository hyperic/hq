/**
 * NOTE: This copyright does *not* cover user programs that use HQ
 * program services by normal system calls through the application
 * program interfaces provided as part of the Hyperic Plug-in Development
 * Kit or the Hyperic Client Development Kit - this is merely considered
 * normal use of the program, and does *not* fall under the heading of
 *  "derived work".
 *
 *  Copyright (C) [2009-2010], VMware, Inc.
 *  This file is part of HQ.
 *
 *  HQ is free software; you can redistribute it and/or modify
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

package org.hyperic.hq.hqu.rendit.metaclass

class MapCategory {
    /**
     * Checks if the value of a key is non-null and an array of size 1 
     *
     * Useful for dealing with parameter checking in controllers
     */
    static boolean hasOne(Map m, String key) {
        m.get(key) != null && m.get(key).size() == 1 
    }

    /**
     * Gets the first element in an array of the value of a key, if it exists.
     *
     * I.e.:   [foo:[1, 2, 3]].getOne('foo') -> 1
     */
    static Object getOne(Map m, String key) {
        def res = m.get(key)
        if (res != null && res.size() > 0)
            return res[0]
        return null
    }
     
    /**
     * Get the first element in an array of the value of a key, else use
     * the passed default
     */
    static Object getOne(Map m, String key, String defalt) {
        Object res = getOne(m, key)
        if (res == null)
            return defalt
        return res
    }
}
