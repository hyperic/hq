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
import java.util.List;
import java.util.Iterator;
import org.hyperic.util.StringList;
import org.hyperic.util.StrongCollection;
import org.hyperic.util.jdbc.JDBC;
import org.xml.sax.SAXException;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

class Table {

    private DBSetup     m_parent;
    private String      m_strName;
    private boolean     m_indexOrganized = false;   // Index Organized Table
    private boolean     m_parallel       = false;   // Parallel processing
    private boolean     m_logging        = false;
    private boolean     m_cache          = false;
    private String      m_tableSpace;
    private String      m_storage;
    private String      m_engine;
    private List        m_listColumns;
    private Collection  m_collIndexes;
    private Collection  m_collConstraints;
    private DataSet     m_dataset;

	protected Table() {}

    protected Table(Node node, int dbtype, DBSetup dbsetup) throws SAXException
    {
        m_parent = dbsetup;

        if(Table.isTable(node))
        {
            NamedNodeMap map = node.getAttributes();

            for(int iTab = 0;iTab < map.getLength();iTab++)
            {
                Node nodeMap = map.item(iTab);

                if(nodeMap.getNodeName().equalsIgnoreCase("name")) {
                    this.m_strName = nodeMap.getNodeValue();
                }
                else if(nodeMap.getNodeName().equalsIgnoreCase(
                    "index-organized")) {
                    this.m_indexOrganized =
                        nodeMap.getNodeValue().equalsIgnoreCase("true");
                }
                else if(nodeMap.getNodeName().equalsIgnoreCase(
                    "parallel")) {
                    this.m_parallel =
                        nodeMap.getNodeValue().equalsIgnoreCase("true");
                }
                else if(nodeMap.getNodeName().equalsIgnoreCase(
                    "logging")) {
                    this.m_logging =
                        nodeMap.getNodeValue().equalsIgnoreCase("true");
                }
                else if(nodeMap.getNodeName().equalsIgnoreCase(
                    "cache")) {
                    this.m_cache =
                        nodeMap.getNodeValue().equalsIgnoreCase("true");
                }
                else if(nodeMap.getNodeName().equalsIgnoreCase(
                    "tablespace")) {
                    this.m_tableSpace =     
                        nodeMap.getNodeValue();
                } 
                else if(nodeMap.getNodeName().equalsIgnoreCase(
                    "storage-options")) {
                    this.m_storage =            
                        nodeMap.getNodeValue();
                }
                else if(nodeMap.getNodeName().equalsIgnoreCase(
                    "engine")) {
                    this.m_engine = 
                        nodeMap.getNodeValue();
                }
                else if(m_parent.isVerbose()) {
                    System.out.println(
                        "Unknown attribute \'" + nodeMap.getNodeName() +
                        "\' in tag \'table\'");
                }
            }

            this.m_listColumns     = Column.getColumns(node, this, dbtype);
            this.m_collIndexes     = Index.getIndexes(this, node, dbtype);
            this.m_collConstraints = Constraint.getConstraints(this, node,
                                                               dbtype);
            this.m_dataset         = new XmlDataSet(this, node);
        } else {
            throw new SAXException("node is not a table.");
        }
    }

    protected Table(ResultSet set, DatabaseMetaData meta, DBSetup dbsetup)
        throws SQLException
    {
        this.m_parent      = dbsetup;
        this.m_strName     = set.getString(3);
        this.m_listColumns = Column.getColumns(meta, this);
        this.m_dataset     = new SqlDataSet(this);
    }

    protected void create(Collection typemaps) throws SQLException
    {
        List collCmds = new java.util.Vector();
        this.getCreateCommands(collCmds, typemaps,
            JDBC.toType(m_parent.getConn().getMetaData().getURL()));

        Iterator iter = collCmds.iterator();
        while(iter.hasNext())
        {
            String strCmd = (String)iter.next();
            m_parent.doSQL(strCmd);
        }
    }

