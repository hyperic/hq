/* **********************************************************************
/*
 * NOTE: This copyright does *not* cover user programs that use Hyperic
 * program services by normal system calls through the application
 * program interfaces provided as part of the Hyperic Plug-in Development
 * Kit or the Hyperic Client Development Kit - this is merely considered
 * normal use of the program, and does *not* fall under the heading of
 * "derived work".
 *
 * Copyright (C) [2004-2010], VMware, Inc.
 * This file is part of Hyperic.
 *
 * Hyperic is free software; you can redistribute it and/or modify
 * it under the terms version 2 of the GNU General Public License as
 * published by the Free Software Foundation. This program is distributed
 * in the hope that it will be useful, but WITHOUT ANY WARRANTY; without
 * even the implied warranty of MERCHANTABILITY or FITNESS FOR A
 * PARTICULAR PURPOSE. See the GNU General Public License for more
 * details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307
 * USA.
 *
 */ 	
package org.hyperic.hq.tests.context;

import java.io.File;
import java.lang.reflect.Modifier;
import java.lang.reflect.ParameterizedType;

import junit.framework.Assert;

import org.hyperic.hq.api.rest.AbstractRestTestDataPopulator;
import org.hyperic.hq.api.rest.AuthenticationTest;
import org.hyperic.hq.api.rest.RestTestCaseBase;
import org.hyperic.hq.api.rest.AbstractRestTestDataPopulator.RestTestData;
import org.hyperic.hq.context.Bootstrap;
import org.hyperic.hq.context.IntegrationTestContextLoader;
import org.springframework.beans.factory.support.AbstractBeanDefinition;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationListener;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.event.ContextClosedEvent;
import org.springframework.context.support.AbstractApplicationContext;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.orm.hibernate3.HibernateTransactionManager;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.util.StringUtils;

import com.meterware.httpunit.HttpNotFoundException;
import com.meterware.httpunit.HttpUnitOptions;
import com.meterware.servletunit.ServletRunner;

public class WebContainerContextLoader extends IntegrationTestContextLoader{

	private WebContextConfiguration webContextConfig ;
	
	public WebContainerContextLoader() {super() ;}//EOM
	
	protected static final String DEFAULT_WEB_XML = "/WEB-INF/web-spring.xml" ; 

	protected static final String DEFAULT_CONTEXT = "/tests" ; 
	protected static final String DEFAULT_CONTEXT_URL = "http://localhost" + DEFAULT_CONTEXT ; 
	
