package org.hyperic.hq.api;

import static org.junit.Assert.*;

import java.util.HashMap;
import java.util.Map;

import org.junit.Before;
import org.junit.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.json.MappingJacksonHttpMessageConverter;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.servlet.mvc.annotation.AnnotationMethodHandlerAdapter;
import org.springframework.web.servlet.mvc.annotation.AnnotationMethodHandlerExceptionResolver;

public class ApiControllerTest {
	private MockHttpServletRequest request;
	private MockHttpServletResponse response;
	private ApiController controller;
	private AnnotationMethodHandlerAdapter handler;
	private AnnotationMethodHandlerExceptionResolver exHandler;
	
	@Before
	public void setup() {
		request = new MockHttpServletRequest();
		response = new MockHttpServletResponse();
		controller = new ApiController();
		handler = new AnnotationMethodHandlerAdapter();
		exHandler = new AnnotationMethodHandlerExceptionResolver();
		
		HttpMessageConverter<?>[] converters = new HttpMessageConverter<?>[handler.getMessageConverters().length + 1];
	
		for (int x = 0; x < handler.getMessageConverters().length; x++) {
			converters[x] = handler.getMessageConverters()[x];
		}
		
		converters[converters.length - 1] = new MappingJacksonHttpMessageConverter();
		
		handler.setMessageConverters(converters);
		exHandler.setMessageConverters(converters);
	}
	
	@Test
	public void testGetByIdSuccessJson() throws Exception {
		Map<String, String> headers = new HashMap<String, String>();
		
		headers.put("Accept", "application/json");
		
		testRequest("/api/resources/1", RequestMethod.GET, headers);
		
		assertEquals(HttpStatus.OK, HttpStatus.valueOf(response.getStatus()));
	}
	
	@Test
	public void testNotFound() {
		
	}
	
	@Test
	public void testBadRequest() {
		try {
			Map<String, String> headers = new HashMap<String, String>();
			
			headers.put("Content-Type", "application/json");
			
			testRequest("/api/resources", RequestMethod.POST, headers);
		} catch(Exception e) {
			exHandler.resolveException(request, response, controller, e);
		}
		
		assertEquals(HttpStatus.BAD_REQUEST, HttpStatus.valueOf(response.getStatus()));
	}
	
	@Test
	public void testUnsupportedContentMediaType() {
		try {
			Map<String, String> headers = new HashMap<String, String>();
			
			headers.put("Content-Type", "application/dummy");
			
			testRequest("/api/resources/1", RequestMethod.PUT, headers, "{some content}");
		} catch(Exception e) {
			exHandler.resolveException(request, response, controller, e);
		}
		
		assertEquals(HttpStatus.UNSUPPORTED_MEDIA_TYPE, HttpStatus.valueOf(response.getStatus()));
	}
	
	@Test
	public void testUnsupportedAcceptMediaType() {
		try {
			Map<String, String> headers = new HashMap<String, String>();
			
			headers.put("Accept", "application/dummy");

			testRequest("/api/resources", RequestMethod.GET, headers);
		} catch(Exception e) {
			exHandler.resolveException(request, response, controller, e);
		}
		
		assertEquals(HttpStatus.NOT_ACCEPTABLE, HttpStatus.valueOf(response.getStatus()));
	}
	
	@Test
	public void testUnsupportedMethod() {
		try {
			Map<String, String> headers = new HashMap<String, String>();
			
			testRequest("/api/resources", RequestMethod.DELETE, headers);
		} catch(Exception e) {
			exHandler.resolveException(request, response, controller, e);
		}
		
		assertEquals(HttpStatus.METHOD_NOT_ALLOWED, HttpStatus.valueOf(response.getStatus()));
	}

	protected Object testRequest(String uri, RequestMethod method, Map<String, String> headers) throws Exception {
		return testRequest(uri, method, headers, null);
	}
	
	protected Object testRequest(String uri, RequestMethod method, Map<String, String> headers, String body) throws Exception {
		request.setRequestURI(uri);
		request.setMethod(method.toString());

		for (Map.Entry<String, String> entry : headers.entrySet()) {
			request.addHeader(entry.getKey(), entry.getValue());
		}
		
		if (body != null) {
			request.setContent(body.getBytes());
		}
		
		return handler.handle(request, response, controller);	
	}
}