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

package org.hyperic.hq.ui.servlet;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hyperic.hq.ui.beans.ChartDataBean;
import org.hyperic.image.chart.Chart;
import org.hyperic.util.units.UnitsConstants;

import javax.servlet.ServletException;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import java.io.IOException;

/**
 * <p>This servlet returns a response that contains the binary data of
 * an image (JPEG or PNG) that can be viewed in a web browser.</p>
 *
 * <p>The chart servlet takes the following parameters (any applicable
 * defaults are in <b>bold</b> and required parameters are in
 * <i>italics</i>):</p>
 *
 * <table border="1">
 * <tr><th> key              </th><th> value                             </th></tr>
 * <tr><td> unitUnits        </td><td> &lt;integer <b>UNIT_NONE</b>&gt;  </td></tr>
 * <tr><td> unitScale        </td><td> &lt;integer <b>SCALE_NONE</b>&gt; </td></tr>
 * <tr><td> showPeak         </td><td> (<b>false</b> | true)             </td></tr>
 * <tr><td> showHighRange    </td><td> (<b>false</b> | true)             </td></tr>
 * <tr><td> showValues       </td><td> (false | <b>true</b>)             </td></tr>
 * <tr><td> showAverage      </td><td> (<b>false</b> | true)             </td></tr>
 * <tr><td> showLowRange     </td><td> (<b>false</b> | true)             </td></tr>
 * <tr><td> showLow          </td><td> (<b>false</b> | true)             </td></tr>
 * <tr><td> showBaseline     </td><td> (<b>false</b> | true)             </td></tr>
 * <tr><td> baseline*        </td><td> &lt;double&gt;                    </td></tr>
 * <tr><td> highRange*       </td><td> &lt;double&gt;                    </td></tr>
 * <tr><td> lowRange*        </td><td> &lt;double&gt;                    </td></tr>
 * </table>
 *
 * <p>* only used and required if corresponding <code>showXXX</code>
 * parameter is <code>true</code><br></p>
 *
 * <p>The <code>unitUnits</code> and <code>unitScale</code> parameters
 * must be valid integers from <code>{@link
 * org.hyperic.util.units.UnitsConstants}</code>.</p>
 *
 * @see org.hyperic.util.units.UnitsConstants
 */
public abstract class ChartServlet extends ImageServlet {
    /** Request parameter for unit scale. */
    public static final String UNIT_UNITS_PARAM = "unitUnits";
    /** Request parameter for unit scale. */
    public static final String UNIT_SCALE_PARAM = "unitScale";

    /** Default image width. */
    public static final int IMAGE_WIDTH_DEFAULT = 755;

    /** Default image height. */
    public static final int IMAGE_HEIGHT_DEFAULT = 300;

    /** Request parameter for whether or not to show the peak. */
    public static final String SHOW_PEAK_PARAM = "showPeak";

    /** Request parameter for whether or not to show high range. */
    public static final String SHOW_HIGHRANGE_PARAM = "showHighRange";

    /** Request parameter for whether or not to show the actual values. */
    public static final String SHOW_VALUES_PARAM = "showValues";

    /** Request parameter for whether or not to show average. */
    public static final String SHOW_AVERAGE_PARAM = "showAverage";

    /** Request parameter for whether or not to show low range. */
    public static final String SHOW_LOWRANGE_PARAM = "showLowRange";

    /** Request parameter for whether or not to show the low. */
    public static final String SHOW_LOW_PARAM = "showLow";

    /** Request parameter for whether or not to show baseline. */
    public static final String SHOW_BASELINE_PARAM = "showBaseline";

    /** Request parameter for baseline. */
    public static final String BASELINE_PARAM = "baseline";

    /** Request parameter for baseline. */
    public static final String HIGHRANGE_PARAM = "highRange";

    /** Request parameter for baseline. */
    public static final String LOWRANGE_PARAM = "lowRange";

    // member data
    private Log log = LogFactory.getLog( ChartServlet.class.getName() );

    private static ThreadLocal chartInfo = new ThreadLocal() {
      protected Object initialValue() {
          return new ChartInfo();
      }
    };

    private static class ChartInfo {
        private int unitScale;
        private int unitUnits;
        private boolean showPeak;
        private boolean showHighRange;
        private boolean showValues;
        private boolean showAverage;
        private boolean showLowRange;
        private boolean showLow;
        private boolean showBaseline;
        private double baseline;

        private double highRange;
        private double lowRange;
    }

    public ChartServlet () {}

