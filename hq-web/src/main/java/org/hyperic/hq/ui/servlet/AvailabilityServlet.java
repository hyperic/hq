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

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * An <code>HttpServlet</code> that performs an availability lookup
 * and returns the image data for the icon that represents the lookup
 * result.
 */
public class AvailabilityServlet extends AvailabilityBaseServlet {

    private static final String ICON_ERR_URL =
        "/images/icon_available_error.gif";

    private static final String ICON_AVAIL_URL =
        "/images/icon_available_green.gif";

    private static final String ICON_UNAVAIL_URL =
        "/images/icon_available_red.gif";

    private static final String ICON_WARNING_URL =
        "/images/icon_available_yellow.gif";

    private static final String ICON_PAUSED_URL =
        "/images/icon_available_orange.gif";

    private static final String ICON_POWERED_OFF_URL =
        "/images/icon_available_black.gif";

    private static final String[] iconUrls = {
        ICON_ERR_URL, ICON_AVAIL_URL, ICON_UNAVAIL_URL, ICON_WARNING_URL,
        ICON_PAUSED_URL, ICON_POWERED_OFF_URL };

    private static Log log =
       LogFactory.getLog(AvailabilityServlet.class.getName());

    protected String[] getIconUrls() {
        return iconUrls;
    }

    protected void sendAvailIcon(HttpServletRequest request,
                                 HttpServletResponse response) {
        try {
            sendIcon(request, response, ICON_AVAIL_URL);
        } catch (Exception e) {
            log.debug("can't send avail icon: ", e);
            sendErrorIcon(request, response);
        }
    }

    protected void sendUnavailIcon(HttpServletRequest request,
                                   HttpServletResponse response) {
        try {
            sendIcon(request, response, ICON_UNAVAIL_URL);
        } catch (Exception e) {
            log.debug("can't send unavail icon: ", e);
            sendErrorIcon(request, response);
        }
    }

    protected void sendWarningIcon(HttpServletRequest request,
                                   HttpServletResponse response) {
        try {
            sendIcon(request, response, ICON_WARNING_URL);
        } catch (Exception e) {
            log.debug("can't send unavail icon: ", e);
            sendErrorIcon(request, response);
        }
    }

    protected void sendErrorIcon(HttpServletRequest request,
                                 HttpServletResponse response) {
        try {
            sendIcon(request, response, ICON_ERR_URL);
            return;
        } catch (Exception e) {
            log.debug("can't send error icon: ", e);
        }
    
        // if we made it this far, we can't even get the error icon
        // sent. bail out with an internal server error.
    
        try {
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            response.flushBuffer();
        } catch (IOException e) {
            log.debug("can't commit server error response !? ", e);
        }
    }

    protected void sendPausedIcon(HttpServletRequest request,
                                  HttpServletResponse response) {
        try {
            sendIcon(request, response, ICON_PAUSED_URL);
        } catch (Exception e) {
            log.debug("can't send paused icon: ", e);
            sendErrorIcon(request, response);
        }
    }

    protected void sendPoweredOffIcon(HttpServletRequest request,
                                  HttpServletResponse response) {
        try {
            sendIcon(request, response, ICON_POWERED_OFF_URL);
        } catch (Exception e) {
            log.debug("can't send paused icon: ", e);
            sendErrorIcon(request, response);
        }
    }
}
