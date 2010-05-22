#set( $symbol_pound = '#' )
#set( $symbol_dollar = '$' )
#set( $symbol_escape = '\' )
import org.hyperic.hq.hqu.rendit.HQUPlugin

import ${controller}Controller

class Plugin extends HQUPlugin {
    void initialize(File pluginDir) {
        super.initialize(pluginDir)
        /**
         * The following can be un-commented to have the plugin's view rendered in HQ.
         *
         * description:  The brief name of the view (e.g.: "Fast Executor")
         * attachType:   one of ['masthead', 'admin', 'resource']
         * controller:   The controller to invoke when the view is to be generated
         * action:       The method within 'controller' to invoke
         * category:     (optional)  If set, specifies either 'tracker' or 'resource' menu
         * resourceType: (if attachType == resource), specifies an list of 
         *               type names to attach to (ex. ['MacOSX', 'Linux'])
         */
        /*
        addView(description:  'A Groovy HQU-${artifactId}',
                attachType:   'masthead', 
                controller:   ${controller}Controller,
                action:       'index', 
                category:     'tracker')
         */
    }
}

