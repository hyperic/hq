package org.hyperic.hq.hqu.grails.web.servlet;

import grails.util.Environment;
import org.codehaus.groovy.grails.commons.ControllerArtefactHandler;
import org.codehaus.groovy.grails.commons.GrailsApplication;
import org.codehaus.groovy.grails.commons.GrailsControllerClass;
import org.codehaus.groovy.grails.plugins.support.aware.GrailsApplicationAware;
import org.codehaus.groovy.grails.web.servlet.GrailsUrlPathHelper;
import org.hyperic.hq.hqu.grails.commons.HQUGrailsApplication;
import org.hyperic.hq.hqu.grails.plugins.support.aware.HQUGrailsApplicationAware;
import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.context.request.WebRequestInterceptor;
import org.springframework.web.servlet.HandlerExecutionChain;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.mvc.Controller;
import org.springframework.web.servlet.handler.AbstractHandlerMapping;
import org.springframework.web.servlet.handler.WebRequestHandlerInterceptorAdapter;
import org.springframework.web.util.UrlPathHelper;

import javax.servlet.http.HttpServletRequest;
import java.util.List;

/**
 * A handler mapping that matches Grails' SimpleController class
 *
 */
public class HQUGrailsControllerHandlerMapping extends AbstractHandlerMapping implements HQUGrailsApplicationAware {

    public static final String MAIN_CONTROLLER_BEAN = "mainSimpleController";
    private GrailsApplication grailsApplication;
    private UrlPathHelper urlHelper = new GrailsUrlPathHelper();

    protected Object getHandlerInternal(HttpServletRequest request) throws Exception {

        String uri = urlHelper.getPathWithinApplication(request);
        if(logger.isDebugEnabled()) {
            logger.debug("Looking up Grails controller for URI ["+uri+"]");
        }
        GrailsControllerClass controllerClass = (GrailsControllerClass) grailsApplication.getArtefactForFeature(
            ControllerArtefactHandler.TYPE, uri);

        return getHandlerForControllerClass(controllerClass, request);
    }

    /**
     * Obtains the handler for the given controller class
     *
     * @param controllerClass The controller class
     * @param request The HttpServletRequest
     * @return The handler
     */
    protected Object getHandlerForControllerClass(GrailsControllerClass controllerClass, HttpServletRequest request) {
        if(controllerClass!=null) {
            try {
            	String prefix = ((HQUGrailsApplication)grailsApplication).getHQUApplicationId();
                return getWebApplicationContext().getBean(prefix+MAIN_CONTROLLER_BEAN, Controller.class);
            }
            catch (NoSuchBeanDefinitionException e) {
                 // ignore
            }
        }
        return null;
    }


    @Override
    protected final HandlerExecutionChain getHandlerExecutionChain(Object handler, HttpServletRequest request) {
        if (handler instanceof HandlerExecutionChain) {
            HandlerExecutionChain chain = (HandlerExecutionChain) handler;
            chain.addInterceptors(lookupInterceptors(getWebApplicationContext()));
            return chain;
        }
        else {
            return new HandlerExecutionChain(handler, lookupInterceptors(getWebApplicationContext()));
        }

    }

    @Override
    protected void extendInterceptors(List interceptors) {
        setInterceptors(establishInterceptors(getWebApplicationContext()));
    }

    protected HandlerInterceptor[] lookupInterceptors(WebApplicationContext applicationContext) {
        HandlerInterceptor[] interceptors;
        if(Environment.getCurrent()==Environment.DEVELOPMENT) {
            interceptors = establishInterceptors(applicationContext);
        }
        else {
            interceptors = this.getAdaptedInterceptors();

        }
        return interceptors;
    }

    /**
     * Evalutes the given WebApplicationContext for all HandlerInterceptor and WebRequestInterceptor instances
     *
     * @param webContext The WebApplicationContext
     * @return An array of HandlerInterceptor instances
     */
    protected HandlerInterceptor[] establishInterceptors(WebApplicationContext webContext) {
        HandlerInterceptor[] interceptors;
        String[] interceptorNames = webContext.getBeanNamesForType(HandlerInterceptor.class);
        String[] webRequestInterceptors = webContext.getBeanNamesForType( WebRequestInterceptor.class);
        interceptors = new HandlerInterceptor[interceptorNames.length+webRequestInterceptors.length];

        // Merge the handler and web request interceptors into a single
        // array. Note that we start with the web request interceptors
        // to ensure that the OpenSessionInViewInterceptor (which is a
        // web request interceptor) is invoked before the user-defined
        // filters (which are attached to a handler interceptor). This
        // should ensure that the Hibernate session is in the proper
        // state if and when users access the database within their
        // filters.
        int j = 0;
        for (String webRequestInterceptor : webRequestInterceptors) {
            interceptors[j++] = new WebRequestHandlerInterceptorAdapter((WebRequestInterceptor) webContext.getBean(webRequestInterceptor));
        }
        for (String interceptorName : interceptorNames) {
            interceptors[j++] = (HandlerInterceptor) webContext.getBean(interceptorName);
        }
        return interceptors;
    }

    public void setGrailsApplication(GrailsApplication grailsApplication) {
        this.grailsApplication = grailsApplication;
    }
    
    public GrailsApplication getGrailsApplication() {
    	return grailsApplication;
    }
}
