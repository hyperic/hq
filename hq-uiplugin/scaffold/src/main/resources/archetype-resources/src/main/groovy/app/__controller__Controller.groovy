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
import org.hyperic.hq.hqu.rendit.BaseController

class ${controller}Controller 
	extends BaseController
{
    protected void init() {
        onlyAllowSuperUsers()
    }
    
    def index(params) {
        // By default, this sends views/${controllerDir}/index.gsp to
        // the browser, providing 'plugin' and 'userName' locals to it
        //
        // The name of the currently-executed action dictates which .gsp file 
        // to render (in this case, index.gsp).
        //
        // If you want to render AJAX, read RenderFrame.groovy for parameters
        // or return a Map from this method and in init(), call:
        //     setJSONMethods(['myJSONMethod', 'anotherJSONMethod'])
    	render(locals:[ plugin :  getPlugin(),
    	                userName: user.name])  
    }
}
