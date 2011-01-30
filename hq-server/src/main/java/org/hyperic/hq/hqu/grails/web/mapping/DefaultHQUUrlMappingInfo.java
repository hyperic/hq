package org.hyperic.hq.hqu.grails.web.mapping;

import grails.util.GrailsNameUtils;

import java.util.Collections;
import java.util.Enumeration;
import java.util.Map;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;

import org.codehaus.groovy.grails.web.mapping.AbstractUrlMappingInfo;
import org.codehaus.groovy.grails.web.mapping.UrlMappingData;
import org.codehaus.groovy.grails.web.mapping.UrlMappingInfo;
import org.codehaus.groovy.grails.web.mapping.exceptions.UrlMappingException;
import org.codehaus.groovy.grails.web.servlet.mvc.GrailsWebRequest;
import org.codehaus.groovy.grails.web.util.WebUtils;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.support.WebApplicationContextUtils;
import org.springframework.web.multipart.MultipartHttpServletRequest;
import org.springframework.web.multipart.MultipartResolver;
import org.springframework.web.servlet.DispatcherServlet;

/**
 * A Class that implements the UrlMappingInfo interface and holds information established from a matched
 * URL
 *
 */
public class DefaultHQUUrlMappingInfo extends AbstractUrlMappingInfo implements UrlMappingInfo {
    
    private Object controllerName;
    private Object actionName;
    private Object id;
    private static final String ID_PARAM = "id";
    private UrlMappingData urlData;
    private Object viewName;
    private ServletContext servletContext;
    private static final String SETTING_GRAILS_WEB_DISABLE_MULTIPART = "grails.web.disable.multipart";
    private boolean parsingRequest;
	private Object uri;


    private DefaultHQUUrlMappingInfo(Map params, UrlMappingData urlData, ServletContext servletContext) {
        this.params = Collections.unmodifiableMap(params);
        this.id = params.get(ID_PARAM);
        this.urlData = urlData;
        this.servletContext = servletContext;
    }

    public DefaultHQUUrlMappingInfo(Object controllerName, Object actionName, Object viewName, Map params, UrlMappingData urlData, ServletContext servletContext) {
        this(params, urlData, servletContext);
        if (controllerName == null && viewName == null)
            throw new IllegalArgumentException("URL mapping must either provide a controller or view name to map to!");
        if (params == null) throw new IllegalArgumentException("Argument [params] cannot be null");
        this.controllerName = controllerName;
        this.actionName = actionName;
        if (actionName == null)
            this.viewName = viewName;
    }

    public DefaultHQUUrlMappingInfo(Object viewName, Map params, UrlMappingData urlData, ServletContext servletContext) {
        this(params, urlData, servletContext);
        this.viewName = viewName;
        if (viewName == null) throw new IllegalArgumentException("Argument [viewName] cannot be null or blank");

    }
    
    public DefaultHQUUrlMappingInfo(Object uri, UrlMappingData data, ServletContext servletContext) {
    	this(Collections.EMPTY_MAP, data,servletContext);
    	this.uri = uri;
    	
    	if (uri == null) throw new IllegalArgumentException("Argument [uri] cannot be null or blank");
    }

    public String toString() {
        return urlData.getUrlPattern();
    }

    public Map getParameters() {
        return params;
    }

    public boolean isParsingRequest() {
        return this.parsingRequest;
    }

    public void setParsingRequest(boolean parsingRequest) {
        this.parsingRequest = parsingRequest;
    }

    public String getControllerName() {
        String controllerName = evaluateNameForValue(this.controllerName);
        if (controllerName == null && getViewName() == null)
            throw new UrlMappingException("Unable to establish controller name to dispatch for [" + this.controllerName + "]. Dynamic closure invocation returned null. Check your mapping file is correct, when assigning the controller name as a request parameter it cannot be an optional token!");
        return controllerName;
    }

    public String getActionName() {
        GrailsWebRequest webRequest = (GrailsWebRequest) RequestContextHolder.getRequestAttributes();

        String name = webRequest != null ? checkDispatchAction(webRequest.getCurrentRequest(), null) : null;
        if (name == null) {
            name = evaluateNameForValue(this.actionName, webRequest);
        }
        return name;
    }

    public String getViewName() {
        return evaluateNameForValue(this.viewName);
    }

    public String getId() {
        return evaluateNameForValue(this.id);
    }

    private String checkDispatchAction(HttpServletRequest request, String actionName) {
        Enumeration paramNames = tryMultipartParams(request, request.getParameterNames());

        for (; paramNames.hasMoreElements();) {
            String name = (String) paramNames.nextElement();
            if (name.startsWith(WebUtils.DISPATCH_ACTION_PARAMETER)) {
                // remove .x suffix in case of submit image
                if (name.endsWith(".x") || name.endsWith(".y")) {
                    name = name.substring(0, name.length() - 2);
                }
                actionName = GrailsNameUtils.getPropertyNameRepresentation(name.substring((WebUtils.DISPATCH_ACTION_PARAMETER).length()));
                break;
            }
        }
        return actionName;
    }

    private Enumeration tryMultipartParams(HttpServletRequest request, Enumeration originalParams) {
        Enumeration paramNames = originalParams;
        boolean disabled = getMultipartDisabled();
        if (!disabled) {
            MultipartResolver resolver = getResolver();
            if (resolver.isMultipart(request)) {
                MultipartHttpServletRequest resolvedMultipartRequest = getResolvedRequest(request, resolver);
                paramNames = resolvedMultipartRequest.getParameterNames();
            }
        }
        return paramNames;
    }

    private MultipartHttpServletRequest getResolvedRequest(HttpServletRequest request, MultipartResolver resolver) {
        MultipartHttpServletRequest resolvedMultipartRequest = (MultipartHttpServletRequest) request.getAttribute(MultipartHttpServletRequest.class.getName());
        if (resolvedMultipartRequest == null) {
            resolvedMultipartRequest = resolver.resolveMultipart(request);
            request.setAttribute(MultipartHttpServletRequest.class.getName(), resolvedMultipartRequest);
        }
        return resolvedMultipartRequest;
    }

    private boolean getMultipartDisabled() {
    	// TODO: add real app lookup based on appname from uri path to support multipart
//        GrailsApplication app = WebUtils.lookupApplication(servletContext);
//        ConfigObject config = app.getConfig();
        boolean disabled = false;
//        Object disableMultipart = config.get(SETTING_GRAILS_WEB_DISABLE_MULTIPART);
//        if (disableMultipart instanceof Boolean) {
//            disabled = ((Boolean) disableMultipart).booleanValue();
//        } else if (disableMultipart instanceof String) {
//            disabled = Boolean.valueOf((String) disableMultipart).booleanValue();
 //       }
        return disabled;
    }

    private MultipartResolver getResolver() {
        WebApplicationContext ctx = WebApplicationContextUtils.getRequiredWebApplicationContext(servletContext);
        MultipartResolver resolver = (MultipartResolver)
                ctx.getBean(DispatcherServlet.MULTIPART_RESOLVER_BEAN_NAME);
        return resolver;
    }

	public String getURI() {
		return evaluateNameForValue(this.uri);
	}


}
