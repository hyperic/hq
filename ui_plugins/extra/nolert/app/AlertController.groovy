import org.hyperic.hq.hqu.rendit.BaseController
import org.hyperic.hq.events.server.session.AlertDefinitionManagerEJBImpl as AlertMan

class AlertController 
	extends BaseController
{
    private alertMan = AlertMan.one

    protected void init() {
        onlyAllowSuperUsers()
    }
    
    def status(params) {
        render(inline: "Alerts allowed: ${alertMan.alertsAllowed()}\n")
    }
    
    def enable(params) {
        alertMan.setAlertsAllowed(true)
        status(params)
    }

    def disable(params) {
        alertMan.setAlertsAllowed(false)
        status(params)
    }
}
