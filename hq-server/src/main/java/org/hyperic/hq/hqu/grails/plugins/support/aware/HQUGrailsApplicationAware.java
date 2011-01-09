package org.hyperic.hq.hqu.grails.plugins.support.aware;

import org.codehaus.groovy.grails.commons.GrailsApplication;
import org.codehaus.groovy.grails.plugins.support.aware.GrailsApplicationAware;

public interface HQUGrailsApplicationAware extends GrailsApplicationAware {
	GrailsApplication getGrailsApplication();

}
