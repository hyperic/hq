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

package org.hyperic.hq.product.servlet.webapp;

import java.util.HashMap;
import java.util.Iterator;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.File;
import java.lang.reflect.Method;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.ServletException;
import javax.servlet.ServletContext;

import javax.management.InstanceNotFoundException;
import javax.management.MBeanAttributeInfo;
import javax.management.MBeanInfo;
import javax.management.MBeanServer;
import javax.management.ObjectInstance;
import javax.management.ObjectName;

import org.hyperic.hq.product.servlet.filter.JMXFilter;
import org.hyperic.hq.product.servlet.filter.JMXFilterInitServlet;
import org.hyperic.hq.product.servlet.mbean.RuntimeInfo;
import org.hyperic.hq.product.servlet.mbean.ServerMeasurement;

/**
 * Servlet class for simple JMX MBean queries over HTTP.
 * 
 * TODO: Support user/pass auth on client side ( using normal container auth )
 * TODO: Add one init param with the IP address of the agent, 
 *       allow only connections from the agent
 * 
 * @jmx:mbean name="hyperic.measurement:type=JMXServlet"
 *
 */
public final class JMXServlet extends HttpServlet {

    private MBeanServer mServer = null;
    private String debug = null;
    
    // Detection of Tomcat 4.1.31
    private static final String TOMCAT_4_1_31 = "Apache Tomcat/4.1.31";
    private boolean forceTomcatCompat = false;

    private static final String DOMAIN = "hyperic-hq";

    public void init() throws ServletException {
        try {
            ObjectName oName;
            //getServletContext().log("Loading JMX servlet" );
            
            debug = getServletConfig().getInitParameter("debug");
            mServer = JMXFilterInitServlet.getMBeanServer();

            oName = new ObjectName(DOMAIN + ":type=ServerMeasurement");
            ServerMeasurement s = new ServerMeasurement();
            String serverInfo = getServletContext().getServerInfo();
            s.setServerInfo(serverInfo);
            if (serverInfo.equals(TOMCAT_4_1_31)) {
                this.forceTomcatCompat = true;
            }
            mServer.registerMBean(s, oName);

            oName = new ObjectName(DOMAIN + ":type=RuntimeInfo");
            mServer.registerMBean(new RuntimeInfo(), oName);

            initSigar();
        } catch (Exception e) {
            getServletContext().log("[JMXServlet] No mserver found " + e);
        }
        //getServletContext().log("Loaded in " + mServer);
    }

    public void destroy() {
        unregisterSigarProcessMBean(SIGAR_PROCESS_NAME);
    }

    private void outputException(PrintWriter out, String msg) {
        outputException(out, new ServletException(msg));
    }

    private void outputException(PrintWriter out, Exception e) {
        out.print("exception=" +
                  e.getClass().getName() + ": " +
                  e.getMessage());
    }
    
    private void outputValue(PrintWriter out, Object value) {
        String sig;
        if(value == null) {
            out.print("value=N;");
            return;
        }
        else if (value instanceof Long) {
            sig = "L";
        }
        else if (value instanceof Integer) {
            sig = "I";
        }
        else if (value instanceof Double) {
            sig = "D";
        }
        else {
            sig = "S";
        }

        out.print("value=" + sig + ";" + value.toString());
    }

    private void listCmd(PrintWriter out, String pat)
        throws Exception 
    {
        ObjectName patternName = null;

        if ((pat != null) && (pat.length() > 0)) {
            patternName = new ObjectName(pat);
        }

        Iterator it = mServer.queryMBeans(patternName, null).iterator();

        while (it.hasNext()) {
            ObjectInstance obj = (ObjectInstance)it.next();
            MBeanInfo info = mServer.getMBeanInfo(obj.getObjectName());
            String name = obj.getObjectName().toString();

            MBeanAttributeInfo[] attrs = info.getAttributes();
            for (int i = 0; i < attrs.length; i++) {
                out.println(name + ":" + attrs[i].getName());
            }
        }
    }

    static Object VOID_PARAM[] = new Object[] {};
    static String VOID_SIG[] = new String[] {};

    private void invokeCmd(PrintWriter out, String query)
        throws Exception
    {
        int ix = query.lastIndexOf(':');
        if (ix > -1) {
            String objName = query.substring(0, ix);
            String opName = query.substring(objName.length() + 1);
            
            boolean special = specialCommand(out, objName, opName);
            if( special ) return;
            
            try {
                //System.out.println("INVOKE: " + objName + " -> " + opName );
                ObjectName oName = new ObjectName(objName);
                Object obj = mServer.invoke(oName, opName,
                                            VOID_PARAM, VOID_SIG);
                outputValue(out, obj);
            } catch (Throwable e) {
                log("Error invoking command " + e.toString());
                outputException(out, (Exception)e);
            }
        }
    }

