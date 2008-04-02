import org.hyperic.hq.hqu.rendit.HQUPlugin

import MethodController

class Plugin extends HQUPlugin {
    Plugin() {
        /**
         * The following can be un-commented to have the plugin's view rendered in HQ.
         *
         * description:  The brief name of the view (e.g.: "Fast Executor")
         * attachType:   one of ['masthead', 'admin']
         * controller:   The controller to invoke when the view is to be generated
         * action:       The method within 'controller' to invoke
         * category:     (optional)  If set, specifies either 'tracker' or 'resource' menu
         */
        addView(description:  'A Groovy HQU-meth_mon',
                attachType:   'admin', 
                controller:   MethodController,
                action:       'index') 
    }
}

