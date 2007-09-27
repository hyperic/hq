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

package org.hyperic.util.data;

/**
 * IHighLowDataPoint is an interface that is used to allow the chart to
 * retrieve the high value and low value of an individual data point. This
 * interface extends IStackedDataPoint and must implement the getValues()
 * method from that interface by returning an array of three double values
 * with the high, low and average. The values can be placed in the array in
 * any order. IStackedDataPoint extends the IDisplayDataPoint interface. The
 * getValue method from that interface should return the average value for
 * the HighLow chart.
 * 
 * @see org.hyperic.util.data.IDisplayDataPoint
 * @see org.hyperic.util.data.IStackedDataPoint
 */
public interface IHighLowDataPoint extends IStackedDataPoint
{
    /**
     * Retrieves the high value of a chart data point.
     * @return A floating point value for a chart data point.
     */
    public double getHighValue();

    /**
     * Retrieves the low value of a chart data point.
     * @return A floating point value for a chart data point.
     */
    public double getLowValue();
}
