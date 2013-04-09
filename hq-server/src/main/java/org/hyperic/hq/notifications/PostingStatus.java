package org.hyperic.hq.notifications;

public class PostingStatus extends BasePostingStatus {
    protected boolean success;
    
    public PostingStatus(long time, boolean success, String message) {
        super(time, message);
        this.success=success;
    }
    public boolean isSuccessful() {
        return success;
    }
}
