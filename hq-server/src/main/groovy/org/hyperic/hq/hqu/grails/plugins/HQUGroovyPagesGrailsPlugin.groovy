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

import grails.util.BuildSettingsHolder
import grails.util.Environment
import grails.util.PluginBuildSettings

import groovy.lang.MetaClass
import java.lang.reflect.Modifier

import org.codehaus.groovy.grails.commons.GrailsClass
import org.codehaus.groovy.grails.commons.GrailsClassUtils
import org.codehaus.groovy.grails.commons.GrailsTagLibClass
import org.codehaus.groovy.grails.commons.TagLibArtefactHandler
import org.codehaus.groovy.grails.web.pages.ext.jsp.TagLibraryResolver
import org.codehaus.groovy.grails.web.plugins.support.WebMetaUtils
import org.codehaus.groovy.grails.web.servlet.GrailsApplicationAttributes
import org.springframework.beans.factory.config.PropertiesFactoryBean
import org.springframework.context.ApplicationContext
import org.springframework.core.io.FileSystemResource
import org.springframework.web.context.request.RequestContextHolder
import org.codehaus.groovy.grails.plugins.web.taglib.*
import org.codehaus.groovy.grails.web.pages.DefaultGroovyPagesUriService
import org.codehaus.groovy.grails.web.pages.GroovyPage
import org.codehaus.groovy.grails.web.pages.GroovyPageBinding
import org.codehaus.groovy.grails.web.pages.GroovyPageOutputStack
import org.codehaus.groovy.grails.web.pages.TagLibraryLookup
import org.codehaus.groovy.grails.web.filters.JavascriptLibraryFilters
import org.codehaus.groovy.grails.plugins.GrailsPluginManager
import grails.util.BuildSettings

import org.hyperic.hq.hqu.grails.web.pages.HQUGroovyPageResourceLoader;
import org.hyperic.hq.hqu.grails.web.pages.HQUGroovyPagesTemplateEngine
import org.hyperic.hq.hqu.grails.web.servlet.view.HQUGrailsViewResolver;

/**
 * Rework of grails GroovyPagesGrailsPlugin.
 * 
 * Changes:
 * - doWithWebDescriptor removed
 * - onChange removed
 * - defining different set of tag libraries
 * 
 */

public class HQUGroovyPagesGrailsPlugin {

    // monitor all resources that end with TagLib.groovy
    def watchedResources = [ "file:./plugins/*/grails-app/taglib/**/*TagLib.groovy",
                             "file:./grails-app/taglib/**/*TagLib.groovy"]


    def version = grails.util.GrailsUtil.getGrailsVersion()
    def dependsOn = [HQUCore: version]
//    def observe = ['controllers']


    // Provide these tag libraries declaratively
	
//	ApplicationTagLib,
//	CountryTagLib,
//	FormatTagLib,
//	FormTagLib,
//	JavascriptTagLib,
//	RenderTagLib,
//	ValidationTagLib,
//	PluginTagLib,
//	SitemeshTagLib,
//	JavascriptLibraryFilters

	
    def providedArtefacts = [
		FormTagLib,
		SitemeshTagLib
    ]



    /**
     * Clear the page cache with the ApplicationContext is loaded
     */
    def doWithApplicationContext = {ApplicationContext ctx ->
        HQUGroovyPagesTemplateEngine templateEngine = ctx.getBean("XgroovyPagesTemplateEngine")
        templateEngine.clearPageCache()
    }

