import org.hyperic.hq.hqu.rendit.HQUPlugin

import MethodController

class Plugin extends HQUPlugin {
    Plugin() {
        addView(description:  'Method Monitor',
                attachType:   'admin', 
                controller:   MethodController,
                action:       'index') 
    }
}

