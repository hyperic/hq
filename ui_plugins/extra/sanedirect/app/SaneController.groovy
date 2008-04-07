import org.hyperic.hq.hqu.rendit.BaseController

import org.hyperic.hq.authz.server.session.Resource
import org.hyperic.hq.authz.server.session.ResourceManagerEJBImpl

class SaneController 
	extends BaseController
{
    private resourceMan = ResourceManagerEJBImpl.one

    protected void init() {
        setXMLMethods(['getInfo'])
    }
    
    private Resource findResource(params) {
        def plat   = params.getOne('platform')
        def platId = params.getOne('platformId')
        def svr    = params.getOne('server')
        def svrId  = params.getOne('serverId')
        def svc    = params.getOne('service')
        def svcId  = params.getOne('serviceId')
        def rsrcId = params.getOne('resourceId')
        
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

        def rsrc
        if (rsrcId != null) {
            rsrc = resourceMan.findResourcePojoById(rsrcId.toInteger())
        } else {
            rsrc = resourceHelper.find(findArgs)
        }
        
        rsrc
    }
    
    def getInfo(xmlOut, params) {
        Resource r = findResource(params)
        
        if (!r) {
            xmlOut.error("Resource not found")
            return xmlOut
        }
        
        xmlOut.resources {
            xmlOut.resource(name: r.name, id: r.id, 
                            prototypeId: r.prototype.id, 
                            prototypeName: r.prototype.name)
        }
        xmlOut
    }
    
    def redirect(params) {
        def ctx    = params.getOne('context')
        def chart  = params.getOne('chart')
        def link   = params.getOne('link')
        
        log.info "Redirecting from ${params}"
        
        def args = [:]
        if (ctx) {
            args.resourceContext = ctx
        }

        args['resource'] = findResource(params) 
        
        if (args['resource']== null) {
            log.warn "Resource specified by [$params] not found"
            render(inline:"Resource specified by [$params] not found")
            return
        }
        
        if (chart) {
            def metric = rsrc.enabledMetrics.find { it.template.name == chart }
            if (!metric) {
                log.warn "No metric [${chart}] found for resource"
                render(inline:"No metric [${chart}] found for resource")
                return
            }
            args['resource'] = metric
            def end   = params.getOne('end', "${System.currentTimeMillis()}").toLong()
            def start = params.getOne('start')?.toLong()
                
            if (start == null)
                start = end - (8 * 60 * 60 * 1000)
             
            if (link?.toBoolean())
                args.resourceContext = null
            else
            	args.resourceContext = [chart: true, start: start, end: end] 
        }
        
        redirectTo(args)
    }
}
