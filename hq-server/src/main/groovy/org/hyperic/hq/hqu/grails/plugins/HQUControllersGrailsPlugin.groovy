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

import org.codehaus.groovy.grails.commons.GrailsClassUtils as GCU
import org.springframework.web.context.request.RequestContextHolder as RCH

import grails.util.GrailsUtil
import java.lang.reflect.Modifier
import org.codehaus.groovy.grails.commons.*
import org.codehaus.groovy.grails.validation.ConstrainedPropertyBuilder
import org.codehaus.groovy.grails.web.binding.DataBindingLazyMetaPropertyMap
import org.codehaus.groovy.grails.web.binding.DataBindingUtils
import org.codehaus.groovy.grails.web.errors.GrailsExceptionResolver
import org.codehaus.groovy.grails.web.filters.HiddenHttpMethodFilter;
import org.codehaus.groovy.grails.web.metaclass.BindDynamicMethod
import org.codehaus.groovy.grails.web.metaclass.ChainMethod
import org.codehaus.groovy.grails.web.metaclass.RedirectDynamicMethod
import org.codehaus.groovy.grails.web.metaclass.RenderDynamicMethod
import org.codehaus.groovy.grails.web.multipart.ContentLengthAwareCommonsMultipartResolver
import org.codehaus.groovy.grails.web.plugins.support.WebMetaUtils
import org.codehaus.groovy.grails.web.servlet.GrailsApplicationAttributes
import org.codehaus.groovy.grails.web.servlet.mvc.SimpleGrailsController
import org.codehaus.groovy.grails.web.servlet.mvc.exceptions.ControllerExecutionException
import org.springframework.beans.factory.config.AutowireCapableBeanFactory
import org.springframework.context.ApplicationContext
import org.springframework.validation.BindException
import org.springframework.validation.Errors
import org.springframework.web.servlet.ModelAndView
import org.springframework.web.servlet.mvc.SimpleControllerHandlerAdapter
import org.codehaus.groovy.grails.web.metaclass.WithFormMethod
import org.codehaus.groovy.grails.web.metaclass.ForwardMethod
import org.springframework.beans.BeanUtils
import org.codehaus.groovy.grails.plugins.DomainClassPluginSupport
import org.springframework.validation.BeanPropertyBindingResult
import org.springframework.web.servlet.mvc.annotation.DefaultAnnotationHandlerMapping
import org.springframework.web.servlet.mvc.annotation.AnnotationMethodHandlerAdapter
import org.springframework.web.servlet.view.DefaultRequestToViewNameTranslator
import org.codehaus.groovy.grails.web.servlet.GrailsControllerHandlerMapping
import org.codehaus.groovy.grails.plugins.GrailsPluginManager
import org.hyperic.hq.hqu.grails.web.servlet.HQUGrailsControllerHandlerMapping;
import org.springframework.core.Ordered

/**
* A plug-in that handles the configuration of controllers for Grails
*
*/
class HQUControllersGrailsPlugin {

   def watchedResources = ["file:./grails-app/controllers/**/*Controller.groovy",
						   "file:./plugins/*/grails-app/controllers/**/*Controller.groovy"]

   def version = grails.util.GrailsUtil.getGrailsVersion()
   def dependsOn = [HQUCore: version, HQUUrlMappings: version]


   def doWithSpring = {
	   
	   def prefix = application.getHQUApplicationId()
	   
	   simpleControllerHandlerAdapter(SimpleControllerHandlerAdapter)
	   exceptionHandler(GrailsExceptionResolver) {
		   exceptionMappings = ['java.lang.Exception': '/error']
	   }
	   if (!application.config.grails.disableCommonsMultipart) {
		   multipartResolver(ContentLengthAwareCommonsMultipartResolver)
	   }
	   "${prefix}mainSimpleController"(SimpleGrailsController.class) {
		   grailsApplication = ref("${prefix}grailsApplication", true)
	   }

	   def handlerInterceptors = springConfig.containsBean("localeChangeInterceptor") ? [ref("localeChangeInterceptor")] : []
	   def interceptorsClosure = {
		   interceptors = handlerInterceptors
		   // setting priority max - 1 to possibly allow other mappings to define lower priority
		   order = Ordered.LOWEST_PRECEDENCE - 1
	   }
	   // allow @Controller annotated beans
	   "${prefix}annotationHandlerMapping"(DefaultAnnotationHandlerMapping, interceptorsClosure)
	   // allow default controller mappings
	   "${prefix}controllerHandlerMappings"(HQUGrailsControllerHandlerMapping, interceptorsClosure)
	   

	   annotationHandlerAdapter(AnnotationMethodHandlerAdapter)
	   viewNameTranslator(DefaultRequestToViewNameTranslator) {
			stripLeadingSlash = false
	   }
	   for(controller in application.controllerClasses) {
		   log.debug "Configuring controller $controller.fullName"
		   if (controller.available) {
			   "${controller.fullName}"(controller.clazz) { bean ->
				   bean.scope = "prototype"
				   bean.autowire = "byName"
			   }

		   }

	   }
   }

