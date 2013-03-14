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
import java.util.Map;
import java.util.Map.Entry;

import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;

import org.apache.commons.lang.NullArgumentException;
import org.hyperic.hq.api.model.ResourceModel;
import org.hyperic.hq.api.model.RestApiConstants;
import org.hyperic.hq.api.transfer.mapping.ExceptionToErrorCodeMapper;
import org.hyperic.hq.appdef.shared.BatchResponse;

@XmlRootElement(name="ResourceResponse", namespace=RestApiConstants.SCHEMA_NAMESPACE)
@XmlType(name="ResourceResponseType", namespace=RestApiConstants.SCHEMA_NAMESPACE)
public class ResourceBatchResponse extends BatchResponseBase {
    private List<ResourceModel> resources;
    
    
    public ResourceBatchResponse() {    }    

    public ResourceBatchResponse(List<ResourceModel> resourcesAddedToInventory, List<FailedResource> failedResources) {
        super(failedResources);
        this.resources = resourcesAddedToInventory;
    }
    
    public ResourceBatchResponse(final ExceptionToErrorCodeMapper exceptionToErrorCodeMapper) { 
    	super(exceptionToErrorCodeMapper) ; 
    }//EOM 
    
    public ResourceBatchResponse(BatchResponse<ResourceModel> batchResponse, ExceptionToErrorCodeMapper exceptionToErrorCodeMapper) {
        if (null == exceptionToErrorCodeMapper) {
            throw new NullArgumentException("exceptionToErrorCodeMap");
        }
        if (null != batchResponse) {
            if (null != batchResponse.getResponse()) {
            	resources = batchResponse.getResponse();
            }  
            
            Map<String,Exception> failedIds = batchResponse.getFailedIds(); 
            if (null != failedIds) {
                List<FailedResource> failedResources = new ArrayList<FailedResource>(failedIds.size());
                for (Entry<String,Exception> failedIdException : failedIds.entrySet()) {
                    Exception exception = failedIdException.getValue();
                    String resourceId = failedIdException.getKey();
                    failedResources.add(new FailedResource(resourceId, exceptionToErrorCodeMapper.getErrorCode(exception), exception.getMessage()));
                }
                super.setFailedResources(failedResources);
            }                                
        }
    }
    
    public List<ResourceModel> getResources() {
        return resources;
    }

    public void setResources(List<ResourceModel> resourcesAddedToInventory) {
        this.resources = resourcesAddedToInventory;
    }
    
}
