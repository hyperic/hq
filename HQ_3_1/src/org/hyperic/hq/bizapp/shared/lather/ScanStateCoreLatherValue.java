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
import org.hyperic.hq.autoinventory.ScanMethodState;
import org.hyperic.hq.autoinventory.ScanStateCore;

public class ScanStateCoreLatherValue
    extends LatherValue
{
    private static final String PROP_ARESERVERSINCLUDED = "areServersIncluded";
    private static final String PROP_CERTDN             = "certDN";
    private static final String PROP_ENDTIME            = "endTime";
    private static final String PROP_GLOBALEXCEPTION    = "globalException";
    private static final String PROP_ISDONE             = "isDone";
    private static final String PROP_ISINTERRUPTED      = "isInterrupted";
    private static final String PROP_PLATFORM           = "platform";
    private static final String PROP_SCANMETHODSTATES   = "scanMethodStates";
    private static final String PROP_STARTTIME          = "startTime";

    public ScanStateCoreLatherValue(){
        super();
    }

    public ScanStateCoreLatherValue(ScanStateCore v){
        super();

        this.setIntValue(PROP_ARESERVERSINCLUDED, 
                         v.getAreServersIncluded() ? 1 : 0);

        if(v.getCertDN() != null){
            this.setStringValue(PROP_CERTDN, v.getCertDN());
        }

        this.setDoubleValue(PROP_ENDTIME, (double)v.getEndTime());

        if(v.getGlobalException() != null){
            this.setObjectValue(PROP_GLOBALEXCEPTION,
                  new StringifiedExceptionLatherValue(v.getGlobalException()));
        }

        this.setIntValue(PROP_ISDONE, v.getIsDone() ? 1 : 0);

        this.setIntValue(PROP_ISINTERRUPTED, v.getIsInterrupted() ? 1 : 0);

        if(v.getPlatform() != null){
            this.setObjectValue(PROP_PLATFORM,
                                new AiPlatformLatherValue(v.getPlatform()));
        }

        if(v.getScanMethodStates() != null){
            ScanMethodState[] states = v.getScanMethodStates();
            
            for(int i=0; i<states.length; i++){
                this.addObjectToList(PROP_SCANMETHODSTATES,
                                    new ScanMethodStateLatherValue(states[i]));
            }
        }
        this.setDoubleValue(PROP_STARTTIME, (double)v.getStartTime());
    }

    public ScanStateCore getScanStateCore(){
        ScanStateCore r = new ScanStateCore();

        r.setAreServersIncluded(this.getIntValue(PROP_ARESERVERSINCLUDED) == 1 
                                ? true : false);

        try {
            r.setCertDN(this.getStringValue(PROP_CERTDN));
        } catch(LatherKeyNotFoundException exc){}

        r.setEndTime((long)this.getDoubleValue(PROP_ENDTIME));

        try {
            LatherValue gExc;

            gExc = this.getObjectValue(PROP_GLOBALEXCEPTION);
            r.setGlobalException(((StringifiedExceptionLatherValue)
                                  gExc).getException());
        } catch(LatherKeyNotFoundException exc){}

        r.setIsDone(this.getIntValue(PROP_ISDONE) == 1 ? true : false);

        r.setIsInterrupted(this.getIntValue(PROP_ISINTERRUPTED) == 1 ? 
                           true : false);

        try {
            LatherValue plat;

            plat = this.getObjectValue(PROP_PLATFORM);
            r.setPlatform(((AiPlatformLatherValue)plat).getAIPlatformValue());
        } catch(LatherKeyNotFoundException exc){}

        try {
            ScanMethodState[] states;
            LatherValue[] lstates;

            lstates = this.getObjectList(PROP_SCANMETHODSTATES);
            states  = new ScanMethodState[lstates.length];
            for(int i=0; i<lstates.length; i++){
                ScanMethodStateLatherValue lState;

                lState    = (ScanMethodStateLatherValue)lstates[i];
                states[i] = lState.getScanMethodState();
            }
            r.setScanMethodStates(states);
        } catch(LatherKeyNotFoundException exc){
            r.setScanMethodStates(new ScanMethodState[0]);
        }

        r.setStartTime((long)this.getDoubleValue(PROP_STARTTIME));

        return r;
    }

    public void validate()
        throws LatherRemoteException
    {
    }
}
