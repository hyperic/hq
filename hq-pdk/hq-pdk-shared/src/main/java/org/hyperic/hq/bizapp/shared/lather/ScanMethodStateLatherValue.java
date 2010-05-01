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
import org.hyperic.hq.appdef.shared.AIServerValue;
import org.hyperic.hq.autoinventory.ScanMethodState;
import org.hyperic.util.StringifiedException;

public class ScanMethodStateLatherValue
    extends LatherValue
{
    private static final String PROP_EXCEPTIONS  = "exceptions";
    private static final String PROP_METHODCLASS = "methodClass";
    private static final String PROP_SERVERS     = "servers";
    private static final String PROP_STATUS      = "status";

    public ScanMethodStateLatherValue(){
        super();
    }

    public ScanMethodStateLatherValue(ScanMethodState v){
        super();

        if(v.getMethodClass() != null){
            this.setStringValue(PROP_METHODCLASS, v.getMethodClass());
        }

        if(v.getStatus() != null){
            this.setStringValue(PROP_STATUS, v.getStatus());
        }

        if(v.getExceptions() != null){
            StringifiedException[] excs = v.getExceptions();

            for(int i=0; i<excs.length; i++){
                this.addObjectToList(PROP_EXCEPTIONS,
                                new StringifiedExceptionLatherValue(excs[i]));
            }
        }

        if(v.getServers() != null){
            AIServerValue[] svrs = v.getServers();

            for(int i=0; i<svrs.length; i++){
                this.addObjectToList(PROP_SERVERS,
                                     new AiServerLatherValue(svrs[i]));
            }
        }
    }

    public ScanMethodState getScanMethodState(){
        ScanMethodState r = new ScanMethodState();

        try {
            r.setMethodClass(this.getStringValue(PROP_METHODCLASS));
        } catch(LatherKeyNotFoundException exc){}

        try {
            r.setStatus(this.getStringValue(PROP_STATUS));
        } catch(LatherKeyNotFoundException exc){}

        try {
            StringifiedException[] excs;
            LatherValue[] lexcs = this.getObjectList(PROP_EXCEPTIONS);

            excs = new StringifiedException[lexcs.length];
            for(int i=0; i<lexcs.length; i++){
                StringifiedExceptionLatherValue lExc;

                lExc    = (StringifiedExceptionLatherValue)lexcs[i];
                excs[i] = lExc.getException();
            }
            r.setExceptions(excs);
        } catch(LatherKeyNotFoundException exc){}

        try {
            AIServerValue[] svrs;
            LatherValue[] lsvrs = this.getObjectList(PROP_SERVERS);

            svrs = new AIServerValue[lsvrs.length];
            for(int i=0; i<lsvrs.length; i++){
                AiServerLatherValue lSvr;

                lSvr    = (AiServerLatherValue)lsvrs[i];
                svrs[i] = lSvr.getAIServerValue();
            }
            r.setServers(svrs);
        } catch(LatherKeyNotFoundException exc){}

        return r;
    }

    public void validate()
        throws LatherRemoteException
    {
    }
}
