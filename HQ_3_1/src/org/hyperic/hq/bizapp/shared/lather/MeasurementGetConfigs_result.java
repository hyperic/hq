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

import org.hyperic.hq.measurement.shared.MeasurementConfigEntity;
import org.hyperic.hq.measurement.shared.MeasurementConfigList;

public class MeasurementGetConfigs_result
    extends LatherValue
{
    private static final String PROP_PLUGINNAME   = "pluginName";
    private static final String PROP_PLUGINTYPE   = "pluginType";
    private static final String PROP_PLUGINCONFIG = "pluginConfig";
    private static final String PROP_NENTS        = "nEnts";

    public MeasurementGetConfigs_result(){
        super();
    }

    public void setConfigs(MeasurementConfigList configs){
        MeasurementConfigEntity[] ents;

        ents = configs.getEntities();
        for(int i=0; i<ents.length; i++){
            this.addStringToList(PROP_PLUGINNAME, ents[i].getPluginName());
            this.addStringToList(PROP_PLUGINTYPE, ents[i].getPluginType());
            this.addByteAToList(PROP_PLUGINCONFIG, ents[i].getConfig());
        }

        this.setIntValue(PROP_NENTS, ents.length);
    }

    public MeasurementConfigList getConfigs()
        throws LatherRemoteException
    {
        MeasurementConfigEntity[] ents;
        MeasurementConfigList res;
        String[] pluginNames, pluginTypes;
        byte[][] configs;
        int nEnts;

        nEnts       = this.getIntValue(PROP_NENTS);
        if(nEnts == 0){
            res = new MeasurementConfigList();
            res.setEntities(new MeasurementConfigEntity[0]);
            return res;
        }

        pluginNames = this.getStringList(PROP_PLUGINNAME);
        pluginTypes = this.getStringList(PROP_PLUGINTYPE);
        configs     = this.getByteAList(PROP_PLUGINCONFIG);
        ents        = new MeasurementConfigEntity[nEnts];

        if(nEnts != pluginNames.length || nEnts != pluginTypes.length ||
           nEnts != configs.length)
        {
            throw new LatherRemoteException("Config size mismatch");
        }

        for(int i=0; i<nEnts; i++){
            ents[i] = new MeasurementConfigEntity();
            ents[i].setPluginName(pluginNames[i]);
            ents[i].setPluginType(pluginTypes[i]);
            ents[i].setConfig(configs[i]);
        }

        res = new MeasurementConfigList();
        res.setEntities(ents);
        return res;
    }

    public void validate()
        throws LatherRemoteException
    {
        try {
            this.getIntValue(PROP_NENTS);
        } catch(LatherKeyNotFoundException exc){
            throw new LatherRemoteException("All values not set");
        }
    }
}
