package org.hyperic.hq.hqu.grails.plugins

import org.hyperic.hq.hqu.grails.hqugapi.AgentHQUGApi
import org.hyperic.hq.hqu.grails.hqugapi.AlertDefinitionHQUGApi


class HQUGApiGrailsPlugin {

	def version = grails.util.GrailsUtil.getGrailsVersion()
	
	def doWithSpringOnce = {
		log.debug("Creating HQUG api beans")
		
		agentHQUGApi(AgentHQUGApi)
		alertDefHQUGApi(AlertDefinitionHQUGApi)
	}

}
