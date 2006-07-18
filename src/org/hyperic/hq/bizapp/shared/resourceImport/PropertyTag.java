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

import org.hyperic.util.xmlparser.XmlAttr;
import org.hyperic.util.xmlparser.XmlAttrException;
import org.hyperic.util.xmlparser.XmlAttrHandler;
import org.hyperic.util.xmlparser.XmlTagEntryHandler;
import org.hyperic.util.xmlparser.XmlTagException;
import org.hyperic.util.xmlparser.XmlTagExitHandler;
import org.hyperic.util.xmlparser.XmlTagHandler;
import org.hyperic.util.xmlparser.XmlTagInfo;

import java.util.Properties;

class PropertyTag
    implements XmlAttrHandler, XmlTagHandler, 
               XmlTagEntryHandler, XmlTagExitHandler
{
    public static final String VAR_START = "${";

    private XmlAttr[] attrs = {
        new XmlAttr("name",  XmlAttr.REQUIRED),
        new XmlAttr("value", XmlAttr.REQUIRED),
    };        

    private Properties props;
    private String curName;
    private String curValue;

    PropertyTag(Properties importProps){
        this.props = importProps;
    }
    
    public String getName(){
        return "property";
    }
    
    public XmlAttr[] getAttributes(){ 
        return this.attrs;
    }
    
    public XmlTagInfo[] getSubTags(){
        return new XmlTagInfo[0];
    }

    public void handleAttribute(int idx, String value)
        throws XmlAttrException
    {
        if(idx == 0)
            this.curName = value;
        else if(idx == 1)
            this.curValue = value;
        else  // Should never occur
            throw new XmlAttrException("Invalid attribute state");
    }

    public void enter(){
        if(this.curName != null || this.curValue != null)
            throw new IllegalStateException("Invalid property state");

        this.curName  = null;
        this.curValue = null;
    }

    public void exit()
        throws XmlTagException
    {
        if(this.curName == null || this.curValue == null)
            throw new IllegalStateException("Invalid property state");

        this.props.setProperty(VAR_START + this.curName + "}", this.curValue);
        this.curName  = null;
        this.curValue = null;
    }
}
