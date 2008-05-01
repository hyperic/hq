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

package org.hyperic.hq.grouping.prop;

/**
 * The CritterProp is a data storage class, containing the values used
 * by critter when evaluating.  
 * 
 * A CritterProp contains an id and type and basically looks like an
 * encapsulated method argument.
 * 
 * Critters are evaulated like: (pseudo code)
 *    Critter1.evaluate(StringCritterProp resourceName, DateCritterProp since)
 *    
 * Where 'StringCritterProp' equates to something implementing this type
 * and 'resourceName' equates to the name of the variable passed to evaluation.
 */
public interface CritterProp {
    /**
     * Get some id that uniqely defines this prop in the context of a 
     * critter prop list.
     * 
     * e.g.  resourceName, startDate, etc.
     * 
     * This id allows an XML document to setup new props. 
     */
    String getId();
    
    
    /**
     * The type that created this prop.
     */
    CritterPropType getType();
}