   def doWithDynamicMethods = {ApplicationContext ctx ->

	   // add common objects and out variable for tag libraries
	   def registry = GroovySystem.getMetaClassRegistry()
	   GrailsPluginManager pluginManager = getManager()

	   for (domainClass in application.domainClasses) {
		   GrailsDomainClass dc = domainClass
		   def mc = domainClass.metaClass

		   mc.constructor = { Map map ->
			   def instance = ctx.containsBean(dc.fullName) ? ctx.getBean(dc.fullName) : BeanUtils.instantiateClass(dc.clazz)
			   DataBindingUtils.bindObjectToDomainInstance(dc,instance, map)
			   DataBindingUtils.assignBidirectionalAssociations(instance,map,dc)
			   return instance
		   }
		   mc.setProperties = {Object o ->
			   DataBindingUtils.bindObjectToDomainInstance(dc, delegate, o)
		   }
		   mc.getProperties = {->
			   new DataBindingLazyMetaPropertyMap(delegate)
		   }
	   }


	   def bind = new BindDynamicMethod()
	   // add commons objects and dynamic methods like render and redirect to controllers
	   for (GrailsClass controller in application.controllerClasses) {
		   MetaClass mc = controller.metaClass

		   Class controllerClass = controller.clazz
		   WebMetaUtils.registerCommonWebProperties(mc, application)
		   
		   def prefix = application.getHQUApplicationId()
		   
		   registerControllerMethods(mc, ctx, prefix)
		   Class superClass = controller.clazz.superclass

		   mc.getPluginContextPath = {->
			   pluginManager.getPluginPathForInstance(delegate) ?: ''
		   }

		   // deal with abstract super classes
		   while (superClass != Object.class) {
			   if (Modifier.isAbstract(superClass.getModifiers())) {
				   WebMetaUtils.registerCommonWebProperties(superClass.metaClass, application)
				   registerControllerMethods(superClass.metaClass, ctx, prefix)
			   }
			   superClass = superClass.superclass
		   }

		   // look for actions that accept command objects and override
		   // each of the actions to make command objects binding before executing
		   for (actionName in controller.commandObjectActions) {
			   def originalAction = controller.getPropertyValue(actionName)
			   def paramTypes = originalAction.getParameterTypes()
			   def closureName = actionName
			   def commandObjectBindingAction = {Object[] varArgs ->

				   def commandObjects = []
				   for (v in varArgs) {
					   commandObjects << v
				   }
				   def counter = 0
				   for (paramType in paramTypes) {

					   if (GroovyObject.class.isAssignableFrom(paramType)) {
						   try {
							   def commandObject;
							   if (counter < commandObjects.size()) {
								   if (paramType.isInstance(commandObjects[counter])) {
									   commandObject = commandObjects[counter]
								   }
							   }

							   if (!commandObject) {
								   commandObject = paramType.newInstance()
								   ctx.autowireCapableBeanFactory?.autowireBeanProperties(commandObject, AutowireCapableBeanFactory.AUTOWIRE_BY_NAME, false)
								   commandObjects << commandObject
							   }
							   def params = RCH.currentRequestAttributes().params
							   bind.invoke(commandObject, "bindData", [commandObject, params] as Object[])
							   def errors = commandObject.errors ?: new BindException(commandObject, paramType.name)
							   def constrainedProperties = commandObject.constraints?.values()
							   constrainedProperties.each {constrainedProperty ->
								   constrainedProperty.messageSource = ctx.getBean("messageSource")
								   constrainedProperty.validate(commandObject, commandObject.getProperty(constrainedProperty.getPropertyName()), errors);
							   }
							   commandObject.errors = errors
						   } catch (Exception e) {
							   throw new ControllerExecutionException("Error occurred creating command object.", e);
						   }
					   }
					   counter++
				   }
				   GCU.getPropertyOrStaticPropertyOrFieldValue(delegate, closureName).call(* commandObjects)
			   }
			   mc."${GrailsClassUtils.getGetterName(actionName)}" = {->
				   def actionDelegate = commandObjectBindingAction.clone()
				   actionDelegate.delegate = delegate
				   actionDelegate
			   }
		   }

		   // look for actions that accept command objects and configure
		   // each of the command object types
		   def commandObjectClasses = controller.commandObjectClasses
		   for(c in commandObjectClasses) {
			   def commandObjectClass = c
			   def commandObject = commandObjectClass.newInstance()
			   def commandObjectMetaClass = commandObjectClass.metaClass
			   commandObjectMetaClass.setErrors = {Errors errors ->
				   RCH.currentRequestAttributes().setAttribute("${commandObjectClass.name}_errors", errors, 0)
			   }
			   commandObjectMetaClass.getErrors = {->
				   RCH.currentRequestAttributes().getAttribute("${commandObjectClass.name}_errors", 0)
			   }

			   commandObjectMetaClass.hasErrors = {->
				   errors?.hasErrors() ? true : false
			   }
			   commandObjectMetaClass.validate = {->
				   DomainClassPluginSupport.validateInstance(delegate, ctx)
			   }
			   def validationClosure = GCU.getStaticPropertyValue(commandObjectClass, 'constraints')
			   if (validationClosure) {
				   def constrainedPropertyBuilder = new ConstrainedPropertyBuilder(commandObject)
				   validationClosure.setDelegate(constrainedPropertyBuilder)
				   validationClosure()
				   commandObjectMetaClass.constraints = constrainedPropertyBuilder.constrainedProperties
			   } else {
				   commandObjectMetaClass.constraints = [:]
			   }
			   commandObjectMetaClass.clearErrors = {->
				   delegate.setErrors (new BeanPropertyBindingResult(delegate, delegate.getClass().getName()))
			   }
		   }
	   }

   }


