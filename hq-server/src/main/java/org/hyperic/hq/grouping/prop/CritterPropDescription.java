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
 * The CritterPropDescription objects are immutable beans, created by
 * {@link CritterType}s to inform the external world about what types
 * of arguments they take.
 * 
 * A list of {@link CritterPropDescription}s can easily be displayed in the UI
 * in the following format:
 * 
 * name:  purpose  (type.getValue())
 *  ...
 *  
 * For instance: 
 *     
 *     Name: a regex to match against the resource name (string) 
 *     Age:  only allow resources created before this time (date)
 *     
 *     
 * Subclasses of this should exist as types in {@link CritterPropType} 
 */
public interface CritterPropDescription  {
    /**
     * Returns the ID for the {@link CritterProp} associated with this
     * description.
     * 
     * The return value from this method should be equal to the value of
     * CritterProp.getId()
     */
    String getId();
    
    /**
     * Returns a localized name for this prop.  Used by the UI
     * to generically argument types for critters.
     */
    String getName();

    /**
     * Return the type (I am a DATE, I am a STRING, etc.)
     */
    CritterPropType getType();

    /**
     * Return a localized purpose.
     */
    public String getPurpose();

    /**
     * If true, the associated {@link CritterProp} is required.
     */
    public boolean isRequired();
}
