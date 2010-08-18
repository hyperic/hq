package org.hyperic.hq.web.admin.escalation;

import org.hibernate.validator.NotNull;
import org.hyperic.hq.escalation.server.session.Escalation;

/**
 * The form object of new escalation page. 
 * ref: "Escalation.java" 
 * @author yechen
 *
 */

public class EscalationForm {
    private int id;
    
    // Name of the escalation chain
    @NotNull
    private String escalationName;
    
    // Description of the escalation chain
    private String description;
    
    // Allow the escalation to be paused (up to maxWaitTime milliseconds)
    private boolean pauseAllowed;

    // Max amount of time that the escalation can be paused
    private long maxPauseTime;

    // If true, notify everyone specified by the chain, else just the previous
    // notifications.
    private boolean notifyAll;
    
    // If true, repeat the escalation chain once it reaches end
    private boolean repeat;

    public EscalationForm() { }
    
    public EscalationForm(Escalation escalation) {
        this.id = escalation.getId();
    	this.escalationName = escalation.getName();
        this.description = escalation.getDescription();//TODO is it possible in html?
        this.maxPauseTime = escalation.getMaxPauseTime();
        this.notifyAll = escalation.isNotifyAll();
        this.pauseAllowed = escalation.isPauseAllowed();
        this.repeat = escalation.isRepeat();
    }
    
    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getEscalationName() {
        return escalationName;
    }

    public void setEscalationName(String escalationName) {
        this.escalationName = escalationName;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public boolean isPauseAllowed() {
        return pauseAllowed;
    }

    public void setPauseAllowed(boolean pauseAllowed) {
        this.pauseAllowed = pauseAllowed;
    }

    public long getMaxPauseTime() {
        return maxPauseTime;
    }

    public void setMaxPauseTime(long maxPauseTime) {
        this.maxPauseTime = maxPauseTime;
    }

    public boolean isNotifyAll() {
        return notifyAll;
    }

    public void setNotifyAll(boolean notifyAll) {
        this.notifyAll = notifyAll;
    }

    public boolean isRepeat() {
        return repeat;
    }

    public void setRepeat(boolean repeat) {
        this.repeat = repeat;
    }

}
