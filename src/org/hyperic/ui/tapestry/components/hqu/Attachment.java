/*
 * NOTE: This copyright does *not* cover user programs that use HQ
 * program services by normal system calls through the application
 * program interfaces provided as part of the Hyperic Plug-in Development
 * Kit or the Hyperic Client Development Kit - this is merely considered
 * normal use of the program, and does *not* fall under the heading of
 * "derived work".
 * 
 * Copyright (C) [2004 - 2008], Hyperic, Inc.
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
package org.hyperic.ui.tapestry.components.hqu;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.tapestry.IMarkupWriter;
import org.apache.tapestry.IRequestCycle;
import org.apache.tapestry.annotations.InjectMeta;
import org.apache.tapestry.annotations.Parameter;
import org.hyperic.ui.tapestry.components.BaseComponent;

/**
 *  Queries the specified URL and renders the returned markup.
 *  Used for rendering HQU Plugins
 *  
 *  TODO: add in an IFrame container option
 *
 */
public abstract class Attachment extends BaseComponent {
   
    private static Log log = LogFactory.getLog(Attachment.class);

    @InjectMeta("org.apache.tapestry.messages-encoding")
    public abstract String getEncoding();

    @Parameter(name = "pluginURL")
    public abstract void setPluginURL(String url);

    public abstract String getPluginURL();

    public void renderComponent(IMarkupWriter writer, IRequestCycle cycle) {
        // write the string version of the plugin markup to the response
        // builder.
        String url = getPluginURL();
        String pluginRender = renderPlugin(url);
        writer.printRaw(pluginRender);
        super.renderComponent(writer, cycle);
    }

    private String renderPlugin(String pluginURL) {
        String encoding = getEncoding();
        try {
            URL u = new URL(pluginURL);
            URLConnection uc = u.openConnection();
            InputStream i = uc.getInputStream();

            // okay, we've got a stream; encode it appropriately
            Reader r = null;
            String charSet = null;
            if (encoding != null && !encoding.equals("")) {
                charSet = encoding;
            } else {
                // charSet extracted according to RFC 2045, section 5.1
                String contentType = getRequest().getContentType();
                if (contentType != null)
                    charSet = contentType;
                else if (charSet == null)
                    charSet = uc.getContentType();
                else if (charSet == null)
                    charSet = "UTF-8";
            }
            try {
                r = new InputStreamReader(i, charSet);
            } catch (Exception ex) {
                r = new InputStreamReader(i, encoding);
            }

            // check response code for HTTP URLs before returning
            if (uc instanceof HttpURLConnection) {
                int status = ((HttpURLConnection) uc).getResponseCode();
                if (status < 200 || status > 299)
                    log.error("Bad return code from URLConnection: " + status
                            + " requesting: " + pluginURL);
            }

            return r.toString();
        } catch (IOException ex) {
            log.error(ex.getMessage());
            return "";
        } catch (RuntimeException ex) {
            log.error(ex.getMessage());
            return "";
        }
    }
}
