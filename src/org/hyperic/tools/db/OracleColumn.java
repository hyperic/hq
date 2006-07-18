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

class OracleColumn extends Column
{
    protected OracleColumn(Node node, Table table) throws SAXException
    {
        super(node, table);
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
        return super.getCreateCommand(cmds, typemaps, dbtype);
    }

    protected String getDefaultCommand(List cmds)
    {
        if(this.hasDefault() == true)
        {
            switch(this.getDefault()) {
            case Column.DEFAULT_AUTO_INCREMENT:
            case Column.DEFAULT_SEQUENCE_ONLY:
                String strSeqName = this.m_strTableName + '_' + this.getName() + "_SEQ";
                
                cmds.add(0, "CREATE SEQUENCE " + this.m_strTableName + '_' + this.getName() + "_SEQ" + " START WITH " + this.getInitialSequence() + " INCREMENT BY " + this.getIncrementSequence() + " NOMAXVALUE NOCYCLE CACHE 10");
                break;
            }
        }

        return "";
    }

    protected void getPostCreateCommands (List cmds) {

        String strSeqName = this.m_strTableName + '_' + this.getName() + "_SEQ";

        if ( hasDefault() ) {
            switch(this.getDefault()) {
            case Column.DEFAULT_AUTO_INCREMENT:
                cmds.add("CREATE OR REPLACE TRIGGER " + strSeqName + "_T " +
                         "BEFORE INSERT ON " + this.m_strTableName + " " +
                         "FOR EACH ROW " +
                         "BEGIN " +
                         "SELECT " + strSeqName + ".NEXTVAL INTO :NEW." + this.getName() + " FROM DUAL; " +
                         "END;");
                break;
            }
        }
    }
    
    protected void getDropCommands(List cmds)
    {
        String strSeqName = this.m_strTableName + '_' + this.getName() + "_SEQ";
        if(this.hasDefault() == true)
        {
            switch(this.getDefault()) {
            case Column.DEFAULT_SEQUENCE_ONLY:
            case Column.DEFAULT_AUTO_INCREMENT:
                cmds.add("DROP SEQUENCE " + strSeqName);
                
                // Dropping the table automatically drops the sequence 
                // before this command gets executed.
                //-- you must mean it drops the trigger automatically, yea?
                //cmds.add("DROP TRIGGER " + strSeqName + "_t");
                
                break;
            }
        }
    }
    
    
    protected static int getClassType()
    {
        return JDBC.ORACLE_TYPE;
    }
}
