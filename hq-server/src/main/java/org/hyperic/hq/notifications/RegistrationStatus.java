package org.hyperic.hq.notifications;

public class RegistrationStatus {
    protected boolean valid;
    protected long creationTime;
    
    public boolean isValid() {
        return valid;
    }

    public void setValid(boolean valid) {
        this.valid = valid;
    }

    public long getCreationTime() {
        return creationTime;
    }

    public void setCreationTime(long creationTime) {
        this.creationTime = creationTime;
    }

}
