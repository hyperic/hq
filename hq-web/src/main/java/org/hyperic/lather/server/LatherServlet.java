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
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.Semaphore;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hyperic.hq.appdef.shared.AIPlatformValue;
import org.hyperic.hq.appdef.shared.AgentUnauthorizedException;
import org.hyperic.hq.bizapp.server.session.LatherDispatcher;
import org.hyperic.hq.bizapp.shared.lather.AiPlatformLatherValue;
import org.hyperic.hq.bizapp.shared.lather.CommandInfo;
import org.hyperic.hq.context.Bootstrap;
import org.hyperic.hq.measurement.server.session.DataInserterException;
import org.hyperic.lather.LatherContext;
import org.hyperic.lather.LatherRemoteException;
import org.hyperic.lather.LatherValue;
import org.hyperic.lather.NullLatherValue;
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

    private static final Log log = LogFactory.getLog(LatherServlet.class);
   
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

    private class ServiceCaller implements Runnable {
        
        private HttpServletResponse resp;
        
        private LatherXCoder xcoder;
        private LatherValue arg;
        private LatherContext ctx;
        private String method;
        private LatherDispatcher latherDispatcher;
        
        private Thread thread;
        private final AtomicBoolean finished = new AtomicBoolean(false);
        private AtomicLong startTime;
        

        private ServiceCaller(HttpServletResponse resp, LatherXCoder xcoder, LatherContext ctx, String method,
                LatherValue arg, LatherDispatcher latherDispatcher) {
            
            this.resp = resp;
            this.xcoder = xcoder;
            this.ctx = ctx;
            this.method = method;
            this.arg = arg;
            this.latherDispatcher = latherDispatcher;
            this.thread = Thread.currentThread();
        }

        private boolean hasStarted() {
            return startTime != null;
        }

        private boolean isFinished() {
            return finished.get();
        }

        private void markFinished() {
            this.thread = null;
            finished.set(true);
        }
                          
        public void run() {
            
            try {
                startTime = new AtomicLong(System.currentTimeMillis());
                LatherValue res = latherDispatcher.dispatch(ctx, method, arg);
                if (thread.isInterrupted()) {
                    return;
                }
                
                if (CommandInfo.CMD_AI_SEND_REPORT.equals(method)) {
                    res = handleAutoApprovals(res);
                }
                
                issueSuccessResponse(this.resp, this.xcoder, res);
            
            } catch(Exception e) {
                Throwable cause = e.getCause();
                // no need to log a full stack trace for known exceptions
                if (e instanceof AgentUnauthorizedException || (cause != null && cause instanceof AgentUnauthorizedException)) {
                    log.warn("unauthorized agent: " + e + ", ip=" + ctx.getCallerIP());
                    log.debug(e,e);
                } else if (e instanceof DataInserterException || (cause != null && cause instanceof DataInserterException)) {
                    log.warn(e);
                    log.debug(e,e);
                } else if (e.getMessage().equals("Server still initializing")) {
                    log.warn(e);
                    log.debug(e,e);
                } else {
                    log.error("error while invoking LatherDispatcher from ip=" + ctx.getCallerIP() +
                              ", method=" + method + ": " + e, e);
                }
                try {
                    issueErrorResponse(resp, e.toString());
                } catch(IOException ioe){
                    log.warn("IO error sending lather response method=" + method + ", ip=" + ctx.getCallerIP() + ": " + ioe, ioe);
                }
            }
        }
        
        private LatherValue handleAutoApprovals(LatherValue arg) throws LatherRemoteException {

            if (arg == null || arg instanceof NullLatherValue) {
                return NullLatherValue.INSTANCE;
            }

            
            AiPlatformLatherValue aiPlatformLatherValue = (AiPlatformLatherValue) arg;
            AIPlatformValue aiPlatformValue = aiPlatformLatherValue.getAIPlatformValue();

            if (aiPlatformValue.isAutoApprove()) {
                this.latherDispatcher.invokeAutoApprove(aiPlatformValue);
            }

            return NullLatherValue.INSTANCE;
        }

        public void interrupt() {
            if (thread != null)
                thread.interrupt();
        }

        public boolean isExpired() {
            if (startTime == null) {
                return false;
            }
            final long now = System.currentTimeMillis();
            if ((startTime.get() + execTimeout) <= now) {
                return true;
            }
            return false;
        }

        public String toString() {
            return "method=" + method + ", ip=" + ctx.getCallerIP();
        }
    }

    private void doServiceCall(HttpServletRequest req, HttpServletResponse resp, String methName, LatherValue args,
            LatherXCoder xCoder, LatherContext ctx)
                    throws IOException {
        
        final LatherDispatcher latherDispatcher = Bootstrap.getBean(LatherDispatcher.class);
        final ServiceCaller caller = new ServiceCaller(resp, xCoder, ctx, methName, args, latherDispatcher);
        final Thread currentThread = Thread.currentThread();
        final String threadName = currentThread.getName();

        try {
            currentThread.setName(methName + "-" + ids.getAndIncrement());
            LatherThreadMonitor.get().register(caller);
            caller.run();
            if (currentThread.isInterrupted()) {
                throw new InterruptedException();
            }
        } catch(InterruptedException exc){
            log.warn("Interrupted while trying to execute lather method=" + methName + " from ip=" + ctx.getCallerIP());
        } finally {
            caller.markFinished();
            currentThread.setName(threadName);
        }
    }
    
    private static class LatherThreadMonitor extends Thread {
        
        private static final LatherThreadMonitor instance = new LatherThreadMonitor();
        
        static {
            log.info("Starting Lather Thread Monitor");
            instance.start();
        }
        
        private static final ConcurrentLinkedQueue<ServiceCaller> queue = new ConcurrentLinkedQueue<LatherServlet.ServiceCaller>();
        
        private LatherThreadMonitor() {
            super("LatherThreadMonitor");
            setDaemon(true);
        }
        
        private static LatherThreadMonitor get() {
            return instance;
        }
        
        public void register(ServiceCaller caller) {
            queue.add(caller);
        }

        public void run() {
            
            final boolean debug = log.isDebugEnabled();
            final long inspectionInterval = 1000;
            int callerIndex = 0;

            while (true) {
                try {
                    
                    for (ServiceCaller caller : queue) {
                        if (caller.isFinished()) {
                            queue.remove(caller);
                        } else if (caller.isExpired()) {
                            log.warn("Expiring Lather thread " + caller);
                            caller.interrupt();
                            queue.remove(caller);
                        }
                        callerIndex++;
                    }
                    sleep(inspectionInterval);
                    if (debug)
                        log.debug("LatherThreadMonitor current queue size = " + queue.size() + ", " + callerIndex + " threads were inspected");
                    callerIndex = 0;

                } catch (Throwable t) {
                    log.error(t,t);
                }
            }
        }
    }
}
