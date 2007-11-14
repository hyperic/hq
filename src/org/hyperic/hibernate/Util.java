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

package org.hyperic.hibernate;

import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.management.MBeanServer;
import javax.management.ObjectName;
import javax.management.MalformedObjectNameException;
import javax.management.InstanceAlreadyExistsException;
import javax.management.MBeanRegistrationException;
import javax.management.NotCompliantMBeanException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.hibernate.EmptyInterceptor;
import org.hibernate.Interceptor;
import org.hibernate.SessionFactory;
import org.hibernate.Hibernate;
import org.hibernate.jmx.StatisticsService;
import org.hibernate.cache.NoCacheProvider;
import org.hibernate.transaction.JTATransactionFactory;

import org.hibernate.cfg.Configuration;
import org.hibernate.cfg.Environment;

import org.hibernate.dialect.Dialect;
import org.hyperic.hibernate.dialect.HQDialect;
import org.hyperic.hibernate.dialect.MySQL5InnoDBDialect;
import org.hyperic.hibernate.dialect.Oracle9Dialect;
import org.hyperic.hibernate.dialect.PostgreSQLDialect;
import org.hibernate.dialect.function.ClassicAvgFunction;
import org.hibernate.dialect.function.ClassicSumFunction;
import org.hibernate.dialect.function.ClassicCountFunction;

import org.hibernate.engine.SessionFactoryImplementor;
import org.hyperic.hq.common.DiagnosticThread;
import org.hyperic.hq.common.DiagnosticObject;
import org.hyperic.hq.product.server.MBeanUtil;
import org.hyperic.util.PrintfFormat;
import org.hyperic.util.StringUtil;
import org.hyperic.util.jdbc.DBUtil;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Properties;
import java.util.Iterator;

import net.sf.ehcache.CacheManager;
import net.sf.ehcache.Cache;

/**
 * from hibernate caveat emptor with modifications to optimize initial 
 * context lookup
 */
public class Util {
    private static Log log = LogFactory.getLog(Util.class);

    private static final String INTERCEPTOR_CLASS = 
        "hibernate.util.interceptor_class";

    private static Configuration configuration;
    private static SessionFactory sessionFactory;

