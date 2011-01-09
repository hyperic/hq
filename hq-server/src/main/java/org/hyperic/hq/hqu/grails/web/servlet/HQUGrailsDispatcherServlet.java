package org.hyperic.hq.hqu.grails.web.servlet;

import grails.util.GrailsUtil;
import grails.util.Metadata;
import groovy.grape.Grape;
import groovy.lang.ExpandoMetaClass;

import java.util.List;
import java.util.Locale;
import java.util.Map;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.codehaus.groovy.grails.commons.BootstrapArtefactHandler;
import org.codehaus.groovy.grails.commons.GrailsBootstrapClass;
import org.codehaus.groovy.grails.commons.GrailsClass;
import org.codehaus.groovy.grails.commons.spring.GrailsApplicationContext;
import org.codehaus.groovy.grails.web.servlet.WrappedResponseHolder;
import org.codehaus.groovy.grails.web.servlet.mvc.GrailsWebRequest;
import org.codehaus.groovy.grails.web.util.WebUtils;
import org.hyperic.hq.hqu.grails.commons.HQUGrailsApplication;
import org.hyperic.hq.hqu.grails.web.context.HQUGrailsConfigUtils;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.beans.factory.access.BootstrapException;
import org.springframework.context.i18n.LocaleContext;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.util.Assert;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.WebRequestInterceptor;
import org.springframework.web.context.support.WebApplicationContextUtils;
import org.springframework.web.multipart.MultipartException;
import org.springframework.web.multipart.MultipartHttpServletRequest;
import org.springframework.web.multipart.MultipartResolver;
import org.springframework.web.servlet.DispatcherServlet;
import org.springframework.web.servlet.HandlerAdapter;
import org.springframework.web.servlet.HandlerExecutionChain;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.LocaleResolver;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.ModelAndViewDefiningException;
import org.springframework.web.servlet.handler.WebRequestHandlerInterceptorAdapter;
import org.springframework.web.servlet.mvc.annotation.AnnotationMethodHandlerAdapter;
import org.springframework.web.util.NestedServletException;

/**
 * Servlet that handles incoming requests for Grails based HQU framework.
 * 
 * <p>This class is based on derivative work of GrailsDispatcherServlet from
 * Grails framework. It mostly contains changes to support handling multiple
 * Grails based application within one servlet.
 *
 */
public class HQUGrailsDispatcherServlet extends DispatcherServlet {
	
	private List<HQUGrailsApplication> applications;
    protected HandlerInterceptor[] interceptors;
    protected MultipartResolver multipartResolver;
    private static final String EXCEPTION_ATTRIBUTE = "exception";

    public HQUGrailsDispatcherServlet() {
        super();
        setDetectAllHandlerMappings(true);
    }

    protected void initFrameworkServlet() throws ServletException, BeansException {
        super.initFrameworkServlet();
        initMultipartResolver();
        
    }

