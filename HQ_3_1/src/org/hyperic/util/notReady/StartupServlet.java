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

package org.hyperic.util.notReady;

import java.io.PrintWriter;

import javax.servlet.ServletException;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.log4j.Logger;

/**
 * Servlet to display server startup status using a log4j appender
 */
public class StartupServlet extends HttpServlet {
    private NotReadyAppender _appender;

    public void init() throws ServletException {
        _appender = new NotReadyAppender();
        Logger.getRootLogger().addAppender(_appender);
    }

    public void destroy() {
    }

    public void doGet(HttpServletRequest request, HttpServletResponse response)
        throws ServletException
    {
        HttpServletResponse hres = (HttpServletResponse) response;
        StringBuffer sb = new StringBuffer();

        String status       = _appender.getStatus();
        String lastError    = _appender.getLastError();
        int percentComplete = _appender.getPercent();

        sb.append("<html>\n")
            .append("<head>\n")
            .append("<title>Hyperic HQ Starting: ")
            .append(percentComplete).append("%</title>\n")
            .append(REFRESH).append("\n\n").append(CSS)
            .append("</head>\n")
            .append("<body onload=\"doLoad()\" bgColor=\"#DBE3F5\">\n")
            .append("<center>\n")
            .append("<h1>Hyperic HQ is starting up, please wait.</h1>\n")
            .append("<table border=\"1\" width=\"80%\"><tr><td>\n")
            .append("    <table border=\"0\" width=\"")
            .append(percentComplete).append("%\"><tr><td class=\"progressBar\">")
            .append("&nbsp;</td></tr></table>\n")
            .append("</td></tr></table>\n")
            .append("<h2>").append(percentComplete).append("% complete.")
            .append("<br><br>Status: ").append(status)
            .append("<br><br>Last Error: ").append(lastError).append("</h2>")
            .append("<br><font class=\"refreshMsg\">")
            .append("This page will automatically refresh</font>\n")
            .append("</center>\n")
            .append("</body>\n")
            .append("</html>\n");

        response.setContentType("text/html");

        try {
            PrintWriter out = response.getWriter();
            out.print(sb.toString());
        } catch (Exception e) {
        }
    }            

    public static final String CSS
        = "\n\n<style type=\"text/css\">\n"
        + "\n  body { "
        + "\n    font-family: Verdana, Helvetica, Arial, sans-serif; "
        + "\n    color: black; "
        + "\n  }"
        + "\n  p { "
        + "\n    font-family: Verdana, Helvetica, Arial, sans-serif; "
        + "\n    color: black; "
        + "\n  }"
        + "\n  pre { "
        + "\n    font-family: Fixed, Clean, Courier New, Courier; "
        + "\n    color: black; "
        + "\n  }"
        + "\n  td { "
        + "\n    font-family: Verdana, Helvetica, Arial, sans-serif; "
        + "\n    color: black; "
        + "\n  }"
        + "\n  h1 { "
        + "\n    font-size: 180%;"
        + "\n    font-family: Verdana, Helvetica, Arial, sans-serif; "
        + "\n    color: black;"
        + "\n  }"
        + "\n  h2 {"
        + "\n    font-size: 120%;"
        + "\n    font-family: Verdana, Helvetica, Arial, sans-serif;"
        + "\n    color: black;"
        + "\n  }"
        + "\n  .progressBar { "
        + "\n    font-size: 180%;"
        + "\n    font-family: Verdana, Helvetica, Arial, sans-serif; "
        + "\n    color: white; background: blue;"
        + "\n  }"
        + "\n  .refreshMsg { "
        + "\n    font-size: 80%;"
        + "\n    font-family: Verdana, Helvetica, Arial, sans-serif; "
        + "\n    color: black;"
        + "\n  }"
        + "\n</style>";

    public static final String REFRESH
        = "\n  <noscript>"
        + "\n      <meta http-equiv=\"refresh\" content=\"5\">"
        + "\n  </noscript>"
        + "\n  <script language=\"JavaScript\">"
        + "\n  var sURL = unescape(window.location.pathname);"
        + "\n  function doLoad() {"
        + "\n     setTimeout( \"refresh()\", 5*1000 )"
        + "\n  }"
        + "\n  function refresh() {"
        + "\n     window.location.href = sURL"
        + "\n  }"
        + "\n  </script>"
        + "\n  <script language=\"JavaScript1.1\">"
        + "\n  function refresh() {"
        + "\n     window.location.replace(sURL);"
        + "\n  }"
        + "\n  </script>"
        + "\n  <script language=\"JavaScript1.2\">"
        + "\n  function refresh() {"
        + "\n      window.location.reload(true);"
        + "\n  }"
        + "\n  </script>";
}

