package org.hyperic.hq.application;

public interface TransactionListener {
    /**
     * Called after a transaction was committed 
     */
    void afterCommit(boolean success);
}
