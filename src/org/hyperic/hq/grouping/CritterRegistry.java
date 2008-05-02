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

package org.hyperic.hq.grouping;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hyperic.hq.grouping.critters.CompatGroupTypeCritterType;
import org.hyperic.hq.grouping.critters.DescendantProtoCritterType;
import org.hyperic.hq.grouping.critters.GroupMembershipCritterType;
import org.hyperic.hq.grouping.critters.MixedGroupTypeCritterType;
import org.hyperic.hq.grouping.critters.NonSystemCritterType;
import org.hyperic.hq.grouping.critters.ProtoCritterType;
import org.hyperic.hq.grouping.critters.ProtoNameCritterType;
import org.hyperic.hq.grouping.critters.ResourceNameCritterType;
import org.hyperic.hq.grouping.critters.ResourceTypeCritterType;
import org.hyperic.hq.grouping.critters.OwnedCritterType;

public class CritterRegistry {
    private static final CritterRegistry INSTANCE = new CritterRegistry();

    private final Log _log = LogFactory.getLog(CritterRegistry.class);
    // maps CritterType class names to CritterType instances
    private final Map _registry = new HashMap();
    
    private CritterRegistry() {
        // Seed ourselves with a few types
        register(new ResourceNameCritterType());
        register(new GroupMembershipCritterType());
        register(new MixedGroupTypeCritterType());
        register(new CompatGroupTypeCritterType());
        register(new ResourceTypeCritterType());
        register(new ProtoNameCritterType());
        register(new ProtoCritterType());
        register(new DescendantProtoCritterType());
        register(new NonSystemCritterType());
        register(new OwnedCritterType());
    }
    
    public static final CritterRegistry getRegistry() {
        return INSTANCE;
    }
    
    public void register(CritterType type) {
        _log.info("Registrying Critter: " + type.getName());
        _log.debug("               from: " + type.getClass().getName());
        
        // map CritterType class names to CritterType instances
        synchronized (_registry) {
            _registry.put(type.getClass().getName(), type);
        }
    }
    
    public void unregister(CritterType type) {
        _log.info("Unregistrying Critter: " + type.getName());
        
        synchronized (_registry) {
            _registry.remove(type.getClass().getName());
        }
    }
    
    public Collection getCritterTypes() {
        synchronized (_registry) {
            return Collections.unmodifiableCollection(_registry.values());
        }
    }
    
    /**
     * Returns the registered CritterType instance with the given class name
     */
    public CritterType getCritterTypeForClass(String className) {
        synchronized (_registry) {
            return (CritterType)_registry.get(className);
        }
    }
}
