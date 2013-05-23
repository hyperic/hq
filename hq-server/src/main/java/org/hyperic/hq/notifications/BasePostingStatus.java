package org.hyperic.hq.notifications;

public abstract class BasePostingStatus {
    protected long time;
    private String message;
    
    public BasePostingStatus(long time, String message) {
        this.time = time;
        this.message=message;
    }
    public abstract boolean isSuccessful();
    public void setMessage(String message) {
        this.message = message;
    }
    public long getTime() {
        return time;
    }
    public void setTime(long time) {
        this.time = time;
    }
    public String getMessage() {
        return this.message;
    }
}
