/*
 * NOTE: This copyright does *not* cover user programs that use Hyperic
 * program services by normal system calls through the application
 * program interfaces provided as part of the Hyperic Plug-in Development
 * Kit or the Hyperic Client Development Kit - this is merely considered
 * normal use of the program, and does *not* fall under the heading of
 * "derived work".
 *
 * Copyright (C) [2004-2011], VMware, Inc.
 * This file is part of Hyperic.
 *
 * Hyperic is free software; you can redistribute it and/or modify
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
package org.hyperic.tools.ant.utils;

import java.sql.DatabaseMetaData;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Statement;

import org.hyperic.hibernate.dialect.MySQL5InnoDBDialect;

/**
 * DataBase product based strategy dealing the sql syntax nuances of each of the products.<br> 
 * <br>
 * Enum member is mapped to the {@link DatabaseMetaData#getDatabaseProductName()} so that<br> 
 * <code>valueOf()</code> could be used as a command pattern.<br>
 * <br>
 * <b>Note:</b> Could not use the Dialect as the queries are much more specialized as well as<br>
 * the fact that the {@link MySQL5InnoDBDialect#getLimitString(String, int, int)}'s offset<br>
 * starts from 1 not 0 (misses the first record). 
 *  
 */
public enum DatabaseType { 
    
    MySQL{ 
        
         @Override
         public final String generatePagedQuery(final String tableName, 
                                 final String columnsClause, final String pkColumnName) {
             return String.format("SELECT %s, %s from %s limit ?,?", pkColumnName, columnsClause, 
                     tableName) ; 
         }//EOM 
         
         @Override
         public final PreparedStatement bindPageInfo(final PreparedStatement ps, 
                             final int iPageNumber, final int iPageSize, final int iNoOfChunks) throws SQLException{
             int iOffset = (iPageNumber == 0 ? 0 : (iPageNumber*iPageSize)) ;
             ps.setInt(1, iOffset) ; 
             ps.setInt(2, iPageSize) ; 
             return ps ; 
         }//EOM

    },//EO MySQL  
    Oracle{ 
        @Override
        public final String generatePagedQuery(final String tableName, 
                            final String columnsClause, final String pkColumnName) {
            
            return String.format("select %s, %s from %s where %s in " +
                    "(select %s from (select %s, rownum rn from %s) where rn > ? and rn <= ?)", 
                    pkColumnName, 
                    columnsClause, 
                    tableName, 
                    pkColumnName, 
                    pkColumnName, 
                    pkColumnName, 
                    tableName) ;
        }//EOM 
        
        @Override
        public final String generateUpdateQuery(final String tableName, final String columnsClause, 
                final String pkColumnName) { 
            return super.generateUpdateQuery(tableName, columnsClause, pkColumnName) ;
        }//EOM
        
        @Override
        public final PreparedStatement bindPageInfo(final PreparedStatement ps, 
                                final int iPageNumber, final int iPageSize, final int iNoOfChunks) 
                                                                   throws SQLException{
            final int iOffset = (iPageNumber == 0 ? 0 : (iPageNumber*iPageSize)) ;
            ps.setInt(1, iOffset) ; 
            ps.setInt(2, (iOffset+iPageSize) ) ; 
            return ps ; 
        }//EOM 
        
        @Override
        public final StringBuilder appendModuloClause(final String columnName, final int modolu, final StringBuilder stmtBuilder) {
            return stmtBuilder.append("mod(").append(columnName).append(",").append(modolu).append(")") ; 
        }//EOM 
        
        
    },//EO Oracle
    PostgreSQL{
        @Override
        public final String generatePagedQuery(final String tableName, 
                                final String columnsClause, final String pkColumnName) {
            /*return String.format("SELECT %s, %s from %s order by %s offset ? limit ?", pkColumnName, columnsClause,
                    tableName, pkColumnName) ;*/
            return String.format("SELECT %s, %s FROM %s where id %% ? = ?", pkColumnName, columnsClause, tableName) ; 
        }//EOM 
        
        @Override
        public final PreparedStatement bindPageInfo(final PreparedStatement ps, 
                                final int iPageNumber, final int iPageSize, final int iNoOfChunks) 
                                                                    throws SQLException{
            ps.setInt(1, iNoOfChunks) ; 
            ps.setInt(2, iPageNumber) ; 
            return ps ; 
        }//EOM 
        
        @Override
        public final void setFetchSize(final int batchSize, final boolean isBigTable, final Statement stmt) throws SQLException{ 
            stmt.setFetchSize((isBigTable ? Integer.MIN_VALUE : batchSize)) ; 
        }//EOM
        
    };//EO Postgres 
    
    /**
     * Binds the pagination parameters into the preparedStatement. 
     * @param ps 
     * @param iPageNumber current page number (partition) 
     * @param iPageSize batchSize 
     * @return the ps formal argument 
     * @throws SQLException
     */
    public abstract PreparedStatement bindPageInfo(final PreparedStatement ps, 
                        final int iPageNumber, final int iPageSize, final int iNoOfChunks) throws SQLException ;
    
    /**
     * Constructs a select query with the following format: 
     * select [pkColumn], [columns clause] from [tableName] [pagination clause]
     * 
     * @param tableName
     * @param columnsClause comma delimited string 
     * @param pkColumnName 
     * @return select query as per the above description. 
     */
    public abstract String generatePagedQuery(final String tableName, final String columnsClause, 
            final String pkColumnName) ;  
    
    /**
     * Constructs an update statement with the following format:
     * update [tablename] set [col=?..] where [pk] = ?
     * @param tableName
     * @param columnsClause
     * @param pkColumnName
     * @return update statement with the above description 
     */
    public String generateUpdateQuery(final String tableName, final String columnsClause, 
            final String pkColumnName) { 
        return String.format("update %s set %s where %s = ?", tableName, columnsClause, pkColumnName) ; 
    }//EOM 
    
    public void setFetchSize(final int batchSize, final boolean isBigTable, final Statement stmt) throws SQLException{ 
        stmt.setFetchSize(batchSize) ; 
    }//EOM 
    
    public StringBuilder appendModuloClause(final String columnName, final int modolu, final StringBuilder stmtBuilder) { 
        return stmtBuilder.append(columnName).append(" % ").append(modolu) ; 
    }//EOM 
    
}//EO enum DatabaseType
