package org.hyperic.hq.notifications;

import java.util.ArrayList;
import java.util.List;

public class BatchPostingStatus {
    protected List<BasePostingStatus> successfuls = new ArrayList<BasePostingStatus>();
    protected List<BasePostingStatus> failures = new ArrayList<BasePostingStatus>();
    
    public BasePostingStatus getLastSuccessful() {
        return this.successfuls==null||this.successfuls.isEmpty()?null:this.successfuls.get(this.successfuls.size()-1);
    }
    public BasePostingStatus getLastFailure() {
        return this.failures==null||this.failures.isEmpty()?null:this.failures.get(this.failures.size()-1);
    }
    public void add(BasePostingStatus status) {
        if (status.isSuccessful()) {
            this.successfuls.add(status);
        } else {
            this.failures.add(status);
        }
    }
    public long size() {
        return (this.successfuls!=null?this.successfuls.size():0) + (this.failures!=null?this.failures.size():0);
    }
    public boolean isEmpty() {
        return this.size()==0;
    }
    public List<BasePostingStatus> getFailures() {
        return failures;
    }
    public void setFailures(List<BasePostingStatus> failures) {
        this.failures = failures;
    }
    public List<BasePostingStatus> getSuccessfuls() {
        return successfuls;
    }
    public void setSuccessfuls(List<BasePostingStatus> successfuls) {
        this.successfuls = successfuls;
    }
    public BasePostingStatus getLast() throws IllegalPostingException {
        BasePostingStatus lastFailure = this.getLastFailure();
        BasePostingStatus lastSuccessful = this.getLastSuccessful();
        if (lastFailure==null) {
            return lastSuccessful;
        }
        if (lastSuccessful==null) {
            return lastFailure;
        }
        if (lastFailure.getTime()==lastSuccessful.getTime()) {
            throw new IllegalPostingException("illegal state - two postings has been made on the same time");
        }
        return lastFailure.getTime()>lastSuccessful.getTime()?lastFailure:lastSuccessful;
    }
}
