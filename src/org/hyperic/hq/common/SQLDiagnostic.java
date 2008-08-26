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

package org.hyperic.hq.common;

public class SQLDiagnostic {
    
    private String testQuery;
    private String fixQuery;
    
    /**
     * 
     * Creates an instance of SQLDiagnostic with the given test and fix database-specific SQL
     *
     * @param test
     * @param fix
     */
    public SQLDiagnostic(String test, String fix) {
        testQuery = test;
        fixQuery = fix;
    }
    
    /**
     * Returns the database-specific SQL query whose execution returns all problematic rows 
     * matching the given criteria.
     * 
     * @return Strings representing the SQL query
     */
    public String getTestQuery() {
        return testQuery;
    }
    
    /**
     * Returns the database-specific SQL query whose execution will fix the problem
     * diagnosed by running the test query SQL obtained by running getTestQuery
     * 
     * @return String representing the SQL fix query
     */
    public String getFixQuery() {
        return fixQuery;
    }
    
    
}
