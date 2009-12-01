package org.hyperic.hq.bizapp.server.session;

import org.hyperic.hibernate.PersistedObject;

public class UpdateStatus 
    extends PersistedObject
{
    private String  _report;
    private int     _updateModeEnum;
    private boolean _ignored;

    protected UpdateStatus() {
    }
    
    UpdateStatus(String report, UpdateStatusMode mode) {
        _report         = report;
        _updateModeEnum = mode.getCode();
        _ignored        = false;
    }
    
    public String getReport() {
        return _report;
    }
    
    protected void setReport(String report) {
        _report = report;
    }
    
    protected int getUpdateModeEnum() {
        return _updateModeEnum;
    }
    
    protected void setUpdateModeEnum(int mode) {
        _updateModeEnum = mode;
    }
    
    public UpdateStatusMode getMode() {
        return UpdateStatusMode.findByCode(_updateModeEnum);
    }
    
    void setMode(UpdateStatusMode mode) {
        _updateModeEnum = mode.getCode();
    }
    
    public boolean isIgnored() {
        return _ignored;
    }
    
    protected void setIgnored(boolean ignored) {
        _ignored = ignored;
    }
}
