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

package org.hyperic.hq.bizapp.agent;

/**
 * TokenData contains information about an authentication token, such
 * as the string token, creation time, etc.
 */
public class TokenData 
    implements Cloneable
{
    private String      token;
    private long        createTime;
    private boolean     locked;

    public TokenData(String token, long createTime, boolean locked){
        this.token      = token;
        this.createTime = createTime;
        this.locked     = locked;
    }
    
    public String getToken(){
        return this.token;
    } 

    public long getCreateTime(){
        return this.createTime;
    }

    public boolean isLocked(){
        return this.locked;
    }

    public void setLocked(boolean locked){
        this.locked = locked;
    }

    public int hashCode(){
        return 37 * this.getToken().hashCode() +
            37 * (int)this.getCreateTime() +
            37 * (this.isLocked() ? 1 : 0);
    }

    public boolean equals(Object other){
        TokenData o;

        if(!(other instanceof TokenData))
            return false;

        o = (TokenData)other;
        return o.getToken().equals(this.getToken()) &&
            o.getCreateTime() == this.getCreateTime() &&
            o.isLocked() == this.isLocked();
    }

    public Object clone(){
        return new TokenData(this.token, this.createTime, 
                             this.locked);
    }
}
