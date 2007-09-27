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
import java.util.Vector;

import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import org.hyperic.util.jdbc.JDBC;

class ConstraintImpl_FK implements ConstraintImpl {

    private String _name = null;
    private Constraint _constraint = null;
    private int _dbtype = 0;
    private String _local  = null;
    private String _references = null;
    private String _onDelete = null;

    public ConstraintImpl_FK ( String name, 
                               Constraint constraint, 
                               int dbtype, 
                               Node node ) throws SAXException {
        _name = name;
        _constraint = constraint;
        _dbtype = dbtype;

        NamedNodeMap attrs = node.getAttributes();
        int numAttrs = attrs.getLength();
        for(int iAttr=0; iAttr<numAttrs; iAttr++) {
            node = attrs.item(iAttr);
            if ( node.getNodeName().equalsIgnoreCase("local") ) {
                _local = node.getNodeValue();

            } else if ( node.getNodeName().equalsIgnoreCase("references") ) {
                _references = node.getNodeValue();

            } else if ( node.getNodeName().equalsIgnoreCase("ondelete") ) {
                _onDelete = node.getNodeValue();

            } else {
                throw new SAXException("Unrecognized attribute in ForeignKey element: " + node.getNodeName());
            }
        }
    }

    public void getPostCreateCommands ( List postCreateCommands ) {

        if ( _onDelete != null && _dbtype == JDBC.CLOUDSCAPE_TYPE ) {
            
            String table = _constraint.getTable().getName();
            int paren1idx = _references.indexOf("(");
            int paren2idx = _references.lastIndexOf(")");
            String refTable = _references.substring(0, paren1idx);
            String refColumn = _references.substring(paren1idx+1, paren2idx);

            postCreateCommands.add
                ("CREATE TRIGGER " + _name + "_t "
                 + "BEFORE DELETE ON " + refTable + " "
                 + "REFERENCING OLD ROW AS deleted" + refTable + " "
                 + "FOR EACH ROW "
                 + "DELETE FROM " + table + " "
                 + "WHERE " + _local + " = deleted" + refTable + "." + refColumn);
        }
    }

    public String getCreateString () {
        if ( _name == null ) _name = "";
        if ( _onDelete == null || _dbtype == JDBC.CLOUDSCAPE_TYPE ) {
            _onDelete = "";
        } else {
            _onDelete = "ON DELETE " + _onDelete.toUpperCase();
        }
        return ", CONSTRAINT " + _name
            + " FOREIGN KEY (" + _local + ") REFERENCES " 
            + _references + " " + _onDelete;
    }

}
