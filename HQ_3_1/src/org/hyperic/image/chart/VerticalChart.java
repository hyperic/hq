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
import java.util.Collection;
import java.util.Iterator;

import org.hyperic.util.data.IDisplayDataPoint;
import org.hyperic.util.data.IEventPoint;
import org.hyperic.util.units.FormattedNumber;
import org.hyperic.util.units.UnitsConstants;
import org.hyperic.util.units.UnitsFormat;

public class VerticalChart extends Chart
{
    protected static final Color DEFAULT_COLOR = new Color(0x00, 0x00, 0xFF);
    
    protected static final Color GOOD_COLOR    // Green
        = new Color(0x48, 0xB3, 0x68);
    protected static final Color DANGER_COLOR  // Red
        = new Color(0xD5, 0x3E, 0x3E);
    protected static final Color UNKNOWN_COLOR // Grey
        = new Color(0x00, 0x00, 0xCC);
        
    private Rectangle m_rect;
    private long      m_timeScale;
    private int       m_cumulativeTrend = Trend.TREND_NONE;

    public VerticalChart() {
        super();
        init();
    }

    public int getCumulativeTrend() {
        return m_cumulativeTrend;
    }
    
    public void setCumulativeTrend(int trend) {
        if(trend < Trend.TREND_NONE || trend > Trend.TREND_UP)
            throw new IllegalArgumentException(
                          "Argument must be a Cumulative type.");
                          
        m_cumulativeTrend = trend;
    }
   
    protected VerticalChart(int width, int height) {
        super(width, height);
        init();
    }

    protected VerticalChart(int charts) {
        super(charts);
        init();
    }
    
    protected VerticalChart(int width, int height,int charts) {
        super(width, height, charts);
        init();
    }

    protected void init() {        
        this.showAverage    = true;
        this.showValueLines = true;
        this.showLow        = true;
        this.showPeak       = true;
    }
    
    protected Collection initData(Collection coll) {
        if( this.m_fmtType == UnitsConstants.UNIT_PERCENTAGE
        &&  (this.m_dLowValue >= 0 && this.m_dPeakValue <= 1) )
        {
            this.floor   = 0;
            this.ceiling = 1;
        }
                
        return coll;    
    }

    protected Point adjustBorders(Point pt) {
        if(pt != null) {        
            // Adjust to add the left and top margins to put in the interior rectangle
            pt.x += m_rect.x;
            pt.y += m_rect.y;
        }
        
        return pt;
    }

    protected Rectangle adjustRectangle(Graphics2D g, Rectangle rect) {
        int cDataPts = this.getDataPoints().size();
        int spread   = this.getUnitSpread(g, rect);
        rect.width   = (spread * (cDataPts - 1)) + (this.valueIndent * 2) +
                       this.lineWidth;
                       
        this.m_rect = rect;
        return rect;
    }

    protected Rectangle getInteriorRectangle(ChartGraphics g) {
        return m_rect;
    }
    
    protected String[] getXLabels() {
        DataPointCollection coll = this.getDataPoints();
        int collSize = coll.size();
        String[] result = new String[collSize];

        for (int i = 0; i < collSize; i++) {
            IDisplayDataPoint dp = (IDisplayDataPoint) coll.get(i); 
            result[i] = ScaleFormatter.formatTime(dp.getTimestamp(),
                                                  this.m_timeScale, collSize);
        }
        
        return result;
    }

    protected int[] getXPoints(ChartGraphics g, Rectangle rect) {
        DataPointCollection coll = this.getDataPoints();
        int collSize = coll.size();
        int[] res  = new int[collSize];
        int spread = this.getUnitSpread(g.graphics, this.getInteriorRectangle(g));
        int xHorzMarks = rect.x + this.valueIndent;

        for (int i = 0, x = xHorzMarks; i < collSize; i++, x += spread)
            res[i] = x;
            
        return res;
    }
    
    private int getUnitSpread(Graphics2D g, Rectangle rect) {
        int cDataPts = this.getDataPoints().size();
        int iSpread  = rect.width - (this.valueIndent * 2);
        
        return (cDataPts > 1) ? (iSpread / (cDataPts - 1)) : iSpread;
    }

