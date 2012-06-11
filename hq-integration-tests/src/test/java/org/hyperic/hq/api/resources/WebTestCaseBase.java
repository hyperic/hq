package org.hyperic.hq.api.resources;

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
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import com.meterware.servletunit.ServletRunner;

@DirtiesContext
@ContextConfiguration(locations = { "classpath:META-INF/hqapi-context.xml" }, loader=WebContainerContextLoader.class)
@WebContextConfiguration(contextRoot=WebTestCaseBase.CONTEXT, contextUrl=WebTestCaseBase.CONTEXT_URL, webXml=WebTestCaseBase.WEB_XML)
@Transactional(propagation=Propagation.NESTED)
public abstract class WebTestCaseBase extends BaseInfrastructureTest{

	protected static final String WEB_XML = "/WEB-INF/web-spring.xml" ; 
	protected static final String CONTEXT = "/tests" ; 
	protected static final String CONTEXT_URL = "http://localhost" + CONTEXT ;
    		
	@Retention(RetentionPolicy.RUNTIME) 
	@Target({ElementType.METHOD, ElementType.TYPE})
	protected @interface ServiceBindingsIteration { 
		String value() ; 
	}//EO inner class ServiceBindingProviderIterator

    protected static abstract class IterationInterceptor<T extends Annotation> implements TestRule { 
    	
    	//cache at a class level as a new instance is initialized for each test method
    	public static Map<Class<?>, Object> clsLevelMetadata = new HashMap<Class<?>, Object>() ;  

    	private Class<T> annotationType ; 
    	
    	public IterationInterceptor(final Class<T> annotationType) { 
    		this.annotationType = annotationType ; 
    	}//EOM 
    	
    	@Override
    	public Statement apply(final Statement base, final Description description) {
    		return new Statement() { 
    			
    			@Override
    			public final void evaluate() throws Throwable {
    				T metadata = description.getAnnotation(annotationType) ;
    				if(metadata == null) {  
    					final Object metadataTemp = clsLevelMetadata.get(annotationType) ; 
    					if(metadataTemp == null) { 
    						metadata = description.getTestClass().getAnnotation(annotationType) ;
    						clsLevelMetadata.put(annotationType, (metadata == null ? Boolean.FALSE : metadata)) ; 
    					}//EO if not yet initialized 
    					else if(!(metadataTemp instanceof Boolean)) metadata = (T) metadataTemp ; 
    				}//EO if cls level metadata was not already cached 
    				if(metadata == null) base.evaluate() ; 
    				else {
    					try{ 
	    					final int iIterationLength = getIterationLength(metadata) ; 
	    					
	    					for(int i=0; i<iIterationLength; i++) {
	    						
	    						doBeforeEvaluation(i, metadata) ; 
	    						
	    						base.evaluate() ; 
	    						
	    					}//EO while there are more platforms  
    					}finally{ 
    						
    					}//EO catch block 
    					
    				}//EO if the getresource annotation exists 
    			}//EOM 
    		};  
    	}//EOM 
    	
    	protected abstract int getIterationLength(final T metadata) ; 
    	
    	protected abstract void doBeforeEvaluation(final int iIterationIndex, final T metadata) ;
    		
    }//EO inner calss IterationInterceptor 
    
	public static final <T> T generateServiceClient(final Class<T> interfaceType, final ServiceBindingType enumServiceBindingType,
																			final String url, final ServletRunner servletRunner) { 
		
		final T serviceProxy = JAXRSClientFactory.create(url, interfaceType, enumServiceBindingType.getProviders());
        final Client client = WebClient.client(serviceProxy) ; 
        client.accept(enumServiceBindingType.getContentType()) ; 
        client.type(enumServiceBindingType.getContentType()) ; 
        
        final ClientConfiguration configuration = WebClient.getConfig(client) ; 
        configuration.getConduitSelector().getEndpoint().getEndpointInfo().setTransportId("test") ; 
        
        final ConduitInitiator conduitInitiator = new HTTPTransportFactory(configuration.getBus()) {
        	
        	public Conduit getConduit(EndpointInfo endpointInfo, EndpointReferenceType target) throws IOException {
        		final HTTPConduit delegate = (HTTPConduit) super.getConduit(endpointInfo, target) ;
        		return new TestHttpConduit(this.bus, delegate, endpointInfo, target, servletRunner) ; 
        	}//EOM 
        };//EO conduitInitiator
        
        ((ConduitInitiatorManager)configuration.getBus().getExtension(ConduitInitiatorManager.class)).registerConduitInitiator("test", conduitInitiator) ;
        return serviceProxy ; 
	}//EOM 
	
	
	protected final String getWADL(final Object serviceObject) throws IOException{
		final Client client = WebClient.client(serviceObject) ; 
		final WebClient webClient = WebClient.fromClient(client) ;  
		
		final ServiceBindingType enumServiceBindingType = ServiceBindingType.reverseValueOf(client.getHeaders().getFirst("Accept")) ; 
		
		final String sBindingType = enumServiceBindingType.getSimpleBindingType() ; 
		webClient.path(client.getCurrentURI()+ "?_wadl&_type=" + sBindingType) ;
    	final Response wadlResponse = webClient.get() ;
    	
    	String wadl = null ;
    	
    	final ByteArrayInputStream bais = (ByteArrayInputStream) wadlResponse.getEntity() ;
    	try{ 
    		final byte[] arrBytes = new byte[bais.available()] ;
    		bais.read(arrBytes) ; 
    		wadl = new String(arrBytes) ;
    		
    	}finally{ 
    		bais.close() ; 
    	}//EO catch block 
    	
    	return wadl ; 
	}//EOM 
    
    protected enum ServiceBindingType { 
    	
    	JSON(Arrays.asList(new Object[]{ new JacksonJsonProvider(), new JacksonJaxbJsonProvider()}), MediaType.APPLICATION_JSON_TYPE){ 
    	},//EO JSON 
    	XML(Arrays.asList(new Object[]{ new JAXBElementProvider()}), MediaType.APPLICATION_XML_TYPE){ 
    	};//EO XML 
    	
    	private static final Map<String,ServiceBindingType> reverseValues = new HashMap<String,ServiceBindingType>() ; 
    	
    	static{ 
    		for(ServiceBindingType enumServiceBindingType : values()) { 
    			reverseValues.put(enumServiceBindingType.getContentType().toString(), enumServiceBindingType) ; 
    		}//EO while there are more values 
    	}//EO static block 
    	
    	private List providers ;
    	private MediaType contentType ; 
    	
    	ServiceBindingType(final List providers, final MediaType contentType) { 
    		this.providers = providers ;  
    		this.contentType = contentType ; 
    	};//EOM 
    	
    	public final List getProviders() { return this.providers ; }//EOM 
    	public final MediaType getContentType() { return this.contentType ; }//EOM 
    	
    	public String getSimpleBindingType() { return this.contentType.getSubtype() ;}//EOM
    	
    	public static final ServiceBindingType reverseValueOf(final String contentType) { 
    		return reverseValues.get(contentType) ; 
    	}//EOM 
    	
    }//EO enum servicebindingType 
    
}//EOC 
