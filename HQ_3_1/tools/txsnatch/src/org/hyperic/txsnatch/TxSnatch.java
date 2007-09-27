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
package org.hyperic.txsnatch;

import org.jboss.ejb.Interceptor;
import org.jboss.ejb.plugins.AbstractInterceptor;
import org.jboss.invocation.Invocation;

/**
 * The Transaction Snatcher allow us to sneak into JBosses transaction
 * interceptor chain and invoke code within our application.
 * 
 * This is accomplished by setting the snatcher once the app has begun
 * loading
 */
public class TxSnatch 
    extends AbstractInterceptor
{
    private static final Object INIT_LOCK = new Object();
    private static Snatcher _snatcher;
    
    public interface Snatcher {
        Object invokeNext(Interceptor next, Invocation v) throws Exception;
        Object invokeHomeNext(Interceptor next, Invocation v) throws Exception;
        Object invokeProxyNext(org.jboss.proxy.Interceptor next,
                               Invocation v) throws Throwable;
    }
    
    public static void setSnatcher(Snatcher snatcher) {
        synchronized (INIT_LOCK) {
            _snatcher = snatcher;
        }
    }
    
    static Snatcher getSnatcher() {
        synchronized (INIT_LOCK) {
            return _snatcher;
        }
    }
    
    public Object invoke(Invocation v) throws Exception {
        Snatcher s;
        
        synchronized (INIT_LOCK) {
            s = _snatcher;
        }

        if (s != null)  {
            return s.invokeNext(getNext(), v);
        }
        return super.invoke(v);
    }

    public Object invokeHome(Invocation v) throws Exception {
        Snatcher s;

        synchronized (INIT_LOCK) {
            s = _snatcher;
        }

        if (s != null) { 
            return s.invokeHomeNext(getNext(), v);
        }
        return super.invokeHome(v);
    }
}
