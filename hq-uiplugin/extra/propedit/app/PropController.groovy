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

import org.hyperic.hq.hqu.rendit.BaseController
import org.hyperic.hq.common.server.session.ServerConfigManagerImpl as SCM

class PropController 
	extends BaseController
{
    def PropController() {
        addBeforeFilter({ 
            if (!user.isSuperUser()) {
                render(inline: "Unauthorized")
                return true
            }
            return false
        })
        setJSONMethods(['setProp'])
    }

    def setProp(params) {
        def key    = params.getOne('key')
        def newVal = params.getOne('newVal')
        
        def props = SCM.one.config
        log.info "Setting system property ${key} to ${newVal}"
        props[key] = newVal
        SCM.one.setConfig(user, props)
    }
    
    def index(params) {
    	render(locals:[props:SCM.one.configProperties.sort {a,b->a.key<=>b.key}])
    }
}
