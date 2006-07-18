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

package org.hyperic.hq.appdef.shared.miniResourceTree;

import org.hyperic.hq.appdef.shared.MiniResourceValue;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;

/**
 * A tree which represents portions of a hierarchy.  The tree
 * representation describes the relationships between all the 
 * different types (platform, server, service, application), 
 */
public class MiniResourceTree 
    implements java.io.Serializable
{
    private ArrayList platforms;

    public MiniResourceTree(){
        this.platforms = new ArrayList();
    }
    
    public MiniPlatformNode addPlatform(MiniResourceValue platform){

        MiniPlatformNode node = new MiniPlatformNode(this, platform);

        this.platforms.add(node);

        return node;
    }

    public void removePlatform(MiniPlatformNode platform){
        this.platforms.remove(platform);
    }

    public List getPlatforms(){
        // Return a list of platforms, sorted by resource creation time.
        Collections.sort(this.platforms,
                         new PlatformNodeComparatorDesc());

        return this.platforms;
    }

    public Iterator getPlatformIterator(){
        return this.platforms.iterator();
    }

    public String toString(){
        return "Resource Tree: Platforms=" + this.platforms;
    }

    private class PlatformNodeComparatorDesc implements Comparator {
        
        public int compare(Object o1, Object o2) {
            MiniPlatformNode p1 = (MiniPlatformNode)o1;
            MiniPlatformNode p2 = (MiniPlatformNode)o2;
            
            Long c1 = p1.getResourceCTime();
            Long c2 = p2.getResourceCTime();
            
            return -(c1.compareTo(c2));
        }
        
        public boolean equals(Object other) {
            return false;
        }
    }
}
