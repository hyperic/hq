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

package org.hyperic.hq.ui;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import javax.ejb.EJBHome;
import javax.naming.Context;
import javax.naming.InitialContext;
import javax.rmi.PortableRemoteObject;
import javax.servlet.ServletContext;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hyperic.hq.bizapp.shared.AIBoss;
import org.hyperic.hq.bizapp.shared.AIBossHome;
import org.hyperic.hq.bizapp.shared.AppdefBoss;
import org.hyperic.hq.bizapp.shared.AppdefBossHome;
import org.hyperic.hq.bizapp.shared.AuthBoss;
import org.hyperic.hq.bizapp.shared.AuthBossHome;
import org.hyperic.hq.bizapp.shared.AuthzBoss;
import org.hyperic.hq.bizapp.shared.AuthzBossHome;
import org.hyperic.hq.bizapp.shared.ConfigBoss;
import org.hyperic.hq.bizapp.shared.ConfigBossHome;
import org.hyperic.hq.bizapp.shared.ControlBoss;
import org.hyperic.hq.bizapp.shared.ControlBossHome;
import org.hyperic.hq.bizapp.shared.EventLogBoss;
import org.hyperic.hq.bizapp.shared.EventLogBossHome;
import org.hyperic.hq.bizapp.shared.EventsBoss;
import org.hyperic.hq.bizapp.shared.EventsBossHome;
import org.hyperic.hq.bizapp.shared.GalertBoss;
import org.hyperic.hq.bizapp.shared.GalertBossHome;
import org.hyperic.hq.bizapp.shared.MeasurementBoss;
import org.hyperic.hq.bizapp.shared.MeasurementBossHome;
import org.hyperic.hq.bizapp.shared.ProductBoss;
import org.hyperic.hq.bizapp.shared.ProductBossHome;
import org.hyperic.hq.bizapp.shared.UpdateBoss;
import org.hyperic.hq.bizapp.shared.UpdateBossHome;
import org.hyperic.hq.ui.exception.ServiceLocatorException;
import org.hyperic.hq.ui.util.UIUtils;
/**
 * A singleton class that looks up and caches BizApp home interfaces
 * and returns boss EJBs. It consults a supplied
 * <code>ServletContext</code> to configure itself if it is necessary
 * to connect to a remote EJB container.
 *
 */
public class ServiceLocator {
    
    private final static Class APPDEF_CLASS  = AppdefBossHome.class;
    private final static String APPDEF_NAME  = AppdefBossHome.JNDI_NAME;

    private final static Class AI_CLASS      = AIBossHome.class;
    private final static String AI_NAME      = AIBossHome.JNDI_NAME;
    
    private final static Class AUTH_CLASS    = AuthBossHome.class;
    private final static String AUTH_NAME    = AuthBossHome.JNDI_NAME;
    
    private final static Class AUTHZ_CLASS   = AuthzBossHome.class;
    private final static String AUTHZ_NAME   = AuthzBossHome.JNDI_NAME;
    
    private final static Class EVENTS_CLASS  = EventsBossHome.class;
    private final static String EVENTS_NAME  = EventsBossHome.JNDI_NAME;
    
    private final static Class MEASURE_CLASS = MeasurementBossHome.class;
    private final static String MEASURE_NAME = MeasurementBossHome.JNDI_NAME;
    
    private final static Class CONFIG_CLASS = ConfigBossHome.class;
    private final static String CONFIG_NAME = ConfigBossHome.JNDI_NAME;

    public static final Class EVENT_LOG_CLASS = EventLogBossHome.class;
    public static final String EVENT_LOG_NAME = EventLogBossHome.JNDI_NAME;
    
    private final static Class PRODUCT_CLASS = ProductBossHome.class;
    private final static String PRODUCT_NAME = ProductBossHome.JNDI_NAME;

    private static final String CONTROL_NAME = ControlBossHome.JNDI_NAME;
    private static final Class CONTROL_CLASS = ControlBossHome.class;

    private static final String GALERT_NAME = GalertBossHome.JNDI_NAME;
    private static final Class GALERT_CLASS = GalertBossHome.class;

    private static final String UPDATE_NAME = UpdateBossHome.JNDI_NAME;
    private static final Class UPDATE_CLASS = UpdateBossHome.class;

