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

package org.hyperic.tools.db;

import java.sql.ResultSet;
import java.sql.SQLException;

class SqlDataSet extends DataSet
{
    private final ResultSet   m_resRows;
    private final Table       m_table;
    
    protected SqlDataSet(Table table) throws SQLException
    {
        super(table.getName(), table.getDBSetup());
        
        this.m_table   = table;
        String strCmd  = table.getQueryCommand();
        this.m_resRows = table.getDBSetup().getConn().createStatement().executeQuery(strCmd);
    }
    
    protected Data getData(int columnIndex)
    {
        Data dataResult = null;
        
        try
        {
            Column col = (Column)this.m_table.getColumns().get(columnIndex);
            dataResult = new Data(col.getName(), this.m_resRows.getString(columnIndex + 1));
        }
        catch(SQLException e)
        {
        }
        
        return dataResult;
    }

    protected boolean next()
    {
        boolean bResult;
        
        try
        {
            bResult = this.m_resRows.next();
        }
        catch(SQLException e)
        {
            bResult = false;
        }
        
        return bResult;
    }
}
