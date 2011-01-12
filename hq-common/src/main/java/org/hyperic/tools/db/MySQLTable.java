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

import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Collection;

import org.w3c.dom.Node;
import org.xml.sax.SAXException;

import org.hyperic.util.StrongCollection;
import org.hyperic.util.jdbc.JDBC;

class MySQLTable extends Table {
    public static final int CLASS_TYPE = JDBC.MYSQL_TYPE;

    public MySQLTable(Node node, int dbtype, DBSetup dbsetup)
        throws SAXException {
        super(node, dbtype, dbsetup);
    }

    public MySQLTable(ResultSet set, DatabaseMetaData meta, DBSetup dbsetup)
        throws SQLException {
        super(set, meta, dbsetup);
    }

	protected static Collection<Table> getTables(DBSetup parent, String username)
        throws SQLException    {
        if(username != null)
            username = username.toUpperCase();

        Collection<Table> coll = new StrongCollection("org.hyperic.tools.db.Table");

        String[]         types   = {"TABLE"};
        DatabaseMetaData meta    = parent.getConn().getMetaData();
        ResultSet        setTabs = meta.getTables(null, username, "%", types);

        // Find Tables
        while(setTabs.next()) {
            coll.add(new MySQLTable(setTabs, meta, parent));
        }
        
        return coll;
    }

    protected String getEngineSyntax() {
        return " ENGINE = InnoDB";
    }
}
