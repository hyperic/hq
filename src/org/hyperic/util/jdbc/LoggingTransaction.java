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

package org.hyperic.util.jdbc;

import javax.transaction.HeuristicMixedException;
import javax.transaction.HeuristicRollbackException;
import javax.transaction.NotSupportedException;
import javax.transaction.RollbackException;
import javax.transaction.SystemException;
import javax.transaction.UserTransaction;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * The LoggingTransaction is used for debugging transaction problems.
 *
 * Typical usage is to create a new instance of this with some base (real) 
 * transaction type, and bind it to javax.transaction.UserTransaction 
 */
public class LoggingTransaction 
    implements UserTransaction
{
    private static final Log _log = LogFactory.getLog(LoggingTransaction.class);
    private final UserTransaction _baseTx;
        
    public LoggingTransaction(UserTransaction baseTx) {
        _baseTx = baseTx;
    }

    public void begin() throws NotSupportedException, SystemException {
        _log.info("LogTx:  begin");
        _baseTx.begin();
    }

    public void commit() 
        throws RollbackException, HeuristicMixedException, 
               HeuristicRollbackException, SecurityException, 
               IllegalStateException, SystemException 
    {
        _log.info("LogTx:  commit");
        _baseTx.commit();
    }

    public int getStatus() throws SystemException {
        _log.info("LogTx:  getStatus: " + _baseTx.getStatus());
        return _baseTx.getStatus();
    }

    public void rollback() 
        throws IllegalStateException, SecurityException, SystemException 
    { 
        _log.info("LogTx:  rollback");
        _baseTx.rollback();
    }

    public void setRollbackOnly() 
        throws IllegalStateException, SystemException 
    {
        _log.info("LogTx:  setRollbackOnly");
        _baseTx.setRollbackOnly();
    }

    public void setTransactionTimeout(int arg0) throws SystemException {
        _log.info("LogTx:  setTransactionTimeout");
        _baseTx.setTransactionTimeout(arg0);
    }
}
