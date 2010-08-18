/**
 * NOTE: This copyright does *not* cover user programs that use HQ
 * program services by normal system calls through the application
 * program interfaces provided as part of the Hyperic Plug-in Development
 * Kit or the Hyperic Client Development Kit - this is merely considered
 * normal use of the program, and does *not* fall under the heading of
 *  "derived work".
 *
 *  Copyright (C) [2010], VMware, Inc.
 *  This file is part of HQ.
 *
 *  HQ is free software; you can redistribute it and/or modify
 *  it under the terms version 2 of the GNU General Public License as
 *  published by the Free Software Foundation. This program is distributed
 *  in the hope that it will be useful, but WITHOUT ANY WARRANTY; without
 *  even the implied warranty of MERCHANTABILITY or FITNESS FOR A
 *  PARTICULAR PURPOSE. See the GNU General Public License for more
 *  details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program; if not, write to the Free Software
 *  Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307
 *  USA.
 *
 */

package org.hyperic.hibernate;

import javax.management.MBeanServer;
import javax.management.ObjectName;

import org.hibernate.HibernateException;
import org.hibernate.jmx.StatisticsService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.orm.hibernate3.LocalSessionFactoryBean;

/**
 * Extension of the {@link LocalSessionFactoryBean} that registers/unregisters
 * the Hibernate stats MBean for the SessionFactory
 * @author jhickey
 * 
 */
public class JmxExposingLocalSessionFactoryBean
    extends LocalSessionFactoryBean {
    
    private MBeanServer mbeanServer;
    private static final String HIBERNATE_STATS_OBJECT_NAME = "Hibernate:type=statistics,application=hq";

    @Override
    public void destroy() throws HibernateException {
        super.destroy();
        try {
            mbeanServer.unregisterMBean(new ObjectName(HIBERNATE_STATS_OBJECT_NAME));
        } catch (Exception e) {
            logger.warn("Error unregistering Hibernate Stats MBean", e);
        }
    }

    @Override
    protected void afterSessionFactoryCreation() throws Exception {
        ObjectName on = new ObjectName(HIBERNATE_STATS_OBJECT_NAME);
        StatisticsService mBean = new StatisticsService();
        mBean.setSessionFactory(getSessionFactory());
        mbeanServer.registerMBean(mBean, on);
    }

    @Autowired
    public void setMbeanServer(MBeanServer mbeanServer) {
        this.mbeanServer = mbeanServer;
    }

}