    private void doCmd(List collCmds) throws SQLException {
        Iterator iter = collCmds.iterator();
        while(iter.hasNext())
        {
            String strCmd = (String)iter.next();
            m_parent.doSQL(strCmd);
        }
    }

    protected void clear() throws SQLException
    {
        List collCmds = new StringList();
        this.getClearCommands(collCmds);

        doCmd(collCmds);
    }

    protected void drop() throws SQLException
    {
        List collCmds = new StringList();
        this.getDropCommands(collCmds);

        doCmd(collCmds);
    }

    protected List getColumns()
    {
        return this.m_listColumns;
    }

    protected String getTableSpace() {
        return this.m_tableSpace;
    }

    protected String getStorage() {
        return this.m_storage;
    }
    
    protected String getEngine() {
        return this.m_engine;
    }

    protected void getCreateCommands(List cmds, Collection typemaps, int dbtype)
    {
        String strCmd = "CREATE TABLE " + this.getName() + " (";

        Iterator iter   = this.getColumns().iterator();
        boolean  bFirst = true;

        List preCreateCommands  = new StringList();
        List postCreateCommands = new StringList();
        Column col = null;

        while(iter.hasNext())
        {
            if(bFirst)
                bFirst = false;
            else
                strCmd += ", ";

            col = (Column) iter.next();
            col.getPreCreateCommands(preCreateCommands);
            strCmd += col.getCreateCommand(cmds, typemaps, dbtype);
            col.getPostCreateCommands(postCreateCommands);
        }

        // Add constraint decls
        iter = this.getConstraints().iterator();
        Constraint constraint = null;
        while ( iter.hasNext() ) {
            constraint = (Constraint) iter.next();
            strCmd += constraint.getCreateString();
            constraint.getPostCreateCommands(postCreateCommands);
        }

        strCmd += ')';
        
        if(this.m_indexOrganized)
            strCmd += this.getIndexOrganizedSyntax(dbtype);

        // deal with the special tablespace attr
        if (this.m_tableSpace != null 
            && !this.m_tableSpace.equals("DEFAULT")
            && !this.getTableSpaceSyntax().equals("")) {
            strCmd += this.getTableSpaceSyntax() + this.getTableSpace();
        }
    
        // add a storage clause if any apply
        strCmd += this.getStorageClause();
        
        if(this.m_parallel)
            strCmd += this.getParallelSyntax();

        if(!this.m_logging)     // Oracle does NOLOGGING
            strCmd += this.getLoggingSyntax();

        if(this.m_cache)
            strCmd += this.getCacheSyntax();

        cmds.addAll(preCreateCommands);
        cmds.add(strCmd);
        cmds.addAll(postCreateCommands);

}
    
    /**
     * Get the storage clause for this table. Returns an empty string if
     * it is not applicable
     * @return
     */
    public String getStorageClause() {
        if(this.m_storage != null && !this.getStorageSyntax().equals("")) {
            return this.getStorageSyntax() + "(" + m_storage + ")";
        }
        else {
            return "";
        }
    }
    
    /**
     * Get the engine clause for this table. Returns an empty string if
     * not applicable
     */
    public String getEngineClause() {
//        if(this.m_engine != null && !this.getEngineSyntax().equals("")) {
//            return this.getEngineSyntax() + "(" + m_engine + ")";
//        }
        // FIXME hardcoded to all tables w/ same engine clause if engine is supported
        if(!this.getEngineSyntax().equals("")) {
            return this.getEngineSyntax();
        }
        else {
            return "";
        }
    }
    
    protected String getIndexOrganizedSyntax(int dbtype) {
        return (dbtype == OracleTable.CLASS_TYPE) ? " ORGANIZATION INDEX" : "";
    }
    
    protected String getParallelSyntax() {
        return "";
    }
    
    protected String getLoggingSyntax() {
        return "";
    }
    
    protected String getCacheSyntax() {
        return "";
    }

