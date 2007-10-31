import org.hyperic.hq.hqu.rendit.HQUPlugin

import org.hyperic.hq.authz.server.session.Resource
import org.hyperic.hq.authz.server.session.ResourceManagerEJBImpl as rme
import org.hyperic.hq.hqu.AttachmentDescriptor
import org.hyperic.hq.hqu.server.session.Attachment

class Plugin extends HQUPlugin {
    Plugin() {
        addMastheadView(true, '/nagiup/index.hqu', 'Nagios Availability', 
                        'resource')    
    }
    
    AttachmentDescriptor getAttachmentDescriptor(Attachment a, Resource r) {
        if (rme.one.resourcesExistOfType('Nagios Plugin')) {        
            return super.getAttachmentDescriptor(a, r)
        } else {
            return null
        }
    }
}
