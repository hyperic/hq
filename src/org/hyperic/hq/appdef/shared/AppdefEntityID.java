/*
 * NOTE: This copyright does *not* cover user programs that use HQ
 * program services by normal system calls through the application
 * program interfaces provided as part of the Hyperic Plug-in Development
 * Kit or the Hyperic Client Development Kit - this is merely considered
 * normal use of the program, and does *not* fall under the heading of
 * "derived work".
 * 
 * Copyright (C) [2004, 2005, 2006], Hyperic, Inc.
 * This file is part of HQ.
 * 
 * HQ is free software; you can redistribute it and/or modify
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

package org.hyperic.hq.appdef.shared;

import java.io.Serializable;
import java.util.List;

import org.hyperic.hq.authz.shared.AuthzConstants;
import org.hyperic.hq.authz.shared.ResourceValue;
import org.hyperic.util.StringUtil;

/**
 * a class which represents a unique object within the Appdef 
 * framework.
 */

public class AppdefEntityID 
    implements Serializable
{ 
    protected int entityType;   // APPDEF_TYPE_* from AppdefEntityConstants
    protected int entityID;     // ID from the database
    
    /**
     * build an AppdefEntityID from a string key of the form:
     *  
     * [entityType]:[entityID]
     * 
     */    
    public AppdefEntityID(String id){
        try {
            if (id == null)
                throw new InvalidAppdefTypeException("Invalid entity type: " +
                                                     id);
                                
            List typeList = StringUtil.explode(id, ":");
        
            if (typeList.size() != 2)
                throw new InvalidAppdefTypeException("Invalid entity type: " +
                                                     id);
        
            this.entityType = new Integer((String)typeList.get(0)).intValue();
            this.entityID   = new Integer((String)typeList.get(1)).intValue();
        } catch (NumberFormatException e) {
            throw new InvalidAppdefTypeException("Invalid entity type: " +
                                                 id);
        }

        if(!AppdefEntityConstants.typeIsValid(entityType)){
            throw new InvalidAppdefTypeException("Invalid entity type: " +
                                               entityType);
        }
    }

    public AppdefEntityID(int entityType, int entityID){
        this.entityType = entityType;
        this.entityID   = entityID;

        if(!AppdefEntityConstants.typeIsValid(entityType)){
            throw new IllegalArgumentException("Invalid entity type: " +
                                               entityType);
        }
    }
    
    public AppdefEntityID(int entityType, Integer entityID) {
        this(entityType, entityID.intValue());
    }

    public AppdefEntityID(ResourceValue rv) {
        String rtName = rv.getResourceTypeValue().getName();
        this.entityID = rv.getInstanceId().intValue();
        if(rtName.equals(AuthzConstants.platformResType)) {
            this.entityType = AppdefEntityConstants.APPDEF_TYPE_PLATFORM;
        }
        else if(rtName.equals(AuthzConstants.serverResType)) {
            this.entityType = AppdefEntityConstants.APPDEF_TYPE_SERVER;
        }
        else if(rtName.equals(AuthzConstants.serviceResType)) {
            this.entityType = AppdefEntityConstants.APPDEF_TYPE_SERVICE;
        }
        else if(rtName.equals(AuthzConstants.applicationResType)) {
            this.entityType = AppdefEntityConstants.APPDEF_TYPE_APPLICATION;
        }
        else if(rtName.equals(AuthzConstants.groupResType)) {
            this.entityType = AppdefEntityConstants.APPDEF_TYPE_GROUP;
        } 
        else {
            throw new IllegalArgumentException(rtName + 
                " is not a valid Appdef Resource Type");
        }
    }

    public int getType(){
        return this.entityType;
    }

    public String getTypeName(){
        return AppdefEntityConstants.typeToString(this.entityType);
    }

    public int getID(){
        return this.entityID;
    }

    public Integer getId(){
        return new Integer(this.entityID);
    }

    public String getAppdefKey(){
        return this.entityType + ":" + this.entityID;
    }

    /**
     * Return the string name of this entity id object's
     * authz resource type.
     */
    public String getAuthzTypeName() {
        switch(getType()) {
        case AppdefEntityConstants.APPDEF_TYPE_PLATFORM:
            return AuthzConstants.platformResType;
        case AppdefEntityConstants.APPDEF_TYPE_SERVER:
            return AuthzConstants.serverResType;
        case AppdefEntityConstants.APPDEF_TYPE_SERVICE:
            return AuthzConstants.serviceResType;
        case AppdefEntityConstants.APPDEF_TYPE_APPLICATION:
            return AuthzConstants.applicationResType;
        case AppdefEntityConstants.APPDEF_TYPE_GROUP:
            return AuthzConstants.groupResType;
        default:
            throw new IllegalArgumentException("Unknown type: " + getType()); 
        }
    }

    /**
     * Return the ID of this entity id object's authz resource type.
     */
    public Integer getAuthzTypeId() {
        switch (getType()) {
        case AppdefEntityConstants.APPDEF_TYPE_PLATFORM:
            return AuthzConstants.authzPlatform;
        case AppdefEntityConstants.APPDEF_TYPE_SERVER:
            return AuthzConstants.authzServer;
        case AppdefEntityConstants.APPDEF_TYPE_SERVICE:
            return AuthzConstants.authzService;
        case AppdefEntityConstants.APPDEF_TYPE_APPLICATION:
            return AuthzConstants.authzApplication;
        case AppdefEntityConstants.APPDEF_TYPE_GROUP:
            return AuthzConstants.authzGroup;
        default:
            throw new IllegalArgumentException("Unknown type: " + getType());
        }
    }

    /**
     * Convenience method to check if this is a platform
     * 
     * @return true if this entity refers to a platform, false otherwise.
     */
    public boolean isPlatform () { 
        return this.getType() == AppdefEntityConstants.APPDEF_TYPE_PLATFORM; 
    }

    /** Convenience method to check if this is a server
     * @return true if this entity refers to a server, false otherwise. */
    public boolean isServer () { 
        return this.getType() == AppdefEntityConstants.APPDEF_TYPE_SERVER; 
    }

    /** Convenience method to check if this is a service
     * @return true if this entity refers to a service, false otherwise. */
    public boolean isService () { 
        return this.getType() == AppdefEntityConstants.APPDEF_TYPE_SERVICE; 
    }

    /** Convenience method to check if this is a application
     * @return true if this entity refers to a application, false otherwise. */
    public boolean isApplication () { 
        return this.getType() == AppdefEntityConstants.APPDEF_TYPE_APPLICATION;
    }

    /** Convenience method to check if this is a group
     * @return true if this entity refers to a group, false otherwise. */
    public boolean isGroup () { 
        return this.getType() == AppdefEntityConstants.APPDEF_TYPE_GROUP; 
    }

    public String toString(){
        return this.getAppdefKey();
    }

    public boolean equals(Object other){
        AppdefEntityID othObj = (AppdefEntityID)other;

        return (othObj.entityType == this.entityType &&
                othObj.entityID   == this.entityID);
    }

    public int hashCode(){
        return this.entityType * this.entityID;
    }

    public static AppdefEntityID newPlatformID(int id) {
        return new AppdefEntityID(AppdefEntityConstants.APPDEF_TYPE_PLATFORM, 
                                  id);
    }

    public static AppdefEntityID newServerID(int id) {
        return new AppdefEntityID(AppdefEntityConstants.APPDEF_TYPE_SERVER, 
                                  id);
    }

    public static AppdefEntityID newServiceID(int id) {
        return new AppdefEntityID(AppdefEntityConstants.APPDEF_TYPE_SERVICE, 
                                  id);
    }

    public static AppdefEntityID newAppID(int id) {
        return new AppdefEntityID(AppdefEntityConstants.APPDEF_TYPE_APPLICATION, 
                                  id);
    }

    public static AppdefEntityID newGroupID(int id) {
        return new AppdefEntityID(AppdefEntityConstants.APPDEF_TYPE_GROUP, id);
    }
}
