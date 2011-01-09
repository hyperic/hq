package org.hyperic.hq.hqu.grails.web.servlet.view;

import grails.util.GrailsUtil;
import groovy.lang.GroovyObject;

import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import javax.servlet.http.HttpServletRequest;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.codehaus.groovy.grails.commons.GrailsApplication;
import org.codehaus.groovy.grails.commons.GrailsResourceUtils;
import org.codehaus.groovy.grails.plugins.GrailsPluginManager;
import org.codehaus.groovy.grails.plugins.PluginManagerAware;
import org.codehaus.groovy.grails.web.servlet.GrailsApplicationAttributes;
import org.codehaus.groovy.grails.web.servlet.mvc.GrailsWebRequest;
import org.codehaus.groovy.grails.web.util.WebUtils;
import org.hyperic.hq.hqu.grails.commons.HQUGrailsApplication;
import org.hyperic.hq.hqu.grails.web.pages.HQUGroovyPagesTemplateEngine;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.ResourceLoaderAware;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.web.servlet.View;
import org.springframework.web.servlet.view.InternalResourceViewResolver;

/**
 * A Grails view resolver which evaluates the existance of a view for different extensions choosing which
 * one to delegate to.
 *
 */
public class HQUGrailsViewResolver extends InternalResourceViewResolver implements ResourceLoaderAware, ApplicationContextAware, PluginManagerAware {
    private String localPrefix;
    private static final Log LOG = LogFactory.getLog(HQUGrailsViewResolver.class);

    public static final String GSP_SUFFIX = ".gsp";
    public static final String JSP_SUFFIX = ".jsp";
    
    private ResourceLoader resourceLoader;
    protected HQUGroovyPagesTemplateEngine templateEngine;

    private static final String GROOVY_PAGE_RESOURCE_LOADER = "groovyPageResourceLoader";
    // no need for static cache since GrailsViewResolver is in app context
    private Map<String, View> VIEW_CACHE = new ConcurrentHashMap<String, View>();
    private static final char DOT = '.';
    private static final char SLASH = '/';
    private GrailsPluginManager pluginManager;
    private GrailsApplication grailsApplication;

    public HQUGrailsViewResolver() {
        setCache(!GrailsUtil.isDevelopmentEnv());
    }

    public void setPrefix(String prefix) {
        super.setPrefix(prefix);
        this.localPrefix = prefix;
    }

    public void setSuffix(String suffix) {
        super.setSuffix(suffix);
    }

    public void setResourceLoader(ResourceLoader resourceLoader) {
         this.resourceLoader = resourceLoader;
    }


    public void setTemplateEngine(HQUGroovyPagesTemplateEngine templateEngine) {
        this.templateEngine = templateEngine;
    }

    protected View loadView(String viewName, Locale locale) throws Exception {
        if(this.templateEngine == null) throw new IllegalStateException("Property [templateEngine] cannot be null");

        if(VIEW_CACHE.containsKey(viewName) && !templateEngine.isReloadEnabled()) {
             return VIEW_CACHE.get(viewName);           
        }
        else {
            // try GSP if res is null

            GrailsWebRequest webRequest = WebUtils.retrieveGrailsWebRequest();

            HttpServletRequest request = webRequest.getCurrentRequest();
            GroovyObject controller = webRequest
                                            .getAttributes()
                                            .getController(request);

//            GrailsApplication application = (GrailsApplication) getApplicationContext().getBean(GrailsApplication.APPLICATION_ID);

            ResourceLoader resourceLoader = establishResourceLoader(grailsApplication);

            String format = request.getAttribute(GrailsApplicationAttributes.CONTENT_FORMAT) != null ? request.getAttribute(GrailsApplicationAttributes.CONTENT_FORMAT).toString() : null;
//            String gspView = localPrefix + viewName + DOT + format + GSP_SUFFIX;
            String gspView = getLocalPrefixPath() + viewName + DOT + format + GSP_SUFFIX;
            Resource res = null;
///WEB-INF/hqu-plugins/test/views/foo/display.null.gsp
            if(format != null) {
                res = resourceLoader.getResource(gspView);
                if(!res.exists()) {
                    gspView = resolveViewForController(controller, grailsApplication, viewName, resourceLoader);
                    res = resourceLoader.getResource(gspView);
                }
            }

            if(res == null || !res.exists()) {
//                gspView = localPrefix + viewName + GSP_SUFFIX;
            	gspView = getLocalPrefixPath() + viewName + GSP_SUFFIX;
                res = resourceLoader.getResource(gspView);
//                URL [file:hqu-plugins/test/views/foo/display.gsp]
                if(!res.exists()) {
                    gspView = resolveViewForController(controller, grailsApplication, viewName, resourceLoader);
                    res = resourceLoader.getResource(gspView);
                }
            }

            if(res.exists()) {
                final View view = createGroovyPageView(webRequest, gspView);
                VIEW_CACHE.put(viewName, view);
                return view;
            }
            else {
            	return null;
            	// TODO: check if we can return null if view is not found. is some other component expecting something else.
//                AbstractUrlBasedView view = buildView(viewName);
//                view.setApplicationContext(getApplicationContext());
//                view.afterPropertiesSet();
//                VIEW_CACHE.put(viewName, view);
//                return view;
            }

        }

    }

