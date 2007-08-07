import org.hyperic.hq.hqu.rendit.BaseController

class SupportController 
	extends BaseController
{
    def platforms(params) {
    	render(action: 'resources', 
    	       locals:[ resources : resourceHelper.findAllPlatforms()],
               contentType:'text/xml')    	                
    }

    def servers(params) {
    	render(action: 'resources', 
    	       locals:[ resources : resourceHelper.findAllServers()],
               contentType:'text/xml')    	                
    }

    def services(params) {
    	render(action: 'resources', 
    	       locals:[ resources : resourceHelper.findAllServices()],
               contentType:'text/xml')    	                
    }
}
