package org.hyperic.hq.hqu.server.session;

import org.hyperic.hq.hqu.UIPluginViewDescriptor;
import org.hyperic.util.HypericEnum;

public abstract class AttachType 
    extends HypericEnum
{
    public static AttachType ADMIN = new AttachType(0, "admin") {
        UIPluginView createView(UIPlugin plugin, 
                                UIPluginViewDescriptor viewInfo) 
        {
            return new UIPluginViewAdmin(plugin, viewInfo);
        }
    };

    abstract UIPluginView createView(UIPlugin plugin, 
                                     UIPluginViewDescriptor viewInfo);
    
    public static AttachType findByDescription(String desc) {
        if (desc.equals("admin")) 
            return ADMIN;

        throw new IllegalArgumentException("Unknown AttachType [" + desc + "]");
    }
    
    public static AttachType findByCode(int code) {
        return (AttachType)HypericEnum.findByCode(AttachType.class, code);
    }
    
    private AttachType(int code, String desc) {
        super(AttachType.class, code, desc);
    }
}
