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

package org.hyperic.util;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

public class CollectionUtil {
    private CollectionUtil() {}
    
    /**
     * Transfer up to 'maxElems' elements from the source to the destination.
     */
    public static void transfer(Collection source, Collection dest, 
                                int maxElems) 
    {
        int j = 0;
        for (Iterator i=source.iterator(); i.hasNext() && j < maxElems; j++) {
            dest.add(i.next());
            i.remove();
        }
    }

    private static void testTransfer() {
        List src, targ;
        
        src  = new ArrayList();
        targ = new ArrayList();
        
        transfer(src, targ, 10);
        assert src.isEmpty() && targ.isEmpty();
        
        src.add("1");
        transfer(src, targ, 0);
        assert src.size() == 1 && targ.isEmpty();
        transfer(src, targ, 1);
        assert src.isEmpty() && targ.size() == 1;
        
        targ.clear();
        src.add("1");
        transfer(src, targ, 10);
        assert src.isEmpty() && targ.size() == 1;
        
    }
    
    public static void main(String[] args) {
        testTransfer();
    }
}
