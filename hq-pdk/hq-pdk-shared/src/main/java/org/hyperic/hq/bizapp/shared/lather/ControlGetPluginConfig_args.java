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

import org.hyperic.hq.bizapp.shared.lather.SecureAgentLatherValue;

import org.hyperic.lather.LatherKeyNotFoundException;
import org.hyperic.lather.LatherRemoteException;

public class ControlGetPluginConfig_args 
    extends SecureAgentLatherValue
{
    private static final String PROP_PLUGINNAME = "pluginName";
    private static final String PROP_MERGE      = "merge";

    public ControlGetPluginConfig_args(){
        super();
    }

    public void setPluginName(String pluginName){
        this.setStringValue(PROP_PLUGINNAME, pluginName);
    }

    public String getPluginName(){
        return this.getStringValue(PROP_PLUGINNAME);
    }

    public void setMerge(boolean merge){
        this.setIntValue(PROP_MERGE, merge ? 1 : 0);
    }

    public boolean getMerge(){
        return this.getIntValue(PROP_MERGE) == 1 ? true : false;
    }

    public void validate()
        throws LatherRemoteException
    {
        super.validate();
        try {
            this.getPluginName();
            this.getMerge();
        } catch(LatherKeyNotFoundException exc){
            throw new LatherRemoteException("All values not set");
        }
    }
}
