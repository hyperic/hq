package org.hyperic.hq.hqu.grails.commons;

import groovy.util.ConfigObject;
import groovy.util.ConfigSlurper;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.codehaus.groovy.grails.commons.ConfigurationHolder;
import org.springframework.beans.factory.FactoryBean;
import org.springframework.beans.factory.InitializingBean;
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
			
			DefaultHQUGrailsApplication app = new DefaultHQUGrailsApplication(findGroovyFiles(resource));
			app.setHQUApplicationId(getAppNameFromDir(resource));			
			applications.add(app);
		}
		setConfig();
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
