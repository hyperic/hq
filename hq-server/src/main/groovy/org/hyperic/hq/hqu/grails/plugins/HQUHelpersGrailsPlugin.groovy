package org.hyperic.hq.hqu.grails.plugins

import org.hyperic.hq.hqu.grails.helpers.AgentHelper

class HQUHelpersGrailsPlugin {
	
	def version = grails.util.GrailsUtil.getGrailsVersion()
	
	def doWithSpringOnce = {
		log.info("HQUHelpersGrailsPlugin doWithSpringOnce")
		
		agentHelper(AgentHelper)
	}
	
}
