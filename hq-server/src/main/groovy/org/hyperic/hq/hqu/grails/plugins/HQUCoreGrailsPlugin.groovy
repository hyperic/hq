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

import org.codehaus.groovy.grails.commons.metaclass.*
import org.codehaus.groovy.grails.support.ClassEditor
import org.springframework.beans.factory.config.CustomEditorConfigurer
import org.springframework.beans.factory.config.MethodInvokingFactoryBean
import org.codehaus.groovy.grails.commons.cfg.MapBasedSmartPropertyOverrideConfigurer
import org.codehaus.groovy.grails.commons.cfg.GrailsPlaceholderConfigurer
import org.springframework.core.io.Resource
import org.codehaus.groovy.grails.commons.spring.DefaultRuntimeSpringConfiguration
import org.codehaus.groovy.grails.commons.spring.RuntimeSpringConfiguration
import org.codehaus.groovy.grails.commons.spring.GrailsRuntimeConfigurator
import org.codehaus.groovy.grails.plugins.support.aware.GrailsApplicationAwareBeanPostProcessor
import org.codehaus.groovy.grails.support.DevelopmentShutdownHook
import grails.util.Environment
import grails.util.Metadata;

import org.codehaus.groovy.grails.aop.framework.autoproxy.GroovyAwareInfrastructureAdvisorAutoProxyCreator
import org.codehaus.groovy.grails.plugins.support.aware.PluginManagerAwareBeanPostProcessor

import org.hyperic.hq.hqu.grails.plugins.support.aware.HQUGrailsApplicationAwareBeanPostProcessor;

/**
 * A plug-in that configures the core shared beans within the Grails application context 
 * 
 */
class HQUCoreGrailsPlugin {
	
	def version = grails.util.GrailsUtil.getGrailsVersion()
    def watchedResources = ["file:./grails-app/conf/spring/resources.xml","file:./grails-app/conf/spring/resources.groovy"]
	
	
	def doWithSpring = {
        xmlns context:"http://www.springframework.org/schema/context"
        xmlns grailsContext:"http://grails.org/schema/context"

		def prefix = application.getHQUApplicationId()
		
		log.info("HQUCoreGrailsPlugin plugin prefix: " + application.getHQUApplicationId())

        addBeanFactoryPostProcessor(new MapBasedSmartPropertyOverrideConfigurer(application.config.beans, application.classLoader))
        addBeanFactoryPostProcessor(new GrailsPlaceholderConfigurer())

        // replace AutoProxy advisor with Groovy aware one
        "org.springframework.aop.config.internalAutoProxyCreator"(GroovyAwareInfrastructureAdvisorAutoProxyCreator)

        // Allow the use of Spring annotated components
        context.'annotation-config'()

        def packagesToScan = []

        def beanPackages = application.config.grails.spring.bean.packages
        if(beanPackages instanceof List) {
            packagesToScan += beanPackages
        }

        def validateablePackages = application.config.grails.validateable.packages
        if(validateablePackages instanceof List) {
            packagesToScan += validateablePackages
        }

        if(packagesToScan) {
            grailsContext.'component-scan'('base-package':packagesToScan.join(','))
        }

		// "${prefix}controllerHandlerMappings" created with every registered app !!! damn...
//        "${prefix}grailsApplicationPostProcessor"(GrailsApplicationAwareBeanPostProcessor, ref("${prefix}grailsApplication", true))
        "${prefix}grailsApplicationPostProcessor"(HQUGrailsApplicationAwareBeanPostProcessor, ref("${prefix}grailsApplication", true))
        if(getParentCtx()?.containsBean('pluginManager'))
            pluginManagerPostProcessor(PluginManagerAwareBeanPostProcessor, ref('pluginManager', true))

        classLoader(MethodInvokingFactoryBean) {
			targetObject = ref("${prefix}grailsApplication", true)
			targetMethod = "getClassLoader"
		}

        // add shutdown hook if not running in war deployed mode
        if(!Metadata.getCurrent().isWarDeployed() || Environment.currentEnvironment == Environment.DEVELOPMENT)
            shutdownHook(DevelopmentShutdownHook)
        
		customEditors(CustomEditorConfigurer) {
			customEditors = [(java.lang.Class.name):ClassEditor.name]
		}
	}
	
	def doWithDynamicMethods = {
		MetaClassRegistry registry = GroovySystem.metaClassRegistry

		def metaClass = registry.getMetaClass(Class.class)
		if(!(metaClass instanceof ExpandoMetaClass)) {
			registry.removeMetaClass(Class.class)
			metaClass = registry.getMetaClass(Class.class)
		}

		metaClass.getMetaClass = {->
			def mc = registry.getMetaClass(delegate)
			if(mc instanceof ExpandoMetaClass) {
				return mc
			}
			else {
 
				registry.removeMetaClass(delegate)
				if(registry.metaClassCreationHandler instanceof ExpandoMetaClassCreationHandle)				
					return registry.getMetaClass(delegate)
			   	else {
				 	def emc = new ExpandoMetaClass(delegate, false, true)
					emc.initialize()
					registry.setMetaClass(delegate, emc)    
					return emc
				}					
			}
		}
	}
	
}