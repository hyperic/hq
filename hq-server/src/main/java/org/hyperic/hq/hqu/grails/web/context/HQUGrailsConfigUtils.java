package org.hyperic.hq.hqu.grails.web.context;

import java.util.List;

import javax.servlet.ServletContext;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.codehaus.groovy.grails.commons.ApplicationAttributes;
import org.codehaus.groovy.grails.commons.BootstrapArtefactHandler;
import org.codehaus.groovy.grails.commons.GrailsApplication;
import org.codehaus.groovy.grails.commons.GrailsBootstrapClass;
import org.codehaus.groovy.grails.commons.GrailsClass;
import org.codehaus.groovy.grails.plugins.GrailsPluginManager;
import org.codehaus.groovy.grails.support.PersistenceContextInterceptor;
import org.codehaus.groovy.grails.web.context.GrailsConfigUtils;
import org.codehaus.groovy.grails.web.context.ServletContextHolder;
import org.hyperic.hq.hqu.grails.commons.HQUGrailsApplication;
import org.hyperic.hq.hqu.grails.commons.spring.HQUGrailsRuntimeConfigurator;
import org.springframework.beans.factory.config.AutowireCapableBeanFactory;
import org.springframework.web.context.WebApplicationContext;

/**
 * Utility methods to configure Application context.
 *
 */
public class HQUGrailsConfigUtils {
	
	private static final Log log = LogFactory.getLog(GrailsConfigUtils.class);

    /**
	 * Executes Grails bootstrap classes for single application.
	 *
	 * @param application The Grails ApplicationContext instance
	 * @param webContext The WebApplicationContext instance
	 * @param servletContext The ServletContext instance
	 */
	public static void executeGrailsBootstraps(GrailsApplication application, WebApplicationContext webContext,
        ServletContext servletContext) {

		PersistenceContextInterceptor interceptor = null;
		String[] beanNames = webContext.getBeanNamesForType(PersistenceContextInterceptor.class);
		if(beanNames.length > 0) {
			interceptor = (PersistenceContextInterceptor)webContext.getBean(beanNames[0]);
		}

	    if(interceptor != null)
	    	interceptor.init();
        // init the Grails application
        try {
            GrailsClass[] bootstraps =  application.getArtefacts(BootstrapArtefactHandler.TYPE);
            for (GrailsClass bootstrap : bootstraps) {
                final GrailsBootstrapClass bootstrapClass = (GrailsBootstrapClass) bootstrap;
                final Object instance = bootstrapClass.getReferenceInstance();
                webContext.getAutowireCapableBeanFactory()
                        .autowireBeanProperties(instance, AutowireCapableBeanFactory.AUTOWIRE_BY_NAME, false);
                bootstrapClass.callInit(servletContext);
            }
            if(interceptor != null)
                interceptor.flush();
        }
        finally {
            if(interceptor != null)
                interceptor.destroy();
        }

	}

	/**
	 * Configuring the web application context.
	 * <p>This method is creating two level application context on top of parent
	 * given by caller. On first level beans for HQU Grails Applications are created
	 * dynamically. Rest of the runtime configuration happens on second level.
	 * 
	 * @param servletContext The servlet context
	 * @param parent The parent web application context
	 * @return New and configured web application context
	 */
	public static WebApplicationContext configureWebApplicationContext(ServletContext servletContext, WebApplicationContext parent) {
		List<HQUGrailsApplication> applications = parent.getBean(HQUGrailsApplication.HQU_APPLICATIONS_ID, List.class);

		if(log.isDebugEnabled()) {
			log.debug("About to configure " + applications.size() + " HQU Grails applications");
		}

		HQUGrailsRuntimeConfigurator parentConfigurator = new HQUGrailsRuntimeConfigurator(applications,parent);		
		WebApplicationContext parentWebContext = parentConfigurator.configureParent(servletContext);		
		WebApplicationContext webContext = null;
		
				
		HQUGrailsRuntimeConfigurator configurator = new HQUGrailsRuntimeConfigurator(applications,parentWebContext);
		try {
			webContext = configurator.configure( servletContext );
		} catch (Exception e) {
			// TODO: handle exceptions
			log.warn("Expected error from GrailsRuntimeConfigurator", e);
		}

        configureServletContextAttributes(servletContext, applications, null, webContext);
        log.info("HQU Grails applications loaded.");
		return webContext;
	}

    public static void configureServletContextAttributes(ServletContext servletContext, List applications, GrailsPluginManager pluginManager, WebApplicationContext webContext) {
        ServletContextHolder.setServletContext(servletContext);

        servletContext.setAttribute(ApplicationAttributes.PLUGIN_MANAGER, pluginManager);
        servletContext.setAttribute(ApplicationAttributes.PARENT_APPLICATION_CONTEXT,webContext.getParent());
        
        // TODO: make sure the ones who expect to find GrailsApplication.APPLICATION_ID attribute doesn't fail
//        servletContext.setAttribute(GrailsApplication.APPLICATION_ID,application);
        
        servletContext.setAttribute(HQUGrailsApplication.HQU_APPLICATIONS_ID,applications);

        servletContext.setAttribute(ApplicationAttributes.APPLICATION_CONTEXT,webContext );
        servletContext.setAttribute(WebApplicationContext.ROOT_WEB_APPLICATION_CONTEXT_ATTRIBUTE, webContext);
    }

}
