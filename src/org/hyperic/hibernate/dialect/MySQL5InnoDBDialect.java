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

import org.hibernate.MappingException;

/**
 * HQ's version of MySQL5InnoDBDialect to create pseudo sequences
 */
public class MySQL5InnoDBDialect
    extends org.hibernate.dialect.MySQL5InnoDBDialect
    implements HQDialect
{

    /*
     * Database table and function to support sequences.  It is assumed that
     * the database has already been prepped by running the following SQL.

        CREATE TABLE `HQ_SEQUENCE` (
            `seq_name` char(50) NOT NULL PRIMARY KEY,
            `seq_val` int(11) DEFAULT NULL
        );
    
        DELIMITER |
        
        CREATE FUNCTION nextseqval (iname CHAR(50))
         RETURNS INT
         DETERMINISTIC
         BEGIN
          SET @new_seq_val = 0;
          UPDATE HQ_SEQUENCE set seq_val = @new_seq_val:=seq_val+1
           WHERE seq_name=iname;
          RETURN @new_seq_val;
         END;

        |
    
     */

    public String getOptimizeStmt(String table, int cost)
    {
        return "ANALYZE TABLE "+table.toUpperCase();
    }

    public String getDeleteJoinStmt(String deleteTable,
                                    String commonKey,
                                    String joinTables,
                                    String joinKeys,
                                    String condition,
                                    int limit)
    {
        String cond = (condition.matches("^\\s*$")) ? "" : " and "+condition;
        String limitCond = (limit <= 0) ? "" : " LIMIT "+limit;
        return ("DELETE FROM "+deleteTable+" WHERE EXISTS"+
               " (SELECT "+commonKey+" FROM "+joinTables+
               " WHERE "+joinKeys+cond+")").toUpperCase();
    }

    public boolean supportsSequences() {
        return true;
    }
    private final static String SEQUENCE_TABLE = "HQ_SEQUENCE";
    private final static String SEQUENCE_NAME  = "seq_name";
    private final static String SEQUENCE_VALUE = "seq_val";

    protected String getCreateSequenceString(String sequenceName)
        throws MappingException {
        return "INSERT INTO " + SEQUENCE_TABLE +
               " (" + SEQUENCE_NAME + "," + SEQUENCE_VALUE + ") VALUES ('" +
               sequenceName + "', " + HypericDialectConstants.SEQUENCE_START +
               ")";
    }

    protected String getDropSequenceString(String sequenceName)
        throws MappingException {
        return "DELETE FROM " + SEQUENCE_TABLE + " WHERE " +
               SEQUENCE_NAME + " = '" + sequenceName + "'";
    }

    public String getSequenceNextValString(String sequenceName)
        throws MappingException {
        return "SELECT " + getSelectSequenceNextValString(sequenceName);
    }

    public String getSelectSequenceNextValString(String sequenceName)
        throws MappingException {
        return "nextseqval('" + sequenceName + "')";
    }

    public String getQuerySequencesString() {
        return "SELECT " + SEQUENCE_TABLE + " FROM " + SEQUENCE_TABLE;
    }

    public boolean supportsMultiInsertStmt() {
        return true;
    }
}
