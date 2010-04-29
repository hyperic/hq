import org.hyperic.hq.context.Bootstrap;
import org.hyperic.hq.hqu.rendit.HQUPlugin

import org.hyperic.hq.authz.server.session.AuthzSubject
import org.hyperic.hq.authz.server.session.Resource
import org.hyperic.hq.authz.shared.ResourceManager;
import org.hyperic.hq.hqu.AttachmentDescriptor
import org.hyperic.hq.hqu.server.session.Attachment

class Plugin extends HQUPlugin {
    
    Plugin() {
        addMastheadView(true, '/saascenter/index.hqu', 'SaaS Center', 'resource')    
    }
    
    AttachmentDescriptor getAttachmentDescriptor(Attachment a, Resource r,
            AuthzSubject u) 
    {
        if (Bootstrap.getBean(ResourceManager.class).resourcesExistOfType('AWS')) {        
            return super.getAttachmentDescriptor(a, r, u)
        } else {
            return null
        }
    }
}    
