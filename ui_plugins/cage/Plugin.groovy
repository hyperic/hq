import org.hyperic.hq.hqu.rendit.HQUPlugin

import CageController

class Plugin extends HQUPlugin {
    void initialize(File pluginDir) {
        super.initialize(pluginDir)
        addView(description:  'Cage',
                attachType:   'resource',
                toRoot:       true,
                controller:   CageController,
                action:       'index')
    }    
}

