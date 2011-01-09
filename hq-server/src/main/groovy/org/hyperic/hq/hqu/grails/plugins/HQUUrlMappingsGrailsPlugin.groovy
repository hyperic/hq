/**
 * NOTE: This copyright does *not* cover user programs that use HQ
 * program services by normal system calls through the application
 * program interfaces provided as part of the Hyperic Plug-in Development
 * Kit or the Hyperic Client Development Kit - this is merely considered
 * normal use of the program, and does *not* fall under the heading of
 *  "derived work".
 *
 *  Copyright (C) [2009-2010], VMware, Inc.
 *  This file is part of HQ.
 *
 *  HQ is free software; you can redistribute it and/or modify
 *  it under the terms version 2 of the GNU General Public License as
 *  published by the Free Software Foundation. This program is distributed
 *  in the hope that it will be useful, but WITHOUT ANY WARRANTY; without
 *  even the implied warranty of MERCHANTABILITY or FITNESS FOR A
 *  PARTICULAR PURPOSE. See the GNU General Public License for more
 *  details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program; if not, write to the Free Software
 *  Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307
 *  USA.
 *
 */
package org.hyperic.hq.hqu.grails.plugins


import org.codehaus.groovy.grails.commons.UrlMappingsArtefactHandler
import org.codehaus.groovy.grails.web.mapping.UrlMappingsHolderFactoryBean
import org.springframework.aop.framework.ProxyFactoryBean
import org.springframework.aop.target.HotSwappableTargetSource
import org.springframework.context.ApplicationContext
import org.springframework.core.io.Resource
import grails.spring.BeanBuilder

/**
* A plug-in that handles the configuration of URL mappings for Grails
*
*/
class HQUUrlMappingsGrailsPlugin {

	def watchedResources = ["file:./grails-app/conf/*UrlMappings.groovy"]

	def version = grails.util.GrailsUtil.getGrailsVersion()
	def dependsOn = [HQUCore:version]

	def doWithSpring = {
		
		def prefix = application.getHQUApplicationId()

		log.info("HQUUrlMappingsGrailsPlugin plugin prefix: " + application.getHQUApplicationId())
		
		"${prefix}grailsUrlMappingsHolderBean"(UrlMappingsHolderFactoryBean) {
            grailsApplication = ref("${prefix}grailsApplication", true)
        }
        "${prefix}urlMappingsTargetSource"(org.springframework.aop.target.HotSwappableTargetSource, ref("${prefix}grailsUrlMappingsHolderBean", false))
        "${prefix}grailsUrlMappingsHolder"(ProxyFactoryBean) {
            targetSource = ref("${prefix}urlMappingsTargetSource", false)
            proxyInterfaces = [org.codehaus.groovy.grails.web.mapping.UrlMappingsHolder]
        }
	}

}