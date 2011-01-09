package org.hyperic.hq.hqu.grails.commons.spring;

import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.codehaus.groovy.grails.commons.spring.ReloadAwareAutowireCapableBeanFactory;
import org.hyperic.hq.hqu.grails.commons.HQUGrailsApplication;
import org.springframework.beans.factory.support.RootBeanDefinition;

public class HQUReloadAwareAutowireCapableBeanFactory extends
		ReloadAwareAutowireCapableBeanFactory {
	
	private final static Log log = LogFactory.getLog(HQUReloadAwareAutowireCapableBeanFactory.class);

	/**
	 * Creates a new bean factory. Registering given HQU Grails applications to factory.
	 * @param applications
	 */
	public HQUReloadAwareAutowireCapableBeanFactory(List<HQUGrailsApplication> applications) {
		super();
		for (HQUGrailsApplication hquGrailsApplication : applications) {
			log.info("Registering bean: " + hquGrailsApplication.getHQUApplicationId()+"grailsApplication");
			registerSingleton(hquGrailsApplication.getHQUApplicationId()+"grailsApplication", hquGrailsApplication);			
		}
	}

	/**
	 * Creates a new bean factory.
	 */
	public HQUReloadAwareAutowireCapableBeanFactory() {
		super();
	}


	protected Object doCreateBean(String beanName, RootBeanDefinition mbd, Object[] args) {

		if(log.isDebugEnabled())
			log.debug("Getting bean: " + beanName);
		
		if(beanName.endsWith("grailsApplication")) {
			String pName = beanName.substring(0, beanName.length()-16);
			
			if(log.isDebugEnabled())
				log.debug("doCreateBean check with pName:" + pName);

			List<HQUGrailsApplication> applications = getBean(HQUGrailsApplication.HQU_APPLICATIONS_ID, List.class);

			for (HQUGrailsApplication hquGrailsApplication : applications) {
				if(hquGrailsApplication.getHQUApplicationId().equals(pName)) {
					if(log.isDebugEnabled())
						log.debug("returning HQUGrailsApplication instance with " + pName);
					return hquGrailsApplication;					
				}
			}
		}
		
		return super.doCreateBean(beanName, mbd, args);
	}
}
