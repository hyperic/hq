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

package org.hyperic.snmp;

import java.util.HashMap;

import java.lang.reflect.Proxy;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.hyperic.util.timer.StopWatch;

/**
 * SNMPSession interface cache.
 * Cache is per-session instance.
 * Currently supports getColumn and getBulk methods only.
 */
class SNMPSessionCache 
    implements InvocationHandler {

    private SNMPSession session;

    private HashMap columnCache = new HashMap();
    private HashMap bulkCache = new HashMap();
    private HashMap tableCache = new HashMap();
    private static Log log = LogFactory.getLog(SNMPSessionCache.class);

    public static final int EXPIRE_DEFAULT = 30 * 1000; //30 seconds
    private int expire;

    SNMPSessionCache(SNMPSession session, int expire) {
        this.session = session;
        this.expire = expire;
    }

    static SNMPSession newInstance(SNMPSession session, int expire)
        throws SNMPException {

        SNMPSessionCache handler = new SNMPSessionCache(session, expire);
        SNMPSession sessionCache;

        try {
            sessionCache = (SNMPSession)
                Proxy.newProxyInstance(SNMPSession.class.getClassLoader(),
                                       new Class[] { SNMPSession.class },
                                       handler);
        } catch (Exception e) {
            throw new SNMPException(e.getMessage());
        }

        return sessionCache;
    }

    private SNMPCacheObject getFromCache(long timeNow,
                                         HashMap cache,
                                         String name,
                                         Object arg) {

        SNMPCacheObject cacheVal = (SNMPCacheObject)cache.get(arg);

        String argDebug = "";

        if (log.isDebugEnabled()) {
            argDebug = " with arg=" + arg;
        }

        if (cacheVal == null) {
            cacheVal = new SNMPCacheObject();
            cacheVal.expire = this.expire;
            cache.put(arg, cacheVal);
        }
        else if ((timeNow - cacheVal.timestamp) > cacheVal.expire) {
            if (log.isDebugEnabled()) {
                log.debug("expiring " + name +
                          " from cache" + argDebug);
            }
            cacheVal.value = null;
        }

        return cacheVal;
    }

    private StringBuffer invokerToString(String name,
                                         Object[] args,
                                         Object cacheKey) {
        StringBuffer invoker =
            new StringBuffer(name);

        invoker.append('(');

        if (args.length != 0) {
            String arg = args[0].toString();

            invoker.append(arg);

            for (int i=1; i<args.length; i++) {
                invoker.append('.').append(args[i].toString());
            }

            if ((cacheKey != null) &&
                !arg.toString().equals(cacheKey)) {
                //note real cache key to match up with expire log
                invoker.append('/').append(cacheKey.toString());
            }

            invoker.append(')');
        }

        return invoker;
    }

    public Object invoke(Object proxy, Method method, Object[] args)
        throws SNMPException {

        SNMPCacheObject cacheVal = null;
        HashMap cache = null;

        Object cacheKey = null;
        Object retval;
        String name = method.getName();

        long timeNow = 0;

        //XXX perhaps more later
        if (name.equals("getBulk")) {
            cache = this.bulkCache;
            cacheKey = args[0];
        }
        else if (name.equals("getTable")) {
            cache = this.tableCache;
            cacheKey =
                new Integer(args[0].hashCode() ^
                            args[1].hashCode());
        }
        else if (name.equals("getColumn")) {
            cache = this.columnCache;
            cacheKey = args[0];
        }

        if (cache != null) {
            timeNow = System.currentTimeMillis();
            cacheVal = getFromCache(timeNow, cache, name, cacheKey);

            if (cacheVal.value != null) {
                return cacheVal.value;
            }
        }

        try {
            retval = method.invoke(this.session, args);
        } catch (InvocationTargetException e) {
            Throwable t =
                ((InvocationTargetException)e).
                getTargetException();

            String msg;

            if (t instanceof MIBLookupException) {
                throw (MIBLookupException)t;
            }
            if (t instanceof SNMPException) {
                msg = "";
            }
            else {
                msg = t.getClass().getName() + ": ";
            }
            
            msg += t.getMessage() + " invoking: " +
                invokerToString(name, args, cacheKey);
            
            throw new SNMPException(msg, t);
        } catch (Exception e) {
            String msg =
                e.getClass().getName() + ": " +
                e.getMessage() + " invoking: " +
                invokerToString(name, args, cacheKey);
            
            throw new SNMPException(msg, e);
        }

        if (cacheVal != null) {
            cacheVal.value = retval;
            cacheVal.timestamp = timeNow;

            if (log.isDebugEnabled()) {
                log.debug(invokerToString(name, args, cacheKey) +
                          " took: " + new StopWatch(timeNow));
            }
        }

        return retval;
    }
}
