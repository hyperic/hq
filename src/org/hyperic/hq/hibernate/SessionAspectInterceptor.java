package org.hyperic.hq.hibernate;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hibernate.HibernateException;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hyperic.hibernate.Util;

/**
 * This is the internal class which gets invoked when a session bean's
 * public method is caught by the aspect.
 * 
 * See also:  etc/aspects/SessionAspect.aj
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

    private boolean setupSessionInternal(String dbgTxt) {
        Session s = (Session)_sessions.get();
        
        if (s == null) {
            if (dbgTxt != null)
                _log.info("New Session:  [" + dbgTxt + "]");
            
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
            s.close();
        } catch(HibernateException e) {
            _log.warn("Error closing session", e);
        }
    }

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
