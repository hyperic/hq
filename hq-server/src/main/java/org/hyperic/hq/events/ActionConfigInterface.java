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

/*
 * Interface which defines what the ActionConfig classes should implement
 * 
 * Created on Mar 31, 2003
 */
package org.hyperic.hq.events;

import org.hyperic.util.config.ConfigResponse;
import org.hyperic.util.config.ConfigSchema;
import org.hyperic.util.config.InvalidOptionException;
import org.hyperic.util.config.InvalidOptionValueException;

/**
 */
public interface ActionConfigInterface {
    /** 
     * Get the configuration schema for the action
     * 
     * @return a config schema
     */    
    public ConfigSchema getConfigSchema();
    
    /** 
     * Get the configuration response for the action based on its properties
     * 
     * @return a config schema
     */    
    public ConfigResponse getConfigResponse()
        throws InvalidOptionException, InvalidOptionValueException;

    /**
     * Initialize the action instance
     * 
     * @param config configuration properties
     */    
    public void init(ConfigResponse config) throws InvalidActionDataException;

    /**
     * Return the name of the class that implements the action
     *
     * @return the Action classname
     */
    public String getImplementor();        

    /**
     * Allow implementor to be overwritten
     *
     * @param the Action classname
     */
    public void setImplementor(String implementor);        
}
