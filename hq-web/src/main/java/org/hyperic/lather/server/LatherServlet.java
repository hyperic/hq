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

package org.hyperic.lather.server;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.Semaphore;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hyperic.hq.autoinventory.ScanStateCore;
import org.hyperic.hq.bizapp.server.session.LatherDispatcher;
import org.hyperic.hq.bizapp.shared.lather.AiSendReport_args;
import org.hyperic.hq.bizapp.shared.lather.CommandInfo;
import org.hyperic.hq.context.Bootstrap;
import org.hyperic.lather.LatherContext;
import org.hyperic.lather.LatherRemoteException;
import org.hyperic.lather.LatherValue;
import org.hyperic.lather.client.LatherHTTPClient;
import org.hyperic.lather.xcode.LatherXCoder;
import org.hyperic.util.encoding.Base64;

/**
 * The purpose of this class is to take servlet requests for
 * remote execution, and translate them into service calls.
 *
 * The servlet understands POST requests which must have the
 * following data:
 *
 * method    = method name
 * args      = an encoded LatherValue object
 * argsClass = the class which the 'args' is encoded for
 *
 * The response is an encoded LatherValue object.
 */
@SuppressWarnings("serial")
public class LatherServlet extends HttpServlet {
    private static final AtomicLong ids = new AtomicLong();
    private static final String PROP_PREFIX = ConnManager.PROP_PREFIX;
    private static final String PROP_MAXCONNS = ConnManager.PROP_MAXCONNS;
    private static final String PROP_EXECTIMEOUT = PROP_PREFIX + "execTimeout";
    private static final String PROP_CONNID = PROP_PREFIX + "connID";

    private final Log log = LogFactory.getLog(LatherServlet.class);
   
    private Random       rand;
    private int          execTimeout;
    private static final AtomicReference<ConnManager> connManager = new AtomicReference<ConnManager>();

    private String getReqCfg(ServletConfig cfg, String prop) throws ServletException {
        String res;
        if ((res = cfg.getInitParameter(prop)) == null) {
            throw new ServletException("init-param '" + prop + "' not set");
        }
        return res;
    }

    public void init(ServletConfig cfg) throws ServletException {
        // Call super per the javadoc
        super.init(cfg);
        rand = new Random();
        if (connManager.get() == null) {
            connManager.compareAndSet(null, getConnManager(cfg));
        }
        execTimeout = Integer.parseInt(getReqCfg(cfg, PROP_EXECTIMEOUT));
    }

    private ConnManager getConnManager(ServletConfig cfg) throws ServletException {
        @SuppressWarnings("unchecked")
        final Enumeration<String> paramNames = cfg.getInitParameterNames();
        final Map<String, Semaphore> maxConnMap = new HashMap<String, Semaphore>();
        while (paramNames.hasMoreElements()) {
            final String name = paramNames.nextElement();
            if (!name.startsWith(PROP_PREFIX) || name.contains(PROP_EXECTIMEOUT)) {
                continue;
            }
            final String param = cfg.getInitParameter(name);
            try {
                final int value = Integer.parseInt(param);
                maxConnMap.put(name.replace(PROP_PREFIX, ""), new Semaphore(value));
            } catch (NumberFormatException e) {
                log.error("could not initialize max conn setting for " + name + " value=" + param);
            }
        }
        if (!maxConnMap.containsKey(PROP_MAXCONNS)) {
            throw new ServletException("init-params do not contain key=" + PROP_MAXCONNS + ")");
        }
        return new ConnManager(maxConnMap);
    }

    private static void issueErrorResponse(HttpServletResponse resp, String errMsg)
    throws IOException {
        resp.setContentType("text/raw");
        resp.setIntHeader(LatherHTTPClient.HDR_ERROR, 1);
        resp.getOutputStream().print(errMsg);
    }

    private static void issueSuccessResponse(HttpServletResponse resp, LatherXCoder xCoder, 
                                             LatherValue res)
    throws IOException {
        ByteArrayOutputStream bOs;
        DataOutputStream dOs;
        byte[] rawData;

        resp.setContentType("text/latherValue");
        resp.setHeader(LatherHTTPClient.HDR_VALUECLASS, 
                       res.getClass().getName());

        bOs = new ByteArrayOutputStream();
        dOs = new DataOutputStream(bOs);
        xCoder.encode(res, dOs);
        rawData = bOs.toByteArray();
        resp.getOutputStream().print(Base64.encode(rawData));
    }

    protected void service(HttpServletRequest req, HttpServletResponse resp)
    throws ServletException, IOException {
        final boolean debug = log.isDebugEnabled();
        boolean gotConn = false;
        int connRnd;
        connRnd = this.rand.nextInt();
        String method = req.getParameter("method");
        try {
            gotConn = connManager.get().grabConn(method);
            if (!gotConn) {
                final String msg = new StringBuilder(128)
                    .append("Denied request from ").append(req.getRemoteAddr())
                    .append(" availablePermits=").append(connManager.get().getAvailablePermits(method))
                    .append(", method=").append(method)
                    .toString();
                if (debug) log.debug(msg);
                resp.sendError(HttpServletResponse.SC_SERVICE_UNAVAILABLE, msg);
                return;
            }
            req.setAttribute(PROP_CONNID, new Integer(connRnd));
            if (debug) {
                log.debug("Accepting request from " + req.getRemoteAddr() +
                          " availablePermits=" + connManager.get().getAvailablePermits(method) +
                          " conID=" + connRnd);
            }
            super.service(req, resp);
        } finally {
            if (gotConn) {
                connManager.get().releaseConn(method);
                if (debug) log.debug("Releasing request from " +  req.getRemoteAddr() +
                                     " availablePermits=" + connManager.get().getAvailablePermits(method) +
                                     " connID=" + connRnd);
            }
        }
    }

