package org.hyperic.hq.api.transfer.mapping;

import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.Provider;

import org.apache.cxf.jaxrs.impl.ResponseBuilderImpl;
import org.hyperic.hq.auth.shared.SessionTimeoutException;
import org.hyperic.hq.context.Bootstrap;

@Provider
public class SessionTimeoutExceptionExceptionMapper implements ExceptionMapper<SessionTimeoutException> {

    
    private ExceptionToErrorCodeMapper errorHandler;

    public Response toResponse(SessionTimeoutException exception) {
        ResponseBuilderImpl builder = new ResponseBuilderImpl();
        builder.status(Response.Status.UNAUTHORIZED); // FORBIDDEN?
        String errorCode = ExceptionToErrorCodeMapper.ErrorCode.SESSION_TIMEOUT.getErrorCode();
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