    private static String JRUN_SERVICE = "ServletEngineService:service=";
    
    /** 
     * Detect if this is an operation that requires a special, server-specific 
     * hack.
     * 
     * XXX We could also return a Command? object, or use interceptors or some
     * other mechanism. Right now there is only one server-specific behavior, 
     * for jrun4 context start command. If more are needed, this should be 
     * refactored.
     * 
     * @return true if this is was handled as a special command.
     */ 
    private boolean specialCommand(PrintWriter out, String objName,
                                   String opName )
    {
        if(objName.startsWith(JRUN_SERVICE) && 
           "start".equals(opName)) {
            
            try {
                ObjectName oName = new ObjectName(objName);
                String name = "/" + oName.getKeyProperty("service");
                // this is our mbean - to get DocBaase
                ObjectName ctxName = new ObjectName(DOMAIN + 
                                                    ":type=Context,name=" + 
                                                    name);
                
                String base = (String)mServer.getAttribute(ctxName, "DocBase");
                
                File f = new File(base, "/WEB-INF/web.xml");
                if(f.exists()) {
                    f.setLastModified(System.currentTimeMillis());
                } else {
                    log("Can't find " + f);
                }
                
                outputValue(out, null);
            } catch (Throwable e) {
                log("Error invoking command " + e.toString());
                outputException(out, (Exception)e);
            }
            
            return true;
        }
        
        return false;
    }
    
