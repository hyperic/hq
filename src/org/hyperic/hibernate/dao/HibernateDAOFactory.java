package org.hyperic.hibernate.dao;

import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hyperic.dao.DAOFactory;
import org.hyperic.hibernate.Util;

/**
 *
 */
public class HibernateDAOFactory extends DAOFactory
{
    private static SessionFactory sessionFactory;

    protected Session getCurrentSession()
    {
        if (sessionFactory == null) {
            synchronized(this) {
                // cache session factory, we cache it because hibernate session factory
                // lookup is an rather expensive JNDI call
                if (sessionFactory == null) {
                    sessionFactory  = Util.getSessionFactory();
                }
            }
        }
        return sessionFactory.getCurrentSession();
    }

    public HibernateDAOFactory()
    {
    }

    public AgentDAO getAgentDAO()
    {
        return new AgentDAO(getCurrentSession());
    }

    public ApplicationDAO getApplicationDAO()
    {
        return new ApplicationDAO(getCurrentSession());
    }

    public ConfigResponseDAO getConfigResponseDAO()
    {
        return new ConfigResponseDAO(getCurrentSession());
    }

    public CpropDAO getCpropDAO()
    {
        return new CpropDAO(getCurrentSession());
    }

    public CpropKeyDAO getCpropKeyDAO()
    {
        return new CpropKeyDAO(getCurrentSession());
    }

    public PlatformDAO getPlatformDAO()
    {
        return new PlatformDAO(getCurrentSession());
    }

    public ServerDAO getServerDAO()
    {
        return new ServerDAO(getCurrentSession());
    }

    public ServiceDAO getServiceDAO()
    {
        return new ServiceDAO(getCurrentSession());
    }

    public ServiceTypeDAO getServiceTypeDAO()
    {
        return new ServiceTypeDAO(getCurrentSession());
    }
}
