package org.hyperic.hq.hqu.grails.plugins.support.aware;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.codehaus.groovy.grails.plugins.support.BeanPostProcessorAdapter;
import org.codehaus.groovy.grails.plugins.support.aware.GrailsApplicationAware;
import org.codehaus.groovy.grails.plugins.support.aware.GrailsConfigurationAware;
import org.hyperic.hq.hqu.grails.commons.HQUGrailsApplication;
import org.springframework.beans.BeansException;

public class HQUGrailsApplicationAwareBeanPostProcessor extends BeanPostProcessorAdapter {
	
	private final static Log log = LogFactory.getLog(HQUGrailsApplicationAwareBeanPostProcessor.class);
    private HQUGrailsApplication grailsApplication;

    public HQUGrailsApplicationAwareBeanPostProcessor(HQUGrailsApplication grailsApplication) {
        this.grailsApplication = grailsApplication;
    }

    public Object postProcessBeforeInitialization(Object bean, String beanName) throws BeansException {
        processAwareInterfaces(grailsApplication,bean,beanName);
        return bean;
    }

    public static void processAwareInterfaces(HQUGrailsApplication grailsApplication, Object bean, String beanName) {
    	
    	log.info("processAwareInterfaces:" + beanName);
    	
        if (bean instanceof HQUGrailsApplicationAware) {

        	if(isBeanMatchingWithApplication(grailsApplication, beanName)) {
            	HQUGrailsApplicationAware app = ((HQUGrailsApplicationAware)bean);
        		app.setGrailsApplication(grailsApplication);
        	}
        	
        }
        
        // TODO: ultimately we need to handle what beans should be processed here.
        if (bean instanceof GrailsApplicationAware) {
        	
        	if(isBeanMatchingWithApplication(grailsApplication, beanName)) {
        		((GrailsApplicationAware)bean).setGrailsApplication(grailsApplication);
        	}
        	
//        	if(beanName.equals("gspTagLibraryLookup")) {
//        		((GrailsApplicationAware)bean).setGrailsApplication(grailsApplication);
//        	}        	
        }

        if(bean instanceof GrailsConfigurationAware) {
            ((GrailsConfigurationAware)bean).setConfiguration(grailsApplication.getConfig());
        }
    }
    
    private static boolean isBeanMatchingWithApplication(HQUGrailsApplication app, String beanName) {
    	if(beanName.endsWith("controllerHandlerMappings")) {
    		String expected = app.getHQUApplicationId() + "controllerHandlerMappings";
    		if(beanName.equals(expected))
    			return true;
    	} else if(beanName.endsWith("gspTagLibraryLookup")) {
    		String expected = app.getHQUApplicationId() + "gspTagLibraryLookup";
    		if(beanName.equals(expected))
    			return true;
    	}
    	return false;
    }

}
