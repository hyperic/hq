package org.hyperic.hq.appdef.shared;

import java.util.List;
import java.util.Map;

/** 
 * A utility class for aggregating batch operation response together with exceptions thrown.
 * Used when a method receives multiple ids to process, where processing of some of
 * them throw expected exceptions. The method needs to return both the result of
 * processing and the failed ids together with their exceptions.
 * 
 * @since   4.5.0
 * @version 1.0 14 May 2012
 * @author Maya Anderson
 */
public class BatchResponse<T> {
    /** The result of the successful part of the batch processing */
    List<T> response;
    
    /** IDs, whose processing failed, and their exceptions */ 
    Map<String,Exception> failedIds;
    
    public BatchResponse() {   }
    
    public BatchResponse(List<T> response, Map<String, Exception> failedIds) {
        this.response = response;
        this.failedIds = failedIds;
    }    
    
    /**
     * @return the result of the successful part of the batch processing
     */
    public List<T> getResponse() {
        return response;
    }
    public void setResponse(List<T> response) {
        this.response = response;
    }
    /**
     * @return IDs, whose processing failed, and their exceptions
     */
    public Map<String, Exception> getFailedIds() {
        return failedIds;
    }
    public void setFailedIds(Map<String, Exception> failedIds) {
        this.failedIds = failedIds;
    }    
    
    public void appendBatchResponse(BatchResponse<T> t)  {
        if (null != t) {
            if (null != t.getResponse()) {
                this.response.addAll(t.getResponse());
            }
            if (null != t.getFailedIds()) {
                this.failedIds.putAll(t.getFailedIds());
            }
        }
    }
    
    public void appendBatchResponse(List<T> response, Map<String, Exception> failedIds) {
        if (null != response) {
            this.response.addAll(response);
        }
        if (null != failedIds) {
            this.failedIds.putAll(failedIds);
        }
    }
}
