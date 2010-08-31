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

import org.hyperic.hq.bizapp.shared.lather.SecureAgentLatherValue;

import org.hyperic.lather.LatherKeyNotFoundException;
import org.hyperic.lather.LatherRemoteException;

public class ControlSendCommandResult_args
    extends SecureAgentLatherValue
{
    private static final String PROP_NAME      = "name";
    private static final String PROP_ID        = "id";
    private static final String PROP_RESULT    = "result";
    private static final String PROP_STARTTIME = "startTime";
    private static final String PROP_ENDTIME   = "endTime";
    private static final String PROP_MESSAGE   = "message";

    public ControlSendCommandResult_args(){
        super();
    }

    public void setName(String name){
        this.setStringValue(PROP_NAME, name);
    }

    public String getName(){
        try {
            return this.getStringValue(PROP_NAME);
        } catch(LatherKeyNotFoundException exc){
            return null;
        }
    }

    public void setId(int id){
        this.setIntValue(PROP_ID, id);
    }

    public int getId(){
        return this.getIntValue(PROP_ID);
    }

    public void setResult(int result){
        this.setIntValue(PROP_RESULT, result);
    }

    public int getResult(){
        return this.getIntValue(PROP_RESULT);
    }

    public void setStartTime(long startTime){
        this.setDoubleValue(PROP_STARTTIME, startTime);
    }

    public long getStartTime(){
        return (long)this.getDoubleValue(PROP_STARTTIME);
    }

    public void setEndTime(long endTime){
        this.setDoubleValue(PROP_ENDTIME, endTime);
    }

    public long getEndTime(){
        return (long)this.getDoubleValue(PROP_ENDTIME);
    }

    public void setMessage(String message){
        if(message == null)
            return;

        this.setStringValue(PROP_MESSAGE, message);
    }

    public String getMessage(){
        try {
            return this.getStringValue(PROP_MESSAGE);
        } catch(LatherKeyNotFoundException exc){
            return null;
        }
    }

    public void validate()
        throws LatherRemoteException
    {
        try {
            this.getId();
            this.getResult();
            this.getStartTime();
            this.getEndTime();
        } catch(LatherKeyNotFoundException exc){
            throw new LatherRemoteException("All values not set");
        }
    }
}