    /**
     * Configures the various Spring beans required by GSP
     */
    def doWithSpring = {
		
		def hquAppId = application.getHQUApplicationId()
		
        // A bean used to resolve JSP tag libraries
        "${hquAppId}jspTagLibraryResolver"(TagLibraryResolver)
        // A bean used to resolve GSP tag libraries
        "${hquAppId}gspTagLibraryLookup"(TagLibraryLookup)

        boolean developmentMode = !application.warDeployed
        Environment env = Environment.current
        boolean enableReload = env.isReloadEnabled() || application.config.grails.gsp.enable.reload == true || (developmentMode && env == Environment.DEVELOPMENT)
        boolean warDeployedWithReload = application.warDeployed && enableReload

        // If the development environment is used we need to load GSP files relative to the base directory
        // as oppose to in WAR deployment where views are loaded from /WEB-INF
        def viewsDir = application.config.grails.gsp.view.dir
        if (viewsDir) {
            log.info "Configuring GSP views directory as '${viewsDir}'"
            "${hquAppId}groovyPageResourceLoader"(org.hyperic.hq.hqu.grails.web.pages.HQUGroovyPageResourceLoader) {
                baseResource = "file:${viewsDir}"
                pluginSettings = new PluginBuildSettings(BuildSettingsHolder.settings)
            }
        }
        else {
            if (developmentMode) {
				log.info("HQUGroovyPagesGrailsPlugin developmentMode true")
                "${hquAppId}groovyPageResourceLoader"(org.hyperic.hq.hqu.grails.web.pages.HQUGroovyPageResourceLoader) {
                    BuildSettings settings = BuildSettingsHolder.settings
                    def location = settings?.baseDir ? HQUGroovyPagesGrailsPlugin.transformToValidLocation(settings.baseDir.absolutePath) : './hq-engine/hq-server/webapps/ROOT/'
     				log.info("HQUGroovyPagesGrailsPlugin location1: " + location)
                    baseResource = "file:$location"
                    pluginSettings = new PluginBuildSettings(settings)
                }
            }
            else {
				log.info("HQUGroovyPagesGrailsPlugin developmentMode false")
                if (warDeployedWithReload) {
                    "${hquAppId}groovyPageResourceLoader"(org.hyperic.hq.hqu.grails.web.pages.HQUGroovyPageResourceLoader) {
                        if(env.hasReloadLocation()) {
                            def location = HQUGroovyPagesGrailsPlugin.transformToValidLocation(env.reloadLocation)                             
                            baseResource = "file:${location}"
                        }
                        else {
                            baseResource = "/WEB-INF"
                        }
                        pluginSettings = new PluginBuildSettings(BuildSettingsHolder.settings)
                    }
                }
            }
        }

        // Setup the main templateEngine used to render GSPs
        XgroovyPagesTemplateEngine(org.hyperic.hq.hqu.grails.web.pages.HQUGroovyPagesTemplateEngine) {
            classLoader = ref("classLoader")
            if (developmentMode || warDeployedWithReload) {
                resourceLoader = ref("${hquAppId}groovyPageResourceLoader")
            }
            if (enableReload) {
                reloadEnabled = enableReload
            }
            tagLibraryLookup = ref("${hquAppId}gspTagLibraryLookup")
            jspTagLibraryResolver = ref("${hquAppId}jspTagLibraryResolver")
			precompiledGspMap = { PropertiesFactoryBean pfb -> 
				ignoreResourceNotFound = true
				location = "classpath:gsp/views.properties"
			}
        }

        // Setup the GroovyPagesUriService
        groovyPagesUriService(org.codehaus.groovy.grails.web.pages.DefaultGroovyPagesUriService) {
        	
        }
        
        // Configure a Spring MVC view resolver
        "${hquAppId}jspViewResolver"(HQUGrailsViewResolver) {
            viewClass = org.springframework.web.servlet.view.JstlView.class
//            prefix = GrailsApplicationAttributes.PATH_TO_VIEWS
			prefix = "WEB-INF/hqu-plugins/"
            suffix = ".jsp"
            templateEngine = XgroovyPagesTemplateEngine
            if (developmentMode) {
                resourceLoader = ref("${hquAppId}groovyPageResourceLoader")
            }
			grailsApplication = ref("${hquAppId}grailsApplication", true)
        }

        // Now go through tag libraries and configure them in spring too. With AOP proxies and so on
        for(taglib in application.tagLibClasses) {
            "${taglib.fullName}"(taglib.clazz) { bean ->
                   bean.autowire = true
                   // Taglib scoping support could be easily added here. Scope could be based on a static field in the taglib class.
				   //bean.scope = 'request'
            }
        }

    }

    static String transformToValidLocation(String location) {
        if(location == '.') return location
        if (!location.endsWith(File.separator)) return "${location}${File.separator}"
        return location
    }

