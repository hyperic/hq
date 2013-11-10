/* **********************************************************************
/*
 * NOTE: This copyright does *not* cover user programs that use Hyperic
 * program services by normal system calls through the application
 * program interfaces provided as part of the Hyperic Plug-in Development
 * Kit or the Hyperic Client Development Kit - this is merely considered
 * normal use of the program, and does *not* fall under the heading of
 * "derived work".
 *
 * Copyright (C) [2004-2010], VMware, Inc.
 * This file is part of Hyperic.
 *
 * Hyperic is free software; you can redistribute it and/or modify
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
package org.hyperic.hq.api.transfer.mapping;

import java.util.Locale;
import java.util.Map;
import java.util.MissingResourceException;
import java.util.ResourceBundle;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response;

import org.apache.commons.logging.Log;
import org.apache.cxf.jaxrs.impl.ResponseBuilderImpl;
import org.hyperic.hq.api.model.resources.FailedResource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;


public class ExceptionToErrorCodeMapper  {
    private static final String DEFAULT_EXCEPTION = "default";
    
    private static final ResourceBundle errorCodesBundle = ResourceBundle.getBundle("org.hyperic.hq.api.transfer.mapping.APIErrorCodes", Locale.ENGLISH) ; 
    
    private Map<String,String> exceptionToErrorCodeMap;

    @Autowired
    @Qualifier("restApiLogger")
    private Log logger;    		
    public Log getLogger() {
        return logger;
    }    
    public void setLogger(Log logger) {
        this.logger = logger;
    }     
    
    public Map<String,String> getExceptionToErrorCodeMap() {
        return exceptionToErrorCodeMap;
    }
    
    public void setExceptionToErrorCodeMap(Map<String,String> exceptionToErrorCodeMap) {
        this.exceptionToErrorCodeMap = exceptionToErrorCodeMap;
    }    

    /* (non-Javadoc)
	 * @see org.hyperic.hq.api.transfer.mapping.IExcpetionToErrorCodeMapper#getErrorCode(java.lang.Exception)
	 */
    public String getErrorCode(Throwable exc) {
        if (null == exc) {
            logger.warn("No error code for null exception");
            return null;
        }
        String errorCode = exceptionToErrorCodeMap.get(exc.getClass().getName());
        if (null == errorCode) {
            errorCode = exceptionToErrorCodeMap.get(DEFAULT_EXCEPTION);
        }
        return errorCode;
    }
    
    public final String getDescription(final String errorCode, final Object...args) { 
    	String description = null ;  
    	try{ 
    		description = errorCodesBundle.getString(errorCode) ;
    		if ((description == null) || description.isEmpty()) {
    		    return null;
    		}
    		if ((args==null) || (args.length==0)) {
    		    return description.replace("%s", "");
    		}
    		return String.format(description, args); 
    	}catch(MissingResourceException mre) {}//EO catch block
    	
    	return null ; 
    }//EOM 
    
    public final FailedResource newFailedResource(final Throwable t, final String resourceID, final String additionalDescription, final Object...args) {
    	return this.newFailedResource(resourceID, this.getErrorCode(t), additionalDescription, args)  ; 
    }//EOM
    
    public final FailedResource newFailedResource(final String resourceID, final String errorCode, String additionalDescription, final Object...args) {
    	if(additionalDescription == null) {
            additionalDescription =  this.getDescription(errorCode, args) ;
        } 
    	return new FailedResource(resourceID, errorCode, additionalDescription) ;
    }//EOM 
      
    
    public final WebApplicationException newWebApplicationException(final Throwable e, final Response.Status status, final ErrorCode errorCode, 
            final Object...errorArguments) {
        ResponseBuilderImpl builder = new ResponseBuilderImpl();
        builder.status(status);
        builder.entity(getDescription(errorCode.getErrorCode(), errorArguments));
        Response response = builder.build();

        WebApplicationException webApplicationException = new WebApplicationException(e, response);
        return webApplicationException;
    }       
    
    public final WebApplicationException newWebApplicationException(final Response.Status status, final ErrorCode errorCode, final Object...errorArguments) {
        return newWebApplicationException(null, status, errorCode, errorArguments);
    }        
    
  
    
    public final void log(final Throwable t) {
    	this.log(t, null/*additionalMessage*/) ; 
    }//EOM 
    
    public final void log(final Throwable t, final String additionalMessage) { 
    	this.logger.error(additionalMessage, t) ; 
    }//EOM 
    
    public enum ErrorCode {
        
        RESOURCE_NOT_FOUND_BY_ID("1001"),
        DUPLICATE_RESOURCE_ID("1002"),
        INVALID_SESSION("2001"),
        SESSION_TIMEOUT("2002"),
        NON_ADMIN_ERR("2003"),
        TEMPLATE_NOT_FOUND("3002"),
        MEASUREMENT_NOT_FOUND("3003"),
        WRONG_DATE_FORMAT("4001"),
        WRONG_DATE_VALUES("4002"),
        BAD_MEASUREMENT_REQ("5001"),
        BAD_REQ_BODY("5002"),
        UPDATE_FAILURE("5003"),
        CLOUD_PROVIDER_NOT_CONFIGURED("6001"),
        BAD_CLOUD_PROVIDER_CONFIGURATION("6002"),
        CLOUD_RESOURCE_NOT_FOUND("6003"),
        SEQUENTIAL_REGISTRATION("7000"),
        MISSING_MANDATORY_FILTER("7001"),
        UNREGISTERED_FOR_NOTIFICATIONS("7002"),
        MISSING_VCENTER_MANDATORY_PROPERTY("8001"),
        CANNOT_UPDATE_SERVER_GUID("8002"),
        VC_CONNECTION_ALREADY_EXISTS("8003"),
        NO_VC_CONNECTION_EXISTS("8004"), 
        UNKNOWN_ENDPOINT("8005"),
        CANNOT_VERIFY_LDAP_SETTINGS("8006"),
        CANNOT_READ_LDAP_SETTINGS("8007"),
        MISSING_LDAP_MANDATORY_PROPERTY("8008"),
        LDAP_CONNECTION_ALREADY_EXISTS("8009"),
        WRONG_CONFIG_TYPE("8010"),
        NO_LDAP_CONNECTION_EXISTS("8011"),
        ILLEGAL_FILTER("8100"), 
        ILLEGAL_FILTER_COMPARATOR("8101"), 
        ILLEGAL_AGG_FILTER("8102"),
        INTERNAL_SERVER_ERROR("9000"),
        REGEX_PATTERN_SYNTAX_ERROR("9001"),
        MULTIPLE_POLICIES_TYPES_MAPPED("9002"),
        POLICY_NOT_FOUND_BY_ID("9003"), 
        FAILED_TO_FETCH_CONFIGURATION("9505"), 
        ALERT_CREATION_ERROR("9506"), 
        PLUGIN_ERROR("9507"), 
        USER_NOT_AUTHORIZED_POLICY("9508"),
        GROUP_NOT_FOUND_BY_ID("9509"),
        USER_NOT_AUTHORIZED_MODIFY_ROLE("9510"),
        POLICY_CREATE_ERROR("9511"),
        USER_NOT_AUTHORIZED_GROUP("9512"),
        ROLE_NOT_FOUND_BY_ID("9513"),
        USER_NOT_FOUND_BY_NAME("9514"),
        AVAILABILITY_METRICS_INVALID("9515");
        
         
        private final String errorCode;

        // Constructor
        ErrorCode(String errorCode) {
            this.errorCode = errorCode;
        }

        public String getErrorCode() {
            return errorCode;
        }
    }


}
