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

package org.hyperic.util.math;

import java.lang.reflect.Array;

/**
 * Sum returns the sum of its arguments.
 */
public class Sum {

    /**
     * Calculates the sum of values of Array of java.lang.Number objects that contain a double value.
     * @param Object Array reference
     * @return Double
     * @exception None - by design.
     */
    public static Double sum (Object values) {
        Double retVal =  new Double(0);
        try {
            double sum = 0D;
            for (int i=0;i<Array.getLength(values);i++) {
                sum += Array.getDouble (values,i);
            }
            retVal = new Double(sum);
        }
        catch (Exception e) {
            e.printStackTrace();
        }
        return retVal;
    }

    /**
     * Private constructor - not for instantiation.
     */
    private Sum () {}

}
