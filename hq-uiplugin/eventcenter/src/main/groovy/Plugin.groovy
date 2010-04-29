import org.hyperic.hq.hqu.rendit.HQUPlugin

import EventController

class Plugin extends HQUPlugin {
    Plugin() {
        addView(description:  'Event Center',
                attachType:   'masthead', 
                controller:   EventController,
                action:       'index', 
                category:     'tracker')
    }
}

