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
package org.hyperic.hq.grouping;

import java.util.List;

public interface CritterType {
    /**
     * Returns a list of {@link CritterPropDescription}s, decribing the order 
     * and constitution of the props
     * 
     * "What arguments do I need to configure this critter?"
     */
    List getPropDescriptions();
    
    /**
     * Returns a localized name of this critter
     *   'Resource Name'
     *   'Modified Time'
     */
    String getName();
    
    /**
     * Returns a localized description
     *   'Matches resource names against a regular expression'
     *   'Matches resources modified before or after a date' 
     */
    String getDescription();
    
    /**
     * Create a new instance of this critter type.  The passed props
     * must match what the type is asking for (via getPropDescriptions())
     * 
     * @param critterProps A list of {@link CritterProp}s, containing
     *                     the values need to create the new instance.
     */
    Critter newInstance(List critterProps)
        throws GroupException;
}
