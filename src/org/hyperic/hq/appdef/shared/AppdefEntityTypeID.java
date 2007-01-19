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

import org.hyperic.hq.common.SystemException;

/**
 * 
 * An object to represent an appdef catalog type such as a ServerType or ServiceType
 * 
 */
public class AppdefEntityTypeID extends AppdefEntityID implements Serializable {

    
    public AppdefEntityTypeID(String id) {
        super(id);
    }

    public AppdefEntityTypeID(int entityType, int entityID) {
        super(entityType, entityID);
    }

    public AppdefEntityTypeID(int entityType, Integer entityID) {
        super(entityType, entityID);
    }

    public AppdefResourceTypeValue getAppdefResourceTypeValue() {
        try {
            // GROOOAAAN
            Integer idInteger = new Integer(entityID);
            switch(this.entityType) {
                case AppdefEntityConstants.APPDEF_TYPE_PLATFORM:
                    return PlatformManagerUtil.getLocalHome().create()
                                .findPlatformTypeValueById(idInteger);
                case AppdefEntityConstants.APPDEF_TYPE_SERVER:
                    return ServerManagerUtil.getLocalHome().create()
                                .findServerTypeById(idInteger);
                case AppdefEntityConstants.APPDEF_TYPE_SERVICE:
                    return ServiceManagerUtil.getLocalHome().create()
                                .findServiceTypeById(idInteger);
                default:
                    throw new IllegalArgumentException("Invalud AppdefType: " +
                            this.toString());
            }
        } catch (Exception e) {
            throw new SystemException(e);
        }
    }
}
