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

package org.hyperic.hq.ui.util;

import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.Constructor;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import javax.servlet.ServletContext;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hyperic.hq.bizapp.shared.AIBoss;
import org.hyperic.hq.bizapp.shared.AppdefBoss;
import org.hyperic.hq.bizapp.shared.AuthBoss;
import org.hyperic.hq.bizapp.shared.AuthzBoss;
import org.hyperic.hq.bizapp.shared.ConfigBoss;
import org.hyperic.hq.bizapp.shared.ControlBoss;
import org.hyperic.hq.bizapp.shared.EventLogBoss;
import org.hyperic.hq.bizapp.shared.EventsBoss;
import org.hyperic.hq.bizapp.shared.GalertBoss;
import org.hyperic.hq.bizapp.shared.MeasurementBoss;
import org.hyperic.hq.bizapp.shared.ProductBoss;
import org.hyperic.hq.bizapp.shared.UpdateBoss;
import org.hyperic.hq.common.shared.HQConstants;
import org.hyperic.hq.ui.Constants;
import org.hyperic.hq.ui.ServiceLocator;
import org.hyperic.hq.ui.exception.ServiceLocatorException;

/**
 * Utilities class that provides convenience methods for operating on
 * the servlet context.
 */
public class ContextUtils {
    private static Log log = LogFactory.getLog(ContextUtils.class);
    
    /** Return the cached <code>ServiceLocator</code>, loading it if
     * necessary.
     *
     * @param ctx the <code>ServletContext</code>
     */
    protected static ServiceLocator getServiceLocator(ServletContext ctx)
        throws ServiceLocatorException {
        ServiceLocator sl = (ServiceLocator)
            ctx.getAttribute(Constants.SERVICE_LOCATOR_CTX_ATTR);
        
        if (sl == null) {
            HashMap attrs = new HashMap();
            Enumeration names = ctx.getAttributeNames();
            while (names.hasMoreElements()) {
                String name = (String) names.nextElement();
                attrs.put(name, ctx.getAttribute(name));
            }

            String className =
                ctx.getInitParameter(Constants.SERVICE_LOCATOR_CTX_ATTR);
            if (className != null && className.length() > 0) {
                try {
                    Class slClass = Class.forName(className);
                    Constructor slConst =
                        slClass.getConstructor(new Class[] { Map.class });
                    sl = (ServiceLocator)
                        slConst.newInstance(new Object[] { attrs });
                } catch (Exception e) {
                    // Fall back to default ServiceLocator
                    log.error("Createing ServiceLocator class " + className +
                              " failed", e);
                }
            }

            if (sl == null)
                sl = new ServiceLocator(attrs);

            ctx.setAttribute(Constants.SERVICE_LOCATOR_CTX_ATTR, sl);
        }

        return sl;
    }
    
    /** Consult the cached <code>ServiceLocator</code> for an instance of
     * <code>AppdefBoss</code>.
     *
     * @param ctx the <code>ServletContext</code>
     */
    public static AppdefBoss getAppdefBoss(ServletContext ctx)
        throws ServiceLocatorException {
        return getServiceLocator(ctx).getAppdefBoss();
    }

    /** Consult the cached <code>ServiceLocator</code> for an instance of
     * <code>AppdefBoss</code>.
     *
     * @param ctx the <code>ServletContext</code>
     */
    public static AIBoss getAIBoss(ServletContext ctx)
        throws ServiceLocatorException {
        return getServiceLocator(ctx).getAIBoss();
    }

    /** Consult the cached <code>ServiceLocator</code> for an instance of
    * <code>ConfigBoss</code>.
    *
    * @param ctx the <code>ServletContext</code>
    */
   public static ConfigBoss getConfigBoss(ServletContext ctx)
       throws ServiceLocatorException {
       return getServiceLocator(ctx).getConfigBoss();
   }

    /** Consult the cached <code>ServiceLocator</code> for an instance of
     * <code>AuthBoss</code>.
     *
     * @param ctx the <code>ServletContext</code>
     */
    public static AuthBoss getAuthBoss(ServletContext ctx)
        throws ServiceLocatorException {            
        return getServiceLocator(ctx).getAuthBoss();
    }

