/*
 * NOTE: This copyright does *not* cover user programs that use HQ program
 * services by normal system calls through the application program interfaces
 * provided as part of the Hyperic Plug-in Development Kit or the Hyperic Client
 * Development Kit - this is merely considered normal use of the program, and
 * does *not* fall under the heading of "derived work". Copyright (C) [2004,
 * 2005, 2006], Hyperic, Inc. This file is part of HQ. HQ is free software; you
 * can redistribute it and/or modify it under the terms version 2 of the GNU
 * General Public License as published by the Free Software Foundation. This
 * program is distributed in the hope that it will be useful, but WITHOUT ANY
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR
 * A PARTICULAR PURPOSE. See the GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License along with
 * this program; if not, write to the Free Software Foundation, Inc., 59 Temple
 * Place, Suite 330, Boston, MA 02111-1307 USA.
 */

package org.hyperic.hq.hibernate;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.PersistenceException;

import org.hibernate.HibernateException;
import org.hyperic.hq.context.Bootstrap;
import org.springframework.orm.hibernate3.SessionFactoryUtils;
import org.springframework.orm.hibernate3.SessionHolder;
import org.springframework.orm.jpa.EntityManagerFactoryUtils;
import org.springframework.orm.jpa.EntityManagerHolder;
import org.springframework.orm.jpa.JpaCallback;
import org.springframework.orm.jpa.JpaTemplate;
import org.springframework.transaction.support.TransactionSynchronizationManager;

/**
 * This class manages the creation and deletion of Hibernate sessions.
 */
public class SessionManager {
  
    private static final SessionManager INSTANCE = new SessionManager();

    private EntityManagerFactory getEntityManagerFactory() {
        return Bootstrap.getBean(EntityManagerFactory.class);
    }

    private JpaTemplate getJpaTemplate() {
        return Bootstrap.getBean(JpaTemplate.class);
    }

    private SessionManager() {
    }

    public interface SessionRunner {
        void run() throws Exception;

        String getName();
    }

    /**
     * Run the passed runner in a session. If there is no session for the
     * current thread, one will be created for the operation and subsequently
     * closed. If a session is already in process, no additional sessions will
     * be created.
     */
    public static void runInSession(SessionRunner r) throws Exception {
        INSTANCE.runInSessionInternal(r);
    }

    private void runInSessionInternal(final SessionRunner r) throws Exception {
        boolean participate = false;
        try {

            if (TransactionSynchronizationManager.hasResource(getEntityManagerFactory())) {
                // Do not modify the Session: just set the participate flag.
                participate = true;
            } else {
                EntityManager entityManager = getEntityManagerFactory().createEntityManager();
                TransactionSynchronizationManager.bindResource(getEntityManagerFactory(), new EntityManagerHolder(entityManager));
            }
            JpaTemplate template = getJpaTemplate();
           
            template.execute(new JpaCallback() {
                
                public Object doInJpa(EntityManager em) throws PersistenceException {
                    try {
                        r.run();
                    } catch (Exception e) {
                        throw new HibernateException(e);
                    }
                    return null;
                }
            });
        } finally {
            if (!participate) {
                EntityManagerHolder entityManagerHolder= (EntityManagerHolder) TransactionSynchronizationManager.unbindResource(getEntityManagerFactory());
                EntityManagerFactoryUtils.closeEntityManager(entityManagerHolder.getEntityManager());
            }
        }

    }
}
