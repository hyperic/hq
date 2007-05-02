import org.hyperic.hq.ui.rendit.BaseController

import org.hyperic.hq.livedata.shared.LiveDataCommand
import org.hyperic.util.config.ConfigResponse

public class TopController extends BaseController {
    private static final STATE_ICONS = ['T' : 'orange', 'Z' : 'red']  
    
    def index = {
        render(args:[groups:resourceHelper.find(all:'groups')])
    }
    
    def killProcess = { params ->
        def plat = resourceHelper.find(platform:params['id'])
        def pid  = params['pid'][0]
        def res  = plat.getLiveData('kill', ['process.pid' : pid,
                                             'process.signal' : 'QUIT'])
        render()                                         
    }
    
    def showProcess = { params ->
        def plat = resourceHelper.find(platform:params['id'])
        def pid  = params['pid'][0]
        def res  = plat.getLiveData('process', ["process.pid" : pid])
                                    
        if (res.hasError()) {
            render(file:'showProcessError', 
                   args:[errorMsg:res.errorMessage])
            return
        }
        
        res = res.objectResult
        render(args:[procData:res, platform:plat, pid:pid])
    }
    
    def showCrack = { params ->
        def group    = resourceHelper.find(group:params['id']) 
        def entIds   = group.appdefGroupEntries
        def cpuData  = []
        def topTable = []
        def getCmds  = []
        for (id in entIds) {
            if (!liveDataHelper.resourceSupports(id, 'top'))
                continue

            getCmds << ([id, 'top', [:] as ConfigResponse] as LiveDataCommand)
        }
        
        def startTime = System.currentTimeMillis()
        for (dataRes in liveDataHelper.getData(getCmds as LiveDataCommand[])) {
            def rsrc = resourceHelper.find(resource:dataRes.appdefEntityID)
            
            if (dataRes.hasError()) { 
                cpuData << [name:rsrc.name, value:dataRes.errorMessage]
                continue
            }
                
            def data = dataRes.objectResult                                  
                
            cpuData << [name:rsrc.name, value:data.cpu]
                
            for (p in data.processes) {
                def niceName = p.baseName
                if (niceName.length() > 40)
                    niceName = niceName[-40..-1]

                def stateIcon = STATE_ICONS["" + p.state]
                stateIcon = (stateIcon == null) ? 'green' : stateIcon                                                 
                topTable << [platform:rsrc, niceName:niceName, 
                             stateIcon:stateIcon, data:p]
            }
        }
        
        topTable = topTable.sort { a, b ->
            b.data.cpuPerc <=> a.data.cpuPerc
        }

        if (topTable.size() > 60) {
            topTable = topTable[0..<60]
        }

        def totTime = System.currentTimeMillis() - startTime
        render(args:[group:group, cpuData:cpuData, topTable:topTable,
                     fetchTime:totTime])
    }
}
