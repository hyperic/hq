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
import java.text.SimpleDateFormat;
import java.util.Date;

public class MiniResourceValue implements Serializable {

    public static final SimpleDateFormat DFORMAT
        = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

    public int resId;
    public int id;
    public int type; // one of AppdefEntityConstants.APPDEF_TYPE_XXX
    public long ctime;
    public String typeName;
    public String name;

    public String notes = null;

    public MiniResourceValue (int resId,
                              int id,
                              int type,
                              String typeName,
                              String name,
                              long ctime) {

        this.resId    = resId;
        this.id       = id;
        this.type     = type;
        this.typeName = typeName;
        this.name     = name;
        this.ctime    = ctime;
    }

    public MiniResourceValue (int resId,
                              int id,
                              int type,
                              String name,
                              String typeName,
                              long ctime,
                              String notes) {
        this.resId    = resId;
        this.id       = id;
        this.type     = type;
        this.typeName = typeName;
        this.name     = name;
        this.ctime    = ctime;
        this.notes    = notes;
    }

    public AppdefEntityID getAppdefID () {
        return new AppdefEntityID(type, id);
    }
    
    public AppdefEntityID getEntityId() {
        return this.getAppdefID();
    }

    public String toString () {
        return "[MiniResourceValue"
            + " resId="+resId
            + " id="+id
            + " type="+type
            + " typeName="+typeName
            + " name="+name
            + " ctime="+DFORMAT.format(new Date(ctime))
            + " notes="+notes
            + "]";
    }
}