    private final static String CONTEXT_FACTORY_NAME =
        "ejb-remote-config.context-factory";
    private final static String PROVIDER_URL_NAME =
        "ejb-remote-config.provider-url";
    private final static String PREFIXES_NAME =
        "ejb-remote-config.url-package-prefixes";
    
    private Log log;
    private InitialContext context;
    private Map cache;
    
    public ServiceLocator(Map attrs) throws ServiceLocatorException {
        log = LogFactory.getLog(ServiceLocator.class.getName());

        try {
            if (attrs != null){
                context = new InitialContext(getRemoteProps(attrs));
            } else {
                context = new InitialContext();            
            }
        } catch (Exception e) {
            throw new ServiceLocatorException(e);
        }

        cache = Collections.synchronizedMap(new HashMap());

        if (log.isDebugEnabled()) {
            log.debug("initialized service locator");
        }
    }

    protected EJBHome lookup(String name, Class clazz)
        throws ServiceLocatorException {
        
        if (cache.containsKey(name)) {
            return (EJBHome) cache.get(name);
        }
        
        try {
            Object ref = context.lookup(name);
            EJBHome home = (EJBHome) PortableRemoteObject.narrow(ref, clazz);
            cache.put(name, home);
            return home;
        }
        catch (Exception e) {
            throw new ServiceLocatorException(e);
        }
    }
    
    /**
     * Return an <code>AppdefBoss</code> instance. If not previously
     * cached, look up the interface and then cache it before creating
     * and returning the boss.
     *
     * @exception ServiceLocatorException if the lookup or create fails
     */
    public AppdefBoss getAppdefBoss() throws ServiceLocatorException {
        AppdefBossHome home = (AppdefBossHome) lookup(APPDEF_NAME, APPDEF_CLASS);
        try {
            return (AppdefBoss) home.create();
        } catch (Exception e) {
            throw new ServiceLocatorException(e);
        }
    }
    
    /**
     * Return an <code>AIBoss</code> instance. If not previously
     * cached, look up the interface and then cache it before creating
     * and returning the boss.
     *
     * @exception ServiceLocatorException if the lookup or create fails
     */
    public AIBoss getAIBoss() throws ServiceLocatorException {
        AIBossHome home = (AIBossHome) lookup(AI_NAME, AI_CLASS);
        try {
            return (AIBoss) home.create();
        } catch (Exception e) {
            throw new ServiceLocatorException(e);
        }
    }
    
    /**
     * Return an <code>AuthBoss</code> instance. If not previously
     * cached, look up the interface and then cache it before creating
     * and returning the boss.
     *
     * @exception ServiceLocatorException if the lookup or create fails
     */
    public AuthBoss getAuthBoss() throws ServiceLocatorException {
        AuthBossHome home = (AuthBossHome) lookup(AUTH_NAME, AUTH_CLASS);
        try {
            return (AuthBoss) home.create();
        } catch (Exception e) {
            throw new ServiceLocatorException(e);
        }
    }
    
    /**
     * Return an <code>AuthzBoss</code> instance. If not previously
     * cached, look up the interface and then cache it before creating
     * and returning the boss.
     *
     * @exception ServiceLocatorException if the lookup or create fails
     */
    public AuthzBoss getAuthzBoss() throws ServiceLocatorException {
        AuthzBossHome home = (AuthzBossHome) lookup(AUTHZ_NAME, AUTHZ_CLASS);
        try {
            return (AuthzBoss) home.create();
        } catch (Exception e) {
            throw new ServiceLocatorException(e);
        }
    }

    /**
     * Return an <code>ConfigBoss</code> instance. If not previously cached,
     * look up the interface and then cache it before creating and returning the
     * boss.
     * 
     * @exception ServiceLocatorException
     *                if the lookup or create fails.
     * @return An instance of the ConfigBoss
     */
    public ConfigBoss getConfigBoss() throws ServiceLocatorException {
        ConfigBossHome home = (ConfigBossHome) lookup(CONFIG_NAME, CONFIG_CLASS);
        try {
            return (ConfigBoss) home.create();
        } catch (Exception e) {
            throw new ServiceLocatorException(e);
        }
    }
    
