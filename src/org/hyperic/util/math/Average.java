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
 * Average implements general math functions for calculating averages.
 */
public class Average {

    /**
     * Calculates the average of a values Array of java.lang.Number objects that contain a double value.
     * @param Object Array reference
     * @return Double
     * @exception None - by design.
     */
    public static Double average (Object values) {
        Double retVal =  new Double(0);
        try {
            for (int i=0;i<Array.getLength(values);i++) {
                retVal = runningAverage (retVal,Array.get(values,i),i+1);
            }
        }
        catch (Exception e) {
            e.printStackTrace();
        }
        return retVal;
    }

    /**
     * @param The values Object array reference.
     * @param The weights Object array reference.
     * @return Double containing weigted average.
     */
    public static Double weightedAverage (Object values, Object weights) {
        Double retVal =  new Double(0);
        try {
            // First sanity check the weights - if they don't add up to 1
            // we should probably throw an exception.
            float wSum = 0f;
            for (int i=0;i<Array.getLength(weights);i++)
                wSum+=Array.getFloat(weights,i);
			// if (wSum != 0)  throw Something!

            for (int i=0;i<Array.getLength(values);i++) {
                retVal = runningAverage (retVal,Array.get(values,i),
                    i+1,  Array.getFloat(weights,i));
            }
        }
        catch (Exception e) {
            e.printStackTrace();
        }
        return retVal;
    }

    /**
     * @param Current average - Number with double value.
     * @param Next value - Number with double value.
     * @param Current index counter
     */
	public static Double runningAverage ( Object average, Object next, int count ){
        return runningAverage (average,next,count,1f);
    }

    /**
     * @param Current average - Number with double value.
     * @param Next value - Number with double value.
     * @param Current index counter
     * @param Weights for weighted average
     */
    public static Double runningAverage ( Object average, Object next, int count, float weight ){

        if (average == null || count == 1) {
            return new Double( weight * ((Number) next).doubleValue() );
        }
        else {
            double nextvalue = ((Number) next).doubleValue();
            double avg =  (  ((double) (count-1) / (double) count ) *
                             ((Number) average).doubleValue())      +
                          (weight* ((1.0 / (double) count)          *
                             (double) nextvalue            )        );
            return new Double(avg);
        }
    }

    /**
     * Private constructor - not for instantiation.
     */
    private Average () {}

}
