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
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.Rectangle;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.text.NumberFormat;

import javax.imageio.ImageIO;

import org.hyperic.hq.context.Bootstrap;
import org.hyperic.util.data.IDataPoint;

public class AvailabilityReportChart extends Chart{
    private static int CIRCLE_SIZE = 11;
    
    private static BufferedImage GOOD_CIRCLE;
    private static BufferedImage DANGER_CIRCLE;
    private static BufferedImage UNKNOWN_CIRCLE;
    private static BufferedImage[] CIRCLES;

    private static final FontMetrics TEXT_METRICS;
    
    private static final Color COLOR_TRANSPARENT = new Color(241,243, 246);
    private static final Color TEXT_COLOR = Color.BLACK;
    
    private static final String LARGEST_NUMBER = "999";
    
    private static final int TEXT_BUFFER = 2;
    private static final int SET_BUFFER  = 5;

    private static final int CIRCLE_WITH_BUFFER_WIDTH;
    private static final int STANDARD_SET_WIDTH;
    private static final int IMAGE_WIDTH;
         
    static {
        // Get Font Metrics
        Image img = new BufferedImage(1, 1, BufferedImage.TYPE_BYTE_BINARY);
        java.awt.Graphics g = img.getGraphics();
        TEXT_METRICS  = g.getFontMetrics(DEFAULT_LEGEND_PLAIN);
        g.dispose();
        
        // Load Images        
        InputStream i;
        
        try {
            i = Bootstrap.getResource("images/icon_available_green.gif").getInputStream();
           
            GOOD_CIRCLE = ImageIO.read(i);
            i.close();
            
            i = Bootstrap.getResource("images/icon_available_red.gif").getInputStream();
          
            DANGER_CIRCLE = ImageIO.read(i);
            i.close();
            
            i = Bootstrap.getResource("images/icon_available_error.gif").getInputStream();
           
            UNKNOWN_CIRCLE = ImageIO.read(i);
            i.close();       
        } catch(IOException e) {
            System.out.println(e);
        }

        CIRCLES =
            new BufferedImage[] { GOOD_CIRCLE, DANGER_CIRCLE, UNKNOWN_CIRCLE };
        CIRCLE_WITH_BUFFER_WIDTH = GOOD_CIRCLE.getWidth() + TEXT_BUFFER;
        STANDARD_SET_WIDTH       = CIRCLE_WITH_BUFFER_WIDTH +
                                   TEXT_METRICS.stringWidth(LARGEST_NUMBER) +
                                   SET_BUFFER;
        IMAGE_WIDTH              = (STANDARD_SET_WIDTH * 3) - SET_BUFFER;                 
    }
    
    public AvailabilityReportChart() {
        super(IMAGE_WIDTH, GOOD_CIRCLE.getHeight());

        setBorder(0);
        //useIndexColors = true;
        indexColors = true;
    }
    
    protected void init() {
        showLeftLabels   = false;
        showBottomLabels = false;
        showLeftLegend   = false;
        showTopLegend    = false;
    }
    
    protected Rectangle draw(ChartGraphics g) {
        g.graphics.setFont(DEFAULT_FONT);

        int yCircle = 0;
        int y2Circle = CIRCLE_SIZE - 1;

        g.graphics.setPaint(COLOR_TRANSPARENT);
        g.graphics.fillRect(0, 0, width, height);

        DataPointCollection datapts = getDataPoints();
        NumberFormat fmt = NumberFormat.getInstance();
        fmt.setMaximumFractionDigits(0);

        g.graphics.setColor(TEXT_COLOR);

        for (int i = 0; i < datapts.size(); i++) {
            double val = ((IDataPoint) datapts.get(i)).getValue();

            if (val > 0) {
                g.graphics.drawImage(CIRCLES[i], STANDARD_SET_WIDTH * i,
                                     yCircle, COLOR_TRANSPARENT, null);
                g.graphics.drawString(fmt.format(val), STANDARD_SET_WIDTH * i +
                                      CIRCLE_WITH_BUFFER_WIDTH, y2Circle);
            }
        }
        
        return new Rectangle(0, 0, height, width);
    }
    
// protected IndexColorModel getIndexColorModel() {
// IndexColorModel cm = super.getIndexColorModel();
//
//        int size = cm.getMapSize();
//        byte r[] = new byte[size];
//        byte g[] = new byte[size];
//        byte b[] = new byte[size];
//        
//        cm.getReds(r);
//        cm.getGreens(g);
//        cm.getBlues(b);
//
//        // Make room by moving the first color to the end of the list
//        r[size-1] = r[0];
//        g[size-1] = g[0];
//        b[size-1] = b[0];
//        
//        // Set our transparent color as the first in the index
//        r[0] = (byte)COLOR_TRANSPARENT.getRed();
//        g[0] = (byte)COLOR_TRANSPARENT.getGreen();
//        b[0] = (byte)COLOR_TRANSPARENT.getBlue();
//        
//        return new IndexColorModel(8, size, r, g, b, 0);  
//    }
    
    protected int getYLabelWidth(Graphics2D g) { return 0; }
    
    protected Rectangle getInteriorRectangle(ChartGraphics g) {
        return new Rectangle(0, 0, width, height);
    }
    
    protected String[] getXLabels() { return null; }
}
