/*
 * NOTE: This copyright does *not* cover user programs that use HQ
 * program services by normal system calls through the application
 * program interfaces provided as part of the Hyperic Plug-in Development
 * Kit or the Hyperic Client Development Kit - this is merely considered
 * normal use of the program, and does *not* fall under the heading of
 * "derived work".
 * 
 * Copyright (C) [2004-2009], Hyperic, Inc.
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

package org.hyperic.hq.bizapp.server.session;

import javax.annotation.PreDestroy;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hyperic.hq.bizapp.shared.LatherBoss;
import org.hyperic.hq.context.Bootstrap;
import org.hyperic.lather.LatherContext;
import org.hyperic.lather.LatherRemoteException;
import org.hyperic.lather.LatherValue;
import org.springframework.transaction.annotation.Transactional;

/**
 * Lather Boss.
 * 
 */

@Transactional
public class LatherBossImpl
    implements LatherBoss
{
    private final Log log =
        LogFactory.getLog(LatherBossImpl.class.getName());

    private LatherDispatcher dispatcher;
    
    
   
    public LatherBossImpl(LatherDispatcher dispatcher) {
        this.dispatcher = dispatcher;
    }
    @PreDestroy
    public void destroyDispatcher() {
        if (dispatcher != null) {
            dispatcher.destroy();
        }
    }
    /**
     * 
     */
    public LatherValue dispatchWithTx(LatherContext ctx, String method, 
                                      LatherValue arg)
        throws LatherRemoteException
    {
        
       return dispatchWithoutTx(ctx, method, arg);
        
    }

    /**
     * 
     * 
     */
    public LatherValue dispatchWithoutTx(LatherContext ctx, String method, 
                                         LatherValue arg)
        throws LatherRemoteException
    {
        try {
            return dispatcher.dispatch(ctx, method, arg);
        } catch(RuntimeException exc){
            log.error("Error dispatching method '" + method + "'", exc);
            throw new LatherRemoteException("Runtime exception: " + 
                                            exc.getMessage());
        }
    }

    /**
     * The main dispatch command which is called via the JBoss-lather 
     * servlet.  It is the responsibility of this routine to take the
     * method/args, and route it to the correct method.
     *
     * @param ctx     Information about the remote caller
     * @param method  Name of the method to invoke
     * @param arg     LatherValue argument object to pass to the method
     *
     * @return an instantiated subclass of the LatherValue class, 
     *         representing the result of the invoked method
     *
     * 
     * 
     */
    public LatherValue dispatch(LatherContext ctx, String method, 
                                LatherValue arg)
        throws LatherRemoteException
    {
        if (dispatcher.methIsTransactional(method)) {
            return dispatchWithTx(ctx, method, arg);
        } else {
            return dispatchWithoutTx(ctx, method, arg);
        }
    }

   
    public static LatherBoss getOne() {
        return Bootstrap.getBean(LatherBoss.class);
    }

}
