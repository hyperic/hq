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

import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.Hashtable;
import java.util.Iterator;
import org.hyperic.util.jdbc.JDBC;
import org.hyperic.util.StrongCollection;
import org.xml.sax.SAXException;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

public class TypeMap
{
    private String    _strGenericType;
    private Hashtable _mapDBType = new Hashtable();
    
    protected TypeMap(Node node) throws SAXException {
        if (TypeMap.isTypeMap(node) == false)
            throw new SAXException("node is not a TypeMap.");
            
        NamedNodeMap type = node.getAttributes();
            
        for(int iType = 0;iType < type.getLength();iType ++) {
            Node   nodeType = type.item(iType);
            String strName  = nodeType.getNodeName();
            String strValue = nodeType.getNodeValue();
            
            if(strName.equalsIgnoreCase("type") == true) {
                _strGenericType = strValue;
                
                NodeList listMaps = node.getChildNodes();
                    
                for(int iMap = 0;iMap < listMaps.getLength();iMap ++) {
                    node = listMaps.item(iMap);
                        
                    if(node.getNodeName().equalsIgnoreCase("map") == true) {
                        String  strDB         = null;
                        String  strMappedType = null;

                        NamedNodeMap map = node.getAttributes();
                            
                        for(int iAttr = 0;iAttr < map.getLength();iAttr ++) {
                            node     = map.item(iAttr);
                            strName  = node.getNodeName();
                            strValue = node.getNodeValue();

                            if(strName.equalsIgnoreCase("db") == true) {
                                // Get the Database
                                strDB = strValue;
                            } else if(strName.equalsIgnoreCase("type") == true){
                                // Get the Column Type
                                strMappedType = strValue;
                            } else {
                                System.out.println("Unknown attribute \'" + 
                                                   node.getNodeName() + 
                                                   "\' in tag \'table\'");
                            }
                        }
            
                        _mapDBType.put(strDB, strMappedType);
                    }
                }
            }
        }
    }
    
    protected boolean contains(String database) {
        return _mapDBType.contains(database);
    }
    
    protected String getGenericType() {
        return _strGenericType;
    }
    
    public String getMappedType(String type, String database) {
        String  strResult;
        
        if(_strGenericType.equalsIgnoreCase(type) == true)
            strResult = (String)_mapDBType.get(database);
        else
            strResult = null;
            
        return strResult;
    }
    
    protected String getMappedType(String type, int database) {
        return getMappedType(type, JDBC.toName(database));
    }

    public static String getMappedType(Collection typemaps, String type, 
                                       int database) 
    {
        return getMappedType(typemaps, type, JDBC.toName(database));
    }

    public static String getMappedType(Collection typemaps, String type, 
                                       String database) 
    {
        Iterator iter = typemaps.iterator();
        String typeName = null;
        while(iter.hasNext()) {
            TypeMap map = (TypeMap)iter.next();
            typeName = map.getMappedType(type, 
                                         database);
            
            if(typeName != null)
                break;
        }
        return typeName;
    }

    protected static boolean isTypeMap(Node node) {
        String strTmp = node.getNodeName();
        return strTmp.equalsIgnoreCase("typemap");
    }

    protected static Collection readTypeMaps(Node node) {
        Collection collResult = new StrongCollection("org.hyperic.tools.db.TypeMap");
        NodeList   listMaps   = node.getChildNodes();

        for(int iMap = 0;iMap < listMaps.getLength();iMap ++) {
            Node   nodeMap = listMaps.item(iMap);
            String strTmp  = nodeMap.getNodeName();
            
            if(strTmp.equalsIgnoreCase("mapping") == true) {
                NodeList listTypes = nodeMap.getChildNodes();

                for(int iType = 0;iType < listTypes.getLength(); iType ++) {
                    try {
                        collResult.add(new TypeMap(listTypes.item(iType)));
                    } catch(SAXException e) {
                    }
                }
            }
        }
        
        return collResult;
    }

    public static Collection loadTypeMapFromFile ( File f ) 
        throws IOException, SAXException 
    {
        Node typeMapNode;
        DBSetup dbsetup = new DBSetup();
        typeMapNode = dbsetup.readDocument(f.getAbsolutePath()).getFirstChild();
        Collection tmaps = readTypeMaps(typeMapNode);
        return tmaps;
    }
}
