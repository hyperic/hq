import org.hyperic.hq.hqu.rendit.HQUPlugin
import org.hyperic.hq.authz.server.session.AuthzSubject
import org.hyperic.hq.authz.server.session.Resource
import org.hyperic.hq.hqu.server.session.Attachment

class Plugin extends HQUPlugin {
    private boolean attachmentIsShown(Attachment a, Resource r, AuthzSubject u){
        // We are currently only functional for groups that contain at least
        // 1 platform
        if (r.isGroup()) {
            return r.getGroupMembers(u).find { m -> m.entityID.isPlatform() } != null
        }
        true
    }

    void initialize(File pluginDir) {
        super.initialize(pluginDir)
        addView(description:  'LiveExec',
                attachType:   'resource',
                toRoot:       true,
                platforms:    'all',
                byPlugin:     'system',
                controller:   LiveController,
                action:       'index',
                showAttachmentIf: {a, r, u -> attachmentIsShown(a, r, u)})        
    }
}
