package org.hyperic.hq.scheduler;

public class QzLock  implements java.io.Serializable {

    // Fields
    private String _lockName;

    // Constructors
    public QzLock() {
    }

    // Property accessors
    public String getLockName() {
        return _lockName;
    }
    
    public void setLockName(String lockName) {
        _lockName = lockName;
    }
}
