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
import java.util.Iterator;

import org.hibernate.Hibernate;
import org.hibernate.SessionFactory;
import org.hibernate.dialect.Dialect;
import org.hibernate.engine.SessionFactoryImplementor;
import org.hibernate.engine.SessionImplementor;
import org.hibernate.id.IdentifierGenerator;
import org.hyperic.hibernate.dialect.HQDialect;
import org.hyperic.hq.context.Bootstrap;
import org.springframework.orm.hibernate3.LocalSessionFactoryBean;

public class Util {
    
  
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
