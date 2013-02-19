package org.hyperic.hq.notifications.model;

public class BaseNotification {
    public int getRegID() {
        return regID;
    }

    public void setRegID(int regID) {
        this.regID = regID;
    }

    protected int regID;
    
    public BaseNotification() {
    }

}
