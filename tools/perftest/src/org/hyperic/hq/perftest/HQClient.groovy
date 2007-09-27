package org.hyperic.hq.perftest

import org.hyperic.perftest.Task

import com.gargoylesoftware.htmlunit.WebClient
import com.gargoylesoftware.htmlunit.html.HtmlForm
import com.gargoylesoftware.htmlunit.html.HtmlPage
import com.gargoylesoftware.htmlunit.html.HtmlPasswordInput
import com.gargoylesoftware.htmlunit.html.HtmlTextArea
import com.gargoylesoftware.htmlunit.html.HtmlTextInput

class HQClient {
    private static typeToAppdefType = [platform:1, server:2, service:3]
    private static Random rand = new Random()
    private static Object resourceInit = new Object()
    private static Map    resources
    
    private WebClient  client
    
    def baseUrl = System.properties['hq.url']
                                    
    def jumpTo(targ, opts) {
        if (targ == 'hub') {
            def ff
            def pageSize
            if (opts.platforms) {
                ff = 1
                pageSize = opts.platforms.page_size
            } else if (opts.servers) {
                ff = 2
                pageSize = opts.servers.page_size
            } else if (opts.services) {
                ff = 3
                pageSize = opts.services.page_size
            } else {
                throw new RuntimeException("Hub jump needs platforms / servers / services. " + 
                                           "Example:  jumpTo('hub', platforms : [page_size: 30])")
            }
            
            if ((pageSize in String) && pageSize == 'unlimited') 
                pageSize = -1
                
            return getPage("${baseUrl}/ResourceHub.do?ff=$ff&view=list&ps=$pageSize")
        } else if (targ in Map && targ.type in ['platform', 'server', 'service'] ) {
            def appdefType = typeToAppdefType[targ.type]
            def aeid       = "${appdefType}:${targ.instanceId}"

            if (opts.inventory) {
                if (opts.inventory == 'main') {
                    return getPage("${baseUrl}/resource/platform/Inventory.do?mode=view&eid=${aeid}")
                } else {
                    throw new RuntimeException("Unsupported inventory type [${opts.inventory}]")
                }
            } else if (opts.monitor) {
                if (opts.monitor == 'indicators') {
                    return getPage("${baseUrl}/Resource.do?eid=${aeid}")
                } else if (opts.monitor == 'metric_data') {
                    return getPage("${baseUrl}/resource/platform/monitor/Visibility.do?mode=resourceMetrics&eid=${aeid}")
                } else {
                    throw new RuntimeException("Unsupported monitor type [${opts.monitor}]")
                } 
            } else if (opts.alert) {
                if (opts.alert == 'configure') {
                    return getPage("${baseUrl}/alerts/Config.do?mode=list&eid=${aeid}")
                } else {
                  throw new RuntimeException("Unsupported alert type [${opts.alert}]")
                }
            } else {
                throw new RuntimeException("Unsupported resource jump for [${targ}]")
            }
    	} 
        throw new RuntimeException("Unhandled target [" + targ + "]")
    }

    def getPerfSupportPage(action) {
    	getPage("${baseUrl}/hqu/perfsupport/support/${action}.hqu") 
    }
    
    def getPage(url) {
        client.getPage(url)
    }
    
    def getHQPage(url) {
		getPage("${baseUrl}${url}")        
    }

    private void initResources() {
        if (resources != null)
            return
            
        def page = getHQPage('/admin/sql.jsp')
        def form = page.getForms().iterator().next()
        def sqlArea = form.getTextAreasByName("sql").iterator().next()
        sqlArea.setText('select id, resource_type_id, instance_id, name ' + 
                        'from eam_resource ' +
                        'where resource_type_id in (301, 303, 305);')
        
        def button = form.getInputByName('ok') 
        
        page = button.click()
        def nodes = page.getByXPath('//tbody//font[1]/text()')
    	def vals  = nodes.nodeValue[4..-10]
    	def platforms = []
        def servers   = []
        def services  = []
        def typeMap = ["301" : 'platform', 
                       "303" : 'server', 
                       "305" : 'service']
        resources = [platform:[], server:[], service:[]]
    	for (int i=0; i<vals.size(); i+=4) {
    	    def typeName = typeMap[vals[i+1]]
    	    resources[typeName] << [id:vals[i+0], type:typeName,
    	                            instanceId:vals[i+2], name:vals[i+3]]
    	}
    }        
        
    def getRandomPlatform() {
        synchronized (resourceInit) {
            initResources()
            return resources.platform[rand.nextInt(resources.platform.size)]
        }
    }
    
    def getRandomServer() {
        synchronized (resourceInit) {
            initResources()
            return resources.server[rand.nextInt(resources.server.size)]
        }
    }

    def getRandomService() {
        synchronized (resourceInit) {
            initResources()
            return resources.service[rand.nextInt(resources.service.size)]
        }
    }

    /**
     * Setup the web client and login.  
     */
    void login() {
        client = new WebClient();
		client.javaScriptEnabled = false        
        client.redirectEnabled   = true
        
        HtmlPage page = (HtmlPage)client.getPage(baseUrl)
        HtmlForm f = page.getFormByName("LoginForm");
        HtmlTextInput userField = (HtmlTextInput)f.getInputByName("j_username");
        userField.setValueAttribute(System.properties['hq.user'])
        HtmlPasswordInput passField = (HtmlPasswordInput)f.getInputByName("j_password");
        passField.setValueAttribute(System.properties['hq.pword'])

        f.submit();
    }
    
    static def preload() {
        def c = new HQClient()
        c.login()
        c.getRandomPlatform()
    }
}
