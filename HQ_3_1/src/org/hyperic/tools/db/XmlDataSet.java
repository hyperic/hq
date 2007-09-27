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

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import org.hyperic.util.StrongCollection;
import org.hyperic.util.StrongList;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

class XmlDataSet extends DataSet
{
    private Iterator    m_iterator;
    private List        m_listCurRow;
    
    protected XmlDataSet(Table table, Node nodeTable)
    {
        super(table.getName(), table.getDBSetup());
        
        Collection collRows = new StrongCollection("java.util.List");
        NodeList   listData = nodeTable.getChildNodes();
        
        for(int i = 0;i < listData.getLength();i++)
        {
            Node node = listData.item(i);
            
            if(XmlDataSet.isDataSet(node) == true)
            {
                List listRow = new StrongList("org.hyperic.tools.db.Data");
                
                NamedNodeMap map = node.getAttributes();

                for(int iAttr = 0;iAttr < map.getLength();iAttr ++)
                    listRow.add(new Data(map.item(iAttr)));
                
                collRows.add(listRow);
            }
        }
        
        this.m_iterator = collRows.iterator();
    }
    
    protected int getNumberColumns()
    {
        return this.m_listCurRow.size();
    }
    
    protected Data getData(int columnIndex)
    {
        return (Data)this.m_listCurRow.get(columnIndex);
    }

    protected boolean next()
    {
        boolean bResult;
                
        bResult = this.m_iterator.hasNext();
            
        if(bResult == true)
            this.m_listCurRow = (List)this.m_iterator.next();
        else
            this.m_listCurRow = null;
        
        return bResult;
    }
    
    protected static boolean isDataSet(Node node)
    {
        String strTmp =  node.getNodeName();
        return strTmp.equalsIgnoreCase("data");
    }
}
