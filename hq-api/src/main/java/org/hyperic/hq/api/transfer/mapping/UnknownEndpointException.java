package org.hyperic.hq.api.transfer.mapping;

public class UnknownEndpointException extends Exception {
    protected long registrationID;

    public UnknownEndpointException(long registrationID) {
        super();
        this.registrationID = registrationID;
    }

    public long getRegistrationID() {
        return registrationID;
    }

    public void setRegistrationID(int registrationID) {
        this.registrationID = registrationID;
    }
    
}
