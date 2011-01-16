package org.hyperic.hq.hqu.grails.plugins

import org.springframework.beans.factory.config.MethodInvokingFactoryBean
import org.codehaus.groovy.grails.commons.GrailsServiceClass
import org.codehaus.groovy.grails.commons.ServiceArtefactHandler

class HQUServicesGrailsPlugin {

	def version = grails.util.GrailsUtil.getGrailsVersion()

	def doWithSpring = {
		
		def hquAppId = application.getHQUApplicationId()
		
		xmlns tx:"http://www.springframework.org/schema/tx"
		tx.'annotation-driven'()

		for(serviceGrailsClass in application.serviceClasses) {
			GrailsServiceClass serviceClass = serviceGrailsClass

			def scope = serviceClass.getPropertyValue("scope")

			"${serviceClass.fullName}ServiceClass"(MethodInvokingFactoryBean) {
				targetObject = ref("${hquAppId}grailsApplication", true)
				targetMethod = "getArtefact"
				arguments = [ServiceArtefactHandler.TYPE, serviceClass.fullName]
			}

			"${serviceClass.propertyName}"(serviceClass.getClazz()) { bean ->
				bean.autowire =  true
				if(scope) {
					bean.scope = scope
				}

			}
		}
	}


}
