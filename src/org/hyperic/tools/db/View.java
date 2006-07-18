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
import java.util.Iterator;
import java.util.List;

import org.hyperic.util.StringList;
import org.hyperic.util.StrongCollection;
import org.hyperic.util.jdbc.JDBC;

import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

class View {
    
    private DBSetup     m_parent;
    private String      m_strName;
    private String      m_strQuery;
    private DataSet     m_dataset;
    
    protected View(Node node, int dbtype, DBSetup dbsetup) throws SAXException
    {
        m_parent = dbsetup;
        boolean queryIsSet = false;

        if(View.isView(node))
        {
            NamedNodeMap map = node.getAttributes();

            for (int iTab = 0; iTab < map.getLength(); iTab++)
            {
                Node nodeMap = map.item(iTab);

                if(nodeMap.getNodeName().equalsIgnoreCase("name")) {
                    this.m_strName = nodeMap.getNodeValue();
                }
                else if( m_parent.isVerbose()) {
                    System.out.println("Unknown attribute \'" 
                        + nodeMap.getNodeName() + "\' in tag \'table\'");
                }
            }
            NodeList childNodes = node.getChildNodes();
            for (int i = 0; i < childNodes.getLength(); i++) {
                Node child = childNodes.item(i);
                if (child.getNodeName().equalsIgnoreCase("query") &&
                    child.hasChildNodes()) {
                    NodeList contentNodes = child.getChildNodes();
                    this.m_strQuery = child.getFirstChild().getNodeValue();
                    queryIsSet = true;
                }
            }
            if (! queryIsSet) {
                throw new SAXException("no query specified");
            }            
        }
        else
        {
            throw new SAXException("node is not a table.");
        }
    }

    /**

    protected View(ResultSet set, DatabaseMetaData meta, DBSetup dbsetup) throws SQLException
    {
        this.m_parent      = dbsetup;
        this.m_strName     = set.getString(3);
        this.m_listColumns = Column.getColumns(meta, this);
        this.m_dataset     = new SqlDataSet(this);
    }

    **/

    protected void create(Collection typemaps) throws SQLException
    {
        List commands = new java.util.Vector();
        this.getCreateCommands(commands, typemaps,  
            JDBC.toType(m_parent.getConn().getMetaData().getURL()));
        Iterator iter = commands.iterator();
        while(iter.hasNext() == true)
        {
            String strCmd = (String)iter.next();
            m_parent.doSQL(strCmd);
        }
    }

    private void doCmd(List collCmds) throws SQLException {
        Iterator iter = collCmds.iterator();
        while(iter.hasNext() == true)
        {
            String strCmd = (String)iter.next();
            m_parent.doSQL(strCmd);
        }
    }

    protected void drop() throws SQLException
    {
        List collCmds = new StringList();      
        this.getDropCommands(collCmds);

        doCmd(collCmds);
    }
    
    protected void getCreateCommands(List cmds, Collection typemaps, int dbtype)
    {
        String strCmd = "CREATE VIEW " + this.getName() + " AS " +
            this.getQuery();
        
        cmds.add(0, strCmd); 
    }

    protected DataSet getDataSet()
    {
        return this.m_dataset;
    }
    
    protected void getDropCommands(List cmds)
    {
        String strCmd = "DROP VIEW " + this.getName();
        cmds.add(strCmd);
    }

    protected String getQueryCommand()
    {
        String strCmd = "SELECT * ";
        
        strCmd = strCmd + "FROM " + this.getName();

        return strCmd;
    }

    protected String getName()
    {
        return this.m_strName.toUpperCase();
    }

    protected String getQuery()
    {
        return this.m_strQuery;
    }
    
    protected static Collection getViews(Node node, int dbtype, DBSetup parent)
    {
        Collection colResult = new StrongCollection("org.hyperic.tools.db.View");
        NodeList listViews = node.getChildNodes();

        String strTmp = node.getNodeName();

        for(int i = 0;i < listViews.getLength(); i++)
        {
            Node nodeView = listViews.item(i);

            if(View.isView(nodeView))
            {
                try
                {
                    colResult.add(new View(nodeView, dbtype, parent));
                }
                catch(SAXException e)
                {
                }
            }
        }
        return colResult;
    }


/**

    protected static Collection getTables(DBSetup parent, String username) throws SQLException
    {
        if(username != null)
            username = username.toUpperCase();
            
        Collection coll = new StrongCollection("org.hyperic.tools.db.Table");

        String[]         types   = {"TABLE"};
        DatabaseMetaData meta    = parent.getConn().getMetaData();
        ResultSet        setTabs = meta.getTables(null, username, "%", types);

        // Find Tables
        while(setTabs.next() == true)
            coll.add(new Table(setTabs, meta, parent));

        return coll;
    }
**/    

    protected static boolean isView(Node nodeTable)
    {
        return nodeTable.getNodeName().equalsIgnoreCase("view");
    }

    protected DBSetup getDBSetup () { return m_parent; }
    
    protected static void uninstallCleanup(DBSetup parent) throws SQLException 
    {}

}
