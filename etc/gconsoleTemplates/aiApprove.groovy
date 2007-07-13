import java.util.ArrayList
import org.hyperic.hq.appdef.server.session.AIQueueManagerEJBImpl
import org.hyperic.hq.appdef.shared.AIIpValue
import org.hyperic.hq.appdef.shared.AIPlatformValue
import org.hyperic.hq.appdef.shared.AIQueueConstants
import org.hyperic.hq.appdef.shared.AIQueueManagerLocal
import org.hyperic.hq.appdef.shared.AIServerValue
import org.hyperic.hq.authz.server.session.AuthzSubjectManagerEJBImpl
import org.hyperic.hq.authz.shared.AuthzSubjectManagerLocal
import org.hyperic.hq.authz.shared.AuthzSubjectValue
import org.hyperic.util.pager.PageControl
import org.hyperic.util.pager.PageList

AuthzSubjectManagerLocal subMan = AuthzSubjectManagerEJBImpl.one
AIQueueManagerLocal aiMan = AIQueueManagerEJBImpl.one

AuthzSubjectValue overlord = subMan.overlord

PageList list = aiMan.getQueue(overlord, true, true, PageControl.PAGE_ALL)

list.each { plat ->
    List platformIds = new ArrayList()
    List serverIds = new ArrayList()
    List ipIds = new ArrayList()

    platformIds.add(plat.id)
    AIServerValue[] servers = plat.AIServerValues
    servers.each { server ->
        serverIds.add(server.id)
    }
    
    AIIpValue[] ips = plat.AIIpValues
    ips.each { ip ->
        ipIds.add(ip.id)  
    }

    System.out.println("Processing platform " + plat.id)
    aiMan.processQueue(overlord, platformIds, serverIds, 
                       ipIds, AIQueueConstants.Q_DECISION_APPROVE)
    System.out.println("Sleeping 20 seconds")
    Thread.sleep(20000)
    System.out.println("Done")
}

return "Processed " + list.size() + " platforms"
