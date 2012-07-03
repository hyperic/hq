package org.hyperic.hq.tests.web;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.lang.annotation.Annotation;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.apache.cxf.jaxrs.client.Client;
import org.apache.cxf.jaxrs.client.ClientConfiguration;
import org.apache.cxf.jaxrs.client.JAXRSClientFactory;
import org.apache.cxf.jaxrs.client.WebClient;
import org.apache.cxf.jaxrs.provider.JAXBElementProvider;
import org.apache.cxf.service.model.EndpointInfo;
import org.apache.cxf.transport.Conduit;
import org.apache.cxf.transport.ConduitInitiator;
import org.apache.cxf.transport.ConduitInitiatorManager;
import org.apache.cxf.transport.http.HTTPConduit;
import org.apache.cxf.transport.http.HTTPTransportFactory;
import org.apache.cxf.ws.addressing.EndpointReferenceType;
import org.codehaus.jackson.jaxrs.JacksonJaxbJsonProvider;
import org.codehaus.jackson.jaxrs.JacksonJsonProvider;
import org.hyperic.hq.api.rest.cxf.TestHttpConduit;
import org.hyperic.hq.test.BaseInfrastructureTest;
import org.hyperic.hq.tests.context.WebContainerContextLoader;
import org.hyperic.hq.tests.context.WebContextConfiguration;
import org.junit.rules.TestRule;
import org.junit.runner.Description;
import org.junit.runners.model.Statement;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import com.meterware.servletunit.ServletRunner;

@DirtiesContext
@ContextConfiguration(locations = { "classpath:META-INF/hqapi-context.xml" , "classpath*:WEB-INF/security-web-context-spring.xml" }, loader=WebContainerContextLoader.class)
@WebContextConfiguration(contextRoot=WebTestCaseBase.CONTEXT, contextUrl=WebTestCaseBase.CONTEXT_URL, webXml=WebTestCaseBase.WEB_XML)
@Transactional(propagation=Propagation.NESTED)
public abstract class WebTestCaseBase extends BaseInfrastructureTest{

	protected static final String WEB_XML = "/WEB-INF/web-spring.xml" ; 
	protected static final String CONTEXT = "/tests" ; 
	public static final String CONTEXT_URL = "http://localhost" + CONTEXT ;
    		
    protected static abstract class IterationInterceptor<T extends Annotation> implements TestRule { 
    	
    	//cache at a class level as a new instance is initialized for each test method
    	public static Map<Class<?>, Object> clsLevelMetadata = new HashMap<Class<?>, Object>() ;  

    	private Class<T> annotationType ; 
    	
    	public IterationInterceptor(final Class<T> annotationType) { 
    		this.annotationType = annotationType ; 
    	}//EOM 
    	
//    	@Override
    	public Statement apply(final Statement base, final Description description) {
    		return new Statement() { 
    			
    			@Override
    			public final void evaluate() throws Throwable {
    				T metadata = description.getAnnotation(annotationType) ;
    				if(metadata == null) {  
    					final Object metadataTemp = clsLevelMetadata.get(annotationType) ; 
    					if(metadataTemp == null) { 
    						metadata = AnnotationUtils.findAnnotation(description.getTestClass(), annotationType) ; 
    						        
    						clsLevelMetadata.put(annotationType, (metadata == null ? Boolean.FALSE : metadata)) ; 
    					}//EO if not yet initialized 
    					else if(!(metadataTemp instanceof Boolean)) metadata = (T) metadataTemp ; 
    				}//EO if cls level metadata was not already cached 
    				if(metadata == null || shouldSkip(metadata)) base.evaluate() ; 
    				else {
    					try{ 
	    					final int iIterationLength = getIterationLength(metadata) ; 
	    					
	    					for(int i=0; i<iIterationLength; i++) {
	    						
	    						doBeforeEvaluation(i, metadata) ; 
	    						
	    						base.evaluate() ; 
	    						
	    						doAfterEvaluation(i, metadata) ; 
	    						
	    					}//EO while there are more platforms  
    					}finally{ 
    						
    					}//EO catch block 
    					
    				}//EO if the getresource annotation exists 
    			}//EOM 
    		};  
    	}//EOM 
    	
    	protected boolean shouldSkip(final T metadata) { return false ; }//EOM 
    	protected abstract int getIterationLength(final T metadata) ; 
    	
    	protected abstract void doBeforeEvaluation(final int iIterationIndex, final T metadata) ;
    	protected void doAfterEvaluation(final int iIterationIndex, final T metadata) {}//EOM
    		
    }//EO inner calss IterationInterceptor 
    
}//EOC 
