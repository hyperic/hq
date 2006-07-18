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
import org.hyperic.hq.autoinventory.ServerSignature;

public class ServerSignatureValue
    extends LatherValue
{
    private static final String PROP_SERVERTYPENAME  = "serverTypeName";
    private static final String PROP_FILEPATTERNS    = "filePatterns";
    private static final String PROP_FILEEXPATTERNS  = "fileExcludePatterns";
    private static final String PROP_REGPATTERNS     = "registryPatterns";

    public ServerSignatureValue(){
        super();
    }

    public ServerSignatureValue(ServerSignature sig){
        super();

        String[] vals;

        this.setStringValue(PROP_SERVERTYPENAME, sig.getServerTypeName());

        vals = sig.getFileMatchPatterns();
        for(int i=0; i<vals.length; i++){
            this.addStringToList(PROP_FILEPATTERNS, vals[i]);
        }

        vals = sig.getFileExcludePatterns();
        for(int i=0; i<vals.length; i++){
            this.addStringToList(PROP_FILEEXPATTERNS, vals[i]);
        }

        vals = sig.getRegistryMatchPatterns();
        for(int i=0; i<vals.length; i++){
            this.addStringToList(PROP_REGPATTERNS, vals[i]);
        }
    }

    private String[] getStrings(String prop) {
        try {
            return this.getStringList(prop);
        } catch (LatherKeyNotFoundException e) {
            return null; //ok
        }
    }
    
    public ServerSignature getSignature(){
        ServerSignature res = new ServerSignature();

        res.setServerTypeName(this.getStringValue(PROP_SERVERTYPENAME));
        res.setFileMatchPatterns(getStrings(PROP_FILEPATTERNS));
        res.setFileExcludePatterns(getStrings(PROP_FILEEXPATTERNS));
        res.setRegistryMatchPatterns(getStrings(PROP_REGPATTERNS));

        return res;
    }

    public void validate()
        throws LatherRemoteException
    {
        try {
            this.getSignature();
        } catch(LatherKeyNotFoundException exc){
            throw new LatherRemoteException("All values not set");
        }
    }
}