    static {
        boolean mocktest = System.getProperty("hq.mocktest") != null;
        // Create the initial SessionFactory from the default configuration
        // files
        try {
            // Replace with Configuration() if you don't use annotations 
            // or JDK 5.0
            configuration = new Configuration();

            // Read not only hibernate.properties, but also hibernate.cfg.xml
            configuration.configure("META-INF/hibernate.cfg.xml");
            if (mocktest) {
                mockTestConfig();
            }
            hibernateVersionConfig();
            // Set global interceptor from configuration
            setInterceptor(configuration, null);

            String jndiName = 
                configuration.getProperty(Environment.SESSION_FACTORY_NAME);

            if (jndiName != null) {
                // Let Hibernate bind the factory to JNDI
                configuration.buildSessionFactory();
                createHQHibernateStatMBean();
            } else {
                // or use static variable handling
                sessionFactory = configuration.buildSessionFactory();
            }
        } catch (Throwable ex) {
            // We have to catch Throwable, otherwise we will miss
            // NoClassDefFoundError and other subclasses of Error
            log.error("Building SessionFactory failed.", ex);
            throw new ExceptionInInitializerError(ex);
        }

        // Add ehcache statistics to the diagnostics
        DiagnosticObject cacheDiagnostics = new DiagnosticObject() {
            private PrintfFormat _fmt = 
                new PrintfFormat("%-50s %-6d %-6d %-6d %6d");
            private PrintfFormat _hdr = 
                new PrintfFormat("%-50s %-6s %-6s %-6s %6s");

            public String getName() {
                return "EhCache Diagnostics";
            }

            public String getShortName() {
                return "ehcacheDiag";
            }

            private List getSortedCaches() {
                CacheManager cacheManager = CacheManager.getInstance();
                String[] caches = cacheManager.getCacheNames();
                List res = new ArrayList(caches.length);
                
                for (int i=0; i<caches.length; i++) {
                    res.add(cacheManager.getCache(caches[i]));
                }
                
                Collections.sort(res, new Comparator() {
                    public int compare(Object o1, Object o2) {
                        Cache c1 = (Cache)o1;
                        Cache c2 = (Cache)o2;

                        return c1.getName().compareTo(c2.getName());
                    }
                });
                return res;
            }
            
            public String getStatus() {
                String separator = System.getProperty("line.separator");
                long totalBytes = 0;
                StringBuffer buf = new StringBuffer(separator);
                Object[] fmtArgs = new Object[5];

                fmtArgs[0] = "Cache";
                fmtArgs[1] = "Size";
                fmtArgs[2] = "Bytes";
                fmtArgs[3] = "Hits";
                fmtArgs[4] = "Misses";
                buf.append(_hdr.sprintf(fmtArgs))
                   .append(separator);
                fmtArgs[0] = "=====";
                fmtArgs[1] = "====";
                fmtArgs[2] = "=====";
                fmtArgs[3] = "====";
                fmtArgs[4] = "=====";
                buf.append(_hdr.sprintf(fmtArgs));

                for (Iterator i=getSortedCaches().iterator(); i.hasNext(); ) {
                    Cache cache = (Cache)i.next();
                    long inMemoryBytes = cache.calculateInMemorySize();
                    totalBytes += inMemoryBytes;
                    fmtArgs[0] = StringUtil.dotProximate(cache.getName(), 50);
                    fmtArgs[1] = new Integer(cache.getSize());
                    fmtArgs[2] = new Long(inMemoryBytes);
                    fmtArgs[3] = new Integer(cache.getHitCount());
                    fmtArgs[4] = new Integer(cache.getMissCountNotFound());
                            
                    buf.append(separator)
                       .append(_fmt.sprintf(fmtArgs));
                }
                buf.append(separator).
                    append("Total mapped cache size=").
                    append(totalBytes).
                    append(" bytes");

                return buf.toString();
            }

            public String toString() {
                return "ehcache";
            }
        };
        DiagnosticThread.addDiagnosticObject(cacheDiagnostics);
    }

    private static void createHQHibernateStatMBean()
        throws MalformedObjectNameException, InstanceAlreadyExistsException,
               MBeanRegistrationException, NotCompliantMBeanException
    {
        // get MBeanServer
        MBeanServer server = MBeanUtil.getMBeanServer();

        //build the MBean name
        ObjectName on =
            new ObjectName("Hibernate:type=statistics,application=hq");
        StatisticsService mBean = new StatisticsService();
        mBean.setSessionFactory(getSessionFactory());
        server.registerMBean(mBean, on);
        log.info("HQ Hibernate Statistics MBean registered " + on);
    }

    private static void mockTestConfig() {
        // set the proper hibernate configuration for mockejb test
        Properties prop = configuration.getProperties();
        String jta = System.getProperty(Environment.USER_TRANSACTION);
        if (jta == null) {
            jta = "javax.transaction.UserTransaction";
        }
        prop.setProperty(Environment.USE_QUERY_CACHE, "false");
        prop.setProperty(Environment.USE_SECOND_LEVEL_CACHE, "false");
        prop.setProperty(Environment.CACHE_PROVIDER,
                         NoCacheProvider.class.getName());
        prop.setProperty(Environment.TRANSACTION_STRATEGY,
                         JTATransactionFactory.class.getName());
        prop.remove(Environment.TRANSACTION_MANAGER_STRATEGY);
        prop.setProperty(Environment.USER_TRANSACTION, jta);
        
        // Setup a managed 'current session' context, since the tests
        // will setup & destroy the session in setup(), teardown()
        prop.setProperty(Environment.CURRENT_SESSION_CONTEXT_CLASS,
                         "managed");
        configuration.setProperties(prop);
    }

    private static void hibernateVersionConfig() {
        if (Environment.VERSION.startsWith("3.2")) {
            hibernate32Config();
        }
    }

