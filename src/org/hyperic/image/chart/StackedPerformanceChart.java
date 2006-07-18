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
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Rectangle;
import java.util.Iterator;

public class StackedPerformanceChart extends HorizontalChart
{
    private static String REQUESTS  = "Requests: ";
    private static String SEMICOLON = ": ";
    private static String URL       = "URL: ";

    //private static final int DEFAULT_HEIGHT = 52;
    private static final int CHART_INDENT   = 25;
    
    private static final Font DEFAULT_TITLE_FONT =
        new Font("Helvetica", Font.BOLD, 11);
    private static final Font DEFAULT_TITLE_TEXT_FONT =
        new Font("Helvetica", Font.PLAIN, 11);

    private PerformanceChart m_perf;
    private int              m_interiorHeight;
     
    public StackedPerformanceChart() {
        this(Chart.DEFAULT_WIDTH, VARIABLE_HEIGHT);
    }

    public StackedPerformanceChart(int width, int charts) {
        this(width, VARIABLE_HEIGHT, charts);    
    }

    public StackedPerformanceChart(int width, int height,int charts) {
        super(width, Chart.VARIABLE_HEIGHT, charts);

        this.m_interiorHeight = height;                    
        m_perf = new PerformanceChart(width, 1);
        m_perf.valueIndent = 8;
        m_perf.valueLines = 10;
        m_perf.setValueLegend(Chart.DEFAULT_VALUE_LEGEND + " (Seconds)");
    }

    protected void init() {        
        super.init();
        
        this.showBottomLegend = false;
        this.showLeftLegend   = false;
        this.topBorder = 0;
        this.bottomBorder = 0;
    }
       
    protected int calcVariableHeight() {
        int height = 0;

        if(this.m_interiorHeight == Chart.VARIABLE_HEIGHT) {
            this.m_interiorHeight = ((PerformanceChart.DEFAULT_BAR_HEIGHT * 2) *
                                    this.getDataPoints().size()) +
                                    (this.lineWidth * 2);
        }
        
        // Iterator through each data set
        Iterator iterBars = this.getDataSetIterator();
        for(int line = 0;iterBars.hasNext() == true;line++) {
            // Calculate the height
            PerfDataPointCollection coll =
                (PerfDataPointCollection)iterBars.next();
            if(coll.size()== 0)
                continue;
            
            this.setChartProperties(m_perf, coll, line, this.getDataSetCount());
            if( line < (this.getDataSetCount() - 1) )
                height ++; 
            height += ( this.m_interiorHeight + m_perf.getExteriorHeight());
        }
        
        if(height == 0) {
            height = this.m_metricsLegend.getHeight();
            this.m_bNoData = true;
        }
            
        return height;
    }

    protected Class getDataCollectionClass() {
        return PerfDataPointCollection.class;
    }
    
    private int getTitleHeight(PerfDataPointCollection coll) {
        int cyText = m_metricsLegend.getHeight();
        int result = cyText; 
        
        if(coll.getURL() != null)
            result += cyText;

        if(coll.getTypeString().length() > 0)
            result += cyText;
            
        return result;
    }
    
    protected Rectangle draw(ChartGraphics g) {
        Rectangle rect = null;

        if(this.hasData() == false)
            return super.draw(g);
            
        // Iterator through each data set
        Iterator iterLines = this.getDataSetIterator();
        for(int line = 0;iterLines.hasNext() == true;line++) {
            // Draw the chart
            PerfDataPointCollection src =
                (PerfDataPointCollection)iterLines.next();
                
            if(src.size() == 0)
                continue;
        
            DataPointCollection dest = m_perf.getDataPoints();            
            dest.clear();
            dest.addAll(src);

            this.setChartProperties(m_perf, src, line, this.getDataSetCount());            
            m_perf.height = m_interiorHeight + m_perf.getExteriorHeight();
            
            ChartGraphics g2 = new ChartGraphics(m_perf, g.graphics);
            m_perf.floor   = this.m_adRangeMarks[0];
            m_perf.ceiling = this.m_adRangeMarks[this.m_adRangeMarks.length-1];
            m_perf.calcRanges(); 
            m_perf.calc(g2.graphics);
            m_perf.draw(g2);
           
            rect = m_perf.getExteriorRectangle();
            m_perf.yOffset += m_perf.height;

            // Draw titles
            this.drawTitles(g, src, rect); 
            
            g.graphics.setColor(this.xLineColor);
            g.graphics.drawLine(rect.x, m_perf.yOffset, rect.x + rect.width, m_perf.yOffset);
                                
            m_perf.yOffset += this.lineWidth;
        }
        
        return rect;
    }
    
    private void drawTitles(ChartGraphics g, PerfDataPointCollection coll,
                           Rectangle rect)
    {
        g.graphics.setColor(this.legendTextColor);

        FontMetrics metrics = g.graphics.getFontMetrics(DEFAULT_TITLE_FONT);
        int         x       = DEFAULT_BORDER_SIZE;
        int         cyTitle = this.getTitleHeight(coll);
        int         yTitle  = rect.y + metrics.getAscent();
                        
        String text = coll.getURL();
        if(text != null) {
            g.graphics.setFont(DEFAULT_TITLE_FONT);
            g.graphics.drawString(URL, x, yTitle);
            g.graphics.setFont(DEFAULT_TITLE_TEXT_FONT);
            g.graphics.drawString(coll.getURL(), x + metrics.stringWidth(URL), yTitle);
                              
            yTitle += this.m_metricsLabel.getHeight();
        }

        String title = coll.getTypeString();
        if(title.length() > 0) {
            title += SEMICOLON;
            g.graphics.setFont(DEFAULT_TITLE_FONT);
            g.graphics.drawString(title, x, yTitle);
                
            text = coll.getTypeName();
            if(text != null) {
                g.graphics.setFont(DEFAULT_TITLE_TEXT_FONT);
                g.graphics.drawString(coll.getTypeName(), x + metrics.stringWidth(title), yTitle);
            }
            
            yTitle += this.m_metricsLabel.getHeight();
        }
            
        g.graphics.setFont(DEFAULT_TITLE_FONT);
        g.graphics.drawString(REQUESTS, x, yTitle);
        g.graphics.setFont(DEFAULT_TITLE_TEXT_FONT);
        g.graphics.drawString(Integer.toString(coll.getRequest()),
                              x + metrics.stringWidth(REQUESTS), yTitle);
    }

    private void setChartProperties(Chart chart,
                   PerfDataPointCollection coll, int chartnum, int total)
    {
        if( chartnum == 0 ) { // First Chart
            m_perf.showTopLabels    = true;
            m_perf.showTopLegend    = true;
            m_perf.showBottomLabels = false;
            m_perf.showBottomLegend = false;
        }
        else if( chartnum < (this.getDataSetCount() - 1) ) { // Middle Chart
            m_perf.showTopLabels    = false;
            m_perf.showTopLegend    = false;
            m_perf.showBottomLabels = false;
            m_perf.showBottomLegend = false;
        }
        else { // Last Chart
            m_perf.showTopLabels    = false;
            m_perf.showTopLegend    = false;
            m_perf.showBottomLabels = true;
            m_perf.showBottomLegend = true;
        }
        
        m_perf.topBorder = this.getTitleHeight(coll);
        m_perf.leftBorder = CHART_INDENT;
        m_perf.setHealthChart(false);
        m_perf.showMinDigits = false;
        m_perf.showStacked   = true;
    }
}
