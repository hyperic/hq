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

public class ServerNotFoundException
    extends AppdefEntityNotFoundException
{
    private static final int MY_TYPE = 
        AppdefEntityConstants.APPDEF_TYPE_SERVER;

    public ServerNotFoundException(String msg){
        super(msg);
    }
    
    public ServerNotFoundException(String msg, Throwable t){
        super(msg, t);
    }

    public ServerNotFoundException(AppdefEntityID id){
        super(id);
    }

    public ServerNotFoundException(AppdefEntityID id, String msg){
        super(id, msg);
    }

    public ServerNotFoundException(int id){
        super(new AppdefEntityID(MY_TYPE, id));
    }
    
    public ServerNotFoundException(int id, Throwable t){
        super(new AppdefEntityID(MY_TYPE, id), t);
    }

    public ServerNotFoundException(Integer id){
        super(new AppdefEntityID(MY_TYPE, id.intValue()));
    }
    
    public ServerNotFoundException(Integer id, Throwable t){
        super(new AppdefEntityID(MY_TYPE, id.intValue()), t);
    }

    public int getAppdefType(){
        return MY_TYPE;
    }
}
