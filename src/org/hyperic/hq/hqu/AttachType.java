package org.hyperic.hq.hqu;

import org.hyperic.util.HypericEnum;

public class AttachType 
    extends HypericEnum
{
    public static AttachType ADMIN = new AttachType(0, "admin"); 
    
    public static AttachType findByDescription(String desc) {
        if (desc.equals("admin")) 
            return ADMIN;

        throw new IllegalArgumentException("Unknown AttachType [" + desc + "]");
    }
    
    public static AttachType findByCode(int code) {
        return (AttachType)HypericEnum.findByCode(AttachType.class, code);
    }
    
    private AttachType(int code, String desc) {
        super(code, desc);
    }
}