    /** Consult the cached <code>ServiceLocator</code> for an instance of
     * <code>AuthzBoss</code>.
     *
     * @param ctx the <code>ServletContext</code>
     */
    public static AuthzBoss getAuthzBoss(ServletContext ctx)
        throws ServiceLocatorException {
        return getServiceLocator(ctx).getAuthzBoss();
    }

    /** Consult the cached <code>ServiceLocator</code> for an instance of
     * <code>EventsBoss</code>.
     *
     * @param ctx the <code>ServletContext</code>
     */
    public static EventsBoss getEventsBoss(ServletContext ctx)
        throws ServiceLocatorException {
        return getServiceLocator(ctx).getEventsBoss();
    }
    
    /** Consult the cached <code>ServiceLocator</code> for an instance of
     * <code>MeasurementBoss</code>.
     *
     * @param ctx the <code>ServletContext</code>
     */
    public static MeasurementBoss getMeasurementBoss(ServletContext ctx)
        throws ServiceLocatorException {
        return getServiceLocator(ctx).getMeasurementBoss();
    }
    
    /** Consult the cached <code>ServiceLocator</code> for an instance of
     * <code>ProductBoss</code>.
     *
     * @param ctx the <code>ServletContext</code>
     */
    public static ProductBoss getProductBoss(ServletContext ctx)
        throws ServiceLocatorException {            
        return getServiceLocator(ctx).getProductBoss();
    }
    
    /** Consult the cached <code>ServiceLocator</code> for an instance of
     * <code>EventBoss</code>.
     *
     * @param ctx the <code>ServletContext</code>
     */
    public static EventLogBoss getEventLogBoss(ServletContext ctx)
        throws ServiceLocatorException {
        return getServiceLocator(ctx).getEventLogBoss();
    }

    /**
     * Consult the cached <code>ServiceLocator</code> for an instance of
     * <code>ControlBoss</code>.
     * 
     * @param ctx
     *            the <code>ServletContext</code>
     */
    public static ControlBoss getControlBoss(ServletContext ctx)
        throws ServiceLocatorException {
        return getServiceLocator(ctx).getControlBoss();
    }
    
    public static GalertBoss getGalertBoss(ServletContext ctx)
        throws ServiceLocatorException {
        return getServiceLocator(ctx).getGalertBoss();
    }

    public static UpdateBoss getUpdateBoss(ServletContext ctx)
        throws ServiceLocatorException  {
        return getServiceLocator(ctx).getUpdateBoss();
    }

    public static boolean usingJDBCAuthentication(ServletContext ctx)
        throws Exception {
        String provider =
            (String) ctx.getAttribute(Constants.JAAS_PROVIDER_CTX_ATTR);
    
        if (provider == null) {
            Properties conf = ContextUtils.getConfigBoss(ctx).getConfig();
            provider = conf.getProperty(HQConstants.JAASProvider);
            ctx.setAttribute(Constants.JAAS_PROVIDER_CTX_ATTR, provider);
        }
    
        return provider != null &&
            provider.equals(HQConstants.JDBCJAASProvider);
    }

    public static void saveProperties(ServletContext ctx,
                                      String filename, 
                                      Properties props)
    throws Exception {
        
        filename = ctx.getRealPath(filename);
        
        FileOutputStream out = new FileOutputStream(filename);        
        props.store((OutputStream) out, null);            
    }

    /**
     * Load the specified properties file and return the properties.
     *
     * @param ctx the <code>ServletContext</code>
     * @param filename the fully qualifed name of the properties file
     * @exception Exception if a problem occurs while loading the file
     */
    public static Properties loadProperties(ServletContext ctx,
                                            String filename)
        throws Exception {
        Properties props = new Properties();
        InputStream is = ctx.getResourceAsStream(filename);
        if (is != null) {
            props.load(is);
            is.close();
        }
    
        return props;
    }

    /** Consult the cached <code>ServiceLocator</code> for an instance of
     * <code>UIUtils</code>.
     *
     * @param ctx the <code>ServletContext</code>
     */
    public static UIUtils getUIUtils(ServletContext ctx)
        throws ServiceLocatorException {
            
        return getServiceLocator(ctx).getUIUtils(ctx);
    }
}
