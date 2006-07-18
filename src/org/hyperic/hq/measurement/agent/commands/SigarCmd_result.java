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

package org.hyperic.hq.measurement.agent.commands;

import java.util.List;

import java.io.IOException;
import java.io.ByteArrayOutputStream;
import java.io.ByteArrayInputStream;
import java.io.ObjectOutputStream;
import java.io.ObjectInputStream;

import org.hyperic.util.encoding.Base64;

import org.hyperic.hq.agent.AgentRemoteException;
import org.hyperic.hq.agent.AgentRemoteValue;

public class SigarCmd_result extends AgentRemoteValue {

    private static final String PROP_LIST = "list";

    public SigarCmd_result()
    {
        super();
    }

    public void setList(List list) 
        throws AgentRemoteException
    {
        try {
            ByteArrayOutputStream bs = new ByteArrayOutputStream();
            ObjectOutputStream os = new ObjectOutputStream(bs);
            String propStr;

            os.writeObject(list);
            propStr = Base64.encode(bs.toByteArray());

            super.setValue(PROP_LIST, propStr);

        } catch (IOException e) {
            throw new AgentRemoteException("Unable to set list: " + e);
        }
    }

    public List getList() 
        throws AgentRemoteException
    {
        String propStr = this.getValue(PROP_LIST);
        List res;
        
        try {
            byte[] data = Base64.decode(propStr);
            ByteArrayInputStream bs = new ByteArrayInputStream(data);
            ObjectInputStream os = new ObjectInputStream(bs);
            
            res = (List)os.readObject();
            return res;
        } catch (IOException e) {
            throw new AgentRemoteException("Unable to get list: " + e);
        } catch (ClassNotFoundException e) {
            throw new AgentRemoteException("Unable to get list: " + e);
        }
    } 

    public SigarCmd_result(AgentRemoteValue args) 
        throws AgentRemoteException 
    {
        String res = args.getValue(PROP_LIST);

        // XXX: should validate the list (e.g. all have the same # of cols)
        super.setValue(PROP_LIST, res);
    }
}
