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

package org.hyperic.hq.product.servlet.filter;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.HashMap;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.UnavailableException;
import javax.servlet.http.HttpServletRequest;

import org.hyperic.hq.product.servlet.mbean.ContextInfo;
import org.hyperic.hq.product.servlet.mbean.ServletInfo;

/** 
 * Needs to be configured in each web application.
 */
public final class JMXFilter implements Filter {

    public static final String UNKNOWN_IP = "0.0.0.0";
    
    static HashMap filters = new HashMap();
    
    // Objects to tell us where to log URL data.
    private BufferedWriter logwriter;
    private RandomAccessFile logfile;
    private boolean logToDisk = false;
    
    private HashMap servlets = new HashMap();
    
    private JMXFilterInitServlet jmxFilterServlet;
    
    private ContextInfo jmxContextInfo = new ContextInfo();
    private String contextName;
    private ServletContext context;
    
    /* Default to 8k.  This is 1k smaller than the default in Java of 9k.
     * We do that because this way we control the flush, and we can gaurantee
     * that we won't write a partial line to the log.
     */
    private int bufferSize = 8 * 1024;
    private int maxLength  = 5 * 1024 * 1024; // Defalut to 5 MB
    private long bufferTime = 0;
    private long lastTime;
    private int written = 0;
    private FilterConfig filterConfig = null;

    //copied from StringUtil.replace
    public static String replace(String source, String find, String replace) {
        int findLen = find.length();

        StringBuffer buffer = new StringBuffer();

        int idx, fromIndex;
                
        for (fromIndex = 0;
             (idx = source.indexOf(find, fromIndex)) != -1;
             fromIndex = idx + findLen)
        {
            buffer.append(source.substring(fromIndex, idx));
            buffer.append(replace);
        }
        if (fromIndex == 0) {
            return source;
        }
        buffer.append(source.substring(fromIndex));

        return buffer.toString();
    }

    private int convertToBytes(String value)
    {
        char specifier = value.charAt(value.length() - 1);
        String number;
        if (Character.isDigit(specifier)) {
            number = value;
        } else {
            number = value.substring(0, value.length() - 1);
        }
        int bytes = Integer.parseInt(number);

        switch (Character.toUpperCase(specifier)) {
            case 'K':
                bytes *= 1024;
                specifier = 'B';
            case 'B':
            default:
                return bytes;
        }
    }

    private long convertToSeconds(String value)
        throws NumberFormatException
    {
        char specifier = value.charAt(value.length() - 1);
        String number;
        if (Character.isDigit(specifier)) {
            number = value;
        } else {
            number = value.substring(0, value.length() - 1);
        }
        long seconds = Long.parseLong(number);

        switch (Character.toUpperCase(specifier)) {
            case 'H':
                seconds *= 60;
                specifier = 'M';
            case 'M':
                seconds *= 60;
                specifier = 'S';
            case 'S':
            default:
                return seconds;
        }
    }

    public JMXFilterInitServlet getJmxFilterServlet() {
        return jmxFilterServlet;
    }

    public void setJmxFilterServlet(JMXFilterInitServlet jmxFilterServlet) {
        this.jmxFilterServlet = jmxFilterServlet;
    }

    public ServletContext getServletContext() {
        return context;
    }
    
    public ContextInfo getJMXContextInfo() {
        return jmxContextInfo;
    }
    
