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

package org.hyperic.util.xmlparser;

/**
 * This class is used to describe which subtags a given tag supports.
 * The object has a 'tag', which will be traversed to, and a validity
 * requirement, indicating if the subtag must exist in the tag, is
 * optional, is valid one or more times, or is valid zero or more times.
 */

public class XmlTagInfo {
    public static final int REQUIRED     = 0;
    public static final int OPTIONAL     = 1;
    public static final int ONE_OR_MORE  = 2;
    public static final int ZERO_OR_MORE = 3;

    private XmlTagHandler tag;
    private int                type;

    /**
     * Create a new tag info object with the specified tag and type.
     *
     * @param tag  The sub tag which will be traversed to
     * @param type One of REQUIRED, OPTIONAL, ONE_OR_MORE, ZERO_OR_MORE
     */
    public XmlTagInfo(XmlTagHandler tag, int type){
        this.tag  = tag;
        this.type = type;

        if(type < REQUIRED || type > ZERO_OR_MORE)
            throw new IllegalArgumentException("Unknown type: " + type);
    }

    XmlTagHandler getTag(){
        return this.tag;
    }

    int getType(){
        return this.type;
    }
}
