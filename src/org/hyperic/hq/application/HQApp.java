package org.hyperic.hq.application;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hibernate.action.Executable;
import org.hibernate.impl.SessionImpl;
import org.hyperic.hibernate.Util;

/**
 * This class represents the central concept of the Hyperic HQ application.  
 * (not the Application resource)
 */
public class HQApp { 
    private static final HQApp INSTANCE = new HQApp(); 
    private final Log _log = LogFactory.getLog(HQApp.class);
    
    private ThreadLocal _txListeners = new ThreadLocal();

    private void scheduleCommitCallback() {
        SessionImpl s = (SessionImpl)
            Util.getSessionFactory().getCurrentSession();
        
        s.getActionQueue().execute(new Executable() {
            public void afterTransactionCompletion(boolean success) {
                runPostCommitListeners(success);
            }

            public void beforeExecutions() {}

            public void execute() {}

            public Serializable[] getPropertySpaces() {
                return new Serializable[0];
            }

            public boolean hasAfterTransactionCompletion() {
                return true;
            }
        });
    }
    
    /**
     * Register a listener to be called after a tx has been successfully
     * committed.
     */
    public void addTransactionListener(TransactionListener listener) {
        List listeners = (List)_txListeners.get();
        
        if (listeners == null) {
            listeners = new ArrayList(1);
            _txListeners.set(listeners);
            scheduleCommitCallback();
        }
        
        listeners.add(listener);
    }

    /**
     * Execute all the post-commit listeners registered with the current thread
     */
    private void runPostCommitListeners(boolean success) {
        List list = (List)_txListeners.get();
        
        if (list == null)
            return;
        
        try {
            for (Iterator i=list.iterator(); i.hasNext(); ) {
                TransactionListener l = (TransactionListener)i.next();
            
                try {
                    l.afterCommit(success);
                } catch(Exception e) {
                    _log.warn("Error running commit listener [" + l + "]", e);
                }
            } 
        } finally {
            list.clear();
        }
    }
    
    public static HQApp getInstance() {
        return INSTANCE;
    }
}
