package org.hyperic.hq.api.transfer.mapping;

import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.Provider;

import org.apache.cxf.jaxrs.impl.ResponseBuilderImpl;
import org.hyperic.hq.auth.shared.SessionNotFoundException;
import org.hyperic.hq.context.Bootstrap;

@Provider
public class SessionNotFoundExceptionMapper implements ExceptionMapper<SessionNotFoundException> {
    
    private ExceptionToErrorCodeMapper errorHandler;

    public Response toResponse(SessionNotFoundException exception) {
        ResponseBuilderImpl builder = new ResponseBuilderImpl();
        builder.status(Response.Status.UNAUTHORIZED); // FORBIDDEN?
        String errorCode = ExceptionToErrorCodeMapper.ErrorCode.INVALID_SESSION.getErrorCode();
        builder.entity(Bootstrap.getBean(ExceptionToErrorCodeMapper.class).getDescription(errorCode));
        Response response = builder.build();        
        return response;
    }

    public ExceptionToErrorCodeMapper getErrorHandler() {
        return errorHandler;
    }

    public void setErrorHandler(ExceptionToErrorCodeMapper errorHandler) {
        this.errorHandler = errorHandler;
    }

}



