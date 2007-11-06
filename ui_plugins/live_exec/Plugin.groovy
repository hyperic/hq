import org.hyperic.hq.hqu.rendit.HQUPlugin
import org.hyperic.hq.hqu.server.session.UIPlugin
import org.hyperic.hq.hqu.server.session.AttachType
import org.hyperic.hq.hqu.ViewDescriptor
import org.hyperic.hq.appdef.server.session.PlatformManagerEJBImpl as PMI
import org.hyperic.hq.authz.server.session.AuthzSubject
import org.hyperic.hq.authz.server.session.Resource
import org.hyperic.hq.hqu.server.session.UIPluginManagerEJBImpl as UIPM
import org.hyperic.hq.hqu.server.session.Attachment
import org.hyperic.hq.hqu.server.session.View
import org.hyperic.hq.hqu.server.session.ViewResource
import org.hyperic.hq.hqu.server.session.ViewResourceCategory
import org.hyperic.hq.authz.server.session.ResourceManagerEJBImpl as RMI
import org.hyperic.hq.hqu.AttachmentDescriptor

class Plugin extends HQUPlugin {
    void deploy(UIPlugin me) {
        def uiMan = UIPM.one

        ViewResource view
        if (me.views.empty) {
            def vd = new ViewDescriptor('/live/index.hqu', 'LiveExec', 
                                        AttachType.RESOURCE)
            view = uiMan.createResourceView(me, vd)
        } else {
            view = me.views.iterator().next()
        }

        def pluginMan = UIPM.one
        def platMan   = PMI.one
        for (pt in platMan.findAllPlatformTypes()) {
            if (pt.plugin != 'system')
                continue
            
            def r = platMan.findResource(pt)
            if (resourceAttached(view, r))
                continue

            uiMan.attachView(view, ViewResourceCategory.VIEWS, r)
        }
        
        def root = RMI.one.findRootResource()
        if (!resourceAttached(view, root))
            uiMan.attachView(view, ViewResourceCategory.VIEWS, root)
    }
    
    private boolean resourceAttached(view, resource) {
        for (a in view.attachments) {
            if (a.resource == resource)
                return true
        }
        return false
    }
    
    AttachmentDescriptor getAttachmentDescriptor(Attachment a, Resource r,
                                                 AuthzSubject user) 
    {
        // We are currently only functional for groups that contain at least
        // 1 platform
        if (r.isGroup()) {
            for (m in r.getGroupMembers(user)) {
                if (m.entityID.isPlatform()) {
                    return super.getAttachmentDescriptor(a, r, user)
                }
            }
            return null
        } else {
            return super.getAttachmentDescriptor(a, r)
        }
    }
}
