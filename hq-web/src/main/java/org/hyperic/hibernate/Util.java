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
import javax.management.MalformedObjectNameException;
import javax.management.NotCompliantMBeanException;

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
import org.hyperic.hibernate.dialect.HQDialect;
import org.hyperic.hq.common.DiagnosticObject;
import org.hyperic.hq.common.DiagnosticsLogger;
import org.hyperic.hq.context.Bootstrap;
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
  
    
    private static Log log = LogFactory.getLog(Util.class);

    

    //TODO call this again sometime after SessionFactory is created
    private static void createHQHibernateStatMBean()
        throws MalformedObjectNameException, InstanceAlreadyExistsException,
               MBeanRegistrationException, NotCompliantMBeanException
    {
       
       /** 
        // get MBeanServer
        MBeanServer server = MBeanUtil.getMBeanServer();

        //build the MBean name
        ObjectName on =
            new ObjectName("Hibernate:type=statistics,application=hq");
        StatisticsService mBean = new StatisticsService();
        mBean.setSessionFactory(getSessionFactory());
        server.registerMBean(mBean, on);
        log.info("HQ Hibernate Statistics MBean registered " + on);
        **/
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
}