    /**
     * Sets up dynamic methods required by the GSP implementation including dynamic tag method dispatch
     */
    def doWithDynamicMethods = { ApplicationContext ctx ->

		def hquAppId = application.getHQUApplicationId()
		
    	WebMetaUtils.registerStreamCharBufferMetaClass()

        TagLibraryLookup gspTagLibraryLookup = ctx.getBean("${hquAppId}gspTagLibraryLookup")
        GrailsPluginManager pluginManager = getManager()

        if(manager?.hasGrailsPlugin("controllers")) {
            for(namespace in gspTagLibraryLookup.availableNamespaces) {
                def propName = GrailsClassUtils.getGetterName(namespace)
                def namespaceDispatcher = gspTagLibraryLookup.lookupNamespaceDispatcher(namespace)
                def controllerClasses = application.controllerClasses*.clazz
                for(Class controllerClass in controllerClasses) {
                    MetaClass mc = controllerClass.metaClass
                    if(!mc.getMetaProperty(namespace)) {
                        mc."$propName" = { namespaceDispatcher }                
                    }
                    registerControllerMethodMissing(mc, gspTagLibraryLookup, ctx)
                    Class superClass = controllerClass.superclass
                    // deal with abstract super classes
                    while (superClass != Object.class) {
                        if (Modifier.isAbstract(superClass.getModifiers())) {
                            registerControllerMethodMissing(superClass.metaClass, gspTagLibraryLookup, ctx)
                        }
                        superClass = superClass.superclass
                    }

                }
            }
        }

        for (GrailsTagLibClass t in application.tagLibClasses) {
            GrailsTagLibClass taglib = t
            MetaClass mc = taglib.metaClass
            String namespace = taglib.namespace ?: GroovyPage.DEFAULT_NAMESPACE

            WebMetaUtils.registerCommonWebProperties(mc, application)

            for(tag in taglib.tagNames) {
                WebMetaUtils.registerMethodMissingForTags(mc, gspTagLibraryLookup, namespace, tag)
            }

            mc.getTagNamesThatReturnObject = {->
            	taglib.getTagNamesThatReturnObject()
            }
            
            mc.throwTagError = {String message ->
                throw new org.codehaus.groovy.grails.web.taglib.exceptions.GrailsTagException(message)
            }
            mc.getPluginContextPath = {->
                pluginManager.getPluginPathForInstance(delegate) ?: ""
            }

            mc.getPageScope = {->
                def request = RequestContextHolder.currentRequestAttributes().currentRequest
                def binding = request.getAttribute(GrailsApplicationAttributes.PAGE_SCOPE)
                if (!binding) {
                    binding = new GroovyPageBinding()
                    request.setAttribute(GrailsApplicationAttributes.PAGE_SCOPE, binding)
                }
                binding
            }

            mc.getOut = {->
            	GroovyPageOutputStack.currentWriter()
            }
            mc.setOut = {Writer newOut ->
            	GroovyPageOutputStack.currentStack().push(newOut,true)
            }
            mc.propertyMissing = {String name ->
                def result = gspTagLibraryLookup.lookupNamespaceDispatcher(name)
                if(result == null) {
                    def tagLibrary = gspTagLibraryLookup.lookupTagLibrary(namespace, name)
                    if(!tagLibrary) tagLibrary = gspTagLibraryLookup.lookupTagLibrary(GroovyPage.DEFAULT_NAMESPACE, name)

                    def tagProperty = tagLibrary?."$name"
                    result = tagProperty ? tagProperty.clone() : null
                }

                if(result!=null) {
                    mc."${GrailsClassUtils.getGetterName(name)}" = {-> result}
                    return result
                }
                else {
                    throw new MissingPropertyException(name, delegate.class)
                }

            }
            mc.methodMissing = {String name, args ->
            	def usednamespace = namespace
                def tagLibrary = gspTagLibraryLookup.lookupTagLibrary(namespace, name)
                if(!tagLibrary) {
                	tagLibrary = gspTagLibraryLookup.lookupTagLibrary(GroovyPage.DEFAULT_NAMESPACE, name)
					usednamespace = GroovyPage.DEFAULT_NAMESPACE
                }
                if(tagLibrary) {
                	WebMetaUtils.registerMethodMissingForTags(mc, gspTagLibraryLookup, usednamespace, name)
                    //WebMetaUtils.registerMethodMissingForTags(mc, tagLibrary, name)
                }
                if (mc.respondsTo(delegate, name, args)) {
                    return mc.invokeMethod(delegate, name, args)
                }
                else {
                    throw new MissingMethodException(name, delegate.class, args)
                }
            }
            ctx.getBean(taglib.fullName).metaClass = mc
        }

    }



    def registerControllerMethodMissing(MetaClass mc, TagLibraryLookup lookup, ApplicationContext ctx) {
        // allow controllers to call tag library methods
        mc.methodMissing = {String name, args ->
            args = args == null ? [] as Object[] : args            
            def tagLibrary = lookup.lookupTagLibrary(GroovyPage.DEFAULT_NAMESPACE, name)
            if (tagLibrary) {
                MetaClass controllerMc = delegate.class.metaClass
                WebMetaUtils.registerMethodMissingForTags(controllerMc, lookup, GroovyPage.DEFAULT_NAMESPACE, name)
                if(controllerMc.respondsTo(delegate, name, args)) {
                  return controllerMc.invokeMethod(delegate, name, args)
                }
                else {
                  throw new MissingMethodException(name, delegate.class, args)
                }
            }
            else {
                throw new MissingMethodException(name, delegate.class, args)
            }
        }

    }

    private PluginBuildSettings createPluginSettings() {
        return new PluginBuildSettings(BuildSettingsHolder.settings);
    }
}