    private static void hibernate32Config() {
        // From hibernate 3.2 migration guide:
        // In alignment with the JPA specification the count, sum and avg
        // function now defaults to return types as specified by the
        // specification. This can result in ClassCastException's at runtime
        // if you used aggregation in HQL queries.

        // The new type rules are described at
        // http://opensource.atlassian.com/projects/hibernate/browse/HHH-1538

        // Since we don't want to break existing code, we use the
        // classic aggregate functions for backward compat.

        configuration.addSqlFunction( "count", new ClassicCountFunction());
        configuration.addSqlFunction( "avg", new ClassicAvgFunction());
        configuration.addSqlFunction( "sum", new ClassicSumFunction()); 
    }

    /**
     * Returns the original Hibernate configuration.
     *
     * @return Configuration
     */
    public static Configuration getConfiguration()
    {
        return configuration;
    }

    /**
     * Returns the global SessionFactory.
     *
     * @return SessionFactory
     */
    public static SessionFactory getSessionFactory() {
        SessionFactory sf;
        String sfName = 
            configuration.getProperty(Environment.SESSION_FACTORY_NAME);

        if (sfName != null) {
            if (log.isDebugEnabled()) {
                log.debug("Looking up SessionFactory JNDI = " + sfName);
            }
            try {
                sf = (SessionFactory) new InitialContext().lookup(sfName);
            } catch (NamingException ex) {
                throw new RuntimeException(ex);
            }
        } else {
            sf = sessionFactory;
        }
        if (sf == null)
            throw new IllegalStateException("SessionFactory not available.");
        return sf;
    }

    /**
     * Closes the current SessionFactory and releases all resources.
     * <p/>
     * The only other method that can be called on HibernateUtil
     * after this one is rebuildSessionFactory(Configuration).
     */
    public static void shutdown() {
        log.debug("Shutting down Hibernate.");
        // Close caches and connection pools
        getSessionFactory().close();

        // Clear static variables
        configuration = null;
        sessionFactory = null;
    }

    public static Interceptor getInterceptor() {
        return configuration.getInterceptor();
    }

    /**
     * Resets global interceptor to default state.
     */
    public static void resetInterceptor() {
        log.debug("Resetting global interceptor to configuration setting");
        setInterceptor(configuration, null);
    }

    /**
     * Either sets the given interceptor on the configuration or looks
     * it up from configuration if null.
     */
    private static void setInterceptor(Configuration configuration, 
                                       Interceptor interceptor)
    {
        String interceptorName = configuration.getProperty(INTERCEPTOR_CLASS);
        if (interceptor == null && interceptorName != null) {
            try {
                Class interceptorClass =
                        Util.class.getClassLoader().loadClass(interceptorName);
                interceptor = (Interceptor) interceptorClass.newInstance();
            } catch (Exception ex) {
                throw new RuntimeException("Could not configure interceptor: "
                                           + interceptorName, ex);
            }
        }
        if (interceptor != null) {
            configuration.setInterceptor(interceptor);
        } else {
            configuration.setInterceptor(EmptyInterceptor.INSTANCE);
        }
    }

    public static HQDialect getHQDialect() {
        return (HQDialect)((SessionFactoryImplementor)getSessionFactory()).getDialect();
    }

    public static Dialect getDialect() {
        return ((SessionFactoryImplementor)getSessionFactory()).getDialect();
    }

    /**
     *
     * @return SQL Connection object associated with current JTA context
     */
    public static Connection getConnection()
    {
        return getSessionFactory().getCurrentSession().connection();
    }

    /**
     * disconnect SQL Connection from current JTA context
     */
    public static void endConnection()
    {
        getSessionFactory().getCurrentSession().disconnect();
    }

    public static void flushCurrentSession()
    {
        getSessionFactory().getCurrentSession().flush();
    }

    public static void initializeAll(Iterator i) {
        while(i.hasNext()) {
            Hibernate.initialize(i.next());
        }
    }

    public static Iterator getTableMappings() {
        return configuration.getTableMappings();
    }
}
