package org.hyperic.hq.notifications;

public abstract class BasePostingStatus {
    protected long time;
    
    public BasePostingStatus(long time) {
        this.time = time;
    }
    public abstract boolean isSuccessful();
    public long getTime() {
        return time;
    }
    public void setTime(long time) {
        this.time = time;
    }
}
