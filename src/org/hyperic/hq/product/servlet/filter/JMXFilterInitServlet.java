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

import java.io.File;
import java.util.ArrayList;
import java.util.Hashtable;

import javax.management.InstanceAlreadyExistsException;
import javax.management.MBeanServer;
import javax.management.MBeanServerFactory;
import javax.management.ObjectName;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;

import org.hyperic.hq.product.servlet.mbean.ContextInfo;
import org.hyperic.hq.product.servlet.mbean.ServletInfo;

import mx4j.server.MBeanServerImpl;

/**
 * Initialize Measurements for the current application.
 * This needs to be loaded on startup.
 *
 * Because of the special issues with classloader and default web.xml, we must
 * take into account multiple use cases. Some containers will create a single
 * instance of this servlet or one one per context, with one class 
 * (and associated statics) or multiple classes.
 */
public class JMXFilterInitServlet extends HttpServlet {
    
    private static MBeanServer mServer = null;

    private static final String DOMAIN = "hyperic-hq";
    private static final String WEB_TMP = File.separator + "tmp";

    private String debug = null;
    
    private static JMXFilterInitServlet singleton;
    
    // ContextName -> JMXFilter
    private static Hashtable filters = new Hashtable();
    // CL -> JMXSessionListener
    private static Hashtable listeners = new Hashtable();
    
    private static Hashtable contextInfoByCL = new Hashtable(); 

    public static void registerFilter( JMXFilter f ) {
        // XXX:  We could use the thread class loader to make sure we don't
        //       run into the classical classloader problem (where this 
        //       class is ended by a parent loader )
        
        // JRUN doesn't seem to set the context loader
        //ClassLoader cl = Thread.currentThread().getContextClassLoader();
        ServletContext sc = f.getServletContext();

        // Servlet already exists and is initialized 
        if(singleton != null && 
           singleton.getServletContext() != null ) {
           
            //singleton.log("JMXREGISTER " + sc + " " + f);
            singleton.registerContextMBean(sc, f);
        }
        
        //System.out.println("REGISTER " + sc + f );
        String contextName = JMXFilterInitServlet.getName(sc);
        if (contextName != null) {
            filters.put( contextName, f );
        }
    }
    
    public static void registerSessionListener(JMXSessionListener sessionL) {
        ClassLoader cl = Thread.currentThread().getContextClassLoader();
        ContextInfo contextInfo = (ContextInfo)contextInfoByCL.get(cl);
        if(contextInfo != null) {
            contextInfo.setSessionListener( sessionL );
        }
        
        listeners.put( cl, sessionL );
    }
    
    public void registerServletMBean( String contextName, String servletPath,
                                      ServletInfo bean ) 
    {
        try {
            //ClassLoader cl = Thread.currentThread().getContextClassLoader();
            //JMXContextInfo ci = (JMXContextInfo)contextInfoByCL.get(cl);
            //if( ci == null ) {
            //    log("Error finding context for servlet using CL " + servletPath );
            //    return;
            //}
            //String contextName = ci.getContextName();
            
            if(servletPath.equals("")) servletPath = File.separator;
            if(contextName.equals("")) contextName = File.separator;

            final String[] escape = { ":", ",", "=" };
            for (int i=0; i<escape.length; i++) {
                servletPath =
                    JMXFilter.replace(servletPath, escape[i], "_");
            }

            ObjectName oname = new ObjectName(DOMAIN + 
                                              ":type=Servlet,name="
                                              + servletPath + 
                                              ",context=" + contextName);
            if (mServer.isRegistered(oname)) {
                mServer.unregisterMBean(oname);
            }
            mServer.registerMBean(bean, oname);
        } catch(Exception ex) {
            log("Error registering servlet mbean: " + ex.toString(), ex);
        }
    }
    
