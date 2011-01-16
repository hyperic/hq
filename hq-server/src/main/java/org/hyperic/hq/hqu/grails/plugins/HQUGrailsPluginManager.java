package org.hyperic.hq.hqu.grails.plugins;

import org.codehaus.groovy.grails.commons.spring.RuntimeSpringConfiguration;
import org.codehaus.groovy.grails.plugins.GrailsPluginManager;

public interface HQUGrailsPluginManager extends GrailsPluginManager {

	/**
	 * Executes the runtime configuration phase of plug-ins.
	 * <p>This method is meant to be run only once per plugin system,
	 * not with every HQU Grails Application. This allows to create beans
	 * which should exist without prefixing with application name.
	 * 
	 * @param springConfig The RuntimeSpringConfiguration instance
	 */
	void doRuntimeConfigurationOnce(RuntimeSpringConfiguration springConfig);

}
