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

package org.hyperic.hibernate;

/**
 * Implementors will be noticed by the container when it updates the creation 
 * time or last modified time for container managed objects. The creation time 
 * and last modified time management policy (container vs. explicit) 
 * may be defined independently depending on the implementation.
 * 
 * @see HypericInterceptor
 */
public interface ContainerManagedTimestampTrackable {
    
    /**
     * Define the management policy for the creation time. If the creation time 
     * is set explicitly, an explicit management policy will be assumed always, 
     * regardless of the return value.
     * 
     * @return <code>true</code> to allow the container to manage the creation time; 
     *         <code>false</code> to manage the creation time explicitly. If 
     *         the creation time is unspecified, then the implementation should 
     *         return <code>false</code>.
     */
    boolean allowContainerManagedCreationTime();
    
    /**
     * Define the management policy for the last modified time.
     * 
     * @return <code>true</code> to allow the container to manage the last modified time; 
     *         <code>false</code> to manage the last modified time explicitly. 
     *         If the last modified time is unspecified, then the implementation 
     *         should return <code>false</code>.
     */
    boolean allowContainerManagedLastModifiedTime();

}