    public void init(FilterConfig filterConfig) throws ServletException
    {
    	this.filterConfig = filterConfig;
        jmxContextInfo.setAvailable(1);                        
        context = filterConfig.getServletContext();
        contextName = JMXFilterInitServlet.getName(context);
        
        JMXFilterInitServlet.registerFilter(this);

        // buffer size defaults to 8k unless otherwise specified
        String bufSize  = filterConfig.getInitParameter("bufferSize");
        if (bufSize == null) {
        	bufferSize = 8 * 1024;
        } else {
            bufferSize = convertToBytes(bufSize);
            if (bufferSize > 8 * 1024) {
                /* If the user specifies a value than 8k it won't have any
                 * effect, so log the message and force it to 8k.
                 */
                filterConfig.getServletContext().log("Buffer size specified " +
                                                     "greater than 8K " +
                                                     "limit, using 8K");
                bufferSize = 8 * 1024;
            } else {
                filterConfig.getServletContext().log("Setting buffer size to " +
                                                     bufferSize);
            }
        }

        // keeping this ghetto-assed bloom-ness for backward compatibility
        // here we enable rt logging based on the presence of a logFile directive
        String filename = filterConfig.getInitParameter("logFile");
        if (filename != null) {
            this.setLogWriter(filename);
        }

        // maximum size for our RT log files in bytes
        String maxLengthParam = filterConfig.getInitParameter("maxLength");
        if (maxLengthParam != null) {
            try {
                this.maxLength = Integer.parseInt(maxLengthParam);
            } catch (NumberFormatException e) {
                filterConfig.getServletContext().log("Invalid maxLength " +
                                                     "parameter: " +
                                                     maxLengthParam + " Using " +
                                                     "default.");
            }
        }

        // now for the new, righteous way of doing rt logging...
        // we'll define a logFile on a specific directory (which can be 
        // defined container wide) and have the name be derived from the
        // context name of  the active webapp.
        String rtLogDir = filterConfig.getInitParameter("responseTimeLogDir");

        if(rtLogDir == null && filename == null) {
            filterConfig.getServletContext().log("No responseTimeLogDir " +
                                                 "parameter specified. " +
                                                 "Response time logging " +
                                                 "will not be enabled.");
        } else {
            // allow the use of system properties in the initParameter
            // not sure how spec-friendly this is, but it is handy as hell
            if (rtLogDir.startsWith("$")) {
                String sysKey = rtLogDir.substring(rtLogDir.indexOf("{") +1, 
                                                   rtLogDir.lastIndexOf("}"));
                
                String sysProp = System.getProperty(sysKey);
                if(sysProp != null) {
                    rtLogDir = replace(rtLogDir, 
                                       "${" + sysKey + "}",
                                       sysProp);
                }
            }

            // Make the log dir available through JMX
            jmxContextInfo.setResponseTimeLogDir(rtLogDir);

            // the context name gets prepended with a // infront of it, 
            // trim that out unfortunately, jmx likes it that way
            String safeCtxName = contextName;
            if(contextName.startsWith("\\")) {
                safeCtxName = contextName.substring(contextName.
                                                    lastIndexOf("\\") + 1);
            }
            this.setLogWriter(rtLogDir + File.separator + 
                              safeCtxName + "_HQResponseTime.log");
        }

        String bufTime  = filterConfig.getInitParameter("bufferTime");
        if(bufTime == null) {
            // Buffer time defaults to 5 minutes.
            bufferTime = convertToSeconds("5m") * 1000;
            lastTime = System.currentTimeMillis();
        } 
        else {
            try { 
                bufferTime = convertToSeconds(bufTime);
                filterConfig.getServletContext().log("Setting buffer time " +
                                                     "limit to " + bufferTime + 
                                                     " seconds");
                // Need to use milliseconds for time limit internally
                bufferTime *= 1000;
                lastTime = System.currentTimeMillis();
                
            } catch (NumberFormatException e) {
                filterConfig.getServletContext().log("Invalid bufferTime " +
                                                     "parameter: " +
                                                     bufferTime + " Using " +
                                                     "default.");
            }
        }
    }
    
    private void setLogWriter(String filename) {
        // Handle context reloads.
        if (this.logwriter != null) {
            try {
                filterConfig.getServletContext().
                    log("Closing ResponseTime log: " + filename);
                this.logwriter.flush();
                this.logwriter.close();
            } catch (IOException ignore) {
            }
        }

    	try {
            logwriter = new BufferedWriter(new FileWriter(filename, true));
            logfile   = new RandomAccessFile(filename, "rw");
            logToDisk = true;
            filterConfig.getServletContext().log("Opening ResponseTime " +
                                                 "log: " + filename);
        } catch (IOException e) {
            filterConfig.getServletContext().log("Could not open " +
                                                 "ResponseTime log: " + 
                                                 filename + " " +
                                                "Please verify that the path " +
                                                "exists and is writable by " +
                                                "the user running the container");
        }
    }
    
