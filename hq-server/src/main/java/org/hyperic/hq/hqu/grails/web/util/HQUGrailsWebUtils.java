package org.hyperic.hq.hqu.grails.web.util;

import java.util.List;

import javax.servlet.ServletContext;

import org.hyperic.hq.hqu.grails.commons.HQUGrailsApplication;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.context.support.WebApplicationContextUtils;

/**
 * Utility methods to access commons objects and perform common web related
 * functions for the internal framework
 */
public class HQUGrailsWebUtils {
	
    /**
     * Looks up the list of HQUGrailsApplication from bean.
     *
     * @return The List of HQUGrailsApplication instances
     */
    @SuppressWarnings("unchecked")
	public static List<HQUGrailsApplication> lookupApplications(ServletContext servletContext) {
        WebApplicationContext wac =
                WebApplicationContextUtils.getRequiredWebApplicationContext(servletContext);

        return (List<HQUGrailsApplication>) wac.getBean(HQUGrailsApplication.HQU_APPLICATIONS_ID);

    }

}
