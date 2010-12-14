package org.hyperic.hq.api;

import java.io.EOFException;
import java.lang.reflect.Method;

import javax.servlet.http.HttpServletRequest;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hyperic.hq.inventory.domain.IdentityAware;
import org.hyperic.hq.inventory.domain.PersistenceAware;
import org.hyperic.hq.inventory.domain.RelationshipAware;
import org.hyperic.hq.reference.RelationshipDirection;
import org.neo4j.graphdb.NotFoundException;
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
	
	// handle case where object is not found
	@ExceptionHandler(NotFoundException.class)
	@ResponseStatus(value = HttpStatus.NOT_FOUND)
	public void handleObjectNotFound(Exception e) {
		if (log.isDebugEnabled()) {
			log.debug("Requested object not found.");
		}
	}
	
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
	
	protected Representation readSingleEntity(String domainName, Long id) throws Exception {
		return executeRead(domainName, "findById", id);
	}
	
	protected Representation readMultipleEntities(String domainName, ListSettings listSettings) throws Exception {
		return executeRead(domainName, "find", listSettings.getPage(), listSettings.getSize());
	}
	
	protected Representation readRelationships(String domainName, Long id, ListSettings listSettings) throws Exception {
		return readRelationships(domainName, id, null, null, listSettings);
	}
	
	protected Representation readRelationships(String domainName, Long id, String relationshipName, String direction, ListSettings listSettings) throws Exception {
		Method method = findMethod(domainName, "findById", Long.class);
		Object target = method.invoke(null, id);
		
		if (target == null) {
			throw new Exception("not found");
		}
		
		if (!(target instanceof RelationshipAware<?>)) {
			throw new Exception("not relationship aware");
		}
		
		RelationshipDirection dir = RelationshipDirection.ALL;
		
		if (RelationshipDirection.INCOMING.toString().equalsIgnoreCase(direction)) {
			dir = RelationshipDirection.INCOMING;
		} else if (RelationshipDirection.OUTGOING.toString().equalsIgnoreCase(direction)) {
			dir = RelationshipDirection.OUTGOING;
		} else if (RelationshipDirection.BOTH_WAYS.toString().equalsIgnoreCase(direction)) {
			dir = RelationshipDirection.BOTH_WAYS;
		}
		
		Object data = ((RelationshipAware<?>) target).getRelationships(null, relationshipName, dir);
		
		return new Representation(data, domainName);
	}
	
	private Representation executeRead(String domainName, String methodName, Object... parameters) throws Exception {
		Class<?>[] paramTypes = new Class<?>[parameters.length];
		
		for (int x = 0; x < parameters.length; x++) {
			paramTypes[x] = parameters[x].getClass();
		}
		
		Method method = findMethod(domainName, methodName, paramTypes);
		Object data = method.invoke(null, parameters);
		
		return new Representation(data, domainName);
	}
	
	private Method findMethod(String domainName, String methodName, Class<?>... paramTypes) throws Exception {
		Class<?> clazz = Domain.getValue(domainName).javaType();
		Method method = ReflectionUtils.findMethod(clazz, methodName, paramTypes);
		
		if (method == null) {
			throw new NoSuchMethodException("Method '" + methodName + "(" + StringUtils.arrayToCommaDelimitedString(paramTypes) + ")' not found for class [" + clazz.getName() + "]");
		}
		
		return method;
	}
	
	protected Representation createSingleEntity(String domainName, HttpServletRequest request) throws Exception {
		return executeWrite(domainName, null, request, Operation.CREATE);
	}
	
	protected Representation updateSingleEntity(String domainName, Long id, HttpServletRequest request) throws Exception {
		return executeWrite(domainName, id, request, Operation.UPDATE);
	}
	
	protected void deleteSingleEntity(String domainName, Long id) throws Exception {
		executeWrite(domainName, id, null, Operation.DELETE);
	}

	protected Representation createRelationship(String domainName, Long fromId, Long toId, String relationshipName) throws Exception {
		return null;
	}
	
	protected void deleteRelationship(String domainName, Long fromId, Long toId, String direction, String relationshipName) throws Exception {
		Class<?> clazz = Domain.getValue(domainName).javaType();
		Method findById = findMethod(domainName, "findById", Long.class);
		Object from = findById.invoke(null, fromId);
		
		if (from instanceof RelationshipAware) {
			Object to = findById.invoke(null, toId);
			RelationshipDirection dir = RelationshipDirection.ALL;
			
			if (RelationshipDirection.INCOMING.toString().equalsIgnoreCase(direction)) {
				dir = RelationshipDirection.INCOMING;
			} else if (RelationshipDirection.OUTGOING.toString().equalsIgnoreCase(direction)) {
				dir = RelationshipDirection.OUTGOING;
			} else if (RelationshipDirection.BOTH_WAYS.toString().equalsIgnoreCase(direction)) {
				dir = RelationshipDirection.BOTH_WAYS;
			}
		
			Method remove = ReflectionUtils.findMethod(RelationshipAware.class, "removeRelationships", clazz, String.class, RelationshipDirection.class);
			
			remove.invoke(from, to, relationshipName, dir);
		}
	}
	
	private Representation executeWrite(String domainName, Long id, final HttpServletRequest request, Operation operation) throws Exception {
		Representation result = null;
		Class<?> clazz = Domain.getValue(domainName).javaType();
		MappingJacksonHttpMessageConverter converter = new MappingJacksonHttpMessageConverter();
		Object target = clazz.newInstance();
		
		if (request != null) {
			ServletServerHttpRequest wrappedRequest = new ServletServerHttpRequest(request);
			
			if (!converter.canRead(clazz, wrappedRequest.getHeaders().getContentType())) {
				throw new HttpMediaTypeNotSupportedException("Media type not supported");
			}
			
			target = converter.read(clazz, wrappedRequest);
		}
		
		((IdentityAware) target).setId(id);
		PersistenceAware<?> entity = (PersistenceAware<?>) target;
			
		switch (operation) {
			case CREATE:
				entity.persist();
				result = new Representation(target, domainName);
				break;
			case UPDATE:
				target = entity.merge();
				result = new Representation(target, domainName);
				break;
			case DELETE:
				entity.remove();
		}
		
		return result;
	}
	
	private enum Operation {
		CREATE, UPDATE, DELETE;
	}
}