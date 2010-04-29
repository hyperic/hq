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
