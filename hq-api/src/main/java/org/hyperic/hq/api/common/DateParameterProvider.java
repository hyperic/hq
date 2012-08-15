package org.hyperic.hq.api.common;

import java.util.Date;

import javax.ws.rs.core.Response;
import javax.ws.rs.ext.Provider;

import org.apache.cxf.jaxrs.ext.ParameterHandler;
import org.hyperic.hq.api.transfer.mapping.ExceptionToErrorCodeMapper;
import org.joda.time.format.DateTimeFormatter;
import org.joda.time.format.ISODateTimeFormat;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Supports the passing of a date in a Date object format through rest API
 * The date passed should be compliant with the ISO-8601 time format, otherwise an exception would be thrown.
 * 
 * @author yakarn
 *
 */
@Provider
public class DateParameterProvider implements ParameterHandler<Date> {
    @Autowired
    private ExceptionToErrorCodeMapper errorHandler ; 
 
    public Date fromString(String timeStr) {
        final DateTimeFormatter dateFormat = ISODateTimeFormat.dateTimeParser() ;
        long timeMilli;
        try {
            timeMilli = dateFormat.parseMillis(timeStr) ; 
        } catch (IllegalArgumentException e) {
            throw errorHandler.newWebApplicationException(Response.Status.BAD_REQUEST, ExceptionToErrorCodeMapper.ErrorCode.WRONG_DATE_FORMAT, e.getMessage());
        }
        Date timeDate = new Date(timeMilli);
        return timeDate;
    }
}