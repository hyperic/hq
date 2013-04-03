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
package org.hyperic.hq.api.model;

import javax.xml.bind.annotation.XmlEnum;
import javax.xml.bind.annotation.XmlType;

import org.hyperic.hq.notifications.model.InternalResourceDetailsType;

@XmlType(namespace=RestApiConstants.SCHEMA_NAMESPACE)
@XmlEnum
public enum ResourceDetailsType {
	BASIC,
	PROPERTIES,
	VIRTUALDATA,
	ALL;
	
	public static ResourceDetailsType valueOf(InternalResourceDetailsType internalResourceDetailsType) {
	    ResourceDetailsType resourceDetailsType=null;
	    if (internalResourceDetailsType!=null) {
            if (internalResourceDetailsType==InternalResourceDetailsType.BASIC) {
                resourceDetailsType = ResourceDetailsType.BASIC;
            } else if (internalResourceDetailsType==InternalResourceDetailsType.PROPERTIES) {
                resourceDetailsType = ResourceDetailsType.PROPERTIES;
            } else if (internalResourceDetailsType==InternalResourceDetailsType.VIRTUALDATA) {
                resourceDetailsType = ResourceDetailsType.VIRTUALDATA;
            } else if (internalResourceDetailsType==InternalResourceDetailsType.ALL) {
                resourceDetailsType = ResourceDetailsType.ALL;
            }
        }
	    return resourceDetailsType;
	}

    public static InternalResourceDetailsType valueOf(ResourceDetailsType resourceDetailsType) {
        InternalResourceDetailsType internalResourceDetailsType=InternalResourceDetailsType.ALL;
        if (resourceDetailsType!=null) {
            if (resourceDetailsType==ResourceDetailsType.BASIC) {
                internalResourceDetailsType = InternalResourceDetailsType.BASIC;
            } else if (resourceDetailsType==ResourceDetailsType.PROPERTIES) {
                internalResourceDetailsType = InternalResourceDetailsType.PROPERTIES;
            } else if (resourceDetailsType==ResourceDetailsType.VIRTUALDATA) {
                internalResourceDetailsType = InternalResourceDetailsType.VIRTUALDATA;
            } else if (resourceDetailsType==ResourceDetailsType.ALL) {
                internalResourceDetailsType = InternalResourceDetailsType.ALL;
            }
        }
        return internalResourceDetailsType;
    }
}//EOE 
