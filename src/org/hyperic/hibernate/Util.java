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

import java.sql.Connection;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.management.InstanceAlreadyExistsException;
import javax.management.MBeanRegistrationException;
import javax.management.MBeanServer;
import javax.management.MalformedObjectNameException;
import javax.management.NotCompliantMBeanException;
import javax.management.ObjectName;

import net.sf.ehcache.Cache;
import net.sf.ehcache.CacheManager;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hibernate.Hibernate;
import org.hibernate.SessionFactory;
import org.hibernate.cfg.Environment;
import org.hibernate.dialect.Dialect;
import org.hibernate.engine.SessionFactoryImplementor;
import org.hibernate.engine.SessionImplementor;
import org.hibernate.id.IdentifierGenerator;
import org.hibernate.jmx.StatisticsService;
import org.hyperic.hibernate.dialect.HQDialect;
import org.hyperic.hq.common.DiagnosticObject;
import org.hyperic.hq.common.DiagnosticThread;
import org.hyperic.hq.context.Bootstrap;
import org.hyperic.hq.product.server.MBeanUtil;
import org.hyperic.util.PrintfFormat;
import org.hyperic.util.StringUtil;
import org.springframework.orm.hibernate3.LocalSessionFactoryBean;

/**
 * from hibernate caveat emptor with modifications to optimize initial 
 * context lookup
 */
public class Util {
    
    /**
     * If the system property with this name is set to <code>true</code>, then 
     * the HQ product is running within a unit test environment.
     */
    public static final String HQ_UNITTEST_RUN = "hq.unittest.run";
    
    private static Log log = LogFactory.getLog(Util.class);

    

    static {
        // Create the initial SessionFactory from the default configuration
        // files
//        try {
//            // Replace with Configuration() if you don't use annotations 
//            // or JDK 5.0
//            configuration = new Configuration();
//
//            // Read not only hibernate.properties, but also hibernate.cfg.xml
//            configuration.configure("META-INF/hibernate.cfg.xml");
//
//            hibernateVersionConfig();
//            // Set global interceptor from configuration
//            setInterceptor(configuration, null);
//
//            String jndiName = 
//                configuration.getProperty(Environment.SESSION_FACTORY_NAME);
//
//            if (jndiName != null) {
//                // Let Hibernate bind the factory to JNDI
//                configuration.buildSessionFactory();
//                createHQHibernateStatMBean();
//            } else {
//                // or use static variable handling
//                sessionFactory = configuration.buildSessionFactory();
//            }
//        } catch (Throwable ex) {
//            // We have to catch Throwable, otherwise we will miss
//            // NoClassDefFoundError and other subclasses of Error
//            log.error("Building SessionFactory failed.", ex);
//            throw new ExceptionInInitializerError(ex);
//        }

        // Add ehcache statistics to the diagnostics
        DiagnosticObject cacheDiagnostics = new DiagnosticObject() {
            private PrintfFormat _fmt = 
                new PrintfFormat("%-55s %-6d %-6d %6d");
            private PrintfFormat _hdr = 
                new PrintfFormat("%-55s %-6s %-6s %6s");

            public String getName() {
                return "EhCache Diagnostics";
            }

            public String getShortName() {
                return "ehcacheDiag";
            }

            private List getSortedCaches() {
                List res = getCaches();
                
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
                StringBuffer buf = new StringBuffer(separator);
                Object[] fmtArgs = new Object[5];

                fmtArgs[0] = "Cache";
                fmtArgs[1] = "Size";
                fmtArgs[2] = "Hits";
                fmtArgs[3] = "Misses";
                buf.append(_hdr.sprintf(fmtArgs))
                   .append(separator);
                fmtArgs[0] = "=====";
                fmtArgs[1] = "====";
                fmtArgs[2] = "====";
                fmtArgs[3] = "=====";
                buf.append(_hdr.sprintf(fmtArgs));

                for (Iterator i=getSortedCaches().iterator(); i.hasNext(); ) {
                    Cache cache = (Cache)i.next();
                    fmtArgs[0] = StringUtil.dotProximate(cache.getName(), 55);
                    fmtArgs[1] = new Integer(cache.getSize());
                    fmtArgs[2] = new Long(cache.getStatistics().getCacheHits());
                    fmtArgs[3] = new Long(cache.getStatistics().getCacheMisses());
                            
                    buf.append(separator)
                       .append(_fmt.sprintf(fmtArgs));
                }

                return buf.toString();
            }

            public String toString() {
                return "ehcache";
            }
        };
        DiagnosticThread.addDiagnosticObject(cacheDiagnostics);
    }

    //TODO call this again sometime after SessionFactory is created
    private static void createHQHibernateStatMBean()
        throws MalformedObjectNameException, InstanceAlreadyExistsException,
               MBeanRegistrationException, NotCompliantMBeanException
    {
        // If we are running within a unit test environment, we don't want 
        // to register this MBean since it is not properly removed on 
        // application undeployment. This causes any future application 
        // deployments within the same server instance to fail.
        if (Boolean.getBoolean(HQ_UNITTEST_RUN)) {
            return;
        }
        
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

        //TODO necessary?
        //configuration.addSqlFunction( "count", new ClassicCountFunction());
        //configuration.addSqlFunction( "avg", new ClassicAvgFunction());
        //configuration.addSqlFunction( "sum", new ClassicSumFunction()); 
    }

   

    /**
     * Returns the global SessionFactory.
     *
     * @return SessionFactory
     */
    public static SessionFactory getSessionFactory() {
        return Bootstrap.getBean(SessionFactory.class);
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
        return Bootstrap.getBean(LocalSessionFactoryBean.class).getConfiguration().getTableMappings();
    }
    
    /**
     * Generate a new ID for a class of the given type.
     * 
     * @param className the persisted class name, as per the .hbm descriptor:
     *                  e.g. org.hyperic.hq.appdef.server.session.CpropKey
     * @param o         The object which will be getting the new ID
     * 
     * @return an Integer id for the new object.  If your class uses Long IDs
     *         then that's too bad ... we'll have to write another method.
     */
    public static Integer generateId(String className, Object o) {
        SessionFactoryImplementor factImpl = 
            (SessionFactoryImplementor)getSessionFactory();
        IdentifierGenerator gen = factImpl.getIdentifierGenerator(className); 
        SessionImplementor sessImpl = (SessionImplementor)
            factImpl.getCurrentSession();
        return (Integer)gen.generate(sessImpl, o);
    }

    private static List getCaches() {
        CacheManager cacheManager = CacheManager.getInstance();
        String[] caches = cacheManager.getCacheNames();
        List res = new ArrayList(caches.length);
        
        for (int i=0; i<caches.length; i++) {
            res.add(cacheManager.getCache(caches[i]));
        }
        return res;
    }
    
    public static List getCacheHealths() {
        List caches = getCaches();
        List healths = new ArrayList(caches.size());
        for (Iterator it = caches.iterator(); it.hasNext(); ) {
            Cache cache = (Cache) it.next();
            Map health = new HashMap();
            health.put("region", cache.getName());
            health.put("size", new Integer(cache.getSize()));
            health.put("hits", new Integer(cache.getHitCount()));
            health.put("misses", new Integer(cache.getMissCountNotFound()));
            healths.add(health);
        }
        return healths;
    }
}
