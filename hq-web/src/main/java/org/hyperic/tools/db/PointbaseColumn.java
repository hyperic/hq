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

import java.sql.SQLException;
import java.util.Collection;
import java.util.List;
import org.hyperic.util.jdbc.JDBC;
import org.hyperic.util.jdbc.PointbaseSequence;
import org.w3c.dom.Node;
import org.xml.sax.SAXException;

class PointbaseColumn extends Column
{
	private	static int	s_iSequenceCount = -1;
	
	private	boolean		m_bSequenceInit  = false;
	private Table		m_table;
			
    protected PointbaseColumn(Node node, Table table) throws SAXException
    {
        super(node, table);
        this.m_table = table;
    }
    
    protected String getSizeCommand(List cmds) {
        if (this.getType().equals("INTEGER"))
            return "";
        else
            return super.getSizeCommand(cmds);
    }

	protected String getDefaultCommand(List cmds)
	{
		String strCmd = new String();
		
		switch(this.getDefault()) {
		case Column.DEFAULT_AUTO_INCREMENT:
			strCmd += "IDENTITY";
			if(this.getInitialSequence() > 0)
				strCmd = strCmd + '(' + this.getInitialSequence();

			if(this.getIncrementSequence() > 0)
				strCmd = strCmd + ',' + this.getIncrementSequence();

			strCmd += ")";
			break;
		case Column.DEFAULT_SEQUENCE_ONLY:
			try {
				PointbaseSequence.initSequences( this.m_table.getDBSetup().getConn() );
			}
			catch(SQLException e) {
			}
							
			cmds.add( PointbaseSequence.getCreateCommand(
				this.m_strTableName + '_' + this.getName() + "_SEQ",
				this.getInitialSequence(),
				this.getIncrementSequence() ) );
				
			break;
		case Column.DEFAULT_CURRENT_TIME:
			strCmd += "CURRENT_TIME";
			break;
		}
        
		return strCmd;
	}

	protected void getDropCommands(List cmds)
	{
		if(this.hasDefault() == true) {
			switch(this.getDefault()) {
			case Column.DEFAULT_SEQUENCE_ONLY:
				cmds.add( PointbaseSequence.getDropCommand(
					this.m_strTableName + '_' + this.getName() + "_SEQ" ) );
				break;
			}
		}
	}
    
    protected static int getClassType()
    {
        return JDBC.POINTBASE_TYPE;
    }
}
