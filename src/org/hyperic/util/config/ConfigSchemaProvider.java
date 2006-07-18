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

package org.hyperic.util.config;

/**
 * A simple interface that allows us to abstract where the schema comes from.
 */
public interface ConfigSchemaProvider {

    /**
     * @param iterationCount The iterationCount.  A schema provider 
     * can return multiple schemas.  If this method returns null, then
     * that means there are no more schemas to provide.
     * @param previous The responses to the previous ConfigSchema.
     * If the iterationCount is zero, then this value should be
     * null - or if not it should be ignored.
     * @return A ConfigSchema from this provider, or null if there
     * are no more schemas.
     * @exception EarlyExitException If the schema provider wants
     * to bail out early from the entire config process.
     */
    public ConfigSchema getSchema (ConfigResponse previous,
                                   int iterationCount) 
        throws EarlyExitException;

    /**
     * @param config The complete config response for this schema.
     * @return Informative text about the result of the config operations.
     * If there is no such text available, this method may return null.
     */
    public String getCompletionText (ConfigResponse config);
}
