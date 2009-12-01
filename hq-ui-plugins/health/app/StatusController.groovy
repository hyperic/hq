import org.hyperic.hq.hqu.rendit.BaseController
import org.hyperic.hq.ha.HAUtil

class StatusController
    extends BaseController
{
    StatusController() {}

   

    /**
     * Indicates if this node is the master node.
     */
    def nodeStatus(params) {
        render(inline: "master=" + HAUtil.isMasterNode())
        return true
    }    
}
