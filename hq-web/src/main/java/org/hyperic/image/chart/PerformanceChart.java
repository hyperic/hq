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
import org.hyperic.util.data.IStackedDataPoint;
import org.hyperic.util.units.UnitsConstants;
import org.hyperic.util.units.UnitsFormat;
import org.hyperic.util.units.UnitNumber;

public class PerformanceChart extends HorizontalChart
{
    private static final Color[] DEFAULT_BAR_COLORS = {
        new Color(0x9B, 0xBA, 0x70),
        new Color(0x12, 0xB3, 0xB3),
        new Color(0xE7, 0x5A, 0x00),
    };

    protected static final int DEFAULT_BAR_HEIGHT = 7;
    private int     m_cyBar;
    private Color[] m_clrBars      = DEFAULT_BAR_COLORS;
    private boolean m_bHealthChart = true;

    public boolean showMinDigits = true;
    public boolean showStacked   = false;
                 
    public PerformanceChart() {
        super();
    }

    public PerformanceChart(int width, int height) {
        super(width, height);
    }

    protected void init() {
        this.m_cyBar = DEFAULT_BAR_HEIGHT;
        
        this.showFullLabels   = true;
        this.showRightLabels  = false;
        this.showLeftLegend   = false;
        this.showBottomLegend = false;
        this.valueIndent = (this.m_cyBar / 2) + this.lineWidth;
        this.valueLines = 5;
        
        this.setFormat(UnitsConstants.UNIT_DURATION,
                       UnitsConstants.SCALE_MILLI);
    }
    
    protected String[] getUnitLabels() {
        return HealthChart.getUnitStrings(this.getDataPoints(),
                                          this.m_bHealthChart);
    }

    protected String[] getXLabels() {
        if(this.m_adRangeMarks == null)
            return null;
        
        if(this.showMinDigits == true)
            return super.getXLabels();
                        
        String[] result = new String[m_adRangeMarks.length];
        
        for(int i = 0;i < m_adRangeMarks.length;i ++) {
            result[i] = UnitsFormat.format(
                            new UnitNumber(m_adRangeMarks[i], m_fmtType,
                                           m_fmtScale)).toString();    
        }

        return result;
    }

    protected void paint(ChartGraphics g, Rectangle rect) {
        super.paint(g, rect);
        
        /////////////////////////////////////////////////////////
        // Draw the Column Bars

        // Calculate Bar Width
        Rectangle rectBar = new Rectangle(
            rect.x + this.lineWidth,
            rect.y + this.lineWidth,
            0, this.m_cyBar );
            
        int cDataPoints = this.getDataPoints().size();
        
        if(cDataPoints == 0)
            return;
            
        int overhang = this.m_cyBar / 2;
        
        Iterator iter = this.getDataPoints().iterator();
        for(int i = 0;iter.hasNext() == true;i++) {
            IDataPoint datapt = (IDataPoint)iter.next();

            if(Double.isNaN(datapt.getValue()))
                continue;
                 
            Point ptData = this.getDataPoint(rect, i);
            if(ptData == null)
                continue;

            // Draw the max bar
            int cx = (ptData.x == rectBar.x ? 1 : ptData.x - rectBar.x + this.lineWidth);
            g.graphics.setColor(this.m_clrBars[2]);
            g.graphics.fillRect(rectBar.x, ptData.y - overhang, cx,
                                this.m_cyBar);
            
            if(this.showStacked == true &&
               datapt instanceof IStackedDataPoint &&
               ((IStackedDataPoint)datapt).getValues().length > 1)
            {
                IStackedDataPoint sdp   = (IStackedDataPoint)datapt;
                double            scale = this.scale(rect.width);
                
                double[] vals = sdp.getValues();

                if(sdp.getValues().length >= 2) {
                    double tmp = (scale * (vals[1] - this.m_floor));
                    cx = (int)Math.round(tmp) + xOffset;
                    if(cx == 0)
                        cx++;
                    g.graphics.setColor(this.m_clrBars[1]);
                    g.graphics.fillRect(rectBar.x, ptData.y - overhang + 2,
                                        cx, this.m_cyBar - 2);    
                }

                cx = (int)(scale * (vals[2] - this.m_floor));
                if(cx == 0)
                    cx++;
                    
                g.graphics.setColor(this.m_clrBars[0]);
                g.graphics.fillRect(rectBar.x, ptData.y - overhang + 2,
                                    cx, this.m_cyBar - 2);    
            }
        }
    }
    
    protected void setHealthChart(boolean health) {
        this.m_bHealthChart = health;
    }
}
