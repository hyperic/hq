import org.hyperic.hq.hqu.rendit.BaseController
import org.hyperic.hq.common.server.session.ServerConfigManagerEJBImpl as SCM

class PropController 
	extends BaseController
{
    def PropController() {
        addBeforeFilter({ 
            if (!user.isSuperUser()) {
                render(inline: "Unauthorized")
                return true
            }
            return false
        })
        setJSONMethods(['setProp'])
    }

    def setProp(params) {
        def key    = params.getOne('key')
        def newVal = params.getOne('newVal')
        
        def props = SCM.one.config
        log.info "Setting system property ${key} to ${newVal}"
        props[key] = newVal
        SCM.one.setConfig(user, props)
    }
    
    def index(params) {
    	render(locals:[props:SCM.one.configProperties.sort {a,b->a.key<=>b.key}])
    }
}
