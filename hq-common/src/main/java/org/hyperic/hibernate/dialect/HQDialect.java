/*                                                                 
 * NOTE: This copyright does *not* cover user programs that use HQ 
 * program services by normal system calls through the application 
 * program interfaces provided as part of the Hyperic Plug-in Development 
 * Kit or the Hyperic Client Development Kit - this is merely considered 
 * normal use of the program, and does *not* fall under the heading of 
 * "derived work". 
 *  
 * Copyright (C) [2004-2007], Hyperic, Inc. 
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

import java.sql.SQLException;
import java.sql.Statement;

public interface HQDialect
{
    /**
     * Returns the table optimize statement for
     * a particular database
     *
     * @param table - name of table
     * @param cost - based on the database,
     *               may be table percentage or random number
     */
    public String getOptimizeStmt(String table, int cost);

    /**
     * Returns true if using the EAM_MEASUREMENT_DATA view instead of
     * constructing it dynamically is optimal
     */
    public boolean useMetricUnion();

    /**
     * Returns true if using the EAM_NUMBERS table to iterate over a
     * certain amount of time ranges is optimal for the database
     */
    public boolean useEamNumbers();

    /**
     * Returns -1 if Max Expressions supported in the db is unlimited.
     * This applies mainly to SQL in statements e.g. where ids in (0, 1, 2,...)
     * Or a sequence of SQL 'and' or 'or' statements in one statement
     */
    public int getMaxExpressions();

    /**
     * Returns true if the database supports a multi insert stmt.
     */
    public boolean supportsMultiInsertStmt();
    

    /**
     * Returns a db specific SQL syntax for a POSIX style Regular Expression.
     * @param column - the column to match against
     * @param regex - the POSIX style regex.  The param passed in allows for
     * prepared statement type syntax.  If that is not desired put quotes around
     * the value to ensure the SQL will not fail.
     * @param ignoreCase - similar to grep -i
     * @param invertMatch - similar to grep -v
     */
    public String getRegExSQL(String column, String regex, boolean ignoreCase,
                              boolean invertMatch);

    /**
     * Returns true if the database supports an insert stmt which
     * updates when the unique key is violated
     */
    public boolean supportsDuplicateInsertStmt();

    /**
     * Returns true if the database contains the specified tableName
     */
    public boolean tableExists(Statement stmt, String tableName)
        throws SQLException;
    
    /**
     * Returns the limit string.
     * 
     * @param num The number of rows to limit by.
     * @return The limit string.
     */
    public String getLimitString(int num);

    /**
     * Returns true if the database contains the specified viewName
     */
    public boolean viewExists(Statement stmt, String viewName)
        throws SQLException;

    /**
     * If true, tells the ComboGenerator to use the SequenceGenerator, else
     * uses the MultipleHiLoPerTableGenerator
     */
    public boolean usesSequenceGenerator();

    /**
     * true if the database supports PL/SQL
     */
    public boolean supportsPLSQL();
    
    /**
     * @return the limit sql associated with the offset and limit params
     */
    public String getLimitBuf(String sql, int offset, int limit);

    /**
     * @return emptyString or a string representing the optimizer hint.  null is not expected here
     */
    public String getMetricDataHint();
    
    public Long getSchemaCreationTimestampInMillis(Statement stmt) throws SQLException;

    /**
     * @return true if HQ should analyze the database type, false if it should not
     */
    public boolean analyzeDb();

    public boolean supportsAsyncCommit();

    public String getSetAsyncCommitStmt(boolean on);
}
