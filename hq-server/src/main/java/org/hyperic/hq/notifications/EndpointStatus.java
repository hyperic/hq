package org.hyperic.hq.notifications;

public class EndpointStatus {
    protected BasePostingStatus lastSuccessful;
    protected BasePostingStatus lastFailure;
    protected long size=0;
    
    public EndpointStatus() {}
    public EndpointStatus(BasePostingStatus lastSuccessful, BasePostingStatus lastFailure) {
        this.addLastFailure(lastFailure);
        this.addLastSuccessful(lastSuccessful);
    }
    public EndpointStatus(EndpointStatus other) {
        this(other.getLastSuccessful(),other.getLastFailure());
    }
    public BasePostingStatus getLastSuccessful() {
        return this.lastSuccessful;
    }
    public BasePostingStatus getLastFailure() {
        return this.lastFailure;
    }
    public void addLastSuccessful(BasePostingStatus otherStatus) {
        if (otherStatus!=null && (this.lastSuccessful==null || this.lastSuccessful.getTime()<otherStatus.getTime())) {
            this.lastSuccessful=otherStatus;
            this.size++;
        }
    }
    public void addLastFailure(BasePostingStatus otherStatus) {
        if (otherStatus!=null && (this.lastFailure==null || this.lastFailure.getTime()<otherStatus.getTime())) {
            this.lastFailure=otherStatus;
            this.size++;
        }
    }
    public void add(BasePostingStatus status) {
        if (status==null) {
            return;
        }
        if (status.isSuccessful()) {
            addLastSuccessful(status);
        } else {
            addLastFailure(status);
        }
    }
    public long size() {
        return this.size;
    }
    public boolean isEmpty() {
        return this.size()==0;
    }
    public BasePostingStatus getLast() {
        BasePostingStatus lastFailure = this.getLastFailure();
        BasePostingStatus lastSuccessful = this.getLastSuccessful();
        if (lastFailure==null) {
            return lastSuccessful;
        }
        if (lastSuccessful==null) {
            return lastFailure;
        }
        return lastFailure.getTime()>lastSuccessful.getTime()?lastFailure:lastSuccessful;
    }
    public void merge(EndpointStatus other) {
        if (other==null) {
            return;
        }
        this.addLastFailure(other.getLastFailure());
        this.addLastSuccessful(other.getLastSuccessful());
    }
}
