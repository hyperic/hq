/**
 * NOTE: This copyright does *not* cover user programs that use HQ
 * program services by normal system calls through the application
 * program interfaces provided as part of the Hyperic Plug-in Development
 * Kit or the Hyperic Client Development Kit - this is merely considered
 * normal use of the program, and does *not* fall under the heading of
 *  "derived work".
 *
 *  Copyright (C) [2009-2010], VMware, Inc.
 *  This file is part of HQ.
 *
 *  HQ is free software; you can redistribute it and/or modify
 *  it under the terms version 2 of the GNU General Public License as
 *  published by the Free Software Foundation. This program is distributed
 *  in the hope that it will be useful, but WITHOUT ANY WARRANTY; without
 *  even the implied warranty of MERCHANTABILITY or FITNESS FOR A
 *  PARTICULAR PURPOSE. See the GNU General Public License for more
 *  details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program; if not, write to the Free Software
 *  Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307
 *  USA.
 *
 */
package org.hyperic.hq.common.shared;

import java.util.Collection;
import java.util.Properties;
import java.util.Set;

import org.hyperic.hq.authz.server.session.AuthzSubject;
import org.hyperic.hq.common.ApplicationException;
import org.hyperic.hq.common.ConfigProperty;
import org.hyperic.util.ConfigPropertyException;

/**
 * Local interface for ServerConfigManager.
 */
public interface ServerConfigManager {
    /**
     * Get the "root" server configuration, that means those keys that have the
     * NULL prefix.
     * @return Properties
     */
    Properties getConfig() throws ConfigPropertyException;

    /**
     * Get the server configuration
     * @param prefix The prefix of the configuration to retrieve.
     * @return Properties
     */
    Properties getConfig(String prefix) throws ConfigPropertyException;

    /**
     * Set the server configuration
     * @throws ConfigPropertyException - if the props object is missing a key
     *         that's currently in the database
     */
    void setConfig(AuthzSubject subject, Properties newProps) throws ApplicationException,
        ConfigPropertyException;

    /**
     * Set the server Configuration
     * @param prefix The config prefix to use when setting properties. The
     *        prefix is used for namespace protection and property scoping.
     * @param newProps The Properties to set.
     * @throws ConfigPropertyException - if the props object is missing a key
     *         that's currently in the database
     */
    void setConfig(AuthzSubject subject, String prefix, Properties newProps) throws ApplicationException,
        ConfigPropertyException;

    
    /**
     * Get a specific server configuration property
     * @param name The name of the configuration property to retrieve.
     * @return Property value
     */    
    String getPropertyValue(String name);    
    
    /**
     * Run an analyze command on all non metric tables. The metric tables are
     * handled seperately using analyzeHqMetricTables() so that only the tables
     * that have been modified are analyzed.
     * @return The time taken in milliseconds to run the command.
     */
    long analyzeNonMetricTables();

    /**
     * Run an analyze command on both the current measurement data slice and the
     * previous data slice if specified.
     * @param analyzePrevMetricDataTable tells method to analyze previous metric
     *        data table as well as the current.
     * @return The time taken in milliseconds to run the command.
     */
    long analyzeHqMetricTables(boolean analyzePrevMetricDataTable);

    /**
     * Get all the {@link ConfigProperty}s
     */
    Collection<ConfigProperty> getConfigProperties();

    /**
     * Gets the GUID for this HQ server instance. The GUID is persistent for the
     * duration of an HQ install and is created upon the first call of this
     * method. If for some reason it can't be determined, 'unknown' will be
     * returned.
     */
    String getGUID();

    /**
     * 
     * @return major part of the server version - x.x or x.x.x. If pattern 
     * fails to match - returns the full server version.
     */
    String getServerMajorVersion();
    
  
    /**
     * @param subject
     * @param toDelete
     */
    void deleteConfig(AuthzSubject subject, Set<String> toDelete);
}
