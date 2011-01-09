package org.hyperic.hq.hqu.grails.commons;

import org.codehaus.groovy.grails.commons.GrailsApplication;

public interface HQUGrailsApplication extends GrailsApplication {
	
    String HQU_APPLICATIONS_ID = "hquGrailsApplications";

	public String getHQUApplicationId();
}