    protected int getYLabelWidth(Graphics2D g) {
        FormattedNumber[] fmtValueLabels =
            UnitsFormat.formatSame(m_adRangeMarks, m_fmtType, m_fmtScale);

        int width;
        int maxWidth = 0;
        int cnt      = fmtValueLabels.length;

        for(int i = 1;i < cnt;i++) {
            width = this.m_metricsLabel.stringWidth(
                        fmtValueLabels[i].toString() );

            if(width > maxWidth)
                maxWidth = width;
        }

        return maxWidth;
    }

    protected Rectangle draw(ChartGraphics g) {
        ///////////////////////////////
        // Paint the chart background

        Rectangle rect = super.draw(g);

        if(this.hasData() == false) return rect;

        ///////////////////////////////////////
        // Paint the chart exterior and lines
        
        Graphics2D graph = g.graphics;
           
        // Calculate points
        double dScale = this.scale(rect.height);
        int lineWidth = this.lineWidth;

        int x2 = rect.x + rect.width;
        int y2 = rect.y + rect.height;
        
        int yAvgLine  = y2 - (int)Math.round(
            (this.getAverageValue() - this.m_floor) * dScale);
        int yLowLine  = y2 - (int)Math.round(
            (this.getLowValue() - this.m_floor) * dScale);
        int yBaseLine = y2 - (int)Math.round(
            (this.baseline - this.m_floor) * dScale);
        int yPeakLine = y2 - (int)Math.round(
            (this.getPeakValue() - this.m_floor) * dScale);

        int yHighBottom = 0;
        if(Double.isNaN(this.highRange) == false)            
            yHighBottom  = Math.min(y2 - lineWidth, y2 - (int)Math.round(
                                    (this.highRange - this.m_floor) * dScale));
            yHighBottom  = Math.max(yHighBottom, rect.y);
        
        int yLowTop = 0;
        if(Double.isNaN(this.lowRange) == false)
            yLowTop = Math.max(rect.y + lineWidth, y2 - (int)Math.round(
                                     (this.lowRange - this.m_floor) * dScale));
            yLowTop = Math.min(yLowTop, y2); 

        int xAvgLabel      = x2 - m_metricsLabel.stringWidth(Chart.AVG) - 3;
        int yAvgLabel      = yAvgLine - 3;
        Rectangle avgLabel = new Rectangle();
        if(this.showAverage)
            avgLabel.setRect(xAvgLabel, yAvgLabel,
                             m_metricsLabel.stringWidth(Chart.AVG),
                             m_metricsLabel.getHeight());

        int xBaselineLabel = x2 - m_metricsLabel.stringWidth(Chart.BASELINE) -
                             4;
        int yBaselineLabel      = yBaseLine - 3;
        Rectangle baselineLabel = new Rectangle();
        if(this.showBaseline)
            baselineLabel.setRect(xBaselineLabel, yBaselineLabel,
                                  m_metricsLabel.stringWidth(Chart.BASELINE),
                                  m_metricsLabel.getHeight());
        
        int xLowLabel      = x2 - m_metricsLabel.stringWidth(Chart.LOW) - 4;
        int yLowLabel      = yLowLine - 3;
        Rectangle lowLabel = new Rectangle();
        if(this.showLow)
            lowLabel.setRect(xLowLabel, yLowLabel,
                             m_metricsLabel.stringWidth(Chart.LOW),
                             m_metricsLabel.getHeight());
        
        int xPeakLabel      = x2 - m_metricsLabel.stringWidth(Chart.PEAK) - 4;
        int yPeakLabel      = yPeakLine - 3;
        Rectangle peakLabel = new Rectangle();
        if(this.showPeak)        
            peakLabel.setRect(xPeakLabel, yPeakLabel,
                              m_metricsLabel.stringWidth(Chart.PEAK),
                              m_metricsLabel.getHeight());                                          

        if(this.showTopLegend)
            g.drawYLegendString(this.getValueLegend());

        //////////////////////////////////////////////////////////
        // Draw the value (Y) axis cross lines and labels

        FormattedNumber[] fmtValueLabels =
            UnitsFormat.formatSame(m_adRangeMarks, m_fmtType, m_fmtScale);

        String[] labels = new String[this.m_adRangeMarks.length];
        int[]    lines  = new int[this.m_adRangeMarks.length];
        
        for(int i = 0;i < lines.length;i ++) {
            lines[i] = rect.y + (int)Math.round(
                            (this.m_adRangeMarks[i] - this.m_floor) * dScale );
                            
            labels[i] = fmtValueLabels[i].toString();
        }
        
        g.drawXLines(lines, labels, true);

        //////////////////////////////////////////////////////////
        // Draw the high range and low range

        boolean bHighLow = false;
        int     cxGuide  = lineWidth * 3;
        int     xGuide   = rect.x - cxGuide;
        
        if(this.showHighRange && Double.isNaN(this.highRange) == false
           && (yHighBottom > (rect.y + lineWidth)))
        {
            graph.setColor(this.highRangeColor);
            graph.fillRect(rect.x + lineWidth, rect.y + lineWidth,
                           rect.width - lineWidth, yHighBottom - rect.y);
                           
            graph.setColor(DANGER_COLOR);
            graph.fillRect(xGuide, rect.y + lineWidth, cxGuide,
                           yHighBottom - rect.y);
                           
            bHighLow = true;
        }
        
        if(this.showLowRange && Double.isNaN(this.lowRange) == false
           && (yLowTop < (y2 - lineWidth)))
        {
            graph.setColor(this.lowRangeColor);
            graph.fillRect(rect.x + lineWidth, yLowTop,
                           rect.width - lineWidth, y2 - yLowTop);

            graph.setColor(DANGER_COLOR);
            graph.fillRect(xGuide, yLowTop, cxGuide, y2 - yLowTop);
                           
            bHighLow = true;
        }

        if(bHighLow) {
            if(this.showHighRange == false)
                yHighBottom = rect.y + lineWidth;
            else {
                yHighBottom ++;
                if(this.showLowRange == false) yLowTop = y2;
            }
                				 
            graph.setColor(GOOD_COLOR);
            graph.fillRect(xGuide, yHighBottom, cxGuide, yLowTop - yHighBottom);
        }

        //////////////////////////////////////////////////////////
        // Draw the unit (X) axis tick marks and labels
        
        lines = this.getXPoints(g, rect);
        g.drawYLines(lines, this.getXLabels(), false, xLabelsSkip);

        
        //////////////////////////////////////////////////////////    
        // Draw Events
        
        if (this.showEvents) {
            if(this.getDataSetCount() == 1) {
                EventPointCollection collEvts = this.getEventPoints();
                if(collEvts.size() > 0){
                    int[] evtDataPts = getDataPointEventIndexes(0);
                
                    g.graphics.setColor(DEFAULT_COLOR);
                
                    for(int i = 0;i < evtDataPts.length;i ++) {
                        if(evtDataPts[i] == -1)
                            continue;
                        
                        IEventPoint evt = (IEventPoint)collEvts.get(i);                
                        g.drawEvent(evt.getEventID(), lines[ evtDataPts[i] ],
                                    y2 + ChartGraphics.HALF_EVENT_HEIGHT +
                                    this.lineWidth);
                    }
                }
            }
        }
        
        ////////////////////////////////////////////////////////////
        // Draw the Bottom Legend

        if(this.showBottomLegend)
            g.drawXLegendString(this.getUnitLegend());        

        //////////////////////////////////////////////////////////
        // Draw the Peak, Avg and Low Lines

        graph.setFont(this.font);
        
        int xLast = 0;
        
        if(this.showLow) {
            graph.setColor(this.lowLineColor);
            graph.drawLine(this.xVertMarks, yLowLine, this.x2VertMarks,
                                yLowLine);
            graph.drawString(Chart.LOW, xLowLabel, yLowLabel);
            
            xLast = xLowLabel;
        }

        if(this.showAverage) {
            if(avgLabel.intersects(lowLabel))
              xAvgLabel = xLast - this.m_metricsLabel.stringWidth(Chart.AVG) -
                          this.m_metricsLabel.charWidth('W'); 
            
            graph.setColor(this.averageLineColor);
            graph.drawLine(xVertMarks, yAvgLine, x2VertMarks, yAvgLine);
            graph.drawString(Chart.AVG, xAvgLabel, yAvgLabel);
            
            xLast = Math.min(xLast, xAvgLabel);
        }
        
        if(this.showPeak) {
            if(peakLabel.intersects(lowLabel)
            || peakLabel.intersects(avgLabel))
                xPeakLabel = xLast -
                             this.m_metricsLabel.stringWidth(Chart.PEAK) -
                             this.m_metricsLabel.charWidth('W'); 
                
            graph.setColor(this.peakLineColor);
            graph.drawLine(xVertMarks, yPeakLine, x2VertMarks, yPeakLine);
            graph.drawString(Chart.PEAK, xPeakLabel, yPeakLabel);
            
            xLast = Math.min(xLast, xPeakLabel);
        }

        if(this.showBaseline && yBaseLine > rect.y && yBaseLine < y2) {
            if(baselineLabel.intersects(lowLabel)
            || baselineLabel.intersects(avgLabel)
            || baselineLabel.intersects(peakLabel))
                xBaselineLabel = xLast -
                               this.m_metricsLabel.stringWidth(Chart.BASELINE) -
                               this.m_metricsLabel.charWidth('W'); 
            
            graph.setColor(this.baselineColor);
            graph.drawLine(xVertMarks, yBaseLine, x2VertMarks, yBaseLine);
            graph.drawString(Chart.BASELINE, xBaselineLabel, yBaselineLabel);
        }

        ///////////////////////////////////////////////////////////
        // Paint the chart interior
        
        if(this.showValues) this.paint(g, rect);

        return rect;
    }
    
