package org.hyperic.hq.perftest

import org.hyperic.perftest.Task

import com.gargoylesoftware.htmlunit.WebClient
import com.gargoylesoftware.htmlunit.html.HtmlForm
import com.gargoylesoftware.htmlunit.html.HtmlPage
import com.gargoylesoftware.htmlunit.html.HtmlPasswordInput
import com.gargoylesoftware.htmlunit.html.HtmlTextArea
import com.gargoylesoftware.htmlunit.html.HtmlTextInput

class HQClient {
    private WebClient client

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
                
            page = client.getPage("${baseUrl}/ResourceHub.do?ff=$ff&view=list&ps=$pageSize")
        } else {
            throw new RuntimeException("Unhandled target [" + opts + "]")
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
}
