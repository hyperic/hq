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

import java.awt.Color;
import java.awt.Point;
import java.awt.Rectangle;
import java.util.Iterator;

import org.hyperic.util.data.IDataPoint;
import org.hyperic.util.data.IHighLowDataPoint;

/**
 * ColumnChart draws a chart with vertical bars that represent the value of
 * each data point. For a description of how to use ColumnChart, see
 * net.hyperic.chart.Chart.
 *
 * @see net.hyperic.chart.Chart
 */
public class ColumnChart extends VerticalChart
{
    protected static final int   DEFAULT_COLUMN_WIDTH = 5;
    protected static final Color DEFAULT_COLUMN_COLOR =
        new Color(0x66, 0x99, 0xFF);
    protected static final Color DEFAULT_HIGHLOW_COLOR =
        new Color(0x66, 0x66, 0x66);
    
    /**
     * Width of column bars.  
     */
    public int columnWidth = DEFAULT_COLUMN_WIDTH;
    
    /**
     * Color of column bars.  
     */
    public Color columnColor = DEFAULT_COLUMN_COLOR;

    /**
     * Determines whether cumulative data point calculations should be performed.  
     */
    public boolean isCumulative = false;

    
    public ColumnChart() {
        super();
    }

    public ColumnChart(int width, int height) {
        super(width, height);
    }
    
    protected void init() {
        super.init();
        this.columnWidth = DEFAULT_COLUMN_WIDTH;
        this.valueIndent = 8;
    }

    protected void paint(ChartGraphics g, Rectangle rect) {
        /////////////////////////////////////////////////////////
        // Draw the Column Bars

        // Calculate Bar Width
        int halfcol = this.columnWidth / 2;
        
        Rectangle rectBar = new Rectangle();
        rectBar.width     = this.columnWidth;

        DataPointCollection coll = this.getDataPoints();
        Iterator            iter = coll.iterator();
        
        for (int index = 0; iter.hasNext() == true; index++) {
            IDataPoint datapt = (IDataPoint)iter.next();
            
            Point ptData = this.getDataPoint(rect, index, coll);
            if (ptData == null)
                continue;

            rectBar.x      = ptData.x - halfcol;
            rectBar.y      = ptData.y;
            rectBar.height = (rect.y + rect.height) - rectBar.y;

            // Draw Bar
            g.graphics.setColor(this.columnColor);
            g.graphics.fillRect(rectBar.x, rectBar.y, rectBar.width, rectBar.height);
            
            if (datapt instanceof IHighLowDataPoint) {
                IHighLowDataPoint hlPt = (IHighLowDataPoint) datapt;
                
                // Make sure there's actually a range
                if (hlPt.getHighValue() == hlPt.getLowValue())
                    continue;
                    
                Point ptHigh = this.adjustBorders(
                        this.getDisplayPoint(rect.height, rect.width,
                                             coll.size(), hlPt.getHighValue(),
                                             index) );
                Point ptLow  = this.adjustBorders(
                        this.getDisplayPoint(rect.height, rect.width,
                                             coll.size(), hlPt.getLowValue(),
                                             index) );

                // Skip if the bars will not be clearly separated
                if (ptLow.y - ptHigh.y < 2)
                    continue;

                g.graphics.setColor(DEFAULT_HIGHLOW_COLOR);
                g.graphics.drawLine(ptLow.x,  ptLow.y, ptHigh.x,  ptHigh.y);

                ptHigh.x -= halfcol;
                ptLow.x  -= halfcol;
            
                // Draw the High, Low Lines
                g.graphics.drawLine(ptHigh.x,  ptHigh.y,
                                    ptHigh.x + rectBar.width - 1,  ptHigh.y);
                g.graphics.drawLine(ptLow.x,  ptLow.y,
                                    ptLow.x + rectBar.width - 1,  ptLow.y);
            }
        }
    }

    protected boolean checkHighLow() {
        return true;
    }
}
