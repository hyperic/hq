package org.hyperic.hq.application;

public interface TransactionListener {
    /**
     * Called after a transaction was successfully committed 
     */
    void afterCommit();
}
