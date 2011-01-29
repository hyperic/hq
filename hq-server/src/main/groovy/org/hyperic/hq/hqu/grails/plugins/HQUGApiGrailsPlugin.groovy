package org.hyperic.hq.hqu.grails.plugins

import org.hyperic.hq.hqu.grails.hqugapi.AgentHQUGApi
import org.hyperic.hq.hqu.grails.hqugapi.AlertDefinitionHQUGApi
import org.hyperic.hq.hqu.grails.hqugapi.ResourceHQUGApi
import org.hyperic.hq.hqu.grails.hqugapi.MeasurementHQUGApi


class HQUGApiGrailsPlugin {

	def version = grails.util.GrailsUtil.getGrailsVersion()
	
	def doWithSpringOnce = {
		log.debug("Creating HQUG api beans")
		
		agentHQUGApi(AgentHQUGApi)
		alertDefHQUGApi(AlertDefinitionHQUGApi)
		resourceHQUGApi(ResourceHQUGApi)
		measurementHQUGApi(MeasurementHQUGApi)
	}

}
