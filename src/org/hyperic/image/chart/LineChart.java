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

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.Stroke;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import org.hyperic.util.data.IDataPoint;
import org.hyperic.util.data.IEventPoint;
import org.hyperic.util.units.UnitsConstants;
import org.hyperic.util.units.UnitsFormat;
import org.hyperic.util.units.UnitNumber;

/**
 * LineChart draws a horizontal chart with a line that represents data point
 * along the line. For a description of how to use LineChart, see
 * net.hyperic.chart.Chart.
 *
 * @see net.hyperic.chart.Chart
 */
public class LineChart extends VerticalChart
{
    private Color[] m_clrDataLines = VerticalChart.DEFAULT_COLORS;
    private boolean m_showLineEvents; 
    
    /**
     * Specified whether the data to be charted is cumulative data.
     */
    public boolean isCumulative = false;
    
    public LineChart() {
        super();
    }

    public LineChart(int charts) {
        super(charts);
    }

    public LineChart(int width, int height) {
        super(width, height);
    }

    public LineChart(int width, int height,int charts) {
        super(width, height, charts);
    }
    
    protected Rectangle draw(ChartGraphics g) {
        m_showLineEvents = this.showEvents;
        super.showEvents = false;
        Rectangle result = super.draw(g);
        this.showEvents  = m_showLineEvents;
        
        return result;
    }
    
    protected void paint(ChartGraphics g, Rectangle rect) {
        int yLabelEvtDot = rect.y + rect.height +
                           ChartGraphics.HALF_EVENT_HEIGHT + this.lineWidth;

        // Backup the current stroke and set the line width to 2 pixels
        Stroke origStroke = g.graphics.getStroke();
        BasicStroke stroke = new BasicStroke(2);
        g.graphics.setStroke(stroke);

        // Iterator through each data set
        Iterator iterLines = this.getDataSetIterator();
        for(int line = 0;iterLines.hasNext() == true;line++) {
            // Draw the Line
            DataPointCollection collDataPoints = 
                (DataPointCollection)iterLines.next();
    
            Point   ptData;
            int     cActualPts = 0;        
            int     cDataPts   = collDataPoints.size();
            int[]   aiX        = new int[cDataPts];
            int[]   aiY        = new int[cDataPts];
            int[]   yDataPt    = new int[cDataPts];
            long[]  timestamp  = new long[cDataPts];
            
            for(int index = 0;index < cDataPts;index ++) {
                ptData = this.getDataPoint(rect, index, collDataPoints);
                
                if(ptData != null) {
                    aiX[cActualPts] = ptData.x;
                    aiY[cActualPts] = ptData.y;
                    yDataPt[index]    = ptData.y;
                    cActualPts ++;
                } else {
                    yDataPt[index] = yLabelEvtDot;
                }
            }

            g.graphics.setColor(this.m_clrDataLines[line]);
            g.graphics.drawPolyline(aiX, aiY, cActualPts);
            
            // Draw Events
            if(m_showLineEvents == true) {
                EventPointCollection collEvts = this.getEventPoints(line);
                if(collEvts.size() > 0) {
                    int[] evtDataPts = this.getDataPointEventIndexes(line);
                    int[] x    = this.getXPoints(g, rect);

                    g.graphics.setColor(this.m_clrDataLines[line]);

                    for(int i = 0;i < evtDataPts.length;i ++) {
                        if(evtDataPts[i] == -1)
                            continue;
                    
                        IEventPoint evt   = (IEventPoint)collEvts.get(i);
                        int         index = evtDataPts[i];
                                    
                        g.drawEvent(evt.getEventID(), x[index], yDataPt[index]);
                    }
                }
            }
        }
        
        // Reset the stroke as it was when we were called
        g.graphics.setStroke(origStroke);
    }
    
    public Color getDataLineColor(int index) {
        return this.m_clrDataLines[index];
    }
    
    public void setDataLineColor(int index, Color color) {
        this.m_clrDataLines[index] = color;
    }
}
