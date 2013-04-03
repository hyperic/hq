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
 *
 * **********************************************************************
 * 29 April 2012
 * Maya Anderson
 * *********************************************************************/
package org.hyperic.hq.api.model;

import java.util.HashMap;
import java.util.Map;

import javax.xml.bind.annotation.XmlEnum;
import javax.xml.bind.annotation.XmlType;

import org.hyperic.hq.appdef.shared.AppdefEntityConstants;

@XmlType(name="resourceTypeEnum", namespace=RestApiConstants.SCHEMA_NAMESPACE)
@XmlEnum
public enum ResourceTypeModel {
    PLATFORM(AppdefEntityConstants.APPDEF_TYPE_PLATFORM),
    SERVER(AppdefEntityConstants.APPDEF_TYPE_SERVER),
    SERVICE(AppdefEntityConstants.APPDEF_TYPE_SERVICE),
    APPLICATION(AppdefEntityConstants.APPDEF_TYPE_APPLICATION),
    ESCALATION(),
    GROUP(AppdefEntityConstants.APPDEF_TYPE_GROUP),
    SUBJECT(),
    ROLE() ; 
    
    private static final Map<Integer, ResourceTypeModel> reverseValues = new HashMap<Integer,ResourceTypeModel>() ;
    private static final int NO_APPDEF_TYPE = -999 ; 
    
    private int appdefTypeID ; 
    
    ResourceTypeModel(){ this.appdefTypeID = NO_APPDEF_TYPE ; }//EOC 
    ResourceTypeModel(final int appdefTypeID) { 
    	this.appdefTypeID = appdefTypeID ; 
    }//EOM 
    
    static { 
    	for(ResourceTypeModel enumResourceType : values()) { 
    		if(enumResourceType.appdefTypeID != NO_APPDEF_TYPE) reverseValues.put(enumResourceType.appdefTypeID, enumResourceType) ;
    	}//EO while there are more resources 
    }//EO static block 
    
    public static final ResourceTypeModel valueOf(final int appDefType) { 
    	return reverseValues.get(appDefType) ;
    }//EOM 
}//EOE 
