package org.hyperic.hq.hqu.grails.commons;

import groovy.lang.GroovyClassLoader;
import groovy.util.ConfigObject;
import groovy.util.ConfigSlurper;
import groovy.util.XmlSlurper;
import groovy.util.slurpersupport.GPathResult;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.codehaus.groovy.control.CompilerConfiguration;
import org.codehaus.groovy.grails.commons.ConfigurationHolder;
import org.codehaus.groovy.grails.compiler.GrailsClassLoader;
import org.springframework.beans.factory.FactoryBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;

/**
 * Holds collection of HQUGrailsApplications. This bean acts as a factory to
 * build custom grails applications.
 * 
 * Init bean:
 * <bean id="hquGrailsApplications" class="org.hyperic.hq.hqu.grails.HQUGrailsApplicationsBean">
 *   <property name="resourceDir">
 *     <value>/WEB-INF/hqu-plugins</value>
 *   </property>
 * </bean>   
 *
 */
public class HQUGrailsApplicationsBean implements FactoryBean, InitializingBean {
	
	private final static Log log = LogFactory.getLog(HQUGrailsApplicationsBean.class);
	private List<HQUGrailsApplication> applications = new ArrayList<HQUGrailsApplication>(); 
	
	// Resource directory containing hqu plugins
    private Resource resourceDir;
	
	public HQUGrailsApplicationsBean() {
		super();
	}

	public void afterPropertiesSet() throws Exception {
		for (Resource resource : findHQUPlugins(resourceDir)) {
			
			if(log.isDebugEnabled())
				log.info("Loading HQU Plugins from: " + resource.getFile().getAbsolutePath());
			
			File descriptor = new File(resource.getFile(), "WEB-INF/grails.xml");
			if(descriptor.isFile()) {
				loadFromCompiled(resource, descriptor);
			} else {
				loadFromResources(resource);
			}
			
//			DefaultHQUGrailsApplication app = new DefaultHQUGrailsApplication(findGroovyFiles(resource));
//			app.setHQUApplicationId(getAppNameFromDir(resource));			
//			applications.add(app);
		}
		setConfig();
	}
	
	private void loadFromResources(Resource resource) throws Exception {
		DefaultHQUGrailsApplication app = new DefaultHQUGrailsApplication(findGroovyFiles(resource));
		app.setHQUApplicationId(getAppNameFromDir(resource));			
		applications.add(app);		
	}

	private void loadFromCompiled(Resource resource, File file) throws Exception {
		
        final ClassLoader parentLoader = Thread.currentThread().getContextClassLoader();
        CompilerConfiguration config = CompilerConfiguration.DEFAULT;
        config.setSourceEncoding("UTF-8");
        GroovyClassLoader classLoader = new GroovyClassLoader(parentLoader);

        Resource classesDir = new FileSystemResource(new File(resource.getFile(),"WEB-INF/classes"));
        
        classLoader.addURL(classesDir.getURL());
		
		Resource descriptor = new FileSystemResource(file);
        List classes = new ArrayList();
        InputStream inputStream = null;

        try {
			inputStream = descriptor.getInputStream();
			
            GPathResult root = new XmlSlurper().parse(inputStream);
            GPathResult resources = (GPathResult) root.getProperty("resources");
            GPathResult grailsClasses = (GPathResult) resources.getProperty("resource");

            for (int i = 0; i < grailsClasses.size(); i++) {
                GPathResult node = (GPathResult) grailsClasses.getAt(i);
                String className = node.text();
                try {
                	Class clazz;
                	if(classLoader instanceof GrailsClassLoader) {
                		clazz=classLoader.loadClass(className);
                	} else {
                		clazz=Class.forName(className, true, classLoader);
                	}
                	classes.add(clazz);
                } catch (ClassNotFoundException e) {
                    log.warn("Class with name ["+className+"] was not found, and hence not loaded. Possible empty class or script definition?");
                }
            }
            Class[] loadedClasses = (Class[])classes.toArray(new Class[classes.size()]);
    		DefaultHQUGrailsApplication app = new DefaultHQUGrailsApplication(loadedClasses, classLoader);
    		app.setHQUApplicationId(getAppNameFromDir(resource));			
    		applications.add(app);		


		} finally {
            if(inputStream!=null)
                inputStream.close();
        }
	}
	
	/**
	 * Find HQU plugin directories from given base directory.
	 * 
	 * @param baseDir Base directory for HQU Plugins
	 * @return List of plugin directories
	 */
	private static Resource[] findHQUPlugins(Resource baseDir) {
		PathMatchingResourcePatternResolver matcher = new PathMatchingResourcePatternResolver();
		Resource[] dirs = new Resource[0];
		try {
			String pattern = "file:" + baseDir.getFile().getAbsolutePath() + "/*";
			dirs = matcher.getResources(pattern);
		} catch (IOException e) {
			log.error("Error finding hqu plugins from dir: " + baseDir, e);
		}
		return dirs;
		
	}

	
	/**
	 * Finds a list of groovy files based on given base directory.
	 * 
	 * @param baseDir Base directory to start matching
	 * @return List of groovy files are Resources
	 */
	private static Resource[] findGroovyFiles(Resource baseDir) {
		PathMatchingResourcePatternResolver matcher = new PathMatchingResourcePatternResolver();
		Resource[] files = new Resource[0];
		try {
			String pattern = "file:" + baseDir.getFile().getAbsolutePath() + "/**/*.groovy";
			files = matcher.getResources(pattern);
		} catch (IOException e) {
			log.error("Error finding files from plugin dir: " + baseDir, e);
		}
		return files;
	}
	
	/**
	 * Returns HQU Plugin name derived from path.
	 * 
	 * @param baseDir Resource directory containing the HQU Plugin
	 * @return HQU Plugin name
	 */
	private String getAppNameFromDir(Resource baseDir) {
		// TODO: should get name from properties file
		return baseDir.getFilename();
	}

	public Object getObject() throws Exception {
		return this.applications;
	}

	public Class<?> getObjectType() {
		return List.class;
	}

	public boolean isSingleton() {
		return true;
	}
	
    public Resource getResourceDir() {
        return resourceDir;
    }

    public void setResourceDir(Resource resourceDir) {
        this.resourceDir = resourceDir;
    }
    
    private void setConfig() {
    	StringBuilder buf = new StringBuilder();
    	buf.append("grails.views.gsp.sitemesh.preprocess = false");
//        ConfigObject config = new ConfigSlurper().parse("grails.views.gsp.keepgenerateddir = \"" + tempdir.getAbsolutePath() + "\"");
        ConfigObject config = new ConfigSlurper().parse(buf.toString());
        ConfigurationHolder.setConfig(config);
    }
	
}
