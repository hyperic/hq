package org.hyperic.hq.hqu.grails.commons.spring;

import grails.spring.BeanBuilder;
import grails.util.GrailsUtil;
import groovy.lang.Closure;
import groovy.lang.Script;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.ServletContext;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.codehaus.groovy.grails.commons.spring.GrailsContextEvent;
import org.codehaus.groovy.grails.commons.spring.GrailsRuntimeConfigurator;
import org.codehaus.groovy.grails.commons.spring.RuntimeSpringConfiguration;
import org.codehaus.groovy.grails.commons.spring.WebRuntimeSpringConfiguration;
import org.codehaus.groovy.grails.exceptions.GrailsConfigurationException;
import org.codehaus.groovy.grails.plugins.GrailsPluginManager;
import org.hyperic.hq.hqu.grails.commons.HQUGrailsApplication;
import org.hyperic.hq.hqu.grails.plugins.DefaultHQUGrailsPluginManager;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanFactoryPostProcessor;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.support.GenericApplicationContext;
import org.springframework.util.ClassUtils;
import org.springframework.web.context.WebApplicationContext;

/**
 * A class that handles the runtime configuration of the Grails ApplicationContext
 *
 */
public class HQUGrailsRuntimeConfigurator implements ApplicationContextAware {

    private static final Log log = LogFactory.getLog(HQUGrailsRuntimeConfigurator.class);

    private List<HQUGrailsApplication> applications;
    private ApplicationContext parent;
    // TODO: do we need to play around with plugin manager. in case we don't support user level grails plugins.
//    private GrailsPluginManager pluginManager;
    private Map<String,GrailsPluginManager> pluginManagers;
    private boolean loadExternalPersistenceConfig;

    public HQUGrailsRuntimeConfigurator(List<HQUGrailsApplication> applications) {
        this(applications, null);
    }

    public HQUGrailsRuntimeConfigurator(List<HQUGrailsApplication> applications, ApplicationContext parent) {
        super();
        this.applications = applications;
        this.parent = parent;
        
        try {
//        	this.pluginManager = PluginManagerHolder.getPluginManager();
        	if (this.pluginManagers == null) {
        		this.pluginManagers = new HashMap<String, GrailsPluginManager>();
        		
        		for (HQUGrailsApplication hquGrailsApplication : applications) {
					pluginManagers.put(hquGrailsApplication.getHQUApplicationId(),new DefaultHQUGrailsPluginManager("**/plugins/*/**GrailsPlugin.groovy", hquGrailsApplication));
				}
        		
//        		PluginManagerHolder.setPluginManager(this.pluginManager);
        	} else {
        		log.debug("Retrieved thread-bound PluginManager instance");
//        		this.pluginManager.setApplication(application);
        	}


        } catch (IOException e) {
        	throw new GrailsConfigurationException("I/O error loading plugin manager!:" + e.getMessage(), e);
        }


    }

    /**
     * Configures the Grails application context at runtime
     *
     * @return A WebApplicationContext instance
     */
    public WebApplicationContext configure() {
        return configure(null);
    }
    
	public WebApplicationContext configureParent(ServletContext context) {
		// TODO: it feels ugly to keep this in the same class
        WebRuntimeSpringConfiguration springConfig = new WebRuntimeSpringConfiguration(parent);
        springConfig.setBeanFactory(new HQUReloadAwareAutowireCapableBeanFactory(applications));
        WebApplicationContext ctx = (WebApplicationContext) springConfig.getApplicationContext();
        return ctx;
	}


    /**
     * Configures the Grails application context at runtime
     *
     * @param context A ServletContext instance
     * @return An ApplicationContext instance
     */
    public WebApplicationContext configure(ServletContext context) {
        return configure(context, true);
    }

