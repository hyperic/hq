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

import org.hyperic.lather.LatherRemoteException;
import org.hyperic.lather.LatherKeyNotFoundException;
import org.hyperic.hq.appdef.shared.AIServiceValue;

public class AiServiceLatherValue
    extends AiLatherValue
{
    private static final String PROP_CTIME              = "CTime";
    private static final String PROP_MTIME              = "MTime";
    private static final String PROP_CONTROLCONFIG      = "controlConfig";
    private static final String PROP_DESCRIPTION        = "description";
    private static final String PROP_ID                 = "id";
    private static final String PROP_MEASUREMENTCONFIG  = "measurementConfig";
    private static final String PROP_NAME               = "name";
    private static final String PROP_PRODUCTCONFIG      = "productConfig";
    private static final String PROP_RESPONSETIMECONFIG = "responseTimeConfig";
    private static final String PROP_SERVERID           = "serverId";
    private static final String PROP_SERVICETYPENAME    = "serviceTypeName";
    private static final String PROP_CPROPS             = "cprops";

    public AiServiceLatherValue(){
        super();
    }

    public AiServiceLatherValue(AIServiceValue v){
        super();

        if(v.cTimeHasBeenSet()){
            this.setDoubleValue(PROP_CTIME, (double)v.getCTime().longValue());
        }

        if(v.mTimeHasBeenSet()){
            this.setDoubleValue(PROP_MTIME, (double)v.getMTime().longValue());
        }

        if(v.controlConfigHasBeenSet()){
            this.setByteAValue(PROP_CONTROLCONFIG, v.getControlConfig());
        }

        if(v.descriptionHasBeenSet()){
            this.setStringValue(PROP_DESCRIPTION, v.getDescription(), 200);
        }

        if(v.idHasBeenSet()){
            this.setIntValue(PROP_ID, v.getId().intValue());
        }

        if(v.measurementConfigHasBeenSet()){
            this.setByteAValue(PROP_MEASUREMENTCONFIG, 
                               v.getMeasurementConfig());
        }

        if(v.nameHasBeenSet()){
            this.setStringValue(PROP_NAME, v.getName());
        }

        if(v.productConfigHasBeenSet()){
            this.setByteAValue(PROP_PRODUCTCONFIG, v.getProductConfig());
        }

        if(v.customPropertiesHasBeenSet()) {
            this.setByteAValue(PROP_CPROPS, v.getCustomProperties());
        }

        if(v.responseTimeConfigHasBeenSet()){
            this.setByteAValue(PROP_RESPONSETIMECONFIG, 
                               v.getResponseTimeConfig());
        }

        if(v.serverIdHasBeenSet()){
            this.setIntValue(PROP_SERVERID, v.getServerId());
        }

        if(v.serviceTypeNameHasBeenSet()){
            this.setStringValue(PROP_SERVICETYPENAME, v.getServiceTypeName());
        }
    }

    public AIServiceValue getAIServiceValue(){
        AIServiceValue r = new AIServiceValue();
        
        try {
            r.setCTime(new Long((long)this.getDoubleValue(PROP_CTIME)));
        } catch(LatherKeyNotFoundException exc){}

        try {
            r.setMTime(new Long((long)this.getDoubleValue(PROP_MTIME)));
        } catch(LatherKeyNotFoundException exc){}

        try {
            r.setControlConfig(this.getByteAValue(PROP_CONTROLCONFIG));
        } catch(LatherKeyNotFoundException exc){}

        try {
            r.setDescription(this.getStringValue(PROP_DESCRIPTION));
        } catch(LatherKeyNotFoundException exc){}
        
        try {
            r.setId(new Integer(this.getIntValue(PROP_ID)));
        } catch(LatherKeyNotFoundException exc){}

        try {
            r.setMeasurementConfig(this.getByteAValue(PROP_MEASUREMENTCONFIG));
        } catch(LatherKeyNotFoundException exc){}

        try {
            r.setName(this.getStringValue(PROP_NAME));
        } catch(LatherKeyNotFoundException exc){}

        try {
            r.setProductConfig(this.getByteAValue(PROP_PRODUCTCONFIG));
        } catch(LatherKeyNotFoundException exc){}

        try {
            r.setResponseTimeConfig(this.getByteAValue(PROP_RESPONSETIMECONFIG));
        } catch(LatherKeyNotFoundException exc){}

        try {
            r.setServerId(this.getIntValue(PROP_SERVERID));
        } catch(LatherKeyNotFoundException exc){}

        try {
            r.setServiceTypeName(this.getStringValue(PROP_SERVICETYPENAME));
        } catch(LatherKeyNotFoundException exc){}

        try {
            r.setCustomProperties(this.getByteAValue(PROP_CPROPS));
        } catch(LatherKeyNotFoundException exc){}

        return r;
    }

    public void validate()
        throws LatherRemoteException
    {
    }
}
