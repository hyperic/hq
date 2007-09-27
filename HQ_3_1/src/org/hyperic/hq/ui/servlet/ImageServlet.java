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

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * <p>This servlet returns a response that contains the binary data of
 * an image (JPEG or PNG) that can be viewed in a web browser.</p>
 *
 * <p>The navigation map servlet takes the following parameters (any
 * applicable defaults are in <b>bold</b> and required parameters are
 * in <i>italics</i>):</p>
 *
 * <table border="1">
 * <tr><th> key              </th><th> value                             </th></tr>
 * <tr><td> imageFormat      </td><td> (<b>png</b> | jpeg)               </td></tr>
 * <tr><td> imageWidth       </td><td> &lt;integer <b>(700)</b>&gt;      </td></tr>
 * <tr><td> imageHeight      </td><td> &lt;integer <b>(350)</b>&gt;      </td></tr>
 * </table>
 *
 */
public abstract class ImageServlet extends ParameterizedServlet {
    /** Request parameter for image format. */
    public static final String IMAGE_FORMAT_PARAM = "imageFormat";
    /** Request parameter value representing a PNG image. */
    public static final String IMAGE_FORMAT_PNG = "png";
    /** Request parameter value representing a JPEG image. */
    public static final String IMAGE_FORMAT_JPEG = "jpeg";
    private static final String[] VALID_IMAGE_FORMATS = {
        IMAGE_FORMAT_PNG,
        IMAGE_FORMAT_JPEG
    };

    /** Request parameter for image width. */
    public static final String IMAGE_WIDTH_PARAM = "imageWidth";
    /** Default image width. */
    public static final int IMAGE_WIDTH_DEFAULT = 700;

    /** Request parameter for image height. */
    public static final String IMAGE_HEIGHT_PARAM = "imageHeight";
    /** Default image height. */
    public static final int IMAGE_HEIGHT_DEFAULT = 350;

    // member data
    private Log log = LogFactory.getLog( ImageServlet.class.getName() );
    public ImageServlet () {}

    public void init() {
        if ( log.isDebugEnabled() ) {
            log.debug( "java.awt.headless=" +
                       System.getProperty("java.awt.headless") );
        }
    }

    public void doGet(HttpServletRequest request, HttpServletResponse response)
        throws ServletException
    {
        try {
            // parse the parameters
            log.debug("Parsing parameters.");
            
            parseParameters(request);
            
            // image format
            String imageFormat = parseStringParameter(request,
                                                      IMAGE_FORMAT_PARAM,
                                                      getDefaultImageFormat(),
                                                      VALID_IMAGE_FORMATS);
            if (log.isDebugEnabled()) {
                StringBuffer sb = new StringBuffer("Parameters:");
                sb.append("\n\t");
                sb.append(IMAGE_FORMAT_PARAM).append(": ").append(imageFormat);
                log.debug(sb.toString());
            }

            Object imgObj = createImage(request);
        
            // render the chart
            log.debug("Rendering image.");
            ServletOutputStream out = response.getOutputStream();
            if ( imageFormat.equals(IMAGE_FORMAT_PNG) ) {
                response.setContentType("image/png");
                renderPngImage(out, imgObj);
            } else {
                response.setContentType("image/jpeg");
                renderJpegImage(out, imgObj);
            }
            out.flush();
        } catch (IOException e) {
            // it's okay to ignore this one
            log.debug("Error writing image to response.", e);
        } catch (Exception e) {
            log.error("Unknown error.", e);
            throw new ServletException("Unknown error.", e);
        }
    }

    /**
     * Create the image being rendered.
     *
     * @param request the servlet request
     */
    protected abstract Object createImage(HttpServletRequest request) throws ServletException;

    /**
     * Render a PNG version of the image into the output stream.
     *
     * @param out the output stream
     */
    protected abstract void renderPngImage(ServletOutputStream out,
                                           Object imgObj) throws IOException;

    /**
     * Render a JPEG version of the image into the output stream.
     *
     * @param out the output stream
     */
    protected abstract void renderJpegImage(ServletOutputStream out,
                                            Object imgObj) throws IOException;

    /**
     * This method will be called automatically by the ImageServlet.
     * It should handle the parsing and error-checking of any specific
     * parameters for the chart being rendered.
     *
     * @param request the HTTP request object
     */
    protected abstract void parseParameters(HttpServletRequest request);

    /**
     * Return the image height.
     * @param request TODO
     *
     * @return the height of the image
     * @see <code>{@link IMAGE_HEIGHT_DEFAULT}</code>
     */
    protected int getImageHeight(HttpServletRequest request) {
        return parseIntParameter( request, IMAGE_HEIGHT_PARAM,
                                  getDefaultImageHeight() );
    }

    /**
     * Return the image width.
     * @param request TODO
     *
     * @return the width of the image
     * @see <code>{@link IMAGE_WIDTH_DEFAULT}</code>
     */
    protected int getImageWidth(HttpServletRequest request) {
        return parseIntParameter( request, IMAGE_WIDTH_PARAM,
                                  getDefaultImageWidth() );
    }

    /**
     * Return the default <code>imageFormat</code>.
     */
    protected String getDefaultImageFormat() {
        return IMAGE_FORMAT_PNG;
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
}

// EOF
