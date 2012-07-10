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
package org.hyperic.hq.api.model.resources;

import java.util.ArrayList;
import java.util.List;

import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;

import org.hyperic.hq.api.model.RestApiConstants;
import org.hyperic.hq.api.transfer.mapping.ExceptionToErrorCodeMapper;

@XmlRootElement(name="ResponseBase", namespace=RestApiConstants.SCHEMA_NAMESPACE)
@XmlType(name="ResponseBaseType", namespace=RestApiConstants.SCHEMA_NAMESPACE)
public class BatchResponseBase {

    private List<FailedResource> failedResources;
    
    //@Transient
    private ExceptionToErrorCodeMapper errorHandler ;  


    public BatchResponseBase() {   }
    
    public BatchResponseBase(final ExceptionToErrorCodeMapper exceptionToErrorCodeMapper) { 
    	this.errorHandler = exceptionToErrorCodeMapper ; 
    }
    
    public BatchResponseBase(List<FailedResource> failedResources) {
        this.failedResources = failedResources;
    }
    

    public List<FailedResource> getFailedResources() {
        return failedResources;
    }

    public void setFailedResources(List<FailedResource> failedResources) {
        this.failedResources = failedResources;
    }
    

    public final void addFailedResource(final Throwable t, final String resourceID, final String additionalDescription, Object...args) {
    	final String errorCode = this.errorHandler.getErrorCode(t) ; 
    	this.addFailedResource(resourceID, errorCode, additionalDescription, args) ; 
    }//EOM
    
    public final void addFailedResource(final String resourceID, final String errorCode, final String additionalDescription, Object...args) { 
    	if(this.failedResources == null) this.failedResources = new ArrayList<FailedResource>()  ; 
    	final FailedResource failedResouce = this.errorHandler.newFailedResource(resourceID, errorCode, additionalDescription, args) ; 
    	this.failedResources.add(failedResouce) ; 
    }//EOM 

}