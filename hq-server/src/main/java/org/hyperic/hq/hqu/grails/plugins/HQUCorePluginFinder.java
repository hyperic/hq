package org.hyperic.hq.hqu.grails.plugins;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.codehaus.groovy.grails.commons.GrailsApplication;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;

import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

/**
 * Class with responsibility for loading core plugin classes. 
 */
public class HQUCorePluginFinder {

	private static final Log log = LogFactory.getLog(HQUCorePluginFinder.class);

	private final PathMatchingResourcePatternResolver resolver;

	private final GrailsApplication application;

	private final Set foundPluginClasses;

	public HQUCorePluginFinder(GrailsApplication application) {
		super();
		this.resolver = new PathMatchingResourcePatternResolver();
		this.application = application;
		this.foundPluginClasses = new HashSet();
	}

	public Set getPluginClasses() {

		// just in case we try to use this twice
		foundPluginClasses.clear();

		try {
			Resource[] resources = resolver
					.getResources("classpath*:org/hyperic/hq/hqu/grails/plugins/**/*GrailsPlugin.class");
			if (resources.length > 0) {
				loadCorePluginsFromResources(resources);
			} else {
				log.warn("WARNING: Grails was unable to load core plugins dynamically. This is normally a problem with the container class loader configuration, see troubleshooting and FAQ for more info. ");
				loadCorePluginsStatically();
			}
		} catch (IOException e) {
            log.warn("WARNING: I/O exception loading core plugin dynamically, attempting static load. This is usually due to deployment onto containers with unusual classloading setups. Message: " + e.getMessage());
            loadCorePluginsStatically();

		}
		return foundPluginClasses;
	}

	private void loadCorePluginsStatically() {

		// TODO: I think we can safely remove this function
		
		// This is a horrible hard coded hack, but there seems to be no way to
		// resolve .class files dynamically
		// on OC4J. If anyones knows how to fix this shout
//		loadCorePlugin("org.codehaus.groovy.grails.plugins.CoreGrailsPlugin");
//        loadCorePlugin("org.codehaus.groovy.grails.plugins.LoggingGrailsPlugin");
//        loadCorePlugin("org.codehaus.groovy.grails.plugins.CodecsGrailsPlugin");
//        loadCorePlugin("org.codehaus.groovy.grails.plugins.i18n.I18nGrailsPlugin");
//		loadCorePlugin("org.codehaus.groovy.grails.plugins.datasource.DataSourceGrailsPlugin");
//		loadCorePlugin("org.codehaus.groovy.grails.plugins.DomainClassGrailsPlugin");
//		loadCorePlugin("org.codehaus.groovy.grails.plugins.ValidationGrailsPlugin");
//		loadCorePlugin("org.codehaus.groovy.grails.plugins.web.ServletsGrailsPlugin");
//		loadCorePlugin("org.codehaus.groovy.grails.plugins.web.ControllersGrailsPlugin");
//        loadCorePlugin("org.codehaus.groovy.grails.plugins.web.GroovyPagesGrailsPlugin");
//        loadCorePlugin("org.codehaus.groovy.grails.plugins.web.mapping.UrlMappingsGrailsPlugin");
//        loadCorePlugin("org.codehaus.groovy.grails.plugins.web.filters.FiltersGrailsPlugin");
//        loadCorePlugin("org.codehaus.groovy.grails.plugins.web.mimes.MimeTypesGrailsPlugin");
//        loadCorePlugin("org.codehaus.groovy.grails.plugins.services.ServicesGrailsPlugin");
//        loadCorePlugin("org.codehaus.groovy.grails.plugins.converters.ConvertersGrailsPlugin");
//        loadCorePlugin("org.codehaus.groovy.grails.plugins.scaffolding.ScaffoldingGrailsPlugin");
    }

	private void loadCorePluginsFromResources(Resource[] resources)
			throws IOException {

		log.debug("Attempting to load [" + resources.length + "] core plugins");
		for (int i = 0; i < resources.length; i++) {
			Resource resource = resources[i];
			String url = resource.getURL().toString();
			int packageIndex = url.indexOf("org/hyperic/hq/hqu/grails");
			url = url.substring(packageIndex, url.length());
			url = url.substring(0, url.length() - 6);
			String className = url.replace('/', '.');

			loadCorePlugin(className);
		}
	}

	private Class attemptCorePluginClassLoad(String pluginClassName) {
		try {
			return application.getClassLoader().loadClass(pluginClassName);
		} catch (ClassNotFoundException e) {
			log.warn("[GrailsPluginManager] Core plugin [" + pluginClassName
					+ "] not found, resuming load without..");
			if (log.isDebugEnabled())
				log.debug(e.getMessage(), e);
		}
		return null;
	}

	private void loadCorePlugin(String pluginClassName) {
		Class pluginClass = attemptCorePluginClassLoad(pluginClassName);

		if (pluginClass != null) {
			addPlugin(pluginClass);
		}
	}

	private void addPlugin(Class plugin) {
		foundPluginClasses.add(plugin);
	}

}
