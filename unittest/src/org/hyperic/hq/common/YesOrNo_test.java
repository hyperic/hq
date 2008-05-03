/*
 * NOTE: This copyright does *not* cover user programs that use HQ
 * program services by normal system calls through the application
 * program interfaces provided as part of the Hyperic Plug-in Development
 * Kit or the Hyperic Client Development Kit - this is merely considered
 * normal use of the program, and does *not* fall under the heading of
 * "derived work".
 *
 * Copyright (C) [2004-2008], Hyperic, Inc.
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

package org.hyperic.hq.common;

import junit.framework.TestCase;

/**
 * Tests the YesOrNo class.
 */
public class YesOrNo_test extends TestCase {
    
    public YesOrNo_test(String name) {
        super(name);
    }
    
    public void testToBoolean() {
        assertEquals(Boolean.TRUE, YesOrNo.YES.toBoolean());
        assertEquals(Boolean.FALSE, YesOrNo.NO.toBoolean());
    }
    
    public void testValueForString() {        
        assertEquals(YesOrNo.NO, YesOrNo.valueFor(null));
        
        assertEquals(YesOrNo.NO, YesOrNo.valueFor("t"));
        assertEquals(YesOrNo.NO, YesOrNo.valueFor("n"));
        assertEquals(YesOrNo.NO, YesOrNo.valueFor("N"));
        assertEquals(YesOrNo.NO, YesOrNo.valueFor(" N "));

        assertEquals(YesOrNo.YES, YesOrNo.valueFor("y"));
        assertEquals(YesOrNo.YES, YesOrNo.valueFor("Y"));
        assertEquals(YesOrNo.YES, YesOrNo.valueFor(" Y "));
        
        assertEquals(YesOrNo.NO, YesOrNo.valueFor("true"));
        assertEquals(YesOrNo.NO, YesOrNo.valueFor("no"));
        assertEquals(YesOrNo.NO, YesOrNo.valueFor("No"));
        assertEquals(YesOrNo.NO, YesOrNo.valueFor(" NO "));

        assertEquals(YesOrNo.YES, YesOrNo.valueFor("yes"));
        assertEquals(YesOrNo.YES, YesOrNo.valueFor(" yes "));
        assertEquals(YesOrNo.YES, YesOrNo.valueFor("YeS"));
    }

}
