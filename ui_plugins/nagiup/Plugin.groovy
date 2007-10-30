import org.hyperic.hq.hqu.rendit.HQUPlugin

import org.hyperic.hq.authz.server.session.Resource
import org.hyperic.hq.hqu.AttachmentDescriptor
import org.hyperic.hq.hqu.server.session.Attachment

class Plugin extends HQUPlugin {
    Plugin() {
        addMastheadView(true, '/nagiup/index.hqu', 'Nagios Availability', 
                        'tracker')    
    }
    
    AttachmentDescriptor getAttachmentDescriptor(Attachment a, Resource r) {
        // TODO:  Return null if there are no nagios check services
        super.getAttachmentDescriptor(a, r)
    }
}
