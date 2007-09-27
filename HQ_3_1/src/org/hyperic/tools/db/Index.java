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
import java.util.Vector;
import org.hyperic.util.StringCollection;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

class Index
{
    private String           m_strName;
    private StringCollection m_collFields;
    private Table            m_table;
    private boolean          m_isUnique;
    private DBSetup          m_parent;
    private String           m_tableSpace;
    protected Index(Node node, Table table, int dbtype) throws SAXException
    {
        m_parent = table.getDBSetup();

        // Bail out early if this is the wrong node (should never happen).
        if( !Index.isIndex(node)) throw new SAXException("node is not an INDEX.");

        NamedNodeMap map = node.getAttributes();
        
        for(int iAttr = 0;iAttr < map.getLength();iAttr ++)
        {
            Node   nodeMap  = map.item(iAttr);
            String strName  = nodeMap.getNodeName();
            String strValue = nodeMap.getNodeValue();
                        
            if(strName.equalsIgnoreCase("name") == true)
            {
                // Get the index Name
                this.m_strName = strValue;
            }
            if(strName.equalsIgnoreCase("unique") == true)
            {
                if ( strValue.equalsIgnoreCase("true") ) {
                    this.m_isUnique = true;
                } else if ( strValue.equalsIgnoreCase("false") ) {
                    this.m_isUnique = false;
                } else {
                    throw new SAXException("value of unique attribute on "
                                           + "INDEX element must be 'true' "
                                           + "or 'false' (was '" + strValue + "')");
                }
            }
            if(strName.equalsIgnoreCase("tablespace") == true)
            {
                this.m_tableSpace = strValue;
            }
        }
        
            
        ///////////////////////////////////////////////////////////////////
        // Get the columns references in the index. This is not currently
        // verified to ensure they are valid.
        
        this.m_collFields   = new StringCollection();
        NodeList listFields = node.getChildNodes();
        
        for(int iField = 0;iField < listFields.getLength();iField++)
        {
            node = listFields.item(iField);
            
            if(Index.isField(node) == true)
            {                
                map = node.getAttributes();
                
                for(int iAttr = 0;iAttr < map.getLength();iAttr++)
                {
                    Node   nodeMap  = map.item(iAttr);
                    String strName  = nodeMap.getNodeName();
                    String strValue = nodeMap.getNodeValue();
                    
                    if(strName.equalsIgnoreCase("name") == true || strName.equalsIgnoreCase("ref") == true)
                        this.m_collFields.add(strValue);
                }
            }
        }
            
        this.m_table = table;
    }

    protected void create() throws SQLException
    {
        String strCmd = this.getCreateString();

        m_parent.doSQL(strCmd);
    }
    
    protected String getCreateString()
    {
        String strCmd;

        if (this.m_isUnique)
        {
            strCmd = "CREATE UNIQUE INDEX " + this.getName() + 
                     " ON " + this.getTable().getName() + " (";
        }
        else 
        {      
            strCmd = "CREATE INDEX " + this.getName() + " ON " + 
                     this.getTable().getName() + " (";
        }
        
        Iterator iter   = this.getFields().iterator();
        boolean  bFirst = true;
        
        while(iter.hasNext() == true)
        {
            if(bFirst == true)
                bFirst = false;
            else
                strCmd += ", ";

            String strField = (String)iter.next();
            
            strCmd += strField;
        }
        
        strCmd += ')';
        // deal with the tablespace
        strCmd += getTableSpaceClause();
        // now deal with storage attribute which is always inherited
        // from the table to ensure consistency
        // this may need to be changed later
        strCmd += m_table.getStorageClause();
        return strCmd;
    }

    protected Collection getFields()
    {
        return this.m_collFields;
    }

    protected String getName()
    {
        return this.m_strName.toUpperCase();
    }
    
    protected Table getTable()
    {
        return this.m_table;
    }

    protected static Collection getIndexes(Table table, Node nodeTable, int dbtype)
    {
        ///////////////////////////////////////////////////////////////
        // Get the Columns Names and Related Info
                
        String     strTableName = nodeTable.getNodeName();
        NodeList   listIdx      = nodeTable.getChildNodes();
        Collection colResult    = new Vector();
        
        for(int i = 0;i < listIdx.getLength();i++)
        {
            Node node = listIdx.item(i);
            
            if(Index.isIndex(node) == true)
            {
                try
                {
                    switch (dbtype) {
                        case OracleTable.CLASS_TYPE :
                            colResult.add(
                                new OracleIndex(node, table, dbtype));
                            break;
                        default :
                            colResult.add(new Index(node, table, dbtype));
                            break;
                    }
                }
                catch(SAXException e)
                {
                }
            }
        }
        
        return colResult;
    }
    
    protected String getTableSpaceClause() {
        if(this.m_tableSpace != null
           && !this.m_tableSpace.equals("DEFAULT")
           && !getTableSpaceSyntax().equals("")) {
            return getTableSpaceSyntax() + m_tableSpace;
        } else {
            return "";
        }
    }
    
    protected String getTableSpaceSyntax() {
        return "";
    }

    protected static boolean isField(Node node)
    {
        return node.getNodeName().equalsIgnoreCase("field");
    }
    
    protected static boolean isIndex(Node node)
    {
        return node.getNodeName().equalsIgnoreCase("index");
    }
}
