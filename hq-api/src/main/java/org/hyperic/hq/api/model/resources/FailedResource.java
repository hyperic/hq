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

import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;

import org.hyperic.hq.api.model.RestApiConstants;

@XmlRootElement(name="FailedResource", namespace=RestApiConstants.SCHEMA_NAMESPACE)
@XmlType(name="FailedResourceType", namespace=RestApiConstants.SCHEMA_NAMESPACE)
public class FailedResource {

    String resourceId;
    String errorCode;
    String errorDescription;
    

    public FailedResource() {    }       
    
    public FailedResource(String resourceId, String errorCode, String errorDescription) {
        this.resourceId = resourceId;
        this.errorCode = errorCode;
        this.errorDescription = errorDescription;
    }

    public String getResourceId() {
        return resourceId;
    }
    public void setResourceId(String id) {
        this.resourceId = id;
    }
    public String getErrorCode() {
        return errorCode;
    }
    public void setErrorCode(String errorCode) {
        this.errorCode = errorCode;
    }
    public String getErrorDescription() {
        return errorDescription;
    }
    public void setErrorDescription(String errorDescription) {
        this.errorDescription = errorDescription;
    }

    @Override
    public final String toString() {
        return "FailedResource [resourceId=" + resourceId + ", errorCode=" + errorCode + ", errorDescription="
                + errorDescription + "]";
    }//EOM 
    
     
}