    public void doPost(HttpServletRequest req, HttpServletResponse resp)
    throws ServletException, IOException {
        ByteArrayInputStream bIs;
        DataInputStream dIs;
        LatherXCoder xCoder;
        LatherValue val;
        String[] method, args, argsClass;
        LatherContext ctx;
        byte[] decodedArgs;
        Class<?> valClass;

        ctx = new LatherContext();
        ctx.setCallerIP(req.getRemoteAddr());
        ctx.setRequestTime(System.currentTimeMillis());

        xCoder    = new LatherXCoder();

        method = req.getParameterValues("method");
        args = req.getParameterValues("args");
        argsClass = req.getParameterValues("argsClass");

        if (method == null || args == null || argsClass == null ||
            method.length != 1 || args.length != 1 || argsClass.length != 1) {
            String msg = "Invalid Lather request made from " + req.getRemoteAddr();
            log.error(msg);
            resp.sendError(HttpServletResponse.SC_SERVICE_UNAVAILABLE, msg);
            return;
        }

        if (log.isDebugEnabled()) {
            log.debug("Invoking method '" + method[0] +
                      "' for connID=" +  req.getAttribute(PROP_CONNID));
        }

        try {
            valClass = Class.forName(argsClass[0], true, 
                                     xCoder.getClass().getClassLoader());
        } catch(ClassNotFoundException exc){
            String msg = "Lather request from " + req.getRemoteAddr() + 
                         " required an argument object of class '" +  argsClass[0] +
                         "' which could not be found";
            log.error(msg);
            resp.sendError(HttpServletResponse.SC_SERVICE_UNAVAILABLE, msg);
            return;
        }

        decodedArgs = Base64.decode(args[0]);
        bIs         = new ByteArrayInputStream(decodedArgs);
        dIs         = new DataInputStream(bIs);

        try {
            val = xCoder.decode(dIs, valClass);
        } catch(LatherRemoteException exc){
            LatherServlet.issueErrorResponse(resp, exc.toString());
            return;
        }

        this.doServiceCall(req, resp, method[0], val, xCoder, ctx);
    }

    private class ServiceCaller 
        extends Thread
    {
        private HttpServletResponse resp;
        
        private LatherXCoder        xcoder;
        private LatherValue arg;
        private LatherContext ctx;
        private String method;
        private Log                 log;
        private LatherDispatcher latherDispatcher;
        

        private ServiceCaller(HttpServletResponse resp, 
                          LatherXCoder xcoder, LatherContext ctx, String method, LatherValue arg, 
                           Log log, LatherDispatcher latherDispatcher)
        {
            super(method + "-" + ids.getAndIncrement());
            this.resp    = resp;
           
            this.xcoder  = xcoder;
            this.ctx = ctx;
            this.method = method;
            this.arg = arg;
            this.log     = log;
            this.latherDispatcher = latherDispatcher;
        }
                          
        private void doInvoke()
            throws IOException 
        {
            LatherValue res;

            try {
                res = latherDispatcher.dispatch(ctx, method, arg);

                if (CommandInfo.CMD_AI_SEND_REPORT.equals(method)) {
                    handleAutoApprovals(arg);
                }

                issueSuccessResponse(this.resp, this.xcoder, res);
            }  catch(IllegalArgumentException exc){
                String msg = "IllegalArgumentException when invoking LatherDispatcher from Ip=" +
                    ctx.getCallerIP() + ", method=" + method;
                log.error(msg, exc);
                resp.sendError(HttpServletResponse.SC_SERVICE_UNAVAILABLE, msg);
            } catch(RuntimeException exc){
                log.error("RuntimeException when invoking LatherDispatcher from Ip=" + ctx.getCallerIP() +
                          ", method=" + method, exc);
                issueErrorResponse(resp, exc.toString());
            } catch(LatherRemoteException exc){
                issueErrorResponse(resp, exc.toString());
            }
        }

        private void handleAutoApprovals(LatherValue arg) throws LatherRemoteException {
            ScanStateCore scanStateCore = ((AiSendReport_args) arg).getCore();
            boolean autoApprove = scanStateCore.getPlatform().isAutoApprove();

            if (autoApprove) {
                this.latherDispatcher.invokeAutoApprove();
            }
        }

        public void run(){
            try {
                this.doInvoke();
            } catch(IOException exc){
                this.log.warn("IOException", exc);
            }
        }
    }

    private void doServiceCall(HttpServletRequest req, HttpServletResponse resp, 
                           String methName, LatherValue args,
                           LatherXCoder xCoder, LatherContext ctx)
        throws IOException
    {
       
        ServiceCaller caller;
      

        LatherDispatcher latherDispatcher = Bootstrap.getBean(LatherDispatcher.class);

        caller = new ServiceCaller(resp, xCoder, 
                               ctx, methName, args,
                              this.log, latherDispatcher);
        caller.start();
        try {
            caller.join(this.execTimeout);
            if(caller.isAlive()){
                this.log.warn("Execution of '" + methName + "' exceeded " +
                              (this.execTimeout / 1000) + " seconds");
                caller.interrupt();
                caller.join(5000);  /* Sleep for a small amount more, to give
                                       it a chance to succeed */
            }
        } catch(InterruptedException exc){
            this.log.warn("Interrupted while trying to join thread " +
                          "executing '" + methName + "'");
        }
    }

   
}
