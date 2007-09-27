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
