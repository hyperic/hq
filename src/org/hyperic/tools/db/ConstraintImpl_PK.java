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
import org.hyperic.util.StringUtil;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

class ConstraintImpl_PK implements ConstraintImpl {

    private String _name = null;
    private int _dbtype = 0;
    private List _fields = null;

    public ConstraintImpl_PK ( String name, int dbtype, Node node ) throws SAXException {
        _name = name;
        _dbtype = dbtype;
        _fields = new Vector();

        NamedNodeMap attrs = null;

        NodeList listFields = node.getChildNodes();
        int numFields = listFields.getLength();
        for(int iField=0; iField<numFields; iField++) {
            node = listFields.item(iField);
            if ( node.getNodeName().equalsIgnoreCase("field") ) {
                attrs = node.getAttributes();
                if ( attrs.getLength() != 1 ) {
                    throw new SAXException("Primary key field element must have only 1 attribute and it must be 'ref'");
                }
                Node refAttr = attrs.item(0);
                if ( !refAttr.getNodeName().equalsIgnoreCase("ref") ) {
                    throw new SAXException("Primary key field element must have only 1 attribute and it must be 'ref'");
                }
                _fields.add(refAttr.getNodeValue());
            }
        }
    }

    public void getPostCreateCommands ( List postCreateCommands ) {}

    public String getCreateString () {
        if ( _name == null ) _name = "";
        return ", CONSTRAINT " + _name
            + " PRIMARY KEY ("
            + StringUtil.listToString(_fields, ", ") 
            + ")";
    }

}
