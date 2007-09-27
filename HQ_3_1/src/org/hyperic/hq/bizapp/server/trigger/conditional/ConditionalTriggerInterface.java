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

package org.hyperic.hq.bizapp.server.trigger.conditional;

import java.util.HashMap;

import org.hyperic.hq.appdef.shared.AppdefEntityID;
import org.hyperic.hq.bizapp.shared.ConditionalTriggerSchema;
import org.hyperic.hq.events.shared.AlertConditionValue;
import org.hyperic.util.config.ConfigResponse;
import org.hyperic.util.config.ConfigSchema;
import org.hyperic.util.config.InvalidOptionException;
import org.hyperic.util.config.InvalidOptionValueException;

/**
 * 
 * Interface for triggers that derive its configuration values from
 * AlertCondition
 */
public interface ConditionalTriggerInterface {
    public static final String CFG_ID = ConditionalTriggerSchema.CFG_ID;

    public static final String CFG_COMPARATOR =
        ConditionalTriggerSchema.CFG_COMPARATOR;

    public static final String CFG_NAME = ConditionalTriggerSchema.CFG_NAME;

    public static final String CFG_OPTION = ConditionalTriggerSchema.CFG_OPTION;

    public static final String CFG_THRESHOLD =
        ConditionalTriggerSchema.CFG_THRESHOLD;

    public static final String CFG_TYPE = ConditionalTriggerSchema.CFG_TYPE;
    
    public static HashMap MAP_COND_TRIGGER = new HashMap();
    
    /**
     * Translate alert condition to config response object
     * @param cond the alert condition
     * @return the equivalent config response object
     */
    public ConfigResponse getConfigResponse(AppdefEntityID id,
                                            AlertConditionValue cond)
        throws InvalidOptionException, InvalidOptionValueException;
    
    /**
     * Return the schema for the configuration of this trigger.  This does not
     * actually get called by anyone, it's here to enforce the authors of
     * triggers to implement getConfigSchema() for the CLI.
     */
    public ConfigSchema getConfigSchema();
}
