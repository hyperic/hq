import org.hyperic.hq.hqu.rendit.HQUPlugin

import DashboardController

class Plugin extends HQUPlugin {
    void initialize(File pluginDir) {
        super.initialize(pluginDir)

        addView(description:  'Operations Center',
                attachType:   'masthead', 
                controller:   DashboardController,
                action:       'index', 
                category:     'tracker')
    }
}

