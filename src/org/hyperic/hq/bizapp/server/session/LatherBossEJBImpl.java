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

import javax.ejb.CreateException;
import javax.ejb.SessionBean;
import javax.ejb.SessionContext;
import javax.naming.NamingException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hyperic.hq.bizapp.shared.LatherBossLocal;
import org.hyperic.hq.bizapp.shared.LatherBossUtil;
import org.hyperic.hq.common.SystemException;
import org.hyperic.lather.LatherContext;
import org.hyperic.lather.LatherRemoteException;
import org.hyperic.lather.LatherValue;

/**
 * Lather Boss.
 * 
 * @ejb:bean name="LatherBoss"
 *      jndi-name="ejb/bizapp/LatherBoss"
 *      local-jndi-name="LocalLatherBoss"
 *      view-type="both"
 *      type="Stateless"
 * @ejb:transaction type="Required"
 */
public class LatherBossEJBImpl
    extends BizappSessionEJB
    implements SessionBean
{
    private final Log _log =
        LogFactory.getLog(LatherBossEJBImpl.class.getName());

    private LatherDispatcher _dispatcher = new LatherDispatcher();
    private SessionContext _ctx;

    /**
     * @ejb:interface-method
     */
    public LatherValue dispatchWithTx(LatherContext ctx, String method, 
                                      LatherValue arg)
        throws LatherRemoteException
    {
        try {
            return dispatchWithoutTx(ctx, method, arg);
        } catch (LatherRemoteException e) {
            _ctx.setRollbackOnly();
            throw e;
        }
    }

    /**
     * 
     * @ejb:interface-method
     */
    public LatherValue dispatchWithoutTx(LatherContext ctx, String method, 
                                         LatherValue arg)
        throws LatherRemoteException
    {
        try {
            return _dispatcher.dispatch(ctx, method, arg);
        } catch(RuntimeException exc){
            _log.error("Error dispatching method '" + method + "'", exc);
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
     * @ejb:interface-method
     */
    public LatherValue dispatch(LatherContext ctx, String method, 
                                LatherValue arg)
        throws LatherRemoteException
    {
        if (_dispatcher.methIsTransactional(method)) {
            return getOne().dispatchWithTx(ctx, method, arg);
        } else {
            return dispatchWithoutTx(ctx, method, arg);
        }
    }

    public void ejbCreate() {}

    public void ejbRemove() {
        if (_dispatcher != null) {
            _dispatcher.destroy();
        }
    }
    
    public static LatherBossLocal getOne() {
        try {
            return LatherBossUtil.getLocalHome().create();
        } catch (CreateException e) {
            throw new SystemException(e);
        } catch (NamingException e) {
            throw new SystemException(e);
        }
    }

    public void ejbActivate() {}
    public void ejbPassivate() {}
    public void setSessionContext(SessionContext ctx) {
        _ctx = ctx;
    }
}
