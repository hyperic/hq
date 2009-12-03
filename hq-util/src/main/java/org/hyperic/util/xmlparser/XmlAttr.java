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
 * A class which defines an attribute and the conditions on which 
 * it is valid.  
 */
public class XmlAttr {
    public static final int REQUIRED = 0;
    public static final int OPTIONAL = 1;

    private String name;
    private int    type;

    /**
     * Create a new XmlAttr attribute with the given name, and
     * type.
     *
     * @param name The name of the XML tag attribute 
     * @param type One of REQUIRED or OPTIONAL, indicating that the
     *             attribute is required for the tag to be valid, or
     *             optional, meaning it is allowed but not required.
     */
    public XmlAttr(String name, int type){
        this.name = name;
        this.type = type;

        if(type != REQUIRED && type != OPTIONAL){
            throw new IllegalArgumentException("Invalid type: " + type);
        }
    }

    public String getName(){
        return this.name;
    }

    public int getType(){
        return this.type;
    }
}
