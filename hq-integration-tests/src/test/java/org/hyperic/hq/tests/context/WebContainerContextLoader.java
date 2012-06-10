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

	private Class<?> testClass ;
	private WebContextConfiguration webContextConfig ;
	
	public WebContainerContextLoader() {super() ;}//EOM
	
	protected static final String DEFAULT_WEB_XML = "/WEB-INF/web-spring.xml" ; 

	protected static final String DEFAULT_CONTEXT = "/tests" ; 
	protected static final String DEFAULT_CONTEXT_URL = "http://localhost" + DEFAULT_CONTEXT ; 
	
	/**
	 * using this method to determine webXml metadata 
	 */
	@Override
	protected final String[] generateDefaultLocations(final Class<?> clazz) {
		if(this.testClass == null) this.testClass = clazz ; 
		return super.generateDefaultLocations(clazz);
	}//EOM  
	
	@Override
	protected final String[] modifyLocations(final Class<?> clazz, final String... locations) {
		if(this.testClass == null) this.testClass = clazz ; 
		return super.modifyLocations(clazz, locations);
	}//EOM 
	
	@Override
	public final ApplicationContext loadContext(final String... locations) throws Exception {
		
		if (logger.isDebugEnabled()) {
            logger.debug("Loading ApplicationContext for locations [" +
                    StringUtils.arrayToCommaDelimitedString(locations) + "].");
        }//EO if logger is enabled 
		
		 
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
		
		Bootstrap.setSpringConfigLocations(locations) ; 
		final File webxmlFile = new File(this.testClass.getResource(webxml).getFile()) ;  
		final ServletRunner sr = new ServletRunner(webxmlFile, contextRoot);
         
		try {
            sr.newClient().getResponse(contextUrl + "/services");
        } catch (HttpNotFoundException e) {
            // ignore, we just want to boot up the servlet
        }//EO catch block    
        
        HttpUnitOptions.setExceptionsThrownOnErrorStatus(true); 
		
        //at this stage the application context would have already been set in the bootstrap 
        final ConfigurableApplicationContext applicationContext = (ConfigurableApplicationContext) Bootstrap.getApplicationContext() ; 
        //register the service runner in the application context for injection purposes 
        applicationContext.getBeanFactory().registerSingleton("servletRunner", sr) ; 
        
        final ProxyingGenericApplicationContext actualContext = new ProxyingGenericApplicationContext((AbstractApplicationContext) applicationContext) ; 
        
        final TestData testDataAnnotation = AnnotationUtils.findAnnotation(this.testClass, TestData.class) ;
        if(testDataAnnotation != null) { 
        	
        	final Class<?>testDataPopulatorClass = testDataAnnotation.value() ; 
        	
        	final DefaultListableBeanFactory beansFactory = (DefaultListableBeanFactory) applicationContext.getBeanFactory() ;
        	final AbstractBeanDefinition beanDefinition = 
        				BeanDefinitionBuilder.rootBeanDefinition(testDataPopulatorClass).getBeanDefinition() ;
        	
        	beansFactory.registerBeanDefinition(testDataPopulatorClass.getName(), beanDefinition) ;

        	final TestDataPopulator testDataPopulator = (TestDataPopulator) applicationContext.getBean(testDataPopulatorClass) ;
        	
        	final PlatformTransactionManager txManager = (PlatformTransactionManager)applicationContext.getBean("transactionManager") ;
        	//set Nested  trasnaction support 
        	if(txManager instanceof HibernateTransactionManager) ((HibernateTransactionManager) txManager).setNestedTransactionAllowed(true) ; 
        	
            //retrieve the bean and invoke the populate in a tx context 
        	final TransactionTemplate txTemplate = new TransactionTemplate(txManager) ;
        	txTemplate.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
        	final TransactionStatus txStatus = txManager.getTransaction(txTemplate);
        	
        	try{ 
            	testDataPopulator.populate() ;
            }catch(Throwable t) {
            	txStatus.setRollbackOnly() ;  
            	actualContext.close() ; 
            	throw (t instanceof RuntimeException ? (RuntimeException)t : new RuntimeException(t)) ;
            }//EO catch block 
        	
        	
/*        	txTemplate.execute(new TransactionCallback<String>() {
        		@Override
        		public final String doInTransaction(final TransactionStatus status) {
        			
    	            try{ 
    	            	testDataPopulator.populate() ;
    	            	return null ;
    	            }catch(Throwable t) { 
    	            	actualContext.close() ; 
    	            	throw (t instanceof RuntimeException ? (RuntimeException)t : new RuntimeException(t)) ;
    	            }finally{
    	            	//rollback the tx 
    	            	//status.setRollbackOnly() ; 
    	            }//EO catch block 
    	            
        		}//EOM 
        	}) ; */
        	
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
        	
        	applicationContext.addApplicationListener(new ApplicationListener<ContextClosedEvent>() {
            	@Override
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
	
}//EOC 
