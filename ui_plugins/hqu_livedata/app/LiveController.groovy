import org.hyperic.hq.ui.rendit.BaseController

import org.hyperic.hq.livedata.shared.LiveDataCommand
import org.hyperic.util.config.ConfigResponse

public class LiveController extends BaseController {
    def index = {
        render(args:[platforms:resourceHelper.find(all:'platforms')])
    }
	
    def showResource = { params ->
        def platId  = Integer.parseInt(params['id'][0])
        def plat    = resourceHelper.find(platform:platId)
        def cmds    = liveDataHelper.getCommands(plat)
        def command = params['command']
        def result
		
        if (command != null) {
            command = command[0]
            def cmd = [plat.entityId, command,
                       [:] as ConfigResponse] as LiveDataCommand
            result = liveDataHelper.getData(cmd).XMLResult
        }

        render(args:[resource:plat, cmds:cmds, result:result, command:command]) 
	}
}
