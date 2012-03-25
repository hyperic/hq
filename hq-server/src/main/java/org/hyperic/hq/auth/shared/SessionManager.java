/*
 * NOTE: This copyright does *not* cover user programs that use HQ
 * program services by normal system calls through the application
 * program interfaces provided as part of the Hyperic Plug-in Development
 * Kit or the Hyperic Client Development Kit - this is merely considered
 * normal use of the program, and does *not* fall under the heading of
 * "derived work".
 * 
 * Copyright (C) [2004-2008], Hyperic, Inc.
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

package org.hyperic.hq.auth.shared;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hyperic.hq.application.Scheduler;
import org.hyperic.hq.authz.server.session.AuthzSubject;
import org.hyperic.hq.context.Bootstrap;
import org.hyperic.hq.measurement.MeasurementConstants;
import org.hyperic.util.collection.IntHashMap;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
@Component
public class SessionManager {
    private final Log _log = LogFactory.getLog(SessionManager.class);
    private final Random _random = new Random();
    private final IntHashMap _cache = new IntHashMap();
    private final Map<String, Integer> _userToSessionId = new HashMap<String, Integer>();
    private static final long DEFAULT_TIMEOUT = 90 * 1000 * 60;
    private static final long HOUR = MeasurementConstants.HOUR;

    @Autowired
    public SessionManager(Scheduler scheduler) {
        final Runnable task = new Runnable() {
            public void run() {
                try {
                    _log.info("cleaning up expired sessions");
                    int num = cleanupExpired();
                    _log.info("done cleaning up expired sessions (" + num + 
                        " expired sessions)");
                } catch (Throwable t) {
                    _log.error(t, t);
                }
            }
        };
        scheduler.scheduleAtFixedRate(
            task, HOUR, HOUR);
    }

    public static SessionManager getInstance() {
        return Bootstrap.getBean(SessionManager.class);
    }

    /**
     * Associates a userid with a session id.  Uses default timeout.
     *
     * @param subject The AuthzSubjectValue to store
     * @return The session id
     */
    public synchronized int put(AuthzSubject subject) {
        return put(subject, DEFAULT_TIMEOUT);
    }

    /**
     * Associates a userid with a session id
     *
     * @param subject The AuthzSubjectValue to store
     * @param timeout The timeout for the session in milliseconds
     * @return The session id
     */
    public synchronized int put(AuthzSubject subject, long timeout) {

        int key;

        do {
            key = _random.nextInt();
        } while (_cache.containsKey(key));
        
        if (_log.isDebugEnabled()) {
        	String name = null;
        	
        	if (subject != null) {
        		name = subject.getName();
        	}
        	
            _log.debug("adding session with user=" + name +
                ",sessionId=" + key + ",timeout=" + timeout);
        }
        _cache.put(key, new AuthSession(subject, timeout));
        if (null != subject) {
        	_userToSessionId.put(subject.getName(), key);
        }
        return key;
    }

    /**
     * Lookup and return sessionId given their username.
     *
     * @param username The username.
     * @return sessionId 
     */
    public synchronized int getIdFromUsername (String username) 
    		throws SessionNotFoundException, SessionTimeoutException {
    	
    	Integer sessionId = _userToSessionId.get(username);
    	if (null != sessionId) {
    		AuthSession session = (AuthSession) _cache.get(sessionId);
    		if (null == session) {
    			_userToSessionId.remove(username);
    			throw new SessionNotFoundException();
    		}
    		// check expiration...
    		if (session.isExpired()) {
    			invalidate(sessionId);
    			throw new SessionTimeoutException();
    		} 
    	}
    	else {
    		throw new SessionNotFoundException();
    	}
    	return sessionId;

    }

    /**
     * Returns a userid given a session id
     *
     * @param sessionId The session id
     * @return The user id
     */
    public synchronized Integer getId(int sessionId) 
        throws SessionNotFoundException, SessionTimeoutException
    {
        return getSubject(sessionId).getId();
    }

    public AuthzSubject getSubject(Integer sessionId) 
        throws SessionNotFoundException, SessionTimeoutException
    {
        return getSubject(sessionId.intValue());
    }
    
    public synchronized AuthzSubject getSubject(int sessionId) 
        throws SessionNotFoundException, SessionTimeoutException
    {
        AuthSession session = (AuthSession)_cache.get(sessionId);
        
        if (session == null) {
            throw new SessionNotFoundException();
        }

        if (session.isExpired()) {
            invalidate(sessionId);

            throw new SessionTimeoutException();
        }

        return session.getAuthzSubject();
    }

    /**
     * Simply perform an authentication when you don't need the actual subject
     */
    public void authenticate(int sessionId)
        throws SessionNotFoundException, SessionTimeoutException
    {
        getSubject(sessionId);
    }
    
    /**
     * Remove the indicated session
     *
     * @param sessionId The session id
     */
    public synchronized void invalidate(int sessionId) {
    	String name = null;	
    	AuthSession sess = (AuthSession)_cache.remove(sessionId);
    	if (sess != null && sess.getAuthzSubject() != null) {
    		name = sess.getAuthzSubject().getName();
    	}
    	if (null != name) {
    		_userToSessionId.remove(name);
    	}
    	if (_log.isDebugEnabled()) {
    		_log.debug("removed session sessionId=" + sessionId +
    				",user=" + name);
    	}
    }
    
    private synchronized int cleanupExpired() {
        int rtn = 0;
        final int[] keys = _cache.getKeys();
        for (int i=0; i<_cache.getKeys().length; i++) {
            final int sessionID = keys[i];
            AuthSession sess = (AuthSession)_cache.get(sessionID);
            if (sess == null) {
                // ignore spaces in intHashMap
                continue;
            } else if (sess.isExpired()) {
                rtn++;
                invalidate(sessionID);
            }
        }
        return rtn;
    }
}
