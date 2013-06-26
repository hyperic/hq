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

package org.hyperic.hq.bizapp.shared.lather;

import org.hyperic.lather.LatherKeyNotFoundException;
import org.hyperic.lather.LatherRemoteException;
import org.hyperic.lather.LatherValue;
import org.hyperic.hq.appdef.shared.AIPlatformValue;
import org.hyperic.hq.product.RuntimeResourceReport;

public class RuntimeResourceReportLatherValue
    extends LatherValue
{
    private static final String PROP_SERVERID = "serverId";
    private static final String PROP_PLATFORM = "platform";

    public RuntimeResourceReportLatherValue(){
        super();
    }

    public RuntimeResourceReportLatherValue(RuntimeResourceReport v){
        super();

        this.setIntValue(PROP_SERVERID, v.getServerId());
        if(v.getAIPlatforms() != null){
            AIPlatformValue[] plats = v.getAIPlatforms();

            for(int i=0; i<plats.length; i++){
                this.addObjectToList(PROP_PLATFORM,
                                     new AiPlatformLatherValue(plats[i]));
            }
        }
    }

    public RuntimeResourceReport getReport(){
        RuntimeResourceReport r = new RuntimeResourceReport();

        r.setServerId(this.getIntValue(PROP_SERVERID));
        try {
            AIPlatformValue[] plats;
            LatherValue[] lPlats;

            lPlats =  (LatherValue[])this.getObjectList(PROP_PLATFORM);
            plats  = new AIPlatformValue[lPlats.length];
            for(int i=0; i<plats.length; i++){
                plats[i] = ((AiPlatformLatherValue)
                            lPlats[i]).getAIPlatformValue();
            }

            r.setAIPlatforms(plats);
        } catch(LatherKeyNotFoundException exc){
            r.setAIPlatforms(new AIPlatformValue[0]);
        }
        return r;
    }

    public void validate()
        throws LatherRemoteException
    {
        try {
            this.getReport();
        } catch(LatherKeyNotFoundException exc){
            throw new LatherRemoteException("All values not set");
        }
    }
}
