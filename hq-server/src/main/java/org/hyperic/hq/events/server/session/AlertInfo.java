package org.hyperic.hq.events.server.session;

public class AlertInfo {
    private final Integer _alertDefId;
    private final Long _ctime;

    AlertInfo(Integer alertDefId, Long ctime) {
        _alertDefId = alertDefId;
        _ctime = ctime;
    }

    AlertInfo(Integer alertDefId, long ctime) {
        _alertDefId = alertDefId;
        _ctime = new Long(ctime);
    }

    Integer getAlertDefId() {
        return _alertDefId;
    }

    Long getCtime() {
        return _ctime;
    }

    public boolean equals(Object rhs) {
        if (rhs == this) {
            return true;
        }
        if (rhs instanceof AlertInfo) {
            AlertInfo obj = (AlertInfo) rhs;
            return obj.getCtime().equals(_ctime) && obj.getAlertDefId().equals(_alertDefId);
        }
        return false;
    }

    public int hashCode() {
        return 17 * _alertDefId.hashCode() + _ctime.hashCode();
    }
}
