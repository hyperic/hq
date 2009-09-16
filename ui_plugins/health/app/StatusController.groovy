import org.hyperic.hq.hqu.rendit.BaseController

class StatusController
    extends BaseController
{
    StatusController() {}

    private isMasterNode() {
        try {
            def svr = org.hyperic.hq.product.server.MBeanUtil.getMBeanServer()
            javax.management.ObjectName o =
                new javax.management.ObjectName("hyperic.jmx:type=Service," +
                                                "name=EEHAService-HASingletonController");
            return svr.getAttribute(o, "MasterNode")
        } catch (Throwable t) {
            return false
        }
    }

    /**
     * Indicates if this node is the master node.
     */
    def nodeStatus(params) {
        render(inline: "master=" + isMasterNode())
        return true
    }    
}
