package org.hyperic.hq.hqu.grails.plugins;

import org.codehaus.groovy.grails.commons.spring.RuntimeSpringConfiguration;
import org.codehaus.groovy.grails.plugins.GrailsPlugin;

public interface HQUGrailsPlugin extends GrailsPlugin {
	
    void doWithRuntimeConfigurationOnce(RuntimeSpringConfiguration springConfig);

}
