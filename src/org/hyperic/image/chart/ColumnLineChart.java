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
import java.awt.Rectangle;

/**
 * ColumnLineChart draws a chart with vertical bars that represent the value of
 * each data point with a connecting data points on top of the bars. For a
 * description of how to use ColumnChart, see net.hyperic.chart.Chart.
 *
 * @see net.hyperic.chart.Chart
 */
public class ColumnLineChart extends ColumnChart
{
    private final LineChart m_lineChart = new LineChart();

    /**
     * Constructs a ColumnLineChart class with a default width, height and
     * properties.
     */
    public ColumnLineChart() {}

    /**
     * Constructs a ColumnLineChart class with a specified width and height.
     *
     * @param width
     *      The width of the chart in pixels.
     * @param height
     *      The height of the chart in pixels.
     */
    public ColumnLineChart(int width, int height) {
        super(width, height);
    }

    protected Rectangle draw(ChartGraphics g) {
        Rectangle rect = super.draw(g);

        this.m_lineChart.width = this.width;
        this.m_lineChart.height = this.height;

        this.m_lineChart.getDataPoints().addAll(this.getDataPoints());
        this.m_lineChart.calcRanges();

        this.m_lineChart.paint(g, rect);

        return rect;
    }
    
    /**
     * Retrieves the color of the chart's datum line. This is the line that
     * represents the chart's data points.
     *
     * @return
     *      A java.awt.Color object that contains the datum line color.
     *
     * @see java.awt.Color
     */
    public Color getDataLineColor() {
        return this.m_lineChart.getDataLineColor(0);
    }
}