/**
 * NOTE: This copyright does *not* cover user programs that use HQ
 * program services by normal system calls through the application
 * program interfaces provided as part of the Hyperic Plug-in Development
 * Kit or the Hyperic Client Development Kit - this is merely considered
 * normal use of the program, and does *not* fall under the heading of
 *  "derived work".
 *
 *  Copyright (C) [2009-2010], VMware, Inc.
 *  This file is part of HQ.
 *
 *  HQ is free software; you can redistribute it and/or modify
 *  it under the terms version 2 of the GNU General Public License as
 *  published by the Free Software Foundation. This program is distributed
 *  in the hope that it will be useful, but WITHOUT ANY WARRANTY; without
 *  even the implied warranty of MERCHANTABILITY or FITNESS FOR A
 *  PARTICULAR PURPOSE. See the GNU General Public License for more
 *  details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program; if not, write to the Free Software
 *  Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307
 *  USA.
 *
 */

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

