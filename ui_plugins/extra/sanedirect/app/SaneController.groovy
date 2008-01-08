import org.hyperic.hq.hqu.rendit.BaseController

class SaneController 
	extends BaseController
{
    def SaneController() {
    }
    
    def redirect(params) {
        def plat   = params.getOne('platform')
        def platId = params.getOne('platformId')
        def svr    = params.getOne('server')
        def svrId  = params.getOne('serverId')
        def svc    = params.getOne('service')
        def svcId  = params.getOne('serviceId')
        def ctx    = params.getOne('context')
        
        log.info "Redirecting from ${params}"
        
        def args = [:]
        if (args.resourceContext) {
            args.resourceContext = ctx
        }

        def findArgs = [:]
        if (platId != null) {
            findArgs['platform'] = Integer.parseInt(platId)
        } else if (plat != null) {
            findArgs['platform'] = plat
        }
        
        if (svrId != null) {
            findArgs['server'] = Integer.parseInt(svrId)
        } else if (svr != null) {
            findArgs['server'] = svr
        }
        
        if (svcId != null) {
            findArgs['service'] = Integer.parseInt(svcId)
        } else if (svc != null) {
            findArgs['service'] = svc
        }

        log.info "finding with ${findArgs}"
        def rsrc = resourceHelper.find(findArgs)
        args['resource'] = rsrc
        redirectTo(args)
    }
}