    /**
     * Create the image being rendered.
     *
     * @param request the servlet request
     */
    protected Object createImage(HttpServletRequest request)
        throws ServletException {
        // initialize the chart
        Chart chart = createChart(request, null);
        initializeChart(chart, request);

        // the subclass is responsible for plotting the data
        log.debug("Plotting data.");
        
        try {
        	plotData(request, chart, null);
        } catch(Exception e) {
        	log.error("failed: ", e);
        }
        
        return chart;
    }

    /**
     * Render a PNG version of the image into the output stream.
     *
     * @param out the output stream
     */
    protected void renderPngImage(ServletOutputStream out, Object imgObj)
        throws IOException {
        Chart chart = (Chart) imgObj;
        chart.writePngImage(out);
    }

    /**
     * Render a JPEG version of the image into the output stream.
     *
     * @param out the output stream
     */
    protected void renderJpegImage(ServletOutputStream out, Object imgObj)
        throws IOException {
        Chart chart = (Chart) imgObj;
        chart.writeJpegImage(out);
    }

    /**
     * This method will be called automatically by the ChartServlet.
     * It should handle the parsing and error-checking of any specific
     * parameters for the chart being rendered.
     *
     * @param request the HTTP request object
     */
    protected void parseParameters(HttpServletRequest request) {
        // units / scale

        ChartInfo info = (ChartInfo)chartInfo.get();
        info.unitUnits = parseIntParameter( request, UNIT_UNITS_PARAM,
                                       getDefaultUnitUnits() );
        info.unitScale = parseIntParameter( request, UNIT_SCALE_PARAM,
                                       getDefaultUnitScale() );

        // chart flags
        info.showPeak = parseBooleanParameter( request, SHOW_PEAK_PARAM,
                                          getDefaultShowPeak() );
        info.showHighRange = parseBooleanParameter( request, SHOW_HIGHRANGE_PARAM,
                                               getDefaultShowHighRange() );
        info.showValues = parseBooleanParameter( request, SHOW_VALUES_PARAM,
                                            getDefaultShowValues() );
        info.showAverage = parseBooleanParameter( request, SHOW_AVERAGE_PARAM,
                                             getDefaultShowAverage() );
        info.showLowRange = parseBooleanParameter( request, SHOW_LOWRANGE_PARAM,
                                              getDefaultShowLowRange() );
        info.showLow = parseBooleanParameter( request, SHOW_LOW_PARAM,
                                         getDefaultShowLow() );
        info.showBaseline = parseBooleanParameter( request, SHOW_BASELINE_PARAM,
                                              getDefaultShowBaseline() );

        // baseline, high range and low range
        if (info.showBaseline) {
            try {
                info.baseline = parseRequiredDoubleParameter(request, BASELINE_PARAM);
            } catch (IllegalArgumentException e) {
                if ( log.isDebugEnabled() ) {
                    log.debug("invalid " + BASELINE_PARAM + ", setting " +
                              SHOW_BASELINE_PARAM + " to: " + false);
                }
                info.showBaseline = false;
            }
        }
        if (info.showHighRange) {
            try {
                info.highRange = parseRequiredDoubleParameter(request, HIGHRANGE_PARAM);
            } catch (IllegalArgumentException e) {
                if ( log.isDebugEnabled() ) {
                    log.debug("invalid " + HIGHRANGE_PARAM + ", setting " +
                              SHOW_HIGHRANGE_PARAM + " to: " + false);
                }
                info.showHighRange = false;
            }
        }
        if (info.showLowRange) {
            try {
                info.lowRange = parseRequiredDoubleParameter(request, LOWRANGE_PARAM);
            } catch (IllegalArgumentException e) {
                if ( log.isDebugEnabled() ) {
                    log.debug("invalid " + LOWRANGE_PARAM + ", setting " +
                              SHOW_LOWRANGE_PARAM + " to: " + false);
                }
                info.showLowRange = false;
            }
        }

        _logParameters();
    }

    /**
     * Create and return the chart.  This method will be called after
     * the parameters have been parsed.
     * @param request TODO
     * @param dataBean TODO
     * @return the newly created chart
     */
    protected abstract Chart createChart(HttpServletRequest request, ChartDataBean dataBean);

    /**
     * Initialize the chart.  This method will be called after the
     * parameters have been parsed and the chart has been created.
     *
     * @param chart the chart
     * @param request TODO
     */
    protected void initializeChart(Chart chart, HttpServletRequest request) {
        ChartInfo info = (ChartInfo)chartInfo.get();
        chart.setFormat(info.unitUnits, info.unitScale);
        chart.showPeak = info.showPeak;
        chart.showHighRange = info.showHighRange;
        chart.showValues = info.showValues;
        chart.showAverage = info.showAverage;
        chart.showLowRange = info.showLowRange;
        chart.showLow = info.showLow;
        chart.showBaseline = info.showBaseline;
        chart.baseline = info.baseline;
        chart.highRange = info.highRange;
        chart.lowRange = info.lowRange;
    }

