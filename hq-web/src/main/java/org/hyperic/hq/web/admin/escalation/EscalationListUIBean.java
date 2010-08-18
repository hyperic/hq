package org.hyperic.hq.web.admin.escalation;

import org.hyperic.hq.escalation.server.session.Escalation;

public class EscalationListUIBean {
    private int escId;
    private String escName;
    private int actionNum;
    private int alertNum;
    
    public EscalationListUIBean() { }

    public EscalationListUIBean(Escalation escalation) {
    	this.actionNum = 0; 
    	this.alertNum = 0;
    	this.escId = escalation.getId();
    	this.escName = escalation.getName();
    }
    
    public void setEscId(int escId) {
        this.escId = escId;
    }
    
    public void setEscName(String escName) {
        this.escName = escName;
    }
    
    public void setActionNum(int actionNum) {
        this.actionNum = actionNum;
    }
    
    public void setAlertNum(int alertNum) {
        this.alertNum = alertNum;
    }
    
    public int getEscId() {
        return escId;
    }
    
    public String getEscName() {
        return escName;
    }
    
    public int getActionNum() {
        return actionNum;
    }
    
    public int getAlertNum() {
        return alertNum;
    }
}