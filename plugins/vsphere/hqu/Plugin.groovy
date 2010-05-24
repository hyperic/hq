import org.hyperic.hq.hqu.rendit.HQUPlugin

import VsphereController

class Plugin extends HQUPlugin {
    void initialize(File pluginDir) {
        super.initialize(pluginDir)

        addView(description:  'HQ vSphere',
                attachType:   'masthead', 
                controller:   VsphereController,
                action:       'index', 
                category:     'tracker')
    }
}

