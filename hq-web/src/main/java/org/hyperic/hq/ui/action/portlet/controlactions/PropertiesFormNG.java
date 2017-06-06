package org.hyperic.hq.ui.action.portlet.controlactions;

import org.hyperic.hq.ui.action.portlet.DashboardBaseFormNG;

public class PropertiesFormNG extends DashboardBaseFormNG {
    /** Holds value of property lastCompleted. */
    private Integer _lastCompleted;

    /** Holds value of property nextScheduled. */
    private Integer _nextScheduled;

    /** Holds value of property mostFrequent. */
    private Integer _mostFrequent;

    /** Holds value of property useLastCompleted. */
    private boolean _useLastCompleted;

    /** Holds value of property useMostFrequent. */
    private boolean _useMostFrequent;

    /** Holds value of property useNextScheduled. */
    private boolean _useNextScheduled;

    /** Holds value of property past. */
    private long _past;

    // -------------------------------------instance variables

    // -------------------------------------constructors

    public PropertiesFormNG() {
        super();
    }

    // -------------------------------------public methods

    public void reset() {
        super.reset();
        // xxx figure this out
        setUseLastCompleted(false);
        setUseMostFrequent(false);
        setUseNextScheduled(false);
    }

    public String toString() {
        StringBuffer s = new StringBuffer();
        return s.toString();
    }

    /**
     * Getter for property lastCompleted.
     * @return Value of property lastCompleted.
     * 
     */
    public Integer getLastCompleted() {
        return _lastCompleted;
    }

    /**
     * Setter for property lastCompleted.
     * @param lastCompleted New value of property lastCompleted.
     * 
     */
    public void setLastCompleted(Integer lastCompleted) {
        _lastCompleted = lastCompleted;
    }

    /**
     * Getter for property lastScheduled.
     * @return Value of property lastScheduled.
     * 
     */
    public Integer getNextScheduled() {
        return _nextScheduled;
    }

    /**
     * Setter for property lastScheduled.
     * @param lastScheduled New value of property lastScheduled.
     * 
     */
    public void setNextScheduled(Integer nextScheduled) {
        _nextScheduled = nextScheduled;
    }

    /**
     * Getter for property mostFrequent.
     * @return Value of property mostFrequent.
     * 
     */
    public Integer getMostFrequent() {
        return _mostFrequent;
    }

    /**
     * Setter for property mostFrequent.
     * @param mostFrequent New value of property mostFrequent.
     * 
     */
    public void setMostFrequent(Integer mostFrequent) {
        _mostFrequent = mostFrequent;
    }

    /**
     * Getter for property useLastCompleted.
     * @return Value of property useLastCompleted.
     * 
     */
    public boolean isUseLastCompleted() {
        return _useLastCompleted;
    }

    /**
     * Setter for property useLastCompleted.
     * @param useLastCompleted New value of property useLastCompleted.
     * 
     */
    public void setUseLastCompleted(boolean useLastCompleted) {
        _useLastCompleted = useLastCompleted;
    }

    /**
     * Getter for property useMostFrequent.
     * @return Value of property useMostFrequent.
     * 
     */
    public boolean isUseMostFrequent() {
        return _useMostFrequent;
    }

    /**
     * Setter for property useMostFrequent.
     * @param useMostFrequent New value of property useMostFrequent.
     * 
     */
    public void setUseMostFrequent(boolean useMostFrequent) {
        _useMostFrequent = useMostFrequent;
    }

    /**
     * Getter for property useNextScheduled.
     * @return Value of property useNextScheduled.
     * 
     */
    public boolean isUseNextScheduled() {
        return _useNextScheduled;
    }

    /**
     * Setter for property useNextScheduled.
     * @param useNextScheduled New value of property useNextScheduled.
     * 
     */
    public void setUseNextScheduled(boolean useNextScheduled) {
        _useNextScheduled = useNextScheduled;
    }

    public long getPast() {
        return _past;
    }

    public void setPast(long past) {
        _past = past;
    }
}
