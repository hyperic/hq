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

import java.sql.SQLException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hibernate.FlushMode;
import org.hibernate.HibernateException;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hyperic.hq.context.Bootstrap;
import org.springframework.orm.hibernate3.HibernateCallback;
import org.springframework.orm.hibernate3.HibernateTemplate;
import org.springframework.orm.hibernate3.SessionFactoryUtils;
import org.springframework.orm.hibernate3.SessionHolder;
import org.springframework.transaction.support.TransactionSynchronizationManager;

/**
 * This class manages the creation and deletion of Hibernate sessions.
 */
public class SessionManager {
    private static final Log _log = LogFactory.getLog(SessionManager.class);

    private static final SessionManager INSTANCE = new SessionManager();

    private SessionFactory getSessionFactory() {
        return Bootstrap.getBean(SessionFactory.class);
    }

    private HibernateTemplate getHibernateTemplate() {
        return Bootstrap.getBean(HibernateTemplate.class);
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

            if (TransactionSynchronizationManager.hasResource(getSessionFactory())) {
                // Do not modify the Session: just set the participate flag.
                participate = true;
            } else {
                Session session = SessionFactoryUtils.getSession(getSessionFactory(), true);
                session.setFlushMode(FlushMode.MANUAL);
                TransactionSynchronizationManager.bindResource(getSessionFactory(), new SessionHolder(session));
            }
            HibernateTemplate template = getHibernateTemplate();
            template.execute(new HibernateCallback() {
                public Object doInHibernate(Session session) throws HibernateException, SQLException {
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

                // single session mode
                SessionHolder sessionHolder = (SessionHolder) TransactionSynchronizationManager.unbindResource(getSessionFactory());
                SessionFactoryUtils.closeSession(sessionHolder.getSession());
            }
        }

    }

    public static Session currentSession() {
        Session res = INSTANCE.getSessionFactory().getCurrentSession();

        if (res == null) {
            throw new HibernateException("Unable to find current session");
        }
        return res;
    }
}