	/**
	 * Initialize the MultipartResolver used by this class.
	 * If no bean is defined with the given name in the BeanFactory
	 * for this namespace, no multipart handling is provided.
     *
     * @throws org.springframework.beans.BeansException Thrown if there is an error initializing the mutlipartResolver
     */
	private void initMultipartResolver() throws BeansException {
		try {
			this.multipartResolver = getWebApplicationContext().getBean(MULTIPART_RESOLVER_BEAN_NAME, MultipartResolver.class);
			if (logger.isInfoEnabled()) {
				logger.info("Using MultipartResolver [" + this.multipartResolver + "]");
			}
		}
		catch (NoSuchBeanDefinitionException ex) {
			// Default is no multipart resolver.
			this.multipartResolver = null;
			if (logger.isInfoEnabled()) {
				logger.info("Unable to locate MultipartResolver with name '"	+ MULTIPART_RESOLVER_BEAN_NAME +
						"': no multipart request handling provided");
			}
		}
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.web.servlet.FrameworkServlet#createWebApplicationContext(org.springframework.web.context.WebApplicationContext)
	 */
    protected WebApplicationContext createWebApplicationContext(WebApplicationContext parent) throws BeansException {
    	WebApplicationContext wac = WebApplicationContextUtils.getWebApplicationContext(getServletContext());
    	WebApplicationContext webContext;
        // construct the SpringConfig for the container managed application
        Assert.notNull(parent, "Grails requires a parent ApplicationContext, is the /WEB-INF/applicationContext.xml file missing?");
        this.applications = parent.getBean(HQUGrailsApplication.HQU_APPLICATIONS_ID, List.class);

        // below section enables some functionality related to groovy's metaclasses.
        // Well end up having exceptions from other classes if it's not enabled.
        // This is originally called from Grails GrailsContextLoader
        ExpandoMetaClass.enableGlobally();
        Metadata metadata = Metadata.getCurrent();
        if(metadata!=null&&metadata.isWarDeployed()) {
            Grape.setEnableAutoDownload(false);
            Grape.setEnableGrapes(false);
        }
        
        if(wac instanceof GrailsApplicationContext) {
    		webContext = wac;
    	}
    	else {
            webContext = HQUGrailsConfigUtils.configureWebApplicationContext(getServletContext(), parent);
            try {
            	
            	for (HQUGrailsApplication application : applications) {
                    HQUGrailsConfigUtils.executeGrailsBootstraps(application, webContext, getServletContext());
				}
            	
            } catch (Exception e) {
                GrailsUtil.deepSanitize(e);
                if(e instanceof BeansException) throw (BeansException)e;
                else {
                    throw new BootstrapException("Error executing bootstraps", e);
                }
            }
        }

        this.interceptors = establishInterceptors(webContext);

        return webContext;
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

    public void destroy() {
        WebApplicationContext webContext = getWebApplicationContext();
        
    	for (HQUGrailsApplication application : applications) {
            GrailsClass[] bootstraps =  application.getArtefacts(BootstrapArtefactHandler.TYPE);
            for (GrailsClass bootstrap : bootstraps) {
                ((GrailsBootstrapClass) bootstrap).callDestroy();
            }
		}
        
        super.destroy();
    }



    public void setApplications(List applications) {
        this.applications = applications;
    }

    /* (non-Javadoc)
	 * @see org.springframework.web.servlet.DispatcherServlet#doDispatch(javax.servlet.http.HttpServletRequest, javax.servlet.http.HttpServletResponse)
	 */
	protected void doDispatch(final HttpServletRequest request, HttpServletResponse response) throws Exception {
        HttpServletRequest processedRequest = request;
        HandlerExecutionChain mappedHandler = null;
        int interceptorIndex = -1;
        final LocaleResolver localeResolver = (LocaleResolver)request.getAttribute(LOCALE_RESOLVER_ATTRIBUTE);


        // Expose current LocaleResolver and request as LocaleContext.
        LocaleContext previousLocaleContext = LocaleContextHolder.getLocaleContext();
        LocaleContextHolder.setLocaleContext(new LocaleContext() {
            public Locale getLocale() {

                return localeResolver.resolveLocale(request);
            }
        });


        // If the request is an include we need to try to use the original wrapped sitemesh
        // response, otherwise layouts won't work properly
        if(WebUtils.isIncludeRequest(request)) {
            response = useWrappedOrOriginalResponse(response);
        }

        GrailsWebRequest requestAttributes = null;
        GrailsWebRequest previousRequestAttributes = null;
        Exception handlerException = null;
        try {
            ModelAndView mv;
            try {
                Object exceptionAttribute = request.getAttribute(EXCEPTION_ATTRIBUTE);
                // only process multipart requests if an exception hasn't occured
                if(exceptionAttribute == null)
                    processedRequest = checkMultipart(request);
                // Expose current RequestAttributes to current thread.
                previousRequestAttributes = (GrailsWebRequest) RequestContextHolder.currentRequestAttributes();
                requestAttributes = new GrailsWebRequest(processedRequest, response, getServletContext());
                copyParamsFromPreviousRequest(previousRequestAttributes, requestAttributes);

                // Update the current web request.
                WebUtils.storeGrailsWebRequest(requestAttributes);

                if (logger.isDebugEnabled()) {
                    logger.debug("Bound request context to thread: " + request);
                    logger.debug("Using response object: " + response.getClass());
                }



                // Determine handler for the current request.
                mappedHandler = getHandler(processedRequest, false);
                if (mappedHandler == null || mappedHandler.getHandler() == null) {
                    noHandlerFound(processedRequest, response);
                    return;
                }

                // Apply preHandle methods of registered interceptors.
                if (mappedHandler.getInterceptors() != null) {
                    for (int i = 0; i < mappedHandler.getInterceptors().length; i++) {
                        HandlerInterceptor interceptor = mappedHandler.getInterceptors()[i];
                        if (!interceptor.preHandle(processedRequest, response, mappedHandler.getHandler())) {
                            triggerAfterCompletion(mappedHandler, interceptorIndex, processedRequest, response, null);
                            return;
                        }
                        interceptorIndex = i;
                    }
                }

                // Actually invoke the handler.
                HandlerAdapter ha = getHandlerAdapter(mappedHandler.getHandler());
                mv = ha.handle(processedRequest, response, mappedHandler.getHandler());

				// Do we need view name translation?
				if ((ha instanceof AnnotationMethodHandlerAdapter) && mv != null && !mv.hasView()) {
					mv.setViewName(getDefaultViewName(request));
				}

                // Apply postHandle methods of registered interceptors.
                if (mappedHandler.getInterceptors() != null) {
                    for (int i = mappedHandler.getInterceptors().length - 1; i >= 0; i--) {
                        HandlerInterceptor interceptor = mappedHandler.getInterceptors()[i];
                        interceptor.postHandle(processedRequest, response, mappedHandler.getHandler(), mv);
                    }
                }
            }
            catch (ModelAndViewDefiningException ex) {
                GrailsUtil.deepSanitize(ex);
                handlerException = ex;
                if (logger.isDebugEnabled())
                    logger.debug("ModelAndViewDefiningException encountered", ex);
                mv = ex.getModelAndView();
            }
            catch (Exception ex) {
                GrailsUtil.deepSanitize(ex);
                handlerException = ex;
                Object handler = (mappedHandler != null ? mappedHandler.getHandler() : null);
                mv = processHandlerException(request, response, handler, ex);
            }

            // Did the handler return a view to render?
            if (mv != null && !mv.wasCleared()) {
                // If an exception occurs in here, like a bad closing tag,
                // we have nothing to render.

                try {
                    render(mv, processedRequest, response);
                } catch (Exception e) {
                    mv = super.processHandlerException(processedRequest, response, mappedHandler, e);
                    handlerException = e;
                    render(mv, processedRequest, response);
                }
            }
            else {
                if (logger.isDebugEnabled()) {
                    logger.debug("Null ModelAndView returned to DispatcherServlet with name '" +
                            getServletName() + "': assuming HandlerAdapter completed request handling");
                }
            }

            // Trigger after-completion for successful outcome.
            triggerAfterCompletion(mappedHandler, interceptorIndex, processedRequest, response, handlerException);
        }

        catch (Exception ex) {
            // Trigger after-completion for thrown exception.
            triggerAfterCompletion(mappedHandler, interceptorIndex, processedRequest, response, ex);
            throw ex;
        }
        catch (Error err) {
            ServletException ex = new NestedServletException("Handler processing failed", err);
            // Trigger after-completion for thrown exception.
            triggerAfterCompletion(mappedHandler, interceptorIndex, processedRequest, response, ex);
            throw ex;
        }

        finally {
            // Clean up any resources used by a multipart request.
            if (processedRequest instanceof MultipartHttpServletRequest && processedRequest != request) {
                if(multipartResolver != null)
                    this.multipartResolver.cleanupMultipart((MultipartHttpServletRequest) processedRequest);
            }
            request.removeAttribute(MultipartHttpServletRequest.class.getName());

            // Reset thread-bound holders
            if(requestAttributes != null) {
                requestAttributes.requestCompleted();
                WebUtils.storeGrailsWebRequest(previousRequestAttributes);
            }
            
            LocaleContextHolder.setLocaleContext(previousLocaleContext);

            if (logger.isDebugEnabled()) {
                logger.debug("Cleared thread-bound request context: " + request);
            }
        }
    }

    protected HttpServletResponse useWrappedOrOriginalResponse(HttpServletResponse response) {
        HttpServletResponse r = WrappedResponseHolder.getWrappedResponse();
        if(r != null) return r;
        return response;
    }

    protected void copyParamsFromPreviousRequest(GrailsWebRequest previousRequestAttributes, GrailsWebRequest requestAttributes) {
        Map previousParams = previousRequestAttributes.getParams();
        Map params =  requestAttributes.getParams();
        for (Object o : previousParams.keySet()) {
            String name = (String) o;
            params.put(name, previousParams.get(name));
        }
    }

    /**
	 * Trigger afterCompletion callbacks on the mapped HandlerInterceptors.
	 * Will just invoke afterCompletion for all interceptors whose preHandle
	 * invocation has successfully completed and returned true.
	 * @param mappedHandler the mapped HandlerExecutionChain
	 * @param interceptorIndex index of last interceptor that successfully completed
	 * @param ex Exception thrown on handler execution, or <code>null</code> if none
	 * @see HandlerInterceptor#afterCompletion
	 */
	protected void triggerAfterCompletion(
			HandlerExecutionChain mappedHandler, int interceptorIndex,
			HttpServletRequest request, HttpServletResponse response, Exception ex)
			throws Exception {

		// Apply afterCompletion methods of registered interceptors.
		if (mappedHandler != null) {
			if (mappedHandler.getInterceptors() != null) {
				for (int i = interceptorIndex; i >= 0; i--) {
					HandlerInterceptor interceptor = mappedHandler.getInterceptors()[i];
					try {
						interceptor.afterCompletion(request, response, mappedHandler.getHandler(), ex);
					}
					catch (Throwable ex2) {
                        GrailsUtil.deepSanitize(ex2);
						logger.error("HandlerInterceptor.afterCompletion threw exception", ex2);
					}
				}
			}
		}
	}

	/**
	 * Convert the request into a multipart request.
	 * If no multipart resolver is set, simply use the existing request.
	 * @param request current HTTP request
	 * @return the processed request (multipart wrapper if necessary)
	 */
	protected HttpServletRequest checkMultipart(HttpServletRequest request) throws MultipartException {
        // Lookup from request attribute. The resolver that handles MultiPartRequest is dealt with earlier inside DefaultUrlMappingInfo with Grails
        HttpServletRequest resolvedRequest = (HttpServletRequest) request.getAttribute(MultipartHttpServletRequest.class.getName());
        if(resolvedRequest!=null) return resolvedRequest;
        return request;
	}

    @Override
    public HandlerExecutionChain getHandler(HttpServletRequest request, boolean cache) throws Exception {
        return super.getHandler(request, cache);    
    }

}
