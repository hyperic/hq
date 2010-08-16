import org.hyperic.hq.authz.server.session.AuthzSubject
import org.hyperic.hq.authz.server.session.Resource
import org.hyperic.hq.hqu.AttachmentDescriptor
import org.hyperic.hq.hqu.server.session.Attachment
import org.hyperic.hq.hqu.rendit.HQUPlugin

import GemfireController

class Plugin extends HQUPlugin {

    private boolean attachmentIsShown(Attachment a, Resource r, AuthzSubject u){
        !r.isGroup()
    }

    void initialize(File pluginDir) {
        super.initialize(pluginDir)
        log.info("[initialize] pluginDir="+pluginDir)
        addView(description:  'GemFire',
            attachType:   'resource',
            toRoot:       false,
            platforms:    'all',
            byPlugin:     'gemfire',
            controller:   GemfireController,
            action:       'index',
            showAttachmentIf: {a, r, u -> attachmentIsShown(a, r, u)})
    }

}

