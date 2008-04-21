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

import org.hibernate.Query;
import org.hyperic.hq.authz.server.session.Resource;

/**
 * A {@link Critter} is a criteria, able to aid in composing complex SQL.
 */
public interface Critter {
    /**
     * Get a list of {@link CritterProps}s which are the current
     * values set for this critter.
     */
    List getProps();
    
    /**
     * Get a SQL segment, suitable for placement within a where clause.
     * 
     * The result of this method is run through 
     * {@link CritterTranslationContext#escapeSql(String)}, which will
     * turn all references delimited '@' into unique identifiers.
     * 
     * @param resourceAlias  The SQL alias for the {@link Resource}
     */
    String getSql(CritterTranslationContext ctx, String resourceAlias);
    
    /**
     * Get additional SQL specifying joins requierd by the critter
     * 
     * The result of this method is run through 
     * {@link CritterTranslationContext#escapeSql(String)} which will
     * turn all references delimited by '@' into unique identifiers.
     */
    String getSqlJoins(CritterTranslationContext ctx, String resourceAlias);
    
    /**
     * Bind any SQL parameters which were previously returned as part
     * of getSql().
     * 
     * The implementor of this method will likely need to use
     * {@link CritterTranslationContext#escape(String)} to bind to variables
     * which match the references returned from getSql, etc.
     */
    void bindSqlParams(CritterTranslationContext ctx, Query q);
    
    /**
     * Returns the {@link CritterType} associated with this Critter
     */
    CritterType getCritterType();
    
    /**
     * Returns a localized description of how the Critter is configured.
     * Should work in the following layout:
     * 
     * If all of the following criteria are met:
     *     - Resource name matches 'web.*' 
     *     - Resource type is Apache 2.0 
     *     
     * (or...)
     * 
     * If any of the following criteria are met:
     *     - Resource is a child of 'my.platform' and of type 'Fileserver File'
     *     - Resource has been modified in the last 2 days
     */
    String getConfig();
}
