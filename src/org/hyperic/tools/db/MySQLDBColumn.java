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

import java.util.Collection;
import java.util.List;
import org.hyperic.util.jdbc.JDBC;
import org.w3c.dom.Node;
import org.xml.sax.SAXException;

class MySQLDBColumn extends Column
{
    protected MySQLDBColumn(Node node, Table table) throws SAXException
    {
        super(node, table);
    }

    protected String getDefaultCommand(List cmds)
    {
        String strCmd ="";

        switch(this.getDefault()) {
          case Column.DEFAULT_AUTO_INCREMENT:
            // -- initial value for autoincrement 
            strCmd += " AUTO_INCREMENT";
            break;
            
          
        }

        return strCmd;
    }
    
    protected String getCreateCommand ( List cmds, Collection typemaps, int dbtype ) {
        String defaultValue = this.getsDefault();
        if ( defaultValue != null ) {
            if ( defaultValue.equalsIgnoreCase("TRUE") ) {
                this.m_sDefault = "1";
                
            } else if ( defaultValue.equalsIgnoreCase("FALSE") ) {
                this.m_sDefault = "0";
            }
        }

        String strCmd = this.getName() + ' ' + this.getMappedType(typemaps, dbtype);

        if(this.getSize() > 0) {
            strCmd = strCmd + this.getSizeCommand(cmds);
        }

        if(this.hasDefault() == true)
        {
            String strDefault = this.getDefaultCommand(cmds);

            if(strDefault.length() > 0)
                strCmd = strCmd + ' ' + strDefault;
        }

        if(this.isRequired() == true)
            strCmd += " NOT NULL";

        if (this.m_sDefault != null)
            strCmd += " DEFAULT '" + this.getsDefault() + "'";

        if(this.isPrimaryKey() == true)
            strCmd += " PRIMARY KEY";

        if (this.m_sReferences != null)
            strCmd += " REFERENCES " + this.getReferences();

        return strCmd; 
    }

    protected static int getClassType()
    {
        return JDBC.MYSQL_TYPE;
    }
    
}
