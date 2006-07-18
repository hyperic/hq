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
import org.hyperic.hq.appdef.shared.AIServerExtValue;
import org.hyperic.hq.appdef.shared.AIServerValue;
import org.hyperic.hq.appdef.shared.AIServiceValue;

public class AiServerLatherValue
    extends AiLatherValue
{
    private static final String PROP_CTIME              = "CTime";
    private static final String PROP_MTIME              = "MTime";
    private static final String PROP_AUTOINVENTORYIDENTIFIER = 
        "autoinventoryIdentifier";
    private static final String PROP_CONTROLCONFIG      = "controlConfig";
    private static final String PROP_DESCRIPTION        = "description";
    private static final String PROP_DIFF               = "diff";
    private static final String PROP_ID                 = "id";
    private static final String PROP_IGNORED            = "ignored";
    private static final String PROP_INSTALLPATH        = "installPath";
    private static final String PROP_MEASUREMENTCONFIG  = "measurementConfig";
    private static final String PROP_NAME               = "name";
    private static final String PROP_PRIMARYKEY         = "primaryKey";
    private static final String PROP_PRODUCTCONFIG      = "productConfig";
    private static final String PROP_QUEUESTATUS        = "queueStatus";
    private static final String PROP_QUEUESTATUSSTR     = "queueStatusStr";
    private static final String PROP_RESPONSETIMECONFIG = "responseTimeConfig";
    private static final String PROP_SERVERTYPENAME     = "serverTypeName";
    private static final String PROP_SERVICESAUTOMANAGED = 
        "servicesAutomanaged";
    private static final String PROP_ISEXT              = "isEXT";
    private static final String PROP_SERVICES           = "services";
    private static final String PROP_PLACEHOLDER        = "placeHolder";
    private static final String PROP_AUTOENABLE         = "autoEnable";
    private static final String PROP_MCONNECT_HASH      = "mConnectHash";
    private static final String PROP_CPROPS             = "cprops";

    public AiServerLatherValue(){
        super();
    }

    public AiServerLatherValue(AIServerValue v){
        super();

        if(v.cTimeHasBeenSet()){
            this.setDoubleValue(PROP_CTIME, (double)v.getCTime().longValue());
        }

        if(v.mTimeHasBeenSet()){
            this.setDoubleValue(PROP_MTIME, (double)v.getMTime().longValue());
        }

        if(v.autoinventoryIdentifierHasBeenSet()){
            this.setStringValue(PROP_AUTOINVENTORYIDENTIFIER, 
                                v.getAutoinventoryIdentifier());
        }

        if(v.controlConfigHasBeenSet()){
            this.setByteAValue(PROP_CONTROLCONFIG, v.getControlConfig());
        }

        if(v.descriptionHasBeenSet()){
            this.setStringValue(PROP_DESCRIPTION, v.getDescription(), 300);
        }

        if(v.diffHasBeenSet()){
            this.setDoubleValue(PROP_DIFF, (double)v.getDiff());
        }

        if(v.idHasBeenSet()){
            this.setIntValue(PROP_ID, v.getId().intValue());
        }

        if(v.ignoredHasBeenSet()){
            this.setIntValue(PROP_IGNORED, v.getIgnored() ? 1 : 0);
        }

        if(v.installPathHasBeenSet()){
            this.setStringValue(PROP_INSTALLPATH, v.getInstallPath());
        }

        if(v.measurementConfigHasBeenSet()){
            this.setByteAValue(PROP_MEASUREMENTCONFIG, 
                               v.getMeasurementConfig());
        }

        if(v.nameHasBeenSet()){
            this.setStringValue(PROP_NAME, v.getName());
        }


        if(v.customPropertiesHasBeenSet()) {
            this.setByteAValue(PROP_CPROPS, v.getCustomProperties());
        }

        if(v.productConfigHasBeenSet()){
            this.setByteAValue(PROP_PRODUCTCONFIG, v.getProductConfig());
        }

        if(v.queueStatusHasBeenSet()){
            this.setIntValue(PROP_QUEUESTATUS, v.getQueueStatus());
        }

        if(v.responseTimeConfigHasBeenSet()){
            this.setByteAValue(PROP_RESPONSETIMECONFIG, 
                               v.getResponseTimeConfig());
        }

        if(v.serverTypeNameHasBeenSet()){
            this.setStringValue(PROP_SERVERTYPENAME, v.getServerTypeName());
        }

        if(v.servicesAutomanagedHasBeenSet()){
            this.setIntValue(PROP_SERVICESAUTOMANAGED, 
                             v.getServicesAutomanaged() ? 1 : 0);
        }

        // If this is a souped up AIServerValue with services hanging off it,
        // then add those as well
        if(v instanceof AIServerExtValue){
            AIServerExtValue svExt = (AIServerExtValue)v;
            AIServiceValue[] services;

            this.setIntValue(PROP_ISEXT, 1);
            this.setIntValue(PROP_PLACEHOLDER, svExt.getPlaceholder() ? 1 : 0);
            this.setIntValue(PROP_AUTOENABLE, svExt.getAutoEnable() ? 1 : 0);
            this.setIntValue(PROP_MCONNECT_HASH, svExt.getMetricConnectHashCode());
            services = svExt.getAIServiceValues();
            if(services != null){
                for(int i=0; i<services.length; i++){
                    this.addObjectToList(PROP_SERVICES,
                                       new AiServiceLatherValue(services[i]));
                }
            }
        } else {
            this.setIntValue(PROP_ISEXT, 0);
        }
    }

    public AIServerValue getAIServerValue(){
        AIServerValue r;
        boolean isExt;

        isExt = this.getIntValue(PROP_ISEXT) == 1;
        if(isExt){
            r = new AIServerExtValue();
        } else {
            r = new AIServerValue();
        }

        try {
            r.setCTime(new Long((long)this.getDoubleValue(PROP_CTIME)));
        } catch(LatherKeyNotFoundException exc){}

        try {
            r.setMTime(new Long((long)this.getDoubleValue(PROP_MTIME)));
        } catch(LatherKeyNotFoundException exc){}

        try {
            r.setAutoinventoryIdentifier(this.getStringValue(PROP_AUTOINVENTORYIDENTIFIER));
        } catch(LatherKeyNotFoundException exc){}

        try {
            r.setControlConfig(this.getByteAValue(PROP_CONTROLCONFIG));
        } catch(LatherKeyNotFoundException exc){}

        try {
            r.setDescription(this.getStringValue(PROP_DESCRIPTION));
        } catch(LatherKeyNotFoundException exc){}

        try {
            r.setDiff((long)this.getDoubleValue(PROP_DIFF));
        } catch(LatherKeyNotFoundException exc){}

        try {
            r.setId(new Integer(this.getIntValue(PROP_ID)));
        } catch(LatherKeyNotFoundException exc){}

        try {
            r.setIgnored(this.getIntValue(PROP_IGNORED) == 1 ? true : false);
        } catch(LatherKeyNotFoundException exc){}

        try {
            r.setInstallPath(this.getStringValue(PROP_INSTALLPATH));
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
            r.setQueueStatus(this.getIntValue(PROP_QUEUESTATUS));
        } catch(LatherKeyNotFoundException exc){}

        try {
            r.setResponseTimeConfig(this.getByteAValue(PROP_RESPONSETIMECONFIG));
        } catch(LatherKeyNotFoundException exc){}

        try {
            r.setServerTypeName(this.getStringValue(PROP_SERVERTYPENAME));
        } catch(LatherKeyNotFoundException exc){}

        try {
            r.setServicesAutomanaged(this.getIntValue(PROP_SERVICESAUTOMANAGED) == 1 ? true : false);
        } catch(LatherKeyNotFoundException exc){}

        try {
            r.setCustomProperties(this.getByteAValue(PROP_CPROPS));
        } catch(LatherKeyNotFoundException exc){}

        if(isExt){
            AIServerExtValue svExt = (AIServerExtValue)r;

            svExt.setPlaceholder(this.getIntValue(PROP_PLACEHOLDER) == 1);
            svExt.setAutoEnable(this.getIntValue(PROP_AUTOENABLE) == 1);
            svExt.setMetricConnectHashCode(this.getIntValue(PROP_MCONNECT_HASH));

            try {
                LatherValue[] services;
                
                services = this.getObjectList(this.PROP_SERVICES);
                for(int i=0; i<services.length; i++){
                    AiServiceLatherValue svc;
                    
                    svc = (AiServiceLatherValue)services[i];
                
                    svExt.addAIServiceValue(svc.getAIServiceValue());
                }
            } catch(LatherKeyNotFoundException exc){
                svExt.setAIServiceValues(new AIServiceValue[0]);
            }
        }

        return r;
    }

    public void validate()
        throws LatherRemoteException
    {
    }
}
