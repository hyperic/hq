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

package org.hyperic.hq.livedata;

import org.hyperic.hq.livedata.shared.LiveDataCommand;
import org.hyperic.util.config.ConfigResponse;
import org.hyperic.util.config.ConfigSchema;

/**
 * Implementations of this interface are able to convert objects returned
 * from LiveData commands into strings, suitable for display. 
 */
public interface LiveDataFormatter {
    /**
     * Format a result from a LiveData execution into a string 
     * @param cmd        The command that was executed
     * @param formatCfg  Configuration for the formatter (a response to
     *                   {@link getConfig(LiveDataCommand)})
     * @param val        The value to format
     */
    String format(LiveDataCommand cmd, ConfigResponse formatCfg, Object val);
    
    /**
     * Get configuration parameters used to pass to format()
     * @param cmd The command that will be executed and later formatted.
     */
    ConfigSchema getConfig(LiveDataCommand cmd);
    
    /**
     * Return true if the formatter can format the specified command.
     */
    boolean canFormat(LiveDataCommand cmd);
    
    /**
     * Get the name of the formatter, used for display purposes.
     */
    String getName();

    /**
     * Get the description of the formatter
     */
    String getDescription();
}
