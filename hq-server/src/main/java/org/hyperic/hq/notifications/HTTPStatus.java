package org.hyperic.hq.notifications;

public class HTTPStatus extends BasePostingStatus {
    protected int code;
    
    public HTTPStatus(long time, int code) {
        super(time);
        this.code=code;
    }

    @Override
    public boolean isSuccessful() {
        return this.code==200;
    }
}
