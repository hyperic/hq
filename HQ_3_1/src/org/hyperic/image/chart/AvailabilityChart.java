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
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Rectangle;
import java.util.Iterator;
import org.hyperic.util.data.IDataPoint;
import org.hyperic.util.units.UnitsConstants;

/**
 * AvailabilityChart is a stacked line chart where the stack is always made up
 * of two segments that add up to 100. For a description of how to use
 * AvailabilityChart, see net.hyperic.chart.Chart.
 *
 * @see net.hyperic.chart.Chart
 */
public class AvailabilityChart extends HealthChart
{
    private static final String DEF_UNIT = "%";
    
    private static final Color  DEF_AVAIL_COLOR
        = GOOD_COLOR;
    private static final Color  DEF_NOT_AVAIL_COLOR
        = DANGER_COLOR;

    public AvailabilityChart(int width, int height) {
        super(width, height);
    }

    protected void init() {        
        super.init();
        
        this.showAverage      = false;
        this.showFullLabels   = true;
        this.showRightLabels  = false;
        this.showLeftLegend   = false;
        this.showBottomLegend = false;
        this.showLow          = false;
        this.showPeak         = false;
        this.showValueLines   = true;
        
        this.floor   = 0;
        this.ceiling = 1;
        this.valueLines = 5;
        
        this.setFormat(UnitsConstants.UNIT_PERCENTAGE,
                       UnitsConstants.SCALE_NONE);
    }
    
    protected void paint(ChartGraphics g, Rectangle rect) {
        /////////////////////////////////////////////////////////
        // Draw the Column Bars

        // Calculate Bar Width
        int iNumPoints = this.getDataPoints().size();

        Rectangle rectBar     = new Rectangle();
        int       cDataPoints = this.getDataPoints().size();
        
        for(int i = 0;i < cDataPoints;i ++) {
            Point ptData = this.getDataPoint(rect, i);
            if(ptData == null)
                continue;
                
            double dVal = ((IDataPoint)this.getDataPoints().get(i)).getValue();

            rectBar.x      = ptData.x - (this.columnWidth / 2);
            rectBar.width  = this.columnWidth;
            rectBar.y      = ptData.y;
            rectBar.height = (rect.y + rect.height) - rectBar.y;

            // Draw the top bar in the stack
            if(dVal < 100) {
                g.graphics.setColor(DEF_NOT_AVAIL_COLOR);
                g.graphics.fillRect(rectBar.x, rect.y + this.lineWidth,
                                    rectBar.width, rectBar.y - rect.y);
            }

            // Draw the bottom bar in the stack
            if(dVal > 0) {
                g.graphics.setColor(DEF_AVAIL_COLOR);
                g.graphics.fillRect(rectBar.x, rectBar.y, rectBar.width,
                                    rectBar.height);
            }
        }
    }
}
