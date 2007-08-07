package org.hyperic.hq.perftest

import org.hyperic.perftest.Task

import com.gargoylesoftware.htmlunit.WebClient
import com.gargoylesoftware.htmlunit.html.HtmlForm
import com.gargoylesoftware.htmlunit.html.HtmlPage
import com.gargoylesoftware.htmlunit.html.HtmlPasswordInput
import com.gargoylesoftware.htmlunit.html.HtmlTextArea
import com.gargoylesoftware.htmlunit.html.HtmlTextInput

class WebTask extends Task { 
	private Closure closure
	
    def WebTask(String desc, Closure c) {
        super(desc, c)
        closure = c
    }

    protected void executeClosure(Closure statsCollector) {
        def client = new HQClient()
        client.login()
        statsCollector() {
            closure(client)
        }
    }
}