    private View createGroovyPageView(GrailsWebRequest webRequest, String gspView) {
        if(LOG.isDebugEnabled()) {
            LOG.debug("Resolved GSP view at URI ["+gspView+"]");
        }
        HQUGroovyPageView gspSpringView = new HQUGroovyPageView();
        gspSpringView.setServletContext(webRequest.getServletContext());
        gspSpringView.setUrl(gspView);
        gspSpringView.setApplicationContext(this.getApplicationContext());
        gspSpringView.setTemplateEngine(templateEngine);
        return gspSpringView;
    }

    /**
     * Attempst to resolve a view relative to a controller
     *
     * @param controller The controller to resolve the view relative to
     * @param application The GrailsApplication instance
     * @param viewName The views name
     * @param resourceLoader The ResourceLoader to use
     * @return The URI of the view
     */
    protected String resolveViewForController(GroovyObject controller, GrailsApplication application, String viewName, ResourceLoader resourceLoader) {
        String gspView;// try to resolve the view relative to the controller first, this allows us to support
        // views provided by plugins
        if(controller != null && application != null) {
            String pathToView = pluginManager != null ? pluginManager.getPluginViewsPathForInstance(controller) : null;
            if(pathToView!= null) {
                gspView = GrailsResourceUtils.WEB_INF +pathToView +viewName+GSP_SUFFIX;

            }
            else {
                gspView = localPrefix + viewName + GSP_SUFFIX;
            }
        }
        else {
            gspView = localPrefix + viewName + GSP_SUFFIX;
        }
        if(LOG.isDebugEnabled()) {
            LOG.debug("Attempting to resolve view for URI ["+gspView+"] using ResourceLoader ["+resourceLoader.getClass()+"]");
        }
        return gspView;
    }
    
    private String getLocalPrefixPath() {
    	return localPrefix + ((HQUGrailsApplication)grailsApplication).getHQUApplicationId() + "/views";
    }

    private ResourceLoader establishResourceLoader(GrailsApplication application) {
        ApplicationContext ctx = getApplicationContext();

        String hquAppId = ((HQUGrailsApplication)grailsApplication).getHQUApplicationId();
        
        if(ctx.containsBean(hquAppId+GROOVY_PAGE_RESOURCE_LOADER) && application != null && !application.isWarDeployed()) {
            return (ResourceLoader)ctx.getBean(hquAppId+GROOVY_PAGE_RESOURCE_LOADER);
        }
        return this.resourceLoader;
    }

    public void setPluginManager(GrailsPluginManager pluginManager) {
        this.pluginManager = pluginManager;
    }

	public void setGrailsApplication(GrailsApplication grailsApplication) {
		this.grailsApplication = grailsApplication;
	}

    
    
}
