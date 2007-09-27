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
 * IDataPoint is an interface that is used to allow the chart to retrieve the
 * value and label of an individual data point. For line and column charts the
 * value is drawn on the Y axis and label is displayed on the X axis. Any
 * that are added to the chart datum collection must implement the IDataPoint
 * interface.
 */
public interface IDataPoint
{
    /**
     * Retrieves the value of a chart data point. The label is displayed on the
     * Y axis for line and column charts.
     *
     * @return A floating point value for a chart data point.
     */
    public double getValue();
}
