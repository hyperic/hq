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

import java.util.List;
import org.hyperic.util.jdbc.JDBC;
import org.w3c.dom.Node;
import org.xml.sax.SAXException;

class PostgresColumn extends Column
{
    protected PostgresColumn(Node node, Table table) throws SAXException
    {
        super(node, table);
    }

    protected String getDefaultCommand(List cmds)
    {
        if ( m_strType.equalsIgnoreCase("auto") || 
             m_iDefault == Column.DEFAULT_SEQUENCE_ONLY ) return "";

        String strSeqName = this.m_strTableName + '_' + this.getName() + "_SEQ";
            
        return "DEFAULT nextval('" + strSeqName + "')";
    }
    
    protected void getPreCreateCommands (List cmds) {
        if ( m_strType.equalsIgnoreCase("auto") ) return;

        String strSeqName = this.m_strTableName + '_' + this.getName() + "_SEQ";
                
        if( hasDefault() ) {
            switch( getDefault() ) {
            case Column.DEFAULT_AUTO_INCREMENT:
            case Column.DEFAULT_SEQUENCE_ONLY:
                cmds.add(0, "CREATE SEQUENCE " + this.m_strTableName + '_' + this.getName() + "_SEQ" + " START " + this.getInitialSequence() + " INCREMENT " + this.getIncrementSequence() + " CACHE 10");
                break;
            }
        }
    }

    protected void getDropCommands(List cmds)
    {
        if(this.hasDefault() == true)
        {
            switch(this.getDefault()) {
            case Column.DEFAULT_AUTO_INCREMENT:
            case Column.DEFAULT_SEQUENCE_ONLY:
                cmds.add("DROP SEQUENCE " + this.m_strTableName + '_' + this.getName() + "_SEQ");
                break;
            }
        }
    }
    
    
    protected static int getClassType()
    {
        return JDBC.PGSQL_TYPE;
    }
}
