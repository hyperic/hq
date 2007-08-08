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
                                    
    def jumpTo(opts, targ) {
        def page
        
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
                
            page = getPage("${baseUrl}/ResourceHub.do?ff=$ff&view=list&ps=$pageSize")
        } else if (targ in Map && targ.type in ['platform', 'server', 'service'] ) {
            def appdefType = typeToAppdefType[targ.type]
            def aeid       = "${appdefType}:${targ.instanceId}"

            if (targ.inventory) {
                if (targ.inventory.main) {
                    page = getPage("${baseUrl}//resource/platform/Inventory.do?mode=view&eid=${aeid}")
                } else {
                    throw new RuntimeException("Unsupported inventory type [${targ.inventory}]")
                }
            } else if (targ.monitor) {
                if (targ.monitor.indicators) {
                    page = getPage("${baseUrl}/Resource.do?eid=${aeid}")
                } else if (targ.monitor.metric_data) {
                    page = getPage("${baseUrl}/resource/platform/monitor/Visibility.do?mode=resourceMetrics&eid=${aeid}")
                } else {
                    throw new RuntimeException("Unsupported monitor type [${targ.monitor}]")
                } 
            } else if (targ.alert) {
                if (targ.alert.configure) {
                    page = getPage("${baseUrl}/alerts/Config.do?mode=list&eid=${aeid}")
                } else {
                  throw new RuntimeException("Unsupported alert type [${targ.alert}]")
                }
            }
    	} else {
            throw new RuntimeException("Unhandled target [" + target + "]")
        }
    }

    def getPerfSupportPage(action) {
    	getPage("${baseUrl}/hqu/perfsupport/support/${action}.hqu") 
    }
    
    def getPage(url) {
        client.getPage(url)
    }

    private void initResources() {
        if (resources == null) {
            resources = [:]
            for (t in [platforms : 'platform', servers : 'server',
                       services : 'service'])
            {
                def page  = getPerfSupportPage(t.key)
                def res   = []
                def nodes = page.xmlDocument.getElementsByTagName('resource')
        
                for (i in 0..<nodes.length) {
                    def attrs = nodes.item(i).attributes
            
		            res << [id:attrs.getNamedItem('id').nodeValue,
			                type:t.value,
		                    instanceId:attrs.getNamedItem('instanceId').nodeValue,
    			            name:attrs.getNamedItem('name').nodeValue]
                }
                
                resources[t.key] = res
            }
        }
    }
    
    def getRandomPlatform() {
        synchronized (resourceInit) {
            initResources()
            return resources.platforms[rand.nextInt(resources.platforms.size)]
        }
    }
    
    def getRandomServer() {
        synchronized (resourceInit) {
            initResources()
            return resources.servers[rand.nextInt(resources.servers.size)]
        }
    }

    def getRandomService() {
        synchronized (resourceInit) {
            initResources()
            return resources.services[rand.nextInt(resources.services.size)]
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
        new HQClient().login()
    }
}
