package org.hyperic.hq.api;

import java.io.EOFException;
import java.lang.reflect.Method;

import javax.servlet.http.HttpServletRequest;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.converter.json.MappingJacksonHttpMessageConverter;
import org.springframework.http.server.ServletServerHttpRequest;
import org.springframework.util.ReflectionUtils;
import org.springframework.util.StringUtils;
import org.springframework.web.HttpMediaTypeNotAcceptableException;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.ResponseStatus;

public abstract class BaseController {
	// log available to subclasses
	protected final Log log = LogFactory.getLog(this.getClass().getName());
	
	// handle case where content-type header value is not supported
	@ExceptionHandler(HttpMediaTypeNotSupportedException.class)
	@ResponseStatus(value = HttpStatus.UNSUPPORTED_MEDIA_TYPE)
	public @ResponseBody ErrorRepresentation handleContentMediaTypeUnsupported(Exception e) {
		return new ErrorRepresentation(e);
	}

	// handle case where content-type header value is not supported
	@ExceptionHandler(HttpMediaTypeNotAcceptableException.class)
	@ResponseStatus(value = HttpStatus.NOT_ACCEPTABLE)
	public void handleAcceptableMediaTypeUnsupported(Exception e) {
		if (log.isWarnEnabled()) {
			log.warn("Accept media type is not supported");
		}
	}
	
	// handle case where http method is not supported
	@ExceptionHandler({HttpRequestMethodNotSupportedException.class})
	@ResponseStatus(value = HttpStatus.METHOD_NOT_ALLOWED)
	public @ResponseBody ErrorRepresentation handleMethodUnsupported(Exception e) {
		return new ErrorRepresentation(e);
	}
	
	// handle case where http method is not implemented
	@ExceptionHandler({NoSuchMethodException.class})
	@ResponseStatus(value = HttpStatus.NOT_IMPLEMENTED)
	public @ResponseBody ErrorRepresentation handleMethodNotImplemented(Exception e) {
		return new ErrorRepresentation(e);
	}

	// handle case where request body or parameters are missing or invalid
	@ExceptionHandler({MissingServletRequestParameterException.class, EOFException.class})
	@ResponseStatus(value = HttpStatus.BAD_REQUEST)
	public @ResponseBody ErrorRepresentation handleBadRequest(Exception e) {
		return new ErrorRepresentation(e);
	}
	
	// handle all other cases where an exception is thrown
	@ExceptionHandler({Exception.class})
	@ResponseStatus(value = HttpStatus.INTERNAL_SERVER_ERROR)
	public @ResponseBody ErrorRepresentation handleServerError(Exception e) {
		return new ErrorRepresentation(e);
	}
	
	protected Representation executeRead(String domainName, String methodName, Object... parameters) throws Exception {
		Domain domain = Domain.getValue(domainName);
		Class<?> clazz = domain.javaType();
		Class<?>[] paramTypes = new Class<?>[parameters.length];
		
		for (int x = 0; x < parameters.length; x++) {
			paramTypes[x] = parameters[x].getClass();
		}

		Method method = ReflectionUtils.findMethod(clazz, methodName, paramTypes);
		
		if (method == null) {
			throw new NoSuchMethodException("Method '" + methodName + "(" + StringUtils.arrayToCommaDelimitedString(paramTypes) + ")' not found for class [" + clazz.getName() + "]");
		}
		
		Object data = method.invoke(null, parameters);
		
		return new Representation(data, domainName);
	}
	
	protected Representation executeCreate(String domainName, HttpServletRequest request) throws Exception {
		return executeWrite(domainName, null, request, Operation.CREATE);
	}
	
	protected Representation executeUpdate(String domainName, Long id, HttpServletRequest request) throws Exception {
		return executeWrite(domainName, id, request, Operation.UPDATE);
	}
	
	protected void executeDelete(String domainName, Long id) throws Exception {
		executeWrite(domainName, id, null, Operation.DELETE);
	}

	private Representation executeWrite(String domainName, Long id, final HttpServletRequest request, Operation operation) throws Exception {
		Domain domain = Domain.getValue(domainName);
		Representation result = null;
		Class<?> clazz = domain.javaType();
		MappingJacksonHttpMessageConverter converter = new MappingJacksonHttpMessageConverter();
		Object target = clazz.newInstance();
		
		if (request != null) {
			ServletServerHttpRequest wrappedRequest = new ServletServerHttpRequest(request);
			
			if (!converter.canRead(clazz, wrappedRequest.getHeaders().getContentType())) {
				throw new HttpMediaTypeNotSupportedException("Media type not supported");
			}
			
			target = converter.read(clazz, wrappedRequest);
		}
		
		Entity entity = (Entity) target;
			
		entity.setId(id);
			
		switch (operation) {
			case CREATE:
				entity.create();
				
				result = new Representation(target, domainName);
				break;
			case UPDATE:
				entity.update();
					
				result = new Representation(target, domainName);
				break;
			case DELETE:
				entity.delete();
		}
		
		return result;
	}
	
	private enum Operation {
		CREATE, UPDATE, DELETE;
	}
}