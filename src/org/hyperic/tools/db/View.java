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
    
    private DBSetup _parent;
    private String _strName;
    private String _strQuery;
    
    protected View(Node node, int dbtype, DBSetup dbsetup) throws SAXException
    {
        _parent = dbsetup;
        boolean queryIsSet = false;

        if(View.isView(node)) {
            NamedNodeMap map = node.getAttributes();

            for (int iTab = 0; iTab < map.getLength(); iTab++) {
                Node nodeMap = map.item(iTab);

                if(nodeMap.getNodeName().equalsIgnoreCase("name")) {
                    _strName = nodeMap.getNodeValue();
                } else if(_parent.isVerbose()) {
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
                    _strQuery = child.getFirstChild().getNodeValue();
                    queryIsSet = true;
                }
            }
            if (!queryIsSet) {
                throw new SAXException("no query specified");
            }            
        } else {
            throw new SAXException("node is not a table.");
        }
    }

    protected void create(Collection typemaps) throws SQLException
    {
        List commands = new java.util.Vector();
        this.getCreateCommands(commands, typemaps,  
            JDBC.toType(_parent.getConn().getMetaData().getURL()));
        Iterator iter = commands.iterator();
        while(iter.hasNext())
        {
            String strCmd = (String)iter.next();
            _parent.doSQL(strCmd);
        }
    }

    private void doCmd(List collCmds) throws SQLException {
        Iterator iter = collCmds.iterator();
        while(iter.hasNext()) {
            String strCmd = (String)iter.next();
            _parent.doSQL(strCmd);
        }
    }

    protected void drop() throws SQLException {
        List collCmds = new StringList();      
        this.getDropCommands(collCmds);

        doCmd(collCmds);
    }
    
    protected void getCreateCommands(List cmds, Collection typemaps,
                                     int dbtype) {
        String strCmd = "CREATE VIEW " + this.getName() + " AS " +
            this.getQuery();
        
        cmds.add(0, strCmd); 
    }

    protected void getDropCommands(List cmds) {
        String strCmd = "DROP VIEW " + this.getName();
        cmds.add(strCmd);
    }

    protected String getQueryCommand() {
        String strCmd = "SELECT * ";
        
        strCmd = strCmd + "FROM " + this.getName();

        return strCmd;
    }

    protected String getName() {
        return _strName.toUpperCase();
    }

    protected String getQuery() {
        return _strQuery;
    }
    
    protected static Collection getViews(Node node, int dbtype,
                                         DBSetup parent)
    {
        Collection colResult =
            new StrongCollection("org.hyperic.tools.db.View");
        NodeList listViews = node.getChildNodes();

        for(int i = 0;i < listViews.getLength(); i++) {
            Node nodeView = listViews.item(i);

            if(View.isView(nodeView)) {
                try {
                    colResult.add(new View(nodeView, dbtype, parent));
                } catch(SAXException e) {
                }
            }
        }
        return colResult;
    }

    protected static boolean isView(Node nodeTable) {
        return nodeTable.getNodeName().equalsIgnoreCase("view");
    }

    protected DBSetup getDBSetup () {
        return _parent;
    }
    
    protected static void uninstallCleanup(DBSetup parent)
        throws SQLException {}
}
