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
import org.hyperic.image.WebImage;
import org.hyperic.image.widget.ResourceTree;

import javax.servlet.ServletException;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletRequest;
import java.io.IOException;

/**
 * <p>This servlet returns a response that contains the binary data of
 * an image (JPEG or PNG) that can be viewed in a web browser.</p>
 *
 * <p>The navigation map servlet takes the following parameters (any
 * applicable defaults are in <b>bold</b> and required parameters are
 * in <i>italics</i>):</p>
 *
 * <table border="1">
 * <tr><th> key              </th><th> value                </th></tr>
 * <tr><td> <i>treeVar</i>   </td><td> &lt;string&gt;       </td></tr>
 * </table>
 *
 */
public class NavMapImageServlet extends ImageServlet {
    /** Request parameter for the tree variable session attribute. */
    public static final String TREE_VAR_PARAM = "treeVar";

    /** Default image width. */
    public static final int IMAGE_WIDTH_DEFAULT = 800;

    // member data
    private Log log = LogFactory.getLog( NavMapImageServlet.class.getName() );
    private static ThreadLocal treeVar = new ThreadLocal() {
        protected Object initialValue() {
            return new String();
        }
    };
    public NavMapImageServlet () {}

    /**
     * Create the image being rendered.
     *
     * @param request the servlet request
     */
    protected Object createImage(HttpServletRequest request) throws ServletException {
        String tree = (String)treeVar.get();
        WebImage image =
            (ResourceTree)request.getSession().getAttribute(tree);
        request.getSession().removeAttribute(tree);
        return image;
    }

    /**
     * Render a PNG version of the image into the output stream.
     *
     * @param out the output stream
     */
    protected void renderPngImage(ServletOutputStream out, Object imgObj)
        throws IOException {
        WebImage image = (WebImage) imgObj;
        if (null != image) {
            image.writePngImage(out);
        }
    }

    /**
     * Render a JPEG version of the image into the output stream.
     *
     * @param out the output stream
     */
    protected void renderJpegImage(ServletOutputStream out, Object imgObj)
        throws IOException {
        WebImage image = (WebImage) imgObj;
        if (null != image) {
            image.writeJpegImage(out);
        }
    }

    /**
     * This method will be called automatically by the ChartServlet.
     * It should handle the parsing and error-checking of any specific
     * parameters for the chart being rendered.
     *
     * @param request the HTTP request object
     */
    protected void parseParameters(HttpServletRequest request) {
        // chart data key
        treeVar.set(parseRequiredStringParameter(request, TREE_VAR_PARAM));

        _logParameters();
    }

    /**
     * Return the default <code>imageWidth</code>.
     */
    protected int getDefaultImageWidth() {
        return IMAGE_WIDTH_DEFAULT;
    }

    //---------------------------------------------------------------
    //-- private helpers
    //---------------------------------------------------------------
    private void _logParameters() {
        if ( log.isDebugEnabled() ) {
            StringBuffer sb = new StringBuffer("Parameters:");
            sb.append("\n");sb.append("\t");
            sb.append(TREE_VAR_PARAM); sb.append(": "); sb.append(treeVar);
            log.debug( sb.toString() );
        }
    }
}

// EOF
