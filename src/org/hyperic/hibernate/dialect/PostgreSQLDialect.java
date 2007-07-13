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

public class PostgreSQLDialect 
    extends org.hibernate.dialect.PostgreSQLDialect
    implements HQDialect
{
    public String getCascadeConstraintsString() {
        return " cascade ";
    }

    public boolean dropConstraints() {
        return false;
    }

	public String getCreateSequenceString(String sequenceName) {
        return new StringBuffer()
            .append("create sequence ")
            .append(sequenceName)
            .append(" start ")
            .append(HypericDialectConstants.SEQUENCE_START)
            .append(" increment 1 ")
            .toString();
    }

    public String getOptimizeStmt(String table, int cost)
    {
        return "ANALYZE "+table;
    }

    public String getDeleteJoinStmt(String deleteTable,
                                    String joinTables,
                                    String joinKeys,
                                    String condition)
    {
        //doesn't map well btwn databases, so I need to do this
        String join = joinTables.replaceAll(deleteTable+"\\s*,?", "");
        return "DELETE FROM "+deleteTable+" USING "+join+
               " WHERE "+joinKeys+" and "+condition;
    }
}