    /** 
     * Extract an unique name from a servlet context. 
     */ 
    static String getName(ServletContext ctx) {

        String docBase = ctx.getRealPath(File.separator);
        String contextName = docBase;
        
        String base = docBase;
        if(docBase == null) {
            return ctx.getServletContextName();
        }

        //ctx.log("Base dir: " + docBase );
        if(base != null) {
            if(base.endsWith(File.separator)) {
                base = base.substring(0, base.length() - 1);
            }
            if( base.endsWith(".war")) {
                base = base.substring(0, base.length() - 4);
            }
            int lastIdx = base.lastIndexOf(File.separator);
            if(lastIdx > 0) {
                base=base.substring(lastIdx );
            }

            //handle another jboss tmp name that endsWith 5 random digits
            int end = base.length();
            while (Character.isDigit(base.charAt(end-1))) {
                --end;
            }
            if ((base.length() - end) == 5) {
                base = base.substring(0, end);
            }

            //JBoss embedded Tomcat prepends /tmpXXXXX to the context name.  
            //The XXXXX is always a random number up to 5 digits long.  Since
            //it can change on restart, strip it out.
            if (base.startsWith(WEB_TMP) &&
                Character.isDigit(base.charAt(4))) { // Check /tmpX
                for (int idx = 5; idx < 9; idx++) {
                    if (!Character.isDigit(base.charAt(idx))) {
                        contextName = File.separator + base.substring(idx);
                        return contextName;
                    }
                }
                base= File.separator + base.substring(9); // Strip tmpXXXXX
            }

            contextName=base;                
        }
        return contextName;
    }

    /** 
     * Create an MBean and init it for a servlet context. Must be called 
     * from init() for all webapps that were registered before init was 
     * called, and also in registerFilter for all new apps - to deal with
     * the different order in the case of a shared class (in parent loader).
     *
     * In both cases, the filter is known. The session listener may be
     * null if no session was created (common case), so it must be registered
     * on the first session. 
     */ 
    public void registerContextMBean(ServletContext ctx, JMXFilter filter) 
    {
        try {
            // Extract the doc name from the context path
            String docBase = ctx.getRealPath(File.separator);
            String contextName = getName(ctx);

            if (contextName == null) {
                log("Unable to determine name for context " + ctx +
                    ". No display-name set?");
                return;
            }

            if(filter == null) {
                log("No filter for context " + contextName );
                return;
            }

            if (contextName.endsWith("ROOT")) {
                return; //XXX Available == 0??
            }
            ContextInfo contextInfo = filter.getJMXContextInfo();
            
            contextInfo.setDocBase(docBase);
            contextInfo.setContextName(contextName);
            
            ClassLoader cl=Thread.currentThread().getContextClassLoader();

            JMXSessionListener listener=(JMXSessionListener)listeners.get(cl);
            if(listener != null) 
                contextInfo.setSessionListener(listener);
            
            if(debug != null)
                log("Set filter " + this + " " + filter);
            
            filter.setJmxFilterServlet(this);
            
            ObjectName oname = 
                new ObjectName(DOMAIN + ":type=Context,name=" + 
                               contextName);
            
            if (mServer.isRegistered(oname)) {
                mServer.unregisterMBean(oname);
            }
            mServer.registerMBean(contextInfo, oname);

            contextInfoByCL.put( cl, contextInfo);
            filter.getJMXContextInfo().setAvailable(1);
        } catch( Exception ex ) {
            log( "Error registering context mbean: " + ex.toString(), ex);
            ex.printStackTrace();
        }
    }

    public static MBeanServer getMBeanServer() {
        if (mServer == null) {
            mServer = findMBeanServer();
        }

        return mServer;
    }

    private static MBeanServer findMBeanServer() {
        ArrayList alist = null;

        try {
            alist = MBeanServerFactory.findMBeanServer(null);
        } catch (NoSuchMethodError e) {
            //e.g. intraspect
            return new MBeanServerImpl(null);
        }
        
        if (alist != null && alist.size() > 0) {
            return (MBeanServer)alist.get(0);
        }

        return MBeanServerFactory.createMBeanServer();
    }

    public void init() throws ServletException {
        try {
            debug = getServletConfig().getInitParameter("debug");            
            
            if(debug != null) 
                log("Loading JMX context servlet " + this + " " + singleton);

            singleton = this;

            mServer = getMBeanServer();
    
            ClassLoader cl = Thread.currentThread().getContextClassLoader();
            
            String contextName = 
                JMXFilterInitServlet.getName(getServletContext());
            if (contextName == null) {
                return;
            }

            JMXFilter filter = (JMXFilter)filters.get(contextName);

            if(filter != null)
                filter.setJmxFilterServlet(this);
            
            if(debug != null) 
                log("LOAD " + getServletContext() + " " + filter);
            
            JMXSessionListener sessionListener = 
                (JMXSessionListener)listeners.get( cl );
            
            if(debug != null) 
                log("Setting up " + filter + " " + sessionListener);

            ServletContext ctx = getServletContext();
            
            registerContextMBean(ctx, filter);
        } catch (Exception e) {
            log("[JMXFilterInitServlet] No mserver found " + e, e);
            e.printStackTrace();
        }
    }
    
    public void destroy() {

    }
}
