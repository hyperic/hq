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

 package org.hyperic.dao;

import org.hibernate.Session;
import org.hyperic.hibernate.dao.AgentDAO;
import org.hyperic.hibernate.dao.ApplicationDAO;
import org.hyperic.hibernate.dao.AuthzSubjectDAO;
import org.hyperic.hibernate.dao.ConfigResponseDAO;
import org.hyperic.hibernate.dao.CpropDAO;
import org.hyperic.hibernate.dao.CpropKeyDAO;
import org.hyperic.hibernate.dao.HibernateDAOFactory;
import org.hyperic.hibernate.dao.HibernateMockDAOFactory;
import org.hyperic.hibernate.dao.PlatformDAO;
import org.hyperic.hibernate.dao.PlatformTypeDAO;
import org.hyperic.hibernate.dao.ServerDAO;
import org.hyperic.hibernate.dao.ServerTypeDAO;
import org.hyperic.hibernate.dao.ServiceDAO;
import org.hyperic.hibernate.dao.ServiceTypeDAO;
import org.hyperic.hibernate.dao.TriggerDAO;
import org.hyperic.hibernate.dao.AgentTypeDAO;
import org.hyperic.hibernate.dao.BaselineDAO;
import org.hyperic.hibernate.dao.CategoryDAO;

public abstract class DAOFactory
{
    public static final int HIBERNATE = 1;
    public static final int HIBERNATE_MOCKTEST = 2;

    public static int DEFAULT = HIBERNATE;

    public abstract AgentDAO getAgentDAO();
    public abstract AgentTypeDAO getAgentTypeDAO();
    public abstract ApplicationDAO getApplicationDAO();
    public abstract ConfigResponseDAO getConfigResponseDAO();
    public abstract CpropDAO getCpropDAO();
    public abstract CpropKeyDAO getCpropKeyDAO();
    public abstract PlatformDAO getPlatformDAO();
    public abstract PlatformTypeDAO getPlatformTypeDAO();
    public abstract ServerDAO getServerDAO();
    public abstract ServerTypeDAO getServerTypeDAO();
    public abstract ServiceDAO getServiceDAO();
    public abstract TriggerDAO getTriggerDAO();
    public abstract ServiceTypeDAO getServiceTypeDAO();
    public abstract AuthzSubjectDAO getAuthzSubjectDAO();

    // Measurement DAOs
    public abstract BaselineDAO getBaselineDAO();
    public abstract CategoryDAO getCategoryDAO();

    public static ThreadLocal defaultSession = new ThreadLocal();

    public static DAOFactory getDAOFactory()
    {
        return getDAOFactory(DEFAULT);
    }

    /**
     * @return mock hibernate factory suitable for use with mockejb
     */
    public static DAOFactory getMockDAOFactory(Session session)
    {
        HibernateMockDAOFactory factory =
            (HibernateMockDAOFactory)getDAOFactory(HIBERNATE_MOCKTEST);
        factory.setCurrentSession(session);
        return factory;
    }

    public static DAOFactory getDAOFactory(int which)
    {
        switch (which) {
        case HIBERNATE:
            return new HibernateDAOFactory();
        case HIBERNATE_MOCKTEST:
            HibernateMockDAOFactory factory = new HibernateMockDAOFactory();
            factory.setCurrentSession((Session)defaultSession.get());
            return factory;
        }
        throw new RuntimeException("DAOFactory type not found: " + which);
    }

    public static void setDefaultDAOFactory(int which)
    {
        DEFAULT = which;
    }

    public static void setMockSession(Session session)
    {
        defaultSession.set(session);
    }
}