    protected void paint(ChartGraphics graph, Rectangle rect) {
        // Subclasses will take care of the painting
    }

    protected int[] getDataPointEventIndexes(int dataSetNumber) {
        DataPointCollection  datapts  = this.getDataPoints(dataSetNumber);
        EventPointCollection collEvts = this.getEventPoints(dataSetNumber);
        Iterator             iterEvts = collEvts.iterator();
        int[]                tmp      = new int[collEvts.size()];
        int                  cActual  = 0;
                    
        for(int i = 0;iterEvts.hasNext();i++) {
            IEventPoint evt = (IEventPoint)iterEvts.next();
                
            int index = this.findDataPointIndex(evt.getTimestamp(), datapts);
            
//            if(index == -1)
//                continue;
                
            tmp[i] = index;
            cActual ++;
        }
        
        /*
        if(cActual == 0)
            return null;
        */
            
        // Compact the array and return it
        int[] res = new int[cActual];
        for(int i = 0;i < res.length;i++)
            res[i] = tmp[i];
            
        return res;
    }

    protected Point getDataPoint(Rectangle rect, int datapoint) { 
        return this.getDataPoint(rect, datapoint, this.getDataPoints());
    }

    protected Point getDataPoint(Rectangle rect, int datapoint,
                                 DataPointCollection coll) {
        Point ptResult =
            super.getDataPoint(rect.height, rect.width, datapoint, coll);
        
        if (ptResult != null)
            this.adjustBorders(ptResult);
        
        return ptResult;
    }
    
    protected void setTimeScale(long scale) {
        this.m_timeScale = scale;
    }
    
    protected int findDataPointIndex(long timestamp, DataPointCollection coll) {
        int collSize = coll.size();
        
        if(collSize == 0)
            return -1;

        long first = ((IDisplayDataPoint)coll.get(0)).getTimestamp();

        if(collSize == 1)
            return ( (first == timestamp) ? 0 : -1 );

        long second   = ((IDisplayDataPoint)coll.get(1)).getTimestamp();
        long interval = second - first;
        long prev     = first - interval;
        int  index;
        
        for (index = 0; index < collSize; index++) {
            IDisplayDataPoint datapt = (IDisplayDataPoint)coll.get(index);
            
            // Break if we find what we're looking for
            if(timestamp > prev && timestamp <= datapt.getTimestamp())
                break;

            // Prepare for the next loop
            prev = datapt.getTimestamp();
        }
            
        // Return a index for an actual point
        return ( (index == collSize) ? -1 : index );
    }
}
