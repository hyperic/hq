package org.hyperic.hq.integration;

import java.util.Set;

import org.hyperic.hq.alert.domain.Alert;
import org.hyperic.hq.inventory.domain.Resource;
import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Transactional
@Component
public class LoadFromDBTester {

	
	public void testThis() {
		Alert alert = Alert.findAlert(1l);
		Resource alertingElement = alert.getResource();
		Set<Alert> alerts = alertingElement.getAlerts();
		//TODO above comes back null
		//TODO remove didn't work so well before - retest
		alert.remove();
		alertingElement.remove();
		
	}
	
	  public static void main(String[] args) {
	        ClassPathXmlApplicationContext appContext = new ClassPathXmlApplicationContext(
	            "classpath:/META-INF/spring/applicationContext.xml");
	        LoadFromDBTester tester= appContext.getBean(LoadFromDBTester.class);
	        tester.testThis();
	        System.exit(0);
	    }
}
