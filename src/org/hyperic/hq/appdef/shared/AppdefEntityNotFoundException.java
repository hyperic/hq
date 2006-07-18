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

import org.hyperic.hq.common.ObjectNotFoundException;

public abstract class AppdefEntityNotFoundException
    extends ObjectNotFoundException 
{
    private AppdefEntityID id;
    
    protected AppdefEntityNotFoundException(String msg){
        super(msg);
    }
    
    protected AppdefEntityNotFoundException(String msg, Throwable t){
        super(msg, t);
    }

    protected AppdefEntityNotFoundException(AppdefEntityID id, String msg){
        super(msg);
        checkType(id);
        this.id = id;
    }

    protected AppdefEntityNotFoundException(AppdefEntityID id){
        super(id + " not found");
        checkType(id);
        this.id = id;
    }
    
    protected AppdefEntityNotFoundException(AppdefEntityID id, Throwable t){
        super(id + " not found", t);
        checkType(id);
        this.id = id;
    }
    
    private void checkType (AppdefEntityID id) {
        if (id.getType() != getAppdefType()) {
            throw new IllegalArgumentException("Invalid type: " + id);
        }
    }

    public AppdefEntityID getNotFoundID(){
        return this.id;
    }

    public void setNotFoundID(AppdefEntityID id){
        this.id = id;
    }

    public abstract int getAppdefType();

    /** Create an exception of the appropriate subclass based on id.getType() */
    public static AppdefEntityNotFoundException build (AppdefEntityID id) {
        switch(id.getType()){
        case AppdefEntityConstants.APPDEF_TYPE_PLATFORM:
            return new PlatformNotFoundException(id);
        case AppdefEntityConstants.APPDEF_TYPE_SERVER:
            return new ServerNotFoundException(id);
        case AppdefEntityConstants.APPDEF_TYPE_SERVICE:
            return new ServiceNotFoundException(id);
        case AppdefEntityConstants.APPDEF_TYPE_APPLICATION:
            return new ApplicationNotFoundException(id);
        default:
            throw new IllegalArgumentException("Unrecognized entity type: "
                                               + id.getType());
        }
    }
}
