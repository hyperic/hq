import org.hyperic.hq.ui.rendit.BaseController

import org.hyperic.hq.livedata.shared.LiveDataCommand
import org.hyperic.util.config.ConfigResponse

public class LiveController extends BaseController {
    def index = {
        render(args:[platforms:resourceHelper.find(all:'platforms')])
    }
	
    def showResource = { params ->
        def plat    = resourceHelper.find(platform:params['id'])
        def cmds    = plat.liveDataCommands 
        def command = params['command']
        def result
		
        if (command != null) {
            command = command[0]
            result = plat.getLiveData(command).XMLResult 
        }

        render(args:[resource:plat, cmds:cmds, result:result, command:command]) 
	}
}
