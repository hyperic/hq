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

import java.awt.Point;
import java.awt.Rectangle;
import java.util.Iterator;

import org.hyperic.util.data.IDataPoint;

/**
 * HighLowChart draws a horizontal chart with shaded areas to display the data point values. For a description
 * of how to use AreaChart, see net.hyperic.chart.Chart.
 *
 * @see net.hyperic.chart.Chart
 */
public class AreaChart extends ColumnChart {
    public AreaChart() {
        super();
    }

    public AreaChart(int width, int height) {
        super(width, height);
    }

    protected void init() {
        super.init();
        this.valueIndent = 0;
    }
    
    protected void paint(ChartGraphics g, Rectangle rect) {
        g.graphics.setColor(this.columnColor);

        DataPointCollection coll = this.getDataPoints();
        Iterator            iter = coll.iterator();

        Rectangle rectBar = new Rectangle();
        rectBar.width     = rect.width / coll.size();

        for(int index = 0;iter.hasNext() == true;index ++) {
            IDataPoint datapt = (IDataPoint)iter.next();
            
            if(Double.isNaN(datapt.getValue()) == true) continue;
                
            Point ptData = this.getDataPoint(rect, index, coll);
            if(ptData == null) continue;

            rectBar.x      = ptData.x;
            rectBar.y      = ptData.y;
            rectBar.height = (rect.y + rect.height) - rectBar.y;

            // Draw Bar
            g.graphics.fillRect(rectBar.x, rectBar.y, rectBar.width, rectBar.height);
        }
    }
}
