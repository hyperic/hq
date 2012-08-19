package org.hyperic.hq.api.common;

import java.util.Date;

import javax.ws.rs.core.Response;
import javax.ws.rs.ext.Provider;

import org.apache.commons.logging.Log;
import org.apache.cxf.jaxrs.ext.ParameterHandler;
import org.hyperic.hq.api.transfer.mapping.ExceptionToErrorCodeMapper;
import org.joda.time.format.DateTimeFormatter;
import org.joda.time.format.ISODateTimeFormat;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;

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
 
    @Autowired
    @Qualifier("restApiLogger")
    Log logger;    

    /**
     * @throws WebApplicationException throws a web app exception when a time with a format which is uncompliant with ISO-8601 is given
     */
    public Date fromString(String timeStr) {
        final DateTimeFormatter dateFormat = ISODateTimeFormat.dateTimeParser() ;
        long timeMilli;
        try {
            timeMilli = dateFormat.parseMillis(timeStr) ; 
        } catch (IllegalArgumentException e) {
            errorHandler.log(e);
            throw errorHandler.newWebApplicationException(e, Response.Status.BAD_REQUEST, ExceptionToErrorCodeMapper.ErrorCode.WRONG_DATE_FORMAT);
        }
        Date timeDate = new Date(timeMilli);
        return timeDate;
    }
}