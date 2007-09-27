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

package org.hyperic.hibernate.dialect;

import java.sql.Statement;
import java.sql.SQLException;

/**
 *
 */
public interface HQDialect
{
    /*
     * Returns the delete statement with joins for
     * a particular database
     */
    public String getDeleteJoinStmt(String deleteTable,
                                    String commonKey,
                                    String joinTables,
                                    String joinKeys,
                                    String condition,
                                    int limit);
    /*
     * Returns the table optimize statement for
     * a particular database
     *
     * @param table - name of table
     * @param cost - based on the database,
     *               may be table percentage or random number
     */
    public String getOptimizeStmt(String table, int cost);

    /*
     * Returns true if the database supports a multi insert stmt.
     */
    public boolean supportsMultiInsertStmt();

    /*
     * Returns true if the database contains the specified viewName
     */
    public boolean viewExists(Statement stmt, String viewName)
        throws SQLException;
    
    /**
     * Returns the limit string.
     * 
     * @param num The number of rows to limit by.
     * @return The limit string.
     */
    public String getLimitString(int num);
    
}