    public void doFilter (ServletRequest request, ServletResponse response, 
                          FilterChain chain)
        throws IOException, ServletException
    {
        HttpServletRequest hRequest = (HttpServletRequest)request;
        HttpServletResponseExt eResponse = 
            new HttpServletResponseExt(response);
        HttpServletRequest hreq = (HttpServletRequest)request;
        int time = 0;
        int bytesReceived = 0;
        int bytesSent = 0;
        
        long t1 = 0, t2 = 0;
        Throwable error = null;
        try {
            t1 = System.currentTimeMillis();
            chain.doFilter(request, eResponse);
        } catch(Throwable t) {
            error = t;
        }

        t2 = System.currentTimeMillis();

        try {
            // HttpSession sess = hreq.getSession(false);
            // if( sess != null ) {
            //     String id=sess.getId();
            // }

            bytesReceived = hreq.getContentLength();
            if(bytesReceived < 0) bytesReceived = 0;

            bytesSent=eResponse.getContentLength();
            if(bytesSent < 0) bytesSent=0;
            
            time = (int)(t2 - t1);
            
            if (logToDisk) {
                // Before logging check the file size
                if (logfile.length() > this.maxLength) {
                    // Truncate.
                    context.log("Truncating HQ response time log");
                    logfile.setLength(0);
                }

                String ip = hreq.getRemoteAddr();
                if (ip == null) ip = UNKNOWN_IP;
                String q = hreq.getQueryString();
                String str = hreq.getServletPath() + 
                    ((q == null) ? "" : "?" + q) +
                    " " + Long.toString(t1) + " " + 
                    Long.toString(t2 - t1) + " " + 
                    eResponse.getStatus() + " " + ip;
                logwriter.write(str);
                logwriter.newLine();
                synchronized (logwriter) {
                    if (t2 - lastTime >= bufferTime) {
                        logwriter.flush();
                        lastTime = t2;
                    }
                    if (written > bufferSize) {
                        logwriter.flush();
                        written = 0;
                    } else {
                        written += str.length();
                    }
                }
            }
        } catch(Throwable t) {
            context.log("Error in filter " + t.toString());
        }

        // update per servlet data
        String key = hRequest.getServletPath();
        ServletInfo servletData = (ServletInfo)servlets.get(key);
        if(key != null && servletData == null) {
            //System.out.println("NEW URL " + jmxFilterServlet );

            // Only register servlet mbean if the response was 200
            if (eResponse.getStatus() != 200)
                return;

            servletData = new ServletInfo();
            // servletData.setName(key)
            servlets.put(key, servletData);
            if( jmxFilterServlet != null ) {
                jmxFilterServlet.registerServletMBean(contextName, key, 
                                                      servletData);
            } else {
                context.log("Can't register servlet " + key );
            }
        }

        servletData.updateTimes(time, (error != null),
                                bytesReceived, bytesSent);
        
        jmxContextInfo.updateCounters(bytesReceived, bytesSent, time,
                                      error != null);
        if( error == null ) return;
        
        if( error instanceof UnavailableException ) {
            servletData.setAvailable(0);
        }
        if( error instanceof IOException ) {
            throw (IOException)error;
        }
        if( error instanceof ServletException ) {
            throw (ServletException)error;
        }
        if( error instanceof RuntimeException ) {
            throw (RuntimeException)error;
        }
        throw new ServletException(error);
    }

    // It's really stupid, but the filter destoy method isn't called if the
    // app is shutdown.  In order for this method to be called, the server
    // itself must be stopped.  I don't think there is anything we can do
    // about that.
    public void destroy() {
        if (this.logwriter != null) {
            try {
                this.logwriter.flush();
                this.logwriter.close();
            } catch (IOException ignore) {
            }
        }                
        try {
            jmxContextInfo.setAvailable(0);
            if(logwriter != null)
                logfile.close();
            if(logfile != null)
                logfile.close();

        } catch (IOException e) { 
        }
    }

    // Weblogic 6.1 requires this, even though its not part of the spec
    public void setFilterConfig(FilterConfig f){ 
        try{init(f);} catch(Exception e){ e.printStackTrace();}
    }
    public FilterConfig getFilterConfig(){return null;}

}
