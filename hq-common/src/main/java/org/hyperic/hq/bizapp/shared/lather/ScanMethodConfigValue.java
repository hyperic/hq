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
import org.hyperic.hq.autoinventory.ScanMethodConfig;
import org.hyperic.util.config.ConfigResponse;
import org.hyperic.util.config.EncodingException;

public class ScanMethodConfigValue
    extends LatherValue
{
    private static final String PROP_METHODCLASS = "methodClass";
    private static final String PROP_CONFIG      = "config";

    public ScanMethodConfigValue(){
        super();
    }

    public ScanMethodConfigValue(ScanMethodConfig cfg)
        throws LatherRemoteException
    {
        super();
        this.setMethodClass(cfg.getMethodClass());
        try {
            this.setConfig(cfg.getConfig());
        } catch(LatherRemoteException exc){
            throw exc;
        } catch(Exception exc){
            throw new LatherRemoteException("Error encoding config: " +
                                            exc.getMessage());
        }
    }

    public void setMethodClass(String methodClass){
        this.setStringValue(PROP_METHODCLASS, methodClass);
    }

    public String getMethodClass(){
        return this.getStringValue(PROP_METHODCLASS);
    }

    public void setConfig(ConfigResponse config)
        throws LatherRemoteException
    {
        byte[] data;

        try {
            data = config.encode();
        } catch(Exception exc){
            throw new LatherRemoteException("Error encoding configuration: " +
                                            exc.getMessage());
        }
        this.setByteAValue(PROP_CONFIG, data);
    }

    public ConfigResponse getConfig()
        throws LatherRemoteException
    {
        try {
            return ConfigResponse.decode(this.getByteAValue(PROP_CONFIG));
        } catch(EncodingException exc){
            throw new LatherRemoteException("Error decoding configuration: " +
                                            exc.getMessage());
        }
    }

    public void validate()
        throws LatherRemoteException
    {
        try {
            this.getMethodClass();
            this.getConfig();
        } catch(LatherKeyNotFoundException exc){
            throw new LatherRemoteException("All values not set");
        }
    }
}

