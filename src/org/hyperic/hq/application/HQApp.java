package org.hyperic.hq.application;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * This class represents the central concept of the Hyperic HQ application.  
 * (not the Application resource)
 */
public class HQApp { 
    private static final HQApp INSTANCE = new HQApp(); 
    private final Log _log = LogFactory.getLog(HQApp.class);
    
    private ThreadLocal _txListeners = new ThreadLocal();

    /**
     * Register a listener to be called after a tx has been successfully
     * committed.
     */
    public void addTransactionListener(TransactionListener listener) {
        List listeners = (List)_txListeners.get();
        
        if (listeners == null) {
            listeners = new ArrayList(1);
            _txListeners.set(listeners);
        }
        
        listeners.add(listener);
    }

    /**
     * Execute all the post-commit listeners registered with the current thread
     */
    public void runPostCommitListeners() {
        List list = (List)_txListeners.get();
        
        if (list == null)
            return;
        
        for (Iterator i=list.iterator(); i.hasNext(); ) {
            TransactionListener l = (TransactionListener)i.next();
            
            try {
                l.afterCommit();
            } catch(Exception e) {
                _log.warn("Error running commit listener [" + l + "]", e);
            }
        }
    }
    
    public static HQApp getInstance() {
        return INSTANCE;
    }
}