    public WebApplicationContext configure(ServletContext context, boolean loadExternalBeans) {
    	
        WebRuntimeSpringConfiguration springConfig = new WebRuntimeSpringConfiguration(parent);
        springConfig.setBeanFactory(new HQUReloadAwareAutowireCapableBeanFactory());

        if (context != null) {
            springConfig.setServletContext(context);
        }

        for (HQUGrailsApplication app : applications) {

        	GrailsPluginManager pMan = pluginManagers.get(app.getHQUApplicationId());
            if (context != null) {
                pMan.setServletContext(context);
                
                if (!pMan.isInitialised()) {
                    pMan.loadPlugins();
                }

                if (!app.isInitialised()) {
                    pMan.doArtefactConfiguration();
                    app.initialise();
                }

                pMan.registerProvidedArtefacts(app);
                pMan.doRuntimeConfiguration(springConfig);
            }

		}

        reset();

        // TODO GRAILS-720 this causes plugin beans to be re-created - should get getApplicationContext always call refresh?
        WebApplicationContext ctx = (WebApplicationContext) springConfig.getApplicationContext();

        for (HQUGrailsApplication app : applications) {
        	GrailsPluginManager pMan = pluginManagers.get(app.getHQUApplicationId());
        	
            app.setMainContext(ctx);
            pMan.setApplicationContext(ctx);
            pMan.doDynamicMethods();

            ctx.publishEvent(new GrailsContextEvent(ctx, GrailsContextEvent.DYNAMIC_METHODS_REGISTERED));
            pMan.doPostProcessing(ctx);
            app.refreshConstraints();
        }

        return ctx;
    }

    // TODO: check what is executed through parent post processors. This is not called for now.
    private void registerParentBeanFactoryPostProcessors(WebRuntimeSpringConfiguration springConfig) {
        if(parent != null) {
            Map parentPostProcessors = parent.getBeansOfType(BeanFactoryPostProcessor.class);
            for (Object o : parentPostProcessors.values()) {
                BeanFactoryPostProcessor postProcessor = (BeanFactoryPostProcessor) o;
                ((ConfigurableApplicationContext) springConfig.getUnrefreshedApplicationContext())
                        .addBeanFactoryPostProcessor(postProcessor);

            }
        }
    }

    private static volatile BeanBuilder springGroovyResourcesBeanBuilder = null;

    /**
     * Attempt to load the beans defined by a BeanBuilder DSL closure in "resources.groovy"
     *
     * @param config
     * @param classLoader
     * @param context
     */
    private static void doLoadSpringGroovyResources(RuntimeSpringConfiguration config, ClassLoader classLoader,

                                                    GenericApplicationContext context) {

        loadExternalSpringConfig(config, classLoader);
        if (context != null) {
            springGroovyResourcesBeanBuilder.registerBeans(context);
        }

    }

    /**
     * Loads any external Spring configuration into the given RuntimeSpringConfiguration object
     * @param config The config instance
     * @param classLoader The class loader
     */
    public static void loadExternalSpringConfig(RuntimeSpringConfiguration config, ClassLoader classLoader) {
        if(springGroovyResourcesBeanBuilder == null) {
            try {
                Class groovySpringResourcesClass = null;
                try {
                    groovySpringResourcesClass = ClassUtils.forName(GrailsRuntimeConfigurator.SPRING_RESOURCES_CLASS,
                        classLoader);
                } catch (ClassNotFoundException e) {
                    // ignore
                }
                if (groovySpringResourcesClass != null) {
                    springGroovyResourcesBeanBuilder = new BeanBuilder(null, config,Thread.currentThread().getContextClassLoader());
                    Script script = (Script) groovySpringResourcesClass.newInstance();
                    script.run();
                    Object beans = script.getProperty("beans");
                    springGroovyResourcesBeanBuilder.beans((Closure) beans);
                }
            } catch (Exception ex) {
                GrailsUtil.deepSanitize(ex);
                log.error("[RuntimeConfiguration] Unable to load beans from resources.groovy", ex);
            }

        }
        else {
            if(!springGroovyResourcesBeanBuilder.getSpringConfig().equals(config)) {
                springGroovyResourcesBeanBuilder.registerBeans(config);
            }
        }
    }


    public static void loadSpringGroovyResources(RuntimeSpringConfiguration config, ClassLoader classLoader) {
        loadExternalSpringConfig(config, classLoader);
    }


    public static void loadSpringGroovyResourcesIntoContext(RuntimeSpringConfiguration config, ClassLoader classLoader,
                                                            GenericApplicationContext context) {
        loadExternalSpringConfig(config, classLoader);  
        doLoadSpringGroovyResources(config, classLoader, context);
    }

    public void setLoadExternalPersistenceConfig(boolean b) {
        this.loadExternalPersistenceConfig = b;
    }

//    public void setPluginManager(GrailsPluginManager manager) {
//        this.pluginManager = manager;
//    }
//
//    public GrailsPluginManager getPluginManager() {
//        return this.pluginManager;
//    }


    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        this.parent = applicationContext;
    }

    /**
     * Resets the GrailsRumtimeConfigurator
     */
    public void reset() {
        springGroovyResourcesBeanBuilder = null;
    }
}
