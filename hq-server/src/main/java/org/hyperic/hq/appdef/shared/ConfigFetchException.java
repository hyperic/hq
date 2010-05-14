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

import org.hyperic.hq.common.ApplicationException;

public class ConfigFetchException extends ApplicationException {
    private String         msg;
    private String         productType;
    private AppdefEntityID culprit;

    /**
     * Indicate that the specified appdef entity needs a config response
     * which has not yet been filled out.
     */
    public ConfigFetchException(String productType, AppdefEntityID id) {
        this.culprit     = id;
        this.productType = productType;
        this.msg         = productType + " configuration has not yet been " +
            "setup for " + id;
    }

    public ConfigFetchException(String productType, AppdefResourceValue ent){
        AppdefEntityID id;

        id = ent.getEntityId();

        this.culprit     = id;
        this.productType = productType;
        this.msg         = productType + " configuration has not yet been " +
            "setup for " + id.getTypeName() + " '" + ent.getName() + "' (id=" +
            id.getID() + ")";
    }

    public AppdefEntityID getEntity(){
        return this.culprit;
    }

    public String getProductType(){
        return this.productType;
    }

    /**
     * Check to see if this exception is thrown because of an entity
     * indicated by the passed arguments.
     */
    public boolean matchesQuery(AppdefEntityID id, String productType){
        return id.equals(this.culprit) && 
            productType.equals(this.productType);
    }

    public String getMessage(){
        return this.msg;
    }
}
