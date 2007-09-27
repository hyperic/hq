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
import org.hyperic.util.StringifiedException;

public class StringifiedExceptionLatherValue
    extends LatherValue
{
    private static final String PROP_CTIME    = "cTime";
    private static final String PROP_CLASS    = "class";
    private static final String PROP_MSG      = "msg";
    private static final String PROP_STACK    = "stack";
    private static final String PROP_TOSTRING = "toString";

    public StringifiedExceptionLatherValue(){
        super();
    }

    public StringifiedExceptionLatherValue(StringifiedException exc){
        super();

        this.setDoubleValue(PROP_CTIME,    (double)exc.getCTime());
        this.setStringValue(PROP_CLASS,    exc.getExceptionClass());
        if(exc.getMessage() != null){
            this.setStringValue(PROP_MSG, exc.getMessage());
        }
        this.setStringValue(PROP_STACK,    exc.getStackTrace());
        this.setStringValue(PROP_TOSTRING, exc.getToString());
    }

    public StringifiedException getException(){
        StringifiedException res;

        res = new StringifiedException();
        res.setCTime((long)this.getDoubleValue(PROP_CTIME));
        res.setExceptionClass(this.getStringValue(PROP_CLASS));
        try {
            res.setMessage(this.getStringValue(PROP_MSG));
        } catch(LatherKeyNotFoundException exc){}       
        res.setStackTrace(this.getStringValue(PROP_STACK));
        res.setToString(this.getStringValue(PROP_TOSTRING));
        return res;
    }

    public void validate()
        throws LatherRemoteException
    {
        try {
            this.getException();
        } catch(LatherKeyNotFoundException exc){
            throw new LatherRemoteException("All values not set");
        }
    }
}

