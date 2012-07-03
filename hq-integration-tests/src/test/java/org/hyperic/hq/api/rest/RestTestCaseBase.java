/*
 * NOTE: This copyright does *not* cover user programs that use HQ
 * program services by normal system calls through the application
 * program interfaces provided as part of the Hyperic Plug-in Development
 * Kit or the Hyperic Client Development Kit - this is merely considered
 * normal use of the program, and does *not* fall under the heading of
 * "derived work".
 * 
 * Copyright (C) [2004-2012], VMWare, Inc.
 * This file is part of HQ.
 * 
 * HQ is free software; you can redistribute it and/or modify
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
 */
package org.hyperic.hq.api.rest;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
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
import org.eclipse.jetty.http.HttpException;
import org.hyperic.hq.api.rest.RestTestCaseBase.SecurityInfo;
import org.hyperic.hq.api.rest.cxf.TestHttpConduit;
import org.hyperic.hq.tests.web.WebTestCaseBase;
import org.junit.rules.ExpectedException;
import org.junit.rules.RuleChain;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.annotation.DirtiesContext;

import com.meterware.servletunit.ServletRunner;

@SecurityInfo(username="hqadmin",password="hqadmin")
@DirtiesContext
public class RestTestCaseBase<V, T extends AbstractRestTestDataPopulator<V>> extends WebTestCaseBase{

	protected final ExpectedException errorInterceptor = ExpectedException.none(); 
    public RuleChain interceptorsChain = RuleChain.outerRule(errorInterceptor).
    		around(new ServiceBindingsIterationInterceptor(ServiceBindingsIteration.class)).
    		around(new SecurityInterceptor()) ; 
	
	@Autowired
	protected T testBed ; 
	
	protected V service ;
	
	@Retention(RetentionPolicy.RUNTIME) 
	@Target({ElementType.METHOD, ElementType.TYPE})
	public @interface ServiceBindingsIteration { 
		String value() ; 
	}//EO inner class ServiceBindingProviderIterator
	
	@Retention(RetentionPolicy.RUNTIME) 
	@Target({ElementType.METHOD, ElementType.TYPE})
	public @interface SecurityInfo { 
		boolean ignore()  default false ;
		String username() default "hqadmin" ; 
		String password() default "hqadmin"  ;
		
	}//EO inner class SecurityInfo 
	
	protected final class SecurityInterceptor extends IterationInterceptor<SecurityInfo>{ 
		
		public SecurityInterceptor() { 
			super(SecurityInfo.class) ; 
		}//EOM 
		
		protected boolean shouldSkip(final SecurityInfo metadata) { 
			return metadata.ignore() ;  
		}//EOM
		
		@Override
		protected final void doBeforeEvaluation(final int iIterationIndex, final SecurityInfo metadata) {
			final Client client =  WebClient.client(service) ; 
			final String credentialsToken = metadata.username() + ":" + metadata.password() ; 
			final String authorizationHeader = "Basic " +org.apache.cxf.common.util.Base64Utility.encode(credentialsToken.getBytes());			
			client.header(HttpHeaders.AUTHORIZATION, authorizationHeader);  
		}//EOM 
		
		@Override
		protected final void doAfterEvaluation(final int iIterationIndex, final SecurityInfo metadata) {
		    final Client client =  WebClient.client(service) ;
		    client.reset() ; 
		}//EOM 
		
		@Override
		protected final int getIterationLength(final SecurityInfo metadata) {
			return 1 ; 
		}//EOM 
		
	}//EO inner class SecurityInterceptor
	
	
	protected final class ServiceBindingsIterationInterceptor extends IterationInterceptor<ServiceBindingsIteration> { 
    	
    	public ServiceBindingsIterationInterceptor(final Class<ServiceBindingsIteration> serviceBindingsIterationType) { 
    		super(serviceBindingsIterationType) ; 
    	}//EOM 
    	
    	 @Override
    	protected final void doBeforeEvaluation(final int iIterationIndex, final ServiceBindingsIteration metadata) {
    		service = testBed.getServices()[iIterationIndex] ;
    		final Client client =  WebClient.client(service) ;
    		WebClient.getConfig(client).getConduitSelector().getEndpoint().getEndpointInfo().setAddress(metadata.value()) ; 
    	}//EOM 
    	
    	 @Override
    	protected final int getIterationLength(final ServiceBindingsIteration metadata) {
    		return testBed.getServices().length ; 
    	}//EOM 
    }//EO inner class PlatformsIterationInterceptor
    
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
    	if(bais == null) { 
    		throw new HttpException(wadlResponse.getStatus()) ; 
    	}//EO if no wadl response was sent 
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
