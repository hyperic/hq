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
import java.util.Iterator;
import java.util.Vector;
import java.util.List;
import org.hyperic.util.StringCollection;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

class Constraint {

    private Table            m_table;
    private ConstraintImpl   m_constraint;
    private DBSetup          m_parent;
    
    protected Constraint (Node node, Table table, int dbtype) throws SAXException {

        m_parent = table.getDBSetup();
        if ( !Constraint.isConstraint(node) ) throw new SAXException("node is not an CONSTRAINT.");

        String cname = null;

        NamedNodeMap map = node.getAttributes();
        
        for (int iAttr=0; iAttr<map.getLength(); iAttr++) {
            Node   nodeMap  = map.item(iAttr);
            String strName  = nodeMap.getNodeName();
            String strValue = nodeMap.getNodeValue();
            
            if ( strName.equalsIgnoreCase("name") ) {
                cname = strValue;
            }
        }

        ///////////////////////////////////////////////////////////////////
        // Get the columns references in the constraint. This is not currently
        // verified to ensure they are valid.
        NodeList listFields = node.getChildNodes();
        int numFields = listFields.getLength();

        for(int iField=0; iField<numFields; iField++) {

            node = listFields.item(iField);
            if ( node.getNodeName().equalsIgnoreCase("primarykey") ) {
                m_constraint = new ConstraintImpl_PK(cname, dbtype, node);
            } else if ( node.getNodeName().equalsIgnoreCase("foreignkey") ) {
                m_constraint = new ConstraintImpl_FK(cname, this, dbtype, node);
            }
        }
        
        this.m_table = table;
    }
    
    protected String getCreateString () {
        return m_constraint.getCreateString();
    }

    protected Table getTable () {
        return m_table;
    }

    protected void getPostCreateCommands ( List postCreateCommands ) {
        m_constraint.getPostCreateCommands(postCreateCommands);
    }

    protected static Collection getConstraints(Table table, Node nodeTable, int dbtype) {
                
        String     strTableName = nodeTable.getNodeName();
        NodeList   listIdx      = nodeTable.getChildNodes();
        Collection colResult    = new Vector();
        
        for(int i=0; i<listIdx.getLength(); i++) {

            Node node = listIdx.item(i);
            
            if ( Constraint.isConstraint(node) ) {
                try {
                    colResult.add(new Constraint(node, table, dbtype));
                } catch(SAXException e) {}
            }
        }
        
        return colResult;
    }

    protected static boolean isConstraint(Node node)
    {
        return node.getNodeName().equalsIgnoreCase("constraint");
    }
}
