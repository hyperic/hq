package org.hyperic.hibernate.dao;

import org.hibernate.Session;

/**
 * This class is specifically meant to be used when running unit tests
 * outside of the J2EE container with mockejb.
 */
public class HibernateMockDAOFactory extends HibernateDAOFactory
{
    private Session session;

    public Session getCurrentSession()
    {
        return session;
    }

    public void setCurrentSession(Session session)
    {
        this.session = session;
    }
}
