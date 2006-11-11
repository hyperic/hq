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

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.hibernate.EmptyInterceptor;
import org.hibernate.Interceptor;
import org.hibernate.SessionFactory;
import org.hibernate.cache.NoCacheProvider;
import org.hibernate.stat.Statistics;
import org.hibernate.stat.SecondLevelCacheStatistics;
import org.hibernate.transaction.JTATransactionFactory;

import org.hibernate.cfg.Configuration;
import org.hibernate.cfg.Environment;

import org.hibernate.dialect.Dialect;
import org.hibernate.dialect.function.ClassicAvgFunction;
import org.hibernate.dialect.function.ClassicSumFunction;
import org.hibernate.dialect.function.ClassicCountFunction;

import org.hibernate.engine.SessionFactoryImplementor;
import org.hyperic.hq.common.DiagnosticThread;
import org.hyperic.hq.common.DiagnosticObject;

import java.sql.Connection;
import java.util.Properties;

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

        // Add second level cache statistics to the diagnostics
        DiagnosticObject cacheDiagnostics = new DiagnosticObject() {
            public String getStatus() {

                Statistics stats = getSessionFactory().getStatistics();
                String[] caches = stats.getSecondLevelCacheRegionNames();

                StringBuffer buf = new StringBuffer();
                for (int i = 0; i < caches.length; i++) {
                    SecondLevelCacheStatistics cacheStats =
                        stats.getSecondLevelCacheStatistics(caches[i]);

                    buf.append("Cache: ")
                        .append(caches[i])
                        .append(" elements=")
                        .append(cacheStats.getElementCountInMemory())
                        .append(" (")
                        .append(cacheStats.getSizeInMemory())
                        .append(" bytes) hits=")
                        .append(cacheStats.getHitCount())
                        .append(" misses=")
                        .append(cacheStats.getMissCount());
                }
                return buf.toString();
            }
        };
        DiagnosticThread.addDiagnosticObject(cacheDiagnostics);
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
}
