import org.hyperic.hq.measurement.server.session.MeasurementManagerEJBImpl as MM
import org.hyperic.hq.authz.server.session.AuthzSubject
import org.hyperic.hq.authz.server.session.Resource
import org.hyperic.hq.hqu.server.session.Attachment
import org.hyperic.hq.hqu.rendit.HQUPlugin
import org.hyperic.hq.hqu.AttachmentDescriptor
import org.hyperic.hq.hqu.SimpleAttachmentDescriptor

class Plugin extends HQUPlugin {
    Plugin() {
        addMastheadView(true, '/systemsdown/index.hqu', 
                        'Currently Down Resources', 'resource')    
    }
    
    AttachmentDescriptor getAttachmentDescriptor(Attachment a, Resource r,
                                                 AuthzSubject u) 
    {
       return super.getAttachmentDescriptor(a, r, u)
    }
}
