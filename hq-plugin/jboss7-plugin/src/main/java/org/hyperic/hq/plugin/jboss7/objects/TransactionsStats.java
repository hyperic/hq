package org.hyperic.hq.plugin.jboss7.objects;

public class TransactionsStats {
private String numberOfAbortedTransactions;
private String numberOfApplicationRollbacks;
private String numberOfCommittedTransactions;
private String numberOfHeuristics;
private String numberOfInflightTransactions;
private String numberOfNestedTransactions;
private String numberOfResourceRollbacks;
private String numberOfTimedOutTransactions;
private String numberOfTransactions;

    /**
     * @return the abortedTransactions
     */
    public String getAbortedTransactions() {
        return numberOfAbortedTransactions;
    }

    /**
     * @param abortedTransactions the abortedTransactions to set
     */
    public void setAbortedTransactions(String abortedTransactions) {
        this.numberOfAbortedTransactions = abortedTransactions;
    }

    /**
     * @return the applicationRollbacks
     */
    public String getApplicationRollbacks() {
        return numberOfApplicationRollbacks;
    }

    /**
     * @param applicationRollbacks the applicationRollbacks to set
     */
    public void setApplicationRollbacks(String applicationRollbacks) {
        this.numberOfApplicationRollbacks = applicationRollbacks;
    }

    /**
     * @return the committedTransactions
     */
    public String getCommittedTransactions() {
        return numberOfCommittedTransactions;
    }

    /**
     * @param committedTransactions the committedTransactions to set
     */
    public void setCommittedTransactions(String committedTransactions) {
        this.numberOfCommittedTransactions = committedTransactions;
    }

    /**
     * @return the heuristics
     */
    public String getHeuristics() {
        return numberOfHeuristics;
    }

    /**
     * @param heuristics the heuristics to set
     */
    public void setHeuristics(String heuristics) {
        this.numberOfHeuristics = heuristics;
    }

    /**
     * @return the inflightTransactions
     */
    public String getInflightTransactions() {
        return numberOfInflightTransactions;
    }

    /**
     * @param inflightTransactions the inflightTransactions to set
     */
    public void setInflightTransactions(String inflightTransactions) {
        this.numberOfInflightTransactions = inflightTransactions;
    }

    /**
     * @return the nestedTransactions
     */
    public String getNestedTransactions() {
        return numberOfNestedTransactions;
    }

    /**
     * @param nestedTransactions the nestedTransactions to set
     */
    public void setNestedTransactions(String nestedTransactions) {
        this.numberOfNestedTransactions = nestedTransactions;
    }

    /**
     * @return the resourceRollbacks
     */
    public String getResourceRollbacks() {
        return numberOfResourceRollbacks;
    }

    /**
     * @param resourceRollbacks the resourceRollbacks to set
     */
    public void setResourceRollbacks(String resourceRollbacks) {
        this.numberOfResourceRollbacks = resourceRollbacks;
    }

    /**
     * @return the timedOutTransactions
     */
    public String getTimedOutTransactions() {
        return numberOfTimedOutTransactions;
    }

    /**
     * @param timedOutTransactions the timedOutTransactions to set
     */
    public void setTimedOutTransactions(String timedOutTransactions) {
        this.numberOfTimedOutTransactions = timedOutTransactions;
    }

    /**
     * @return the transactions
     */
    public String getTransactions() {
        return numberOfTransactions;
    }

    /**
     * @param transactions the transactions to set
     */
    public void setTransactions(String transactions) {
        this.numberOfTransactions = transactions;
    }

    @Override
    public String toString() {
        return "TransactionsStats{" + "numberOfAbortedTransactions=" + numberOfAbortedTransactions + ", numberOfApplicationRollbacks=" + numberOfApplicationRollbacks + ", numberOfCommittedTransactions=" + numberOfCommittedTransactions + ", numberOfHeuristics=" + numberOfHeuristics + ", numberOfInflightTransactions=" + numberOfInflightTransactions + ", numberOfNestedTransactions=" + numberOfNestedTransactions + ", numberOfResourceRollbacks=" + numberOfResourceRollbacks + ", numberOfTimedOutTransactions=" + numberOfTimedOutTransactions + ", numberOfTransactions=" + numberOfTransactions + '}';
    }
}