    /**
     * Return an <code>EventsBoss</code> instance. If not previously
     * cached, look up the interface and then cache it before creating
     * and returning the boss.
     *
     * @exception ServiceLocatorException if the lookup or create fails
     */
    public EventsBoss getEventsBoss() throws ServiceLocatorException {
        EventsBossHome home = (EventsBossHome) lookup(EVENTS_NAME, EVENTS_CLASS);
        try {
            return (EventsBoss) home.create();
        } catch (Exception e) {
            throw new ServiceLocatorException(e);
        }
    }
    
    /**
     * Return a <code>ProductBoss</code> instance. If not previously
     * cached, look up the interface and then cache it before creating
     * and returning the boss.
     *
     * @exception ServiceLocatorException if the lookup or create fails
     */
    public ProductBoss getProductBoss() throws ServiceLocatorException {
        ProductBossHome home =
            (ProductBossHome) lookup(PRODUCT_NAME, PRODUCT_CLASS);
        try {
            return (ProductBoss) home.create();
        } catch (Exception e) {
            throw new ServiceLocatorException(e);
        }
    }
    
    /**
     * Return an <code>MeasurementBoss</code> instance. If not previously
     * cached, look up the interface and then cache it before creating
     * and returning the boss.
     *
     * @exception ServiceLocatorException if the lookup or create fails
     */
    public MeasurementBoss getMeasurementBoss()
    throws ServiceLocatorException {
        MeasurementBossHome home =
            (MeasurementBossHome) lookup(MEASURE_NAME, MEASURE_CLASS);
        try {
            return (MeasurementBoss) home.create();
        } catch (Exception e) {
            throw new ServiceLocatorException(e);
        }
    }

    /**
     * Return the UIUtils implementor
     * @return
     */
    public UIUtils getUIUtils(ServletContext ctx) {
        return null;
    }
    
    //-------------------------------------private methods

    private String getAttr(Map attrs, String name) {
        return (String) attrs.get(name);
    }

    // define remote properties for retrieving initial context
    private Properties getRemoteProps(Map attrs) {
        Properties props = new Properties();

        String factory = getAttr(attrs, CONTEXT_FACTORY_NAME);
        if (factory != null) {
            props.setProperty(Context.INITIAL_CONTEXT_FACTORY, factory);
        }

        String url = getAttr(attrs, PROVIDER_URL_NAME);
        if (url != null) {
            props.setProperty(Context.PROVIDER_URL, url);
        }

        String prefixes = getAttr(attrs, PREFIXES_NAME);
        if (prefixes != null) {
            props.setProperty(Context.URL_PKG_PREFIXES, prefixes);
        }

        return props;
    }

    /**
     * Return a <code>EventLogBoss</code> instance. If not previously cached,
     * look up the interface and then cache it before creating and returning the
     * boss.
     * 
     * @exception ServiceLocatorException
     *                if the lookup or create fails
     */
    public EventLogBoss getEventLogBoss()
        throws ServiceLocatorException {
            EventLogBossHome home = (EventLogBossHome) lookup(EVENT_LOG_NAME,
                                                              EVENT_LOG_CLASS);
            try {
                return (EventLogBoss) home.create();
            } catch (Exception e) {
                throw new ServiceLocatorException(e);
            }
        }

    /**
     * Return an <code>ControlBoss</code> instance. If not previously cached,
     * look up the interface and then cache it before creating and returning the
     * boss.
     * 
     * @exception ServiceLocatorException
     *                if the lookup or create fails.
     * @return An instance of the ConrolBoss
     */
    public ControlBoss getControlBoss()
        throws ServiceLocatorException {
            ControlBossHome home = (ControlBossHome) lookup(CONTROL_NAME,
                                                            CONTROL_CLASS);
            try {
                return (ControlBoss) home.create();
            } catch (Exception e) {
                throw new ServiceLocatorException(e);
            }
        }

    public GalertBoss getGalertBoss()
        throws ServiceLocatorException 
    {
        GalertBossHome home = (GalertBossHome)lookup(GALERT_NAME, GALERT_CLASS);
                                                      
        try {
            return (GalertBoss) home.create();
        } catch (Exception e) {
            throw new ServiceLocatorException(e);
        }
    }

    public UpdateBoss getUpdateBoss() throws ServiceLocatorException {
        UpdateBossHome home = (UpdateBossHome)lookup(UPDATE_NAME, UPDATE_CLASS);
                                                      
        try {
            return (UpdateBoss) home.create();
        } catch (Exception e) {
            throw new ServiceLocatorException(e);
        }
    }

}
