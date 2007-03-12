package org.hyperic.hq.bizapp.server.session;

import org.hyperic.util.HypericEnum;

public class UpdateStatusMode 
    extends HypericEnum
{
    public static final UpdateStatusMode ALL = 
        new UpdateStatusMode(0, "All");
    public static final UpdateStatusMode MAJOR = 
        new UpdateStatusMode(1, "Major");
    public static final UpdateStatusMode NONE = 
        new UpdateStatusMode(2, "None");
    
    private UpdateStatusMode(int code, String desc) {
        super(code, desc);
    }
    
    public static UpdateStatusMode findByCode(int code) {
        return (UpdateStatusMode)findByCode(UpdateStatusMode.class, code);
    }
}
