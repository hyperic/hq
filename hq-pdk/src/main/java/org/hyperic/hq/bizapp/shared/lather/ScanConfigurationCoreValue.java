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
import org.hyperic.util.config.ConfigResponse;
import org.hyperic.util.config.EncodingException;
import org.hyperic.hq.autoinventory.ScanMethodConfig;
import org.hyperic.hq.autoinventory.ScanConfigurationCore;
import org.hyperic.hq.autoinventory.ServerSignature;

public class ScanConfigurationCoreValue
    extends LatherValue
{
    private static final String PROP_NSCANCONFIGS = "nscanConfigs";
    private static final String PROP_SCANCONFIGS  = "scanConfigs";
    private static final String PROP_NSERVERSIGS  = "nserverSigs";
    private static final String PROP_SERVERSIGS   = "serverSigs";
    private static final String PROP_CONFIG       = "configResponse";

    public ScanConfigurationCoreValue(){
        super();
    }

    public ScanConfigurationCoreValue(ScanConfigurationCore core)
        throws LatherRemoteException
    {
        super();

        ScanMethodConfig[] configs;
        ServerSignature[] sigs;
        ConfigResponse configResponse;

        configs = core.getScanMethodConfigs();
        sigs    = core.getServerSignatures();
        configResponse = core.getConfigResponse();

        this.setIntValue(PROP_NSCANCONFIGS, configs.length);
        for(int i=0; i<configs.length; i++){
            this.addObjectToList(PROP_SCANCONFIGS, 
                                 new ScanMethodConfigValue(configs[i]));
        }

        this.setIntValue(PROP_NSERVERSIGS, sigs.length);
        for(int i=0; i<sigs.length; i++){
            this.addObjectToList(PROP_SERVERSIGS, 
                                 new ServerSignatureValue(sigs[i]));
        }

        try {
            this.setByteAValue(PROP_CONFIG, configResponse.encode());
        } catch (Exception e) {
            throw new LatherRemoteException("Error encoding configuration: " +
                                            e.getMessage());
        }
    }

    public ScanConfigurationCore getCore()
        throws LatherRemoteException
    {
        ScanConfigurationCore res = new ScanConfigurationCore();
        ScanMethodConfig[] configs;
        ServerSignature[] sigs;
        LatherValue[] lVals;

        if(this.getIntValue(PROP_NSCANCONFIGS) != 0){
            lVals   =  (LatherValue[])this.getObjectList(PROP_SCANCONFIGS);
            configs = new ScanMethodConfig[lVals.length];
            for(int i=0; i<lVals.length; i++){
                ScanMethodConfigValue scVal = (ScanMethodConfigValue)lVals[i];
                
                configs[i] = new ScanMethodConfig();
                configs[i].setMethodClass(scVal.getMethodClass());
                configs[i].setConfig(scVal.getConfig());
            }
        } else {
            configs = new ScanMethodConfig[0];
        }

        if(this.getIntValue(PROP_NSERVERSIGS) != 0){
            lVals =  (LatherValue[])this.getObjectList(PROP_SERVERSIGS);
            sigs = new ServerSignature[lVals.length];
            for(int i=0; i<lVals.length; i++){
                ServerSignatureValue svVal = (ServerSignatureValue)lVals[i];
                
                sigs[i] = svVal.getSignature();
            }
        } else {
            sigs = new ServerSignature[0];
        }

        res.setScanMethodConfigs(configs);
        res.setServerSignatures(sigs);
        res.setConfigResponse(getConfig());
        return res;
    }
 
    private ConfigResponse getConfig()
        throws LatherRemoteException {
    
        try {
            return ConfigResponse.decode(this.getByteAValue(PROP_CONFIG));
        } catch(EncodingException e) {
            throw new LatherRemoteException("Error decoding configuration: " +
                                            e.getMessage());
        }
    }

    public void validate()
        throws LatherRemoteException
    {
        try {
            this.getCore();
        } catch(LatherKeyNotFoundException exc){
            throw new LatherRemoteException(exc.getMessage() + " key not set");
        }
    }
}

