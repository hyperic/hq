import org.hyperic.hq.authz.server.session.AuthzSubject
import org.hyperic.hq.authz.server.session.Resource
import org.hyperic.hq.hqu.AttachmentDescriptor
import org.hyperic.hq.hqu.server.session.Attachment
import org.hyperic.hq.hqu.rendit.HQUPlugin

import GemfireController

class Plugin extends HQUPlugin {
    void initialize(File pluginDir) {
        super.initialize(pluginDir)

        addView(description:  'Gemfire',
            attachType:   'resource',
            controller:   GemfireController,
            action:       'index',
            resourceType: ['GemFire Distributed System'])
        
        addView(description:  'Gemfire',
            attachType:   'resource',
            controller:   GemfireController,
            action:       'index',
            resourceType: ['Cache Server'])
    }
}

