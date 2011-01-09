package org.hyperic.hq.hqu.grails.commons.spring;

import org.codehaus.groovy.grails.commons.spring.GrailsResourceHolder;
import org.hyperic.hq.hqu.grails.commons.HQUGrailsResourceUtils;
import org.springframework.core.io.Resource;

public class HQUGrailsResourceHolder extends GrailsResourceHolder{

	public String getClassName(Resource resource) {
		return HQUGrailsResourceUtils.getClassName(resource);
//		return super.getClassName(resource);
	}
}