    protected String getTableSpaceSyntax() {
        return "";
    }

    protected String getStorageSyntax() {
        return "";
    }
    
    protected String getEngineSyntax() {
        return "";
    }

    protected DataSet getDataSet()
    {
        return this.m_dataset;
    }

    protected void getClearCommands(List cmds)
    {
        String strCmd = "DELETE FROM " + this.getName();
        cmds.add(strCmd);
    }

    protected void getDropCommands(List cmds)
    {
        String strCmd = "DROP TABLE " + this.getName();
        cmds.add(strCmd);

        Iterator iter = this.getColumns().iterator();

        while(iter.hasNext())
            ((Column)iter.next()).getDropCommands(cmds);
    }

    protected Collection getConstraints()
    {
        return this.m_collConstraints;
    }

    protected Collection getIndexes()
    {
        return this.m_collIndexes;
    }

    protected String getQueryCommand()
    {
        String strCmd = "SELECT ";

        Iterator iter = this.getColumns().iterator();

        while(iter.hasNext())
        {
            strCmd += ((Column)iter.next()).getName();

            if(iter.hasNext())
                strCmd += ',';

            strCmd += ' ';
        }

        strCmd = strCmd + "FROM " + this.getName();

        return strCmd;
    }

    protected String getName()
    {
        return this.m_strName.toUpperCase();
    }

    protected static Collection<Table> getTables(Node node, int dbtype, DBSetup parent)
    {
        Collection<Table> colResult =
            new StrongCollection("org.hyperic.tools.db.Table");
        NodeList listTabs    = node.getChildNodes();

        String strTmp = node.getNodeName();

        for (int iTab = 0; iTab < listTabs.getLength(); iTab++) {
            Node nodeTab = listTabs.item(iTab);

            if (Table.isTable(nodeTab)) {
                try {
                    switch (dbtype) {
                        case OracleTable.CLASS_TYPE :
                            colResult.add(
                                new OracleTable(nodeTab, dbtype, parent));
                            break;
                        case MySQLTable.CLASS_TYPE :
                            colResult.add(
                                new MySQLTable(nodeTab, dbtype, parent));
                            break;
                        default :
                            colResult.add(new Table(nodeTab, dbtype, parent));
                            break;
                    }
                } catch (SAXException e) {
                }
            }
        }

        return colResult;
    }

    protected static Collection<Table> getTables(DBSetup parent, String username)
        throws SQLException
    {
		int dbtype = JDBC.toType(parent.getConn().getMetaData().getURL());
		// when exporting a database, the particulars of how the metadata
		// with the list of tables is accessed is likely very driver-specific
        switch (dbtype) {
            case OracleTable.CLASS_TYPE:
                return OracleTable.getTables(parent, username);
            case CloudscapeTable.CLASS_TYPE:
            	return CloudscapeTable.getTables(parent, username);
        }

        if(username != null)
            username = username.toUpperCase();

        Collection<Table> coll = new StrongCollection("org.hyperic.tools.db.Table");
        DatabaseMetaData meta    = parent.getConn().getMetaData();
        String[]         types   = {"TABLE"};
        // there doesn't seem to be a general case for this but we
        // know this works for Oracle, so we'll start there
        ResultSet        setTabs = meta.getTables(null, username, "%", types);

        // Find Tables
        while(setTabs.next())
            coll.add(new Table(setTabs, meta, parent));

        return coll;
    }

    protected static boolean isTable(Node nodeTable)
    {
        return nodeTable.getNodeName().equalsIgnoreCase("table");
    }

	//	Can be overridden to do cleanup on an uninstall
	protected static void uninstallCleanup(DBSetup parent) throws SQLException 
	{
		int dbtype = JDBC.toType(parent.getConn().getMetaData().getURL());
		
		if( dbtype == PointbaseTable.getClassType() )
			PointbaseTable.uninstallCleanup(parent.getConn());
	}
	
    protected DBSetup getDBSetup () { return m_parent; }
}
