import org.hyperic.hq.hqu.rendit.HQUPlugin

import saascenterController

class Plugin extends HQUPlugin {
    void initialize(File pluginDir) {
        super.initialize(pluginDir)
        addView(description:  'A Groovy HQU-saasCenter',
                attachType:   'masthead', 
                controller:   SaascenterController,
                action:       'index', 
                category:     'tracker')
    }
}