	@Override
	public final ApplicationContext loadContext(final String... locations) throws Exception {
		
		if (logger.isDebugEnabled()) {
            logger.debug("Loading ApplicationContext for locations [" +
                    StringUtils.arrayToCommaDelimitedString(locations) + "].");
        }//EO if logger is enabled 
		
		//attempt to retrieve the class level webContextConfiguration annotations
		//if undefined use the default values defined in this class 
		this.webContextConfig = AnnotationUtils.findAnnotation(this.testClass, WebContextConfiguration.class) ;
		
		String webxml = null, contextRoot = null, contextUrl = null ; 
		if(this.webContextConfig == null) { 
			webxml = DEFAULT_CONTEXT ; 
			contextRoot = DEFAULT_CONTEXT ; 
			contextUrl = DEFAULT_CONTEXT_URL ; 
		}else { 
			webxml = this.webContextConfig.webXml() ; 
			contextRoot = this.webContextConfig.contextRoot() ; 
			contextUrl = this.webContextConfig.contextUrl() ; 
		}//EO else no custom annotation was defined 
		
		//set the spring context files locations in the bootstrap to be picked up by the TestBootstapContextListener class and 
		//injected into the mock servlet configuration as the contextConfigLocation context-param 
		Bootstrap.setSpringConfigLocations(locations) ; 
		//create the mock web container which shall be initialized in accordance with the webxmlFile's configuration 
		//and which shall initalize the spring context using the Bootstrap.getSpringConfigLocations 
		final File webxmlFile = new File(this.testClass.getResource(webxml).getFile()) ;  
		final ServletRunner sr = new ServletRunner(webxmlFile, contextRoot);
         
		try {
			//lazy load the services 
            sr.newClient().getResponse(contextUrl + "/services");
        } catch (HttpNotFoundException e) {
            // ignore, we just want to boot up the servlet
        }//EO catch block    
        
        HttpUnitOptions.setExceptionsThrownOnErrorStatus(true); 
		
        //at this stage the webApplicationContext would have already been set in the bootstrap by the TestBootstapContextListener 
        final ConfigurableApplicationContext applicationContext = (ConfigurableApplicationContext) Bootstrap.getApplicationContext() ; 
        //register the service runner in the application context for injection purposes 
        applicationContext.getBeanFactory().registerSingleton("servletRunner", sr) ; 
        
        //wrap the context with a ProxyingGenericApplicationContext instance so as to control its shutdown 
        final ProxyingGenericApplicationContext actualContext = new ProxyingGenericApplicationContext((AbstractApplicationContext) applicationContext) ;
        
        final TestDataPopulator testDataPopulator = this.getTestDataPopulator(applicationContext) ; 
        
        //if the test class defines a TestData Annotation instantiate the test data populator class it defines and invoke its populate()  
        if(testDataPopulator != null) { 
        	
        	//the populate() invocation shall be executed in the context of a transaction so that it could be rolledback when the context 
        	//shuts down  
        	final PlatformTransactionManager txManager = (PlatformTransactionManager)applicationContext.getBean("transactionManager") ;
        	//set explicit nested tx support 
        	if(txManager instanceof HibernateTransactionManager) ((HibernateTransactionManager) txManager).setNestedTransactionAllowed(true) ; 
        	
            //retrieve the txManager bean and invoke the populate in a new top level transaction. 
        	//the transaction would be closed during the application context's shut down. 
        	//all test methods are expected to be marked with Trasnactional(propagation=NESTED) 
        	//which would cause spring to create a save point in already opened top level transaation 
        	//and rollback onto it after each test method 
        	final TransactionTemplate txTemplate = new TransactionTemplate(txManager) ;
        	txTemplate.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
        	final TransactionStatus txStatus = txManager.getTransaction(txTemplate);
        	
        	try{ 
        		//invoke the populate() which is responsible for initializing the one-off test data 
        		//shared across all test methods 
            	testDataPopulator.populate() ;
            }catch(Throwable t) {
            	//ensure that the rx is rolledback prior to closing the session so that the top level transaction 
            	//would be rolled back 
            	txStatus.setRollbackOnly() ;  
            	actualContext.close() ; 
            	throw (t instanceof RuntimeException ? (RuntimeException)t : new RuntimeException(t)) ;
            }//EO catch block 
        	
        	//Ensure that the context is shutdown prior to JVM shutdown so that resources are cleared properly. 
        	Runtime.getRuntime().addShutdownHook(new Thread() { 
        		
        		private TestDataPopulator populator = testDataPopulator ;  
        		
        		@Override
        		public final void run() {
        			try{ 
        				actualContext.destroy() ;
        			}catch(Throwable t) { 
        				t.printStackTrace()  ; 
        			}//EO catch block 
        		}//EOM 
        	}) ; 
        	
        	//Register shutdown sequence in which the webcontainer is shutdown and any top level transaction 
        	//is rolledback so that the actions of the test data populator are reverted. 
        	applicationContext.addApplicationListener(new ApplicationListener<ContextClosedEvent>() {
//            	@Override
            	public void onApplicationEvent(ContextClosedEvent event) {
            		sr.shutDown() ;
            		if(txStatus != null) { 
            			//rollback test data populator 
            			txStatus.setRollbackOnly() ;
            			txManager.commit(txStatus)  ;
            		}//EO if managed to create 
            	}//EOM 
    		});
        	
        }//EO if there was a test data annotation handler
		
        return actualContext ; 
	}//EOM
	
	private final TestDataPopulator getTestDataPopulator(final ConfigurableApplicationContext applicationContext) { 
	    TestDataPopulator testDataPopulator  = null ; 
	    
	    //first attempt to locate a RestTestdata annotation, if found, attempt 
        Class<? extends TestDataPopulator> testDataPopulatorClass = null ;  
         
        //first attempt to retrieve a RestTestData annotation 
        final RestTestData restTestDataAnnotation = AnnotationUtils.findAnnotation(this.testClass, RestTestData.class) ;
        final TestData testDataAnnotation = AnnotationUtils.findAnnotation(this.testClass, TestData.class) ;

        if(testDataAnnotation != null) { 
            testDataPopulatorClass = testDataAnnotation.value() ;
        }else if(RestTestCaseBase.class.isAssignableFrom(this.testClass)  ) { 
            final ParameterizedType genericSuprtClassType = (ParameterizedType) AuthenticationTest.class.getGenericSuperclass() ; 
            final ParameterizedType testDataPopulatorType = (ParameterizedType) genericSuprtClassType.getActualTypeArguments()[1] ;
            testDataPopulatorClass = (Class<? extends TestDataPopulator>) testDataPopulatorType.getRawType() ;
        }//EO else if not an instance of ResttestCaseBase
        
        //if the test class defines a TestData Annotation instantiate the test data populator class it defines and invoke its populate()  
        if(testDataPopulatorClass != null) { 
            
            final DefaultListableBeanFactory beansFactory = (DefaultListableBeanFactory) applicationContext.getBeanFactory() ;
            final AbstractBeanDefinition beanDefinition = 
                        BeanDefinitionBuilder.rootBeanDefinition(testDataPopulatorClass).getBeanDefinition() ;
            
            beansFactory.registerBeanDefinition(testDataPopulatorClass.getName(), beanDefinition) ;

            testDataPopulator = (TestDataPopulator) applicationContext.getBean(testDataPopulatorClass) ;
            
            //set the rest test data in the instance 
            if(testDataPopulator instanceof AbstractRestTestDataPopulator) { 
                ((AbstractRestTestDataPopulator) testDataPopulator).setRestTestData(restTestDataAnnotation) ;
            }//EO if instanceof AbstractRestTestDataPopulator
        }///EO if a testdatapopulator class was inferred 
        
        return testDataPopulator  ; 
	}//EOM 
	
}//EOC 