    /**
     * This method will be called automatically by the ChartServlet.
     * It should handle adding data to the chart, setting up the X and
     * Y axis labels, etc.
     *
     * @param request the HTTP request
     * @param dataBean TODO
     */
    protected abstract void plotData(HttpServletRequest request, Chart chart,
                                     ChartDataBean dataBean)
        throws ServletException;

    /**
     * Return the value of property <code>showLow</code>.
     */
    public boolean getShowLow() {
        return ((ChartInfo)chartInfo.get()).showLow;
    }

    /**
     * Return the value of property <code>showPeak</code>.
     */
    public boolean getShowPeak() {
        return ((ChartInfo)chartInfo.get()).showPeak;
    }

    /**
     * Return the value of property <code>showAverage</code>.
     */    
    public boolean getShowAvg() {
        return ((ChartInfo)chartInfo.get()).showAverage;
    }

    /**
     * Return the default <code>unitUnits</code>.
     */
    protected int getDefaultUnitUnits() {
        return UnitsConstants.UNIT_NONE;
    }

    /**
     * Return the default <code>unitScale</code>.
     */
    protected int getDefaultUnitScale() {
        return UnitsConstants.SCALE_NONE;
    }

    /**
     * Return the default <code>imageWidth</code>.
     */
    protected int getDefaultImageWidth() {
        return IMAGE_WIDTH_DEFAULT;
    }

    /**
     * Return the default <code>imageHeight</code>.
     */
    protected int getDefaultImageHeight() {
        return IMAGE_HEIGHT_DEFAULT;
    }

    /**
     * Return the default <code>showPeak</code>.
     */
    protected boolean getDefaultShowPeak() {
        return false;
    }

    /**
     * Return the default <code>showHighRange</code>.
     */
    protected boolean getDefaultShowHighRange() {
        return false;
    }

    /**
     * Return the default <code>showValues</code>.
     */
    protected boolean getDefaultShowValues() {
        return true;
    }

    /**
     * Return the default <code>showAverage</code>.
     */
    protected boolean getDefaultShowAverage() {
        return false;
    }

    /**
     * Return the default <code>
     Range</code>.
     */
    protected boolean getDefaultShowLowRange() {
        return false;
    }

    /**
     * Return the default <code>showLow</code>.
     */
    protected boolean getDefaultShowLow() {
        return false;
    }

    /**
     * Return the default <code>showBaseline</code>.
     */
    protected boolean getDefaultShowBaseline() {
        return false;
    }

    //---------------------------------------------------------------
    //-- private helpers
    //---------------------------------------------------------------
    private void _logParameters() {
        if ( log.isDebugEnabled() ) {
            ChartInfo info = (ChartInfo)chartInfo.get();
            StringBuffer sb = new StringBuffer("Parameters:");
            sb.append("\n");sb.append("\t");
            sb.append(UNIT_UNITS_PARAM); sb.append(": "); sb.append(info.unitUnits);
            sb.append("\n");sb.append("\t");
            sb.append(UNIT_SCALE_PARAM); sb.append(": "); sb.append(info.unitScale);
            sb.append("\n");sb.append("\t");
            sb.append(SHOW_PEAK_PARAM); sb.append(": "); sb.append(info.showPeak);
            sb.append("\n");sb.append("\t");
            sb.append(SHOW_HIGHRANGE_PARAM); sb.append(": "); sb.append(info.showHighRange);
            sb.append("\n");sb.append("\t");
            sb.append(SHOW_VALUES_PARAM); sb.append(": "); sb.append(info.showValues);
            sb.append("\n");sb.append("\t");
            sb.append(SHOW_AVERAGE_PARAM); sb.append(": "); sb.append(info.showAverage);
            sb.append("\n");sb.append("\t");
            sb.append(SHOW_LOWRANGE_PARAM); sb.append(": "); sb.append(info.showLowRange);
            sb.append("\n");sb.append("\t");
            sb.append(SHOW_LOW_PARAM); sb.append(": "); sb.append(info.showLow);
            sb.append("\n");sb.append("\t");
            sb.append(SHOW_BASELINE_PARAM); sb.append(": "); sb.append(info.showBaseline);
            sb.append("\n");sb.append("\t");
            sb.append(BASELINE_PARAM); sb.append(": "); sb.append(info.baseline);
            sb.append("\n");sb.append("\t");
            sb.append(HIGHRANGE_PARAM); sb.append(": "); sb.append(info.highRange);
            sb.append("\n");sb.append("\t");
            sb.append(LOWRANGE_PARAM); sb.append(": "); sb.append(info.lowRange);
            log.debug( sb.toString() );
        }
    }
}

// EOF
