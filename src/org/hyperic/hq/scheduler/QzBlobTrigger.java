package org.hyperic.hq.scheduler;

public class QzBlobTrigger extends org.hyperic.hq.scheduler.QzTrigger
    implements java.io.Serializable {

    // Fields
     private byte[] _blobData;

    // Constructors
    public QzBlobTrigger() {
    }

    // Property accessors
    public byte[] getBlobData() {
        return _blobData;
    }
    
    public void setBlobData(byte[] blobData) {
        _blobData = blobData;
    }
}


