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

package org.hyperic.lather.util;

import org.hyperic.lather.LatherKeyNotFoundException;
import org.hyperic.lather.LatherValue;
import org.hyperic.lather.client.LatherClient;
import org.hyperic.lather.client.LatherHTTPClient;

import org.hyperic.util.StringUtil;

/**
 * This class is a utility which calls a target method a number of
 * times, to determine the cost associated with the round-trip
 * lather invocation.
 */
public class LatherSpeed {
    public static class WeenieLatherValue
        extends LatherValue
    {
        public WeenieLatherValue(){
            super();
            this.setStringValue("testArg", "testArgValue");
        }

        public void setStringValue(String key, String value){
            super.setStringValue(key, value);
        }

        public String getStringValue(String key)
            throws LatherKeyNotFoundException
        {
            return super.getStringValue(key);
        }

        public void validate(){
        }
    }

    public static void main(String[] args) 
        throws Exception 
    {
        WeenieLatherValue val, res;
        LatherClient client;
        long startTime, endTime;
        int iters;

        if(args.length != 3){
            System.err.println("Syntax: LatherSpeed <testURL> <method> " +
                               "<# iterations>");
            System.exit(-1);
            return;
        }

        client = new LatherHTTPClient(args[0]);
        iters  = Integer.parseInt(args[2]);
        val    = new WeenieLatherValue();

        startTime = System.currentTimeMillis();
        for(int i=0; i<iters; i++){
            res = (WeenieLatherValue)client.invoke(args[1], val);
        }
        endTime = System.currentTimeMillis();

        System.out.println(iters + " iterations executed in " +
                           StringUtil.formatDuration(endTime - startTime));
        System.out.println((iters / (double)(endTime - startTime) * 1000.0) +
                           " iterations per second");
    }
}
