package org.hyperic.hq.bizapp.server.session;

import java.util.ResourceBundle;

import org.hyperic.util.HypericEnum;

public class UpdateStatusMode 
    extends HypericEnum
{
    private static final String BUNDLE = "org.hyperic.hq.bizapp.Resources";
    
    public static final UpdateStatusMode ALL = 
        new UpdateStatusMode(0, "All", "update.mode.all");
    public static final UpdateStatusMode MAJOR = 
        new UpdateStatusMode(1, "Major", "update.mode.major");
    public static final UpdateStatusMode NONE = 
        new UpdateStatusMode(2, "None", "update.mode.none");
    
    private UpdateStatusMode(int code, String desc, String localeProp) {
        super(code, desc, localeProp, ResourceBundle.getBundle(BUNDLE));
    }
    
    public static UpdateStatusMode findByCode(int code) {
        return (UpdateStatusMode)findByCode(UpdateStatusMode.class, code);
    }
}
