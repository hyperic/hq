package org.hyperic.hq.hqu.grails.plugins

import org.hyperic.hq.hqu.grails.web.filters.HQUGrailsRequestInterceptor

class HQUFiltersGrailsPlugin {
	
	def version = grails.util.GrailsUtil.getGrailsVersion()
	
	def doWithSpringOnce = {
		
		// interceptor to handle wrapped webUser in session scoped bean
		hquGrailsRequestInterceptor(HQUGrailsRequestInterceptor)
	}
		
}
