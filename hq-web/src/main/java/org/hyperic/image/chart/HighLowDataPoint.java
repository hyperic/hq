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

import org.hyperic.util.data.IHighLowDataPoint;

/**
 *
 * TODO To change the template for this generated type comment go to
 * Window - Preferences - Java - Code Style - Code Templates
 */
public class HighLowDataPoint extends DataPoint implements IHighLowDataPoint
{
    private double m_high;
    private double m_low;

    public HighLowDataPoint(double high, double low, double avg) {
        super(avg, null);
        init(high, low);
    }
    
    public HighLowDataPoint(double high, double low, double avg, long timestamp) {
        super(avg, timestamp);
        init(high, low);
    }
    
    public HighLowDataPoint(double high, double low, double avg, String label) {
        super(avg, label);
        init(high, low);
    }

    private void init(double high, double low) {
        m_high = high;
        m_low  = low;
    }
    
    public double getAverageValue() {
        return this.getValue();
    }
    
    public double getLowValue() {
        return m_low;
    }
    
    public double getHighValue() {
        return m_high;
    }
    
    public double[] getValues() {
        double[] vals = new double[3];
        
        vals[0] = m_high;
        vals[1] = m_low;
        vals[2] = this.getValue();
        
        return vals;
    }
}
