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

import org.hyperic.hq.ui.rendit.BaseController

import org.hyperic.hq.livedata.shared.LiveDataCommand
import org.hyperic.hq.appdef.shared.AppdefEntityID
import org.hyperic.util.config.ConfigResponse

public class JvmstatsController extends BaseController {

    def index = {
        render(args:[groups:resourceHelper.find(all:'groups')])
    }

    def showStats = { params ->
        def group = resourceHelper.find(group:params['id'][0])
        def entIds = group.appdefGroupEntries
        def config = ['ObjectName' : 'java.lang:type=Memory',
                      'Attribute' : 'HeapMemoryUsage'] as ConfigResponse
        def stats = []
        def cmds = []
        for (id in entIds) {
            if (!liveDataHelper.resourceSupports(id, 'get'))
                continue

            cmds << ([id, 'get', config] as LiveDataCommand)
        }

        for (dataRes in liveDataHelper.getData(cmds as LiveDataCommand[])) {
            def rsrc = resourceHelper.find(resource:dataRes.appdefEntityID)

            if (dataRes.hasError()) {
                stats << [name:rsrc.name, value:dataRes.errorMessage]
                continue
            }

            def data = dataRes.objectResult

            stats << [resource:rsrc, data:data]
        }

        render(args:[group:group, stats:stats])
    }

    def gc = { params ->
        def group = params['group'][0]
        def id    = params['id'][0]
        def type  = params['type'][0]
        def aid = [type + ':' + id] as AppdefEntityID
        def config = ['ObjectName' : 'java.lang:type=Memory',
                      'Method' : 'gc' ] as ConfigResponse
        def cmd = [aid, 'invoke', config] as LiveDataCommand;

        def res = liveDataHelper.getData(cmd as LiveDataCommand)

        def result
        if (res.hasError()) {
            result = res.errorMessage
        } else {
            result = 'Command completed successfully!'
        }

        render(args:[group:group, result:result])
    }
}
