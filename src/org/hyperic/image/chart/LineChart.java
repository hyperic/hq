/*
 * NOTE: This copyright does *not* cover user programs that use HQ
 * program services by normal system calls through the application
 * program interfaces provided as part of the Hyperic Plug-in Development
 * Kit or the Hyperic Client Development Kit - this is merely considered
 * normal use of the program, and does *not* fall under the heading of
 * "derived work".
 * 
 * Copyright (C) [2004-2008], Hyperic, Inc.
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
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.Stroke;
import java.util.Iterator;

import org.hyperic.util.data.IEventPoint;

/**
 * LineChart draws a horizontal chart with a line that represents data point
 * along the line. For a description of how to use LineChart, see
 * net.hyperic.chart.Chart.
 *
 * @see net.hyperic.chart.Chart
 */
public class LineChart extends VerticalChart
{
    private boolean m_showLineEvents;
    
    private static final Color[] DEFAULT_COLORS = {
        DEFAULT_COLOR,
        new Color(0xFF, 0x00, 0x00), 
        new Color(0xCC, 0x00, 0x99),
        new Color(0x9B, 0xBA, 0x70),
        new Color(0xFF, 0xFF, 0x33),
        new Color(0x00, 0xFF, 0x00),
        new Color(0x00, 0xFF, 0xFF),
        new Color(0xA6, 0x78, 0x38),
        new Color(0x99, 0x66, 0x99),
        new Color(0x74, 0x90, 0xAA),
        new Color(0xE7, 0x5A, 0x00),
        new Color(0xB3, 0xA6, 0x36),
        new Color(0x11, 0xA6, 0x60),
        new Color(0x08, 0x99, 0x94),
        new Color(0x12, 0xB3, 0xB3),
        new Color(0x13, 0x7D, 0xBF),
        new Color(0x4A, 0x36, 0xB3),
        new Color(0x80, 0x00, 0xE8),
    };
    
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
        m_showLineEvents = showEvents;
        super.showEvents = false;
        Rectangle result = super.draw(g);
        showEvents  = m_showLineEvents;
        
        return result;
    }
    
    protected void paint(ChartGraphics g, Rectangle rect) {
        int yLabelEvtDot = rect.y + rect.height +
                           ChartGraphics.HALF_EVENT_HEIGHT + lineWidth;

        // Backup the current stroke and set the line width to 2 pixels
        Stroke origStroke = g.graphics.getStroke();
        BasicStroke stroke = new BasicStroke(2);
        g.graphics.setStroke(stroke);

        // Iterator through each data set
        Iterator iterLines = getDataSetIterator();
        for (int line = 0; iterLines.hasNext(); line++) {
            // Draw the Line
            DataPointCollection collDataPoints = 
                (DataPointCollection)iterLines.next();
    
            Point   ptData;
            int     cActualPts = 0;        
            int     cDataPts   = collDataPoints.size();
            int[]   aiX        = new int[cDataPts];
            int[]   aiY        = new int[cDataPts];
            int[]   yDataPt    = new int[cDataPts];
            for(int index = 0;index < cDataPts;index ++) {
                ptData = getDataPoint(rect, index, collDataPoints);

                if (ptData != null) {
                    aiX[cActualPts] = ptData.x;
                    aiY[cActualPts] = ptData.y;
                    yDataPt[index] = ptData.y;
                    cActualPts++;
                } else {
                    yDataPt[index] = yLabelEvtDot;
                }
            }

            g.graphics.setColor(getDataLineColor(line));
            g.graphics.drawPolyline(aiX, aiY, cActualPts);
            
            // Draw Events
            if(m_showLineEvents) {
                EventPointCollection collEvts = getEventPoints(line);
                if (collEvts.size() > 0) {
                    int[] evtDataPts = getDataPointEventIndexes(line);
                    int[] x = getXPoints(g, rect);

                    g.graphics.setColor(getDataLineColor(line));

                    for (int i = 0; i < evtDataPts.length; i++) {
                        if (evtDataPts[i] == -1)
                            continue;

                        IEventPoint evt = (IEventPoint) collEvts.get(i);
                        int index = evtDataPts[i];

                        g.drawEvent(evt.getEventID(), x[index], yDataPt[index]);
                    }
                }
            }
        }
        
        // Reset the stroke as it was when we were called
        g.graphics.setStroke(origStroke);
    }
    
    public Color getDataLineColor(int index) {
        return DEFAULT_COLORS[index];
    }
    
    public static int getNumColors() {
        return DEFAULT_COLORS.length;
    }
}
