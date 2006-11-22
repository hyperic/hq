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
