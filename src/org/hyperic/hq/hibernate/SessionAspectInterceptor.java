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

package org.hyperic.hq.hibernate;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hibernate.HibernateException;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hyperic.hibernate.Util;

/**
 * This class manages the creation and deletion of Hibernate sessions.
 */
public class SessionAspectInterceptor { 
    private static final Log _log = 
        LogFactory.getLog(SessionAspectInterceptor.class);

    private static final SessionAspectInterceptor INSTANCE =
        new SessionAspectInterceptor();

    private ThreadLocal    _sessions = new ThreadLocal();
    private SessionFactory _factory = Util.getSessionFactory();
    
    private SessionAspectInterceptor() {
    }

    public interface SessionRunner {
        void run() throws Exception;
        String getName();
    }
    
    /**
     * Run the passed runner in a session.  If there is no session for the 
     * current thread, one will be created for the operation and subsequently
     * closed.  If a session is already in process, no additional sessions
     * will be created. 
     */
    public static void runInSession(SessionRunner r) 
        throws Exception
    {
        INSTANCE.runInSessionInternal(r);
    }
    
    private void runInSessionInternal(SessionRunner r) 
        throws Exception
    {
        boolean setup = false;
        
        try {
            setup = setupSessionInternal(r.getName());
            r.run();
        } finally {
            if (setup)
                cleanupSessionInternal();
        }
    }
    
    private boolean setupSessionInternal(String dbgTxt) {
        Session s = (Session)_sessions.get();
        
        if (s == null) {
            if (dbgTxt != null && false /* Disabled for now */) {
                _log.info("New Session:  [" + dbgTxt + "]");
            }
            
            if (_log.isDebugEnabled()) {
                _log.debug("Setting up session for Thread[" + 
                           Thread.currentThread().getName() + "]");
            }
            s = _factory.openSession();
            _sessions.set(s);
            return true;
        }
        return false;
    }
    
    /**
     * Create a session if it does not exist.
     * @param dbgTxt text to print when creating a session
     * @return true if a session was created
     */
    public static boolean setupSession(String dbgTxt) {
        return INSTANCE.setupSessionInternal(dbgTxt);
    }
    
    private void cleanupSessionInternal() {
        Session s = (Session)_sessions.get();
        
        try {
            if (_log.isDebugEnabled()) {
                _log.debug("Closing session for Thread[" + 
                           Thread.currentThread().getName() + "]");
            }
            
            _sessions.set(null);
            s.flush();
            s.close();
        } catch(HibernateException e) {
            _log.warn("Error closing session", e);
        }
    }

    /**
     * Close the current session.
     */
    public static void cleanupSession() {
        INSTANCE.cleanupSessionInternal();
    }
    
    public static Session currentSession() {
        Session res = (Session)INSTANCE._sessions.get();
        
        if (res == null) {
            throw new HibernateException("Unable to find current session");
        }
        return res;
    }
}
