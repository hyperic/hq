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

package org.hyperic.hq.agent.commands;

import org.hyperic.hq.agent.AgentAssertionException;
import org.hyperic.hq.agent.AgentRemoteException;
import org.hyperic.hq.agent.AgentRemoteValue;
import org.hyperic.hq.agent.PropertyPair;


// Abstract for now, since the Agent is currently not using it.
public abstract class SetProperties_args extends AgentRemoteValue {
    private static final String PARAM_PNAME  = "propName"; 
    private static final String PARAM_PVAL   = "propValue"; 
    private static final String PARAM_NPROPS = "nProps";   // # of properties

    private void setup(){
        this.setNumProperties(0);
    }

    public SetProperties_args(){
        super();
        this.setup();
    }

    public SetProperties_args(AgentRemoteValue args) 
        throws AgentRemoteException 
    {
        int nProps;

        this.setup();
        nProps = args.getValueAsInt(SetProperties_args.PARAM_NPROPS);

        for(int i=0; i<nProps; i++){
            String key = args.getValue(SetProperties_args.PARAM_PNAME + i);
            String val = args.getValue(SetProperties_args.PARAM_PVAL  + i);

            this.addProperty(key, val);
        }
    }

    public int getNumProperties(){
        String val = this.getValue(SetProperties_args.PARAM_NPROPS);

        return Integer.parseInt(val);
    }

    private void setNumProperties(int newMeas){
        super.setValue(SetProperties_args.PARAM_NPROPS, 
                       Integer.toString(newMeas));
    }

    public void addProperty(String key, String value){
        int curProps = this.getNumProperties();

        super.setValue(SetProperties_args.PARAM_PNAME + curProps, key);
        super.setValue(SetProperties_args.PARAM_PVAL  + curProps, value);

        this.setNumProperties(curProps + 1);
    }


    public void setValue(String key, String val){
        throw new AgentAssertionException("This should never be called");
    }

    public PropertyPair getProperty(int propNum){
        String kval = SetProperties_args.PARAM_PNAME + propNum;
        String nval = SetProperties_args.PARAM_PVAL + propNum;

        return new PropertyPair(this.getValue(kval), this.getValue(nval));
    }
}
