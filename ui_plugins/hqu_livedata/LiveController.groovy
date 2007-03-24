import org.hyperic.hq.ui.rendit.BaseController

import org.hyperic.util.config.ConfigResponse

public class LiveController extends BaseController {
    def index = {
        render(args:[platforms:resourceHelper.allPlatforms])
    }
	
    def showResource = { params ->
        def platId  = Integer.parseInt(params['id'][0])
        def plat    = resourceHelper.find(platform:platId)
        def cmds    = liveDataHelper.getCommands(plat)
        def command = params['command']
        def result
		
        if (command != null) {
            command = command[0]
            result = liveDataHelper.getData(plat, command, [:]).XMLResult
        }

        render(args:[resource:plat, cmds:cmds, result:result, command:command]) 
	}
}