    private void dmpCmd(HttpServletRequest request, PrintWriter out,
                        String pat)
        throws Exception 
    {
        ObjectName patternName = null;

        if ((pat != null) && (pat.length() > 0) && ! "*".equals(pat)) {
            patternName = new ObjectName(pat);
        }

        Iterator it = mServer.queryMBeans(patternName, null).iterator();

        out.println("Time: " + System.currentTimeMillis());
        out.println("Remote: " + request.getRemoteAddr());
        out.println("User: " + request.getRemoteUser());
        out.println("Server: " + getServletContext().getServerInfo());
        out.println();
        
        while (it.hasNext()) {
            ObjectInstance obj = (ObjectInstance)it.next();
            ObjectName oname = obj.getObjectName();
            MBeanInfo info;
            
            try {
                info = mServer.getMBeanInfo(oname);
            } catch (InstanceNotFoundException e) {
                //Object no longer exists
                continue;
            }

            // we don't want to risk opening a dbconnection
            // which won't get closed (i.e. Tomcat) :@(
            if (oname.getKeyProperty("type") != null &&
                oname.getKeyProperty("type").equals("DataSource")) {
                continue;
            }

            String name = oname.toString();

            // ERS Tomcat uses Standalone: rather than Catalina: prefix for some 
            // object names.  Standardize on Catalina:
            if (name.startsWith("Standalone:")) {
                name = "Catalina:" + name.substring(11); //Standalone:
            }

            // Ignore servlet types.  These objects cannot be removed
            // from the MBeanServer since RT uses them.  For now we just remove
            // them from the servlet so there is less data to parse when collecting
            // metrics.  Also remove all JBoss MBeans.
            if ((name.indexOf("type=Servlet") != -1) ||
                (name.indexOf("j2eeType=Servlet") != -1) ||
                (name.startsWith("jboss") &&
                 !name.startsWith("jboss.web.deployment") &&
                 (name.indexOf("type=GlobalRequestProcessor") == -1) &&
                 (name.indexOf("type=ThreadPool") == -1))  ||
                 name.startsWith("jmx"))
                continue;

            // Ignore JRockit MBeans that use attribute names that are incompatible
            // with our manifest parser
            if (name.equals("com.jrockit:type=JRockitPerfCounters")) {
                continue;
            }

            // Tomcat 4.1.31 changed the connector object names
            if (this.forceTomcatCompat &&
                name.indexOf("type=GlobalRequestProcessor") != -1) {
                name = JMXFilter.replace(name, "-", "");
            }

            // It seems that ObjectName.toString() results are inconsistant between
            // various JMX versions.  JBoss 4.0.2's JMX version outputs:
            // hyperic-hq:name=/hyperic-hq,type=Context rather than the expected
            // hypeirc-hq:type=Context,name=/hyperic-hq
            // Hack around this here.
            if (name.startsWith("hyperic-hq:name=") &&
                name.endsWith("type=Context")) {
                String context = name.substring(name.indexOf(":") + 1,
                                                name.indexOf(","));
                out.println("Name: hyperic-hq:type=Context," + context);
            } else if (name.startsWith("jboss.web:name=") &&
                       name.endsWith("type=GlobalRequestProcessor")) {
                String connector = name.substring(name.indexOf(":") + 1,
                                                  name.indexOf(","));
                out.println("Name: Catalina:type=GlobalRequestProcessor," +
                            connector);
            } else if (name.startsWith("jboss.web:name=") &&
                       name.endsWith("type=ThreadPool")) {
                String threadPool = name.substring(name.indexOf(":") + 1,
                                                   name.indexOf(","));
                out.println("Name: Catalina:type=ThreadPool," +
                            threadPool);
            } else if (name.startsWith("jboss.web:")) {
                String attributes = name.substring(name.indexOf(":") + 1);
                out.println("Name: Catalina:" + attributes);
            } else {
                out.println("Name: " + name);
            }

            String code = info.getClassName();

            MBeanAttributeInfo[] attrs = info.getAttributes();
            if(attrs != null && attrs.length > 0) {
                HashMap seenAttributes = new HashMap();
                for (int i = 0; i < attrs.length; i++) {
                    String attName = attrs[i].getName();                    
                    try {
                        if (seenAttributes.containsKey(attName)) {
                            continue; // Avoid dups
                        }
                        seenAttributes.put(attName, Boolean.TRUE);

                        // Some attributes may throw exception - don't
                        // drop the others
                        try {
                            Object o = mServer.getAttribute(oname, attName);
                            if(attName.indexOf( "=") >=0 ||
                               attName.indexOf( ":") >=0 ||
                               attName.indexOf(".") >= 0 ||
                               attName.indexOf( " ") >=0 ) {
                                continue;
                            }
                            if("modelerType".equalsIgnoreCase(attName)) {
                                if (code !=null ) continue; // prevent duplicates
                                code = (String)o;       
                            }
                            String str = escape(getStringValue(o));
                            if(str != null && 
                               str.indexOf( '\n') <0 &&  
                               str.indexOf( '\r') < 0) 
                                {
                                    // Temp - Escaping needs testing
                                    if(str.length() < 100) {
                                        out.println( attrs[i].getName() +
                                                     ": " + str);
                                    }
                                }
                        } catch(Throwable t) {
                            // Ignore this attribute
                        }
                    } catch(Throwable t) {
                        if(debug != null) 
                            log("Error getting " + oname +  " " + 
                                attName + " " + t.toString());
                    }
                }
            }
            out.println();
        }

        // XXX: Hack to provide more server metrics.
        // Provide an aggregate of all deployed contexts
        ObjectName allContexts = new ObjectName("hyperic-hq:type=Context,*");
        it = mServer.queryMBeans(allContexts, null).iterator();

        int requests = 0;
        long totaltime = 0;
        while (it.hasNext()) {
            ObjectInstance obj = (ObjectInstance)it.next();
            ObjectName oname = obj.getObjectName();
            
            Integer numReq = (Integer)mServer.getAttribute(oname, "RequestCount");
            requests += numReq.intValue();
            Long totTime = (Long)mServer.getAttribute(oname, "TotalTime");
            totaltime += totTime.longValue();
        }
        
        out.println("Name: hyperic-hq:type=AllContextInfo");
        out.println("RequestCount: " + requests);
        out.println("TotalTime: " + totaltime);
        // XXX: End hack.
    }

    private String escape(String value) {
        // XXX: The only invalid char is \n.  Also need to keep the
        //      string short and split it with \nSPACE.
        int idx = value.indexOf("\n");
        if(idx < 0) return value;
        //if( idx < 0 && value.length() < 78) return value;

        int prev = 0;
        StringBuffer sb = new StringBuffer();
        if(idx < 0) {
            // No \n, long line 
            appendHead(sb, value, 0, value.length());
            return sb.toString();
        }

        while(idx >= 0) {
            appendHead(sb, value, prev, idx - 1);

            sb.append( "\\n\n ");
            prev = idx + 1;
            if(idx == value.length() -1) break;
            idx = value.indexOf('\n', idx+1);
        }

        if(prev < value.length())
            appendHead(sb, value, prev, value.length());

        return sb.toString();
    }

    private void appendHead(StringBuffer sb, String value,
                            int start, int end) {
        int pos = start;
        while(end -pos > 78 ) {
            sb.append(value.substring(pos, pos + 78));
            sb.append("\n ");
            pos = pos + 78;
        }
        sb.append(value.substring(pos,end));
    }
        
    private String getStringValue(Object o) {
        if(o instanceof Double ||
           o instanceof Long ||
           o instanceof Integer ) {
            return o.toString();            
        }

        if(o == null)
            return null;
        if(o instanceof String ) {
            // Quote String
            String str=(String)o;
            return str;            
        }
        return o.toString();
    }
        
