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

package org.hyperic.lather.test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Random;

import junit.framework.TestCase;

import org.hyperic.util.StringUtil;
import org.hyperic.lather.LatherValue;
import org.hyperic.lather.xcode.LatherXCoder;

public class SpeedTest
    extends TestCase
{
    private static final int NUM_MEAS   = 1000;
    private static final int NUM_VALUES = 1000;
    
    public SpeedTest(String name){
        super(name);
    }

    /**
     * Generate a value similar to that used by the spider measurement
     * messages.
     */
    private LatherValue genValue(Random r){
        PassThroughLatherValue res = new PassThroughLatherValue();
        int cid = r.nextInt();

        for(int i=0; i<NUM_MEAS; i++){
            res.addIntToList("measCID", cid);
            res.addStringToList("measDSN", XCoderTest.getRandomString(r));
            res.addDoubleToList("measVal", r.nextDouble());
        }
        return res;
    }

    public void testSpeed() 
        throws Exception 
    {
        ByteArrayOutputStream bOs;
        ByteArrayInputStream bIs;
        DataOutputStream dOs;
        DataInputStream dIs;
        LatherXCoder xCoder = new LatherXCoder();
        ArrayList l, e;
        Random r = new Random();
        byte[] rawData;
        long start, end, nBytes;

        l = new ArrayList();
        for(int i=0; i<NUM_VALUES; i++){
            l.add(this.genValue(r));
        }

        bOs = new ByteArrayOutputStream();
        dOs = new DataOutputStream(bOs);
        start = System.currentTimeMillis();
        e = new ArrayList();
        for(Iterator i=l.iterator(); i.hasNext(); ){
            LatherValue v = (LatherValue)i.next();

            xCoder.encode(v, dOs);
            e.add(bOs.toByteArray());
            bOs.reset();
        }
        end = System.currentTimeMillis();

        l = null;
        System.gc();

        System.out.println("Encoded " + NUM_VALUES + " large entries in " +
                           StringUtil.formatDuration(end - start));

        start = System.currentTimeMillis();
        nBytes = 0;
        for(Iterator i=e.iterator(); i.hasNext(); ){
            rawData = (byte[])i.next();
            nBytes += rawData.length;
            bIs     = new ByteArrayInputStream(rawData);
            dIs     = new DataInputStream(bIs);

            xCoder.decode(dIs, PassThroughLatherValue.class);
        }
        end = System.currentTimeMillis();
        System.out.println("Decoded " + NUM_VALUES + " large entries in " +
                           StringUtil.formatDuration(end - start));
        System.out.println("Size of average encoded value = " + 
                           nBytes / NUM_VALUES + " bytes");
        
    }
}