   def registerControllerMethods(MetaClass mc, ApplicationContext ctx, String prefix) {
	   mc.getActionUri = {-> "/$controllerName/$actionName".toString()}
	   mc.getControllerUri = {-> "/$controllerName".toString()}
	   mc.getTemplateUri = {String name ->
		   def webRequest = RCH.currentRequestAttributes()
		   webRequest.attributes.getTemplateUri(name, webRequest.currentRequest)
	   }
	   mc.getViewUri = {String name ->
		   def webRequest = RCH.currentRequestAttributes()
		   webRequest.attributes.getViewUri(name, webRequest.currentRequest)
	   }
	   mc.setErrors = {Errors errors ->
		   RCH.currentRequestAttributes().setAttribute(GrailsApplicationAttributes.ERRORS, errors, 0)
	   }
	   mc.getErrors = {->
		   RCH.currentRequestAttributes().getAttribute(GrailsApplicationAttributes.ERRORS, 0)
	   }
	   mc.setModelAndView = {ModelAndView mav ->
		   RCH.currentRequestAttributes().setAttribute(GrailsApplicationAttributes.MODEL_AND_VIEW, mav, 0)
	   }
	   mc.getModelAndView = {->
		   RCH.currentRequestAttributes().getAttribute(GrailsApplicationAttributes.MODEL_AND_VIEW, 0)
	   }
	   mc.getChainModel = {->
		   RCH.currentRequestAttributes().flashScope["chainModel"]
	   }
	   mc.hasErrors = {->
		   errors?.hasErrors() ? true : false
	   }

//	   def redirect = new RedirectDynamicMethod(ctx)
	   def render = new RenderDynamicMethod()
	   def bind = new BindDynamicMethod()
	   // the redirect dynamic method
//	   mc.redirect = {Map args ->
//		   redirect.invoke(delegate, "redirect", args)
//	   }
	   mc.chain = {Map args ->
		   ChainMethod.invoke delegate, args
	   }
	   // the render method
	   mc.render = {Object o ->
		   render.invoke(delegate, "render", [o?.inspect()] as Object[])
	   }

	   mc.render = {String txt ->
		   render.invoke(delegate, "render", [txt] as Object[])
	   }
	   mc.render = {Map args ->
		   render.invoke(delegate, "render", [args] as Object[])
	   }
	   mc.render = {Closure c ->
		   render.invoke(delegate, "render", [c] as Object[])
	   }
	   mc.render = {Map args, Closure c ->
		   render.invoke(delegate, "render", [args, c] as Object[])
	   }
	   // the bindData method
	   mc.bindData = {Object target, Object args ->
		   bind.invoke(delegate, "bindData", [target, args] as Object[])
	   }
	   mc.bindData = {Object target, Object args, List disallowed ->
		   bind.invoke(delegate, "bindData", [target, args, [exclude: disallowed]] as Object[])
	   }
	   mc.bindData = {Object target, Object args, List disallowed, String filter ->
		   bind.invoke(delegate, "bindData", [target, args, [exclude: disallowed], filter] as Object[])
	   }
	   mc.bindData = {Object target, Object args, Map includeExclude ->
		   bind.invoke(delegate, "bindData", [target, args, includeExclude] as Object[])
	   }
	   mc.bindData = {Object target, Object args, Map includeExclude, String filter ->
		   bind.invoke(delegate, "bindData", [target, args, includeExclude, filter] as Object[])
	   }
	   mc.bindData = {Object target, Object args, String filter ->
		   bind.invoke(delegate, "bindData", [target, args, filter] as Object[])
	   }

	   // the withForm method
	   def withFormMethod = new WithFormMethod()
	   mc.withForm = { Closure callable ->
		  withFormMethod.withForm(delegate.request, callable)
	   }

	   def forwardMethod = new ForwardMethod(ctx.getBean("${prefix}grailsUrlMappingsHolder"))
	   mc.forward = { Map params ->
		   forwardMethod.forward(delegate.request,delegate.response, params)
	   }

   }

}