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

package org.hyperic.hq.bizapp.shared.resourceImport;

import org.hyperic.util.xmlparser.XmlTagEntryHandler;
import org.hyperic.util.xmlparser.XmlTagExitHandler;
import org.hyperic.util.xmlparser.XmlTagHandler;
import org.hyperic.util.xmlparser.XmlTagInfo;
import org.hyperic.util.xmlparser.XmlUnAttrHandler;

class CustomPropsTag
    implements XmlTagHandler, XmlUnAttrHandler,
               XmlTagEntryHandler, XmlTagExitHandler
{
    private XmlCustomPropsValue  props;
    private XmlResourceValue       entVal;

    CustomPropsTag(XmlResourceValue entVal){
        this.props  = null;
        this.entVal = entVal;
    }
    
    public String getName(){
        return "customProps";
    }

    public XmlTagInfo[] getSubTags(){
        return new XmlTagInfo[0];
    }

    public void handleUnknownAttribute(String name, String value){
        this.props.setValue(name, value);
    }

    public void enter(){
        if(this.props != null || this.entVal == null){
            throw new IllegalStateException("CustomPropsTag used in an " +
                                            "illegal fashion");
        }
        
        this.props = new XmlCustomPropsValue();
    }

    public void exit(){
        this.entVal.setCustomProps(this.props);
        this.props  = null;
        this.entVal = null;
    }
}
