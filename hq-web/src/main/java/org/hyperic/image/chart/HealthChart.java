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

package org.hyperic.image.chart;

import org.hyperic.util.data.IDisplayDataPoint;

public class HealthChart extends ColumnChart
{
    protected HealthChart(int width, int height) {
        super(width, height);
    }
    
    protected void init() {
        super.init();
        this.columnWidth = 11;
    }
    
    protected String[] getXLabels() {
        return HealthChart.getUnitStrings(this.getDataPoints(), true);
    }

    protected static String[] getUnitStrings(DataPointCollection datapts,
                                             boolean showHealthLabels)
    {
        String[] result = new String[datapts.size()];

        // Assume 8 hours
        double interval = 8.0 / result.length;
        for (int i = 0; i < result.length; i++) {
            if(showHealthLabels == true) {
                int remainder = result.length - i;
                
                if (remainder > 1) {
                    double time = remainder * interval;
                    String label = (time % 1 == 0) ?
                        String.valueOf((int) time) : String.valueOf(time);

                    result[i] = "-" + label + "hr";
                }
                else
                    result[i] = "Now";
            }
            else
                result[i] = ((IDisplayDataPoint)datapts.get(i)).getLabel();
        }

        return result;
    }
}
