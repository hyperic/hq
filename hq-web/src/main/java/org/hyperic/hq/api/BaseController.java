package org.hyperic.hq.api;

import java.io.EOFException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hyperic.hq.api.representation.ErrorResponse;
import org.neo4j.graphdb.NotFoundException;
import org.springframework.http.HttpStatus;
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
	public @ResponseBody ErrorResponse handleContentMediaTypeUnsupported(Exception e) {
		return new ErrorResponse(e);
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
	public @ResponseBody ErrorResponse handleMethodUnsupported(Exception e) {
		return new ErrorResponse(e);
	}
	
	// handle case where http method is not implemented
	@ExceptionHandler({NoSuchMethodException.class})
	@ResponseStatus(value = HttpStatus.NOT_IMPLEMENTED)
	public @ResponseBody ErrorResponse handleMethodNotImplemented(Exception e) {
		return new ErrorResponse(e);
	}

	// handle case where request body or parameters are missing or invalid
	@ExceptionHandler({MissingServletRequestParameterException.class, EOFException.class})
	@ResponseStatus(value = HttpStatus.BAD_REQUEST)
	public @ResponseBody ErrorResponse handleBadRequest(Exception e) {
		return new ErrorResponse(e);
	}
	
	// handle all other cases where an exception is thrown
	@ExceptionHandler({Exception.class})
	@ResponseStatus(value = HttpStatus.INTERNAL_SERVER_ERROR)
	public @ResponseBody ErrorResponse handleServerError(Exception e) {
		return new ErrorResponse(e);
	}
}