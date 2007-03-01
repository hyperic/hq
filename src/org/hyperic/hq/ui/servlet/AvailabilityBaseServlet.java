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

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.hyperic.hq.appdef.shared.AppdefEntityID;
import org.hyperic.hq.appdef.shared.AppdefEntityTypeID;
import org.hyperic.hq.bizapp.shared.MeasurementBoss;
import org.hyperic.hq.measurement.MeasurementConstants;
import org.hyperic.hq.ui.Constants;
import org.hyperic.hq.ui.exception.ParameterNotFoundException;
import org.hyperic.hq.ui.util.ContextUtils;
import org.hyperic.hq.ui.util.RequestUtils;
import org.hyperic.util.StringUtil;
import org.hyperic.util.file.FileUtil;
import org.hyperic.util.timer.StopWatch;

/**
 * Base class for availability servlets
 */
public abstract class AvailabilityBaseServlet extends HttpServlet {
    
    private static Log log =
        LogFactory.getLog(AvailabilityBaseServlet.class.getName());

    private Map iconData = new HashMap();

    public void init() {
        // read the icon bytes and cache them
        String[] urls = getIconUrls();
        for (int i = 0; i < urls.length; i++) {
            String url = urls[i];
            ByteArrayOutputStream bs = new ByteArrayOutputStream();
            try {
                FileUtil.copyStream(
                    getServletContext().getResourceAsStream(url), bs);
            } catch (IOException e) {
                log.debug("Could not load icon " + url, e);
            }
            iconData.put(url, new IconBytes(bs.toByteArray()));
        }
    }

    protected void doGet(HttpServletRequest request,
                         HttpServletResponse response)
        throws ServletException, IOException {
        try {
            int sessionId  = RequestUtils.getSessionId(request).intValue();
            AppdefEntityID[] eids = null;
            try {
                eids = RequestUtils.getEntityIds(request);
            } catch (ParameterNotFoundException e) {
                // okay, auto-group of platforms
            }
            
            AppdefEntityTypeID atid = null;
            String ctype = RequestUtils.getStringParameter(
                request, Constants.CHILD_RESOURCE_TYPE_ID_PARAM, null);
    
            if(ctype != null) {
                // looks like we got an autogroup
                atid = new AppdefEntityTypeID(ctype);
            }

            // Don't cache availability icons.
            RequestUtils.bustaCache(request, response);
    
            if (eids != null && log.isDebugEnabled())
                log.debug("Getting availability for resources [" +
                          StringUtil.arrayToString(eids) + "]");

            double val;
            StopWatch timer = new StopWatch();

            MeasurementBoss boss =
                ContextUtils.getMeasurementBoss(getServletContext());
            if (null == ctype) {
                val = boss.getAvailability(sessionId, eids[0]);
            } else {
                val = boss.getAGAvailability(sessionId, eids, atid);
            }

            if (log.isTraceEnabled()) {
                log.trace("Elapsed time: " + timer.getElapsed() + " ms");
            }

            if (val == MeasurementConstants.AVAIL_UNKNOWN) {
                sendErrorIcon(request, response);
            } else if (val == MeasurementConstants.AVAIL_DOWN) {
                sendUnavailIcon(request, response);
            } else if (val == MeasurementConstants.AVAIL_UP) {
                sendAvailIcon(request, response);
            } else if (val == MeasurementConstants.AVAIL_PAUSED) {
                sendPausedIcon(request, response);
            } else {
                sendWarningIcon(request, response);
            }
        } catch (Throwable t) {
            log.debug("Can't get availability measurement: ", t);
            sendErrorIcon(request, response);
            return;
        }
        sendAvailIcon(request, response);
    }

    protected abstract String[] getIconUrls();

    protected void sendIcon(HttpServletRequest request,
                            HttpServletResponse response, String url)
        throws IOException {
        response.setStatus(HttpServletResponse.SC_OK);
        response.setContentType("image/gif");
        IconBytes icon = (IconBytes) iconData.get(url);
        response.setContentLength(icon.getLength());
        response.getOutputStream().write(icon.getBytes());
        response.flushBuffer();
    }
    
    protected abstract void sendAvailIcon(HttpServletRequest request,
            HttpServletResponse response);

    protected abstract void sendUnavailIcon(HttpServletRequest request,
            HttpServletResponse response);

    protected abstract void sendWarningIcon(HttpServletRequest request,
            HttpServletResponse response);

    protected abstract void sendPausedIcon(HttpServletRequest request,
            HttpServletResponse response);

    protected abstract void sendErrorIcon(HttpServletRequest request,
            HttpServletResponse response);

    protected class IconBytes {
        private byte[] bytes;
    
        public IconBytes(byte[] theBytes) {
            bytes = theBytes;
        }
    
        public byte[] getBytes() {
            return bytes;
        }
    
        public int getLength() {
            return bytes.length;
        }
    }
}