    public void doGet(HttpServletRequest request,
                      HttpServletResponse response)
        throws IOException, ServletException
    {
        try {
            PrintWriter out = response.getWriter();
            String query = request.getQueryString();
            response.setContentType("text/plain");
            if(mServer == null ) {
                outputException(out, "No mbean server");
                return;
            }
            
            if (query != null) {
                if (query.startsWith("available")) {
                    // All good
                    return;
                }
                if (query.startsWith("lst=")) {
                    dmpCmd( request, out, query.substring(4));
                    return;
                }
                if (query.startsWith("qry=")) {
                    dmpCmd( request, out, query.substring(4));
                    return;
                }
                if (query.startsWith("dmp=")) {
                    dmpCmd( request, out, query.substring(4));
                    return;
                }
                if (query.startsWith("invoke")) {
                    try {
                        invokeCmd(out, query.substring(7));
                    } catch (Exception e) {
                        outputException(out, e);
                    }
                    return;
                }
                if (query.startsWith("list")) {
                    try {
                        listCmd(out, query.substring(5));
                    } catch (Exception e) {
                        outputException(out, e);
                    }
                    return;
                }
                
                int ix = query.lastIndexOf(':');
                
                if (ix > -1) {
                    String objName = query.substring(0, ix);
                    String atrName = query.substring(objName.length() + 1);
                    
                    try {
                        ObjectName oName = new ObjectName(objName);
                        Object obj = mServer.getAttribute(oName, atrName);
                        outputValue(out, obj);
                    } catch (Exception e) {
                        outputException(out, e);
                    }
                }
                else {
                    outputException(out, "Invalid query=" + query);
                }
            }
            else {
                outputException(out, "Invalid null query");
            }
        } catch( Throwable t ) {
            log("Error in JMX servlet " + t.toString(), t );
        }
    }
    
    // Sigar stuff
    
    private static final String SIGAR_PROCESS_NAME =
        DOMAIN + ":type=ProcessInfo";

    /*
     * Yeah, yeah.. reflection bad.
     * But good here to make SigarProcessMBean registraion
     * optional at runtime.. i.e. we don't require sigar.jar
     * in the classpath to run.
     *
     * Also, register only happens at startup and unregister
     * at shutdown, so performance does not matter here.
     */
    private final static String sigarClassName =
        "org.hyperic.sigar.jmx.SigarProcess";

    private static Object sigarProcess = null;
    private static Class  sigarProcessClass = null;

    private Object getSigarProcess()
        throws Exception 
    {
        if (sigarProcess != null) {
            return sigarProcess;
        }

        sigarProcessClass = Class.forName(sigarClassName);

        Constructor c =
            sigarProcessClass.getConstructor(new Class[0]);

        sigarProcess = c.newInstance(new Object[0]);

        return sigarProcess;
    }

    private void registerSigarProcessMBean(String name)
        throws Exception 
    {
        Object s = getSigarProcess();

        if (s == null) {
            return;
        }

        mServer.registerMBean(s, new ObjectName(name));
    }

    private void unregisterSigarProcessMBean(String name) {
        if (sigarProcess == null) {
            return;
        }

        try {
            mServer.unregisterMBean(new ObjectName(name));

            Method sigarProcessClose =
                sigarProcessClass.getMethod("close", new Class[0]);

            sigarProcessClose.invoke(sigarProcess, new Object[0]);
            sigarProcess = null;
            log("unregistered sigar MBean");
        } catch (Exception e) {
            log("Failed to unregister sigar MBean: " + e);
        }
    }

    private void initSigar() {
        final String SIGAR_PATH = "org.hyperic.sigar.path";

        if (System.getProperty(SIGAR_PATH) == null) {
            ServletContext ctx = getServletContext();
            String base = ctx.getRealPath("/");
            //sigar native libraries not in WEB-INF/lib
            //to avoid problems when embedded in jboss.
            String[] libs = {
                "native-lib", "WEB-INF/lib"
            };
            for (int i=0; i<libs.length; i++) {
                File lib = new File(base, libs[i]);
                if (lib.exists()) {
                    System.setProperty(SIGAR_PATH, lib.toString());
                    break;
                }
            }
        }

         try {
             registerSigarProcessMBean(SIGAR_PROCESS_NAME);
         } catch (Throwable e) {
             if(e instanceof InvocationTargetException) {
                 e=((InvocationTargetException)e).getCause();
             }
             //this is optional, will fail if unable to find
             //sigar.jar and/or native lib in server/lib
             log("Unable to register sigar process MBean " + 
                 SIGAR_PROCESS_NAME +
                 " " + e.toString());
             if(debug != null)
                 getServletContext().log("error loading sigar", e);
         }
    }
}
