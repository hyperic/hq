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

import org.hyperic.util.ArrayUtil;
import org.hyperic.util.xmlparser.XmlAttr;
import org.hyperic.util.xmlparser.XmlAttrException;
import org.hyperic.util.xmlparser.XmlAttrHandler;
import org.hyperic.util.xmlparser.XmlTagEntryHandler;
import org.hyperic.util.xmlparser.XmlTagException;
import org.hyperic.util.xmlparser.XmlTagExitHandler;
import org.hyperic.util.xmlparser.XmlTagHandler;
import org.hyperic.util.xmlparser.XmlTagInfo;

import java.util.List;

class PlatformTag
    extends ResourceTag
    implements XmlAttrHandler, XmlTagHandler, 
               XmlTagEntryHandler, XmlTagExitHandler
{
    private XmlAttr[]      attrs;
    private XmlPlatformValue platVal;      // Current working platform
    private List                platformList; // List for us to fill out

    PlatformTag(List platformList){
        super();
        String[] required, optional;
        
        required = XmlPlatformValue.getRequiredAttributes();
        optional = XmlPlatformValue.getOptionalAttributes();

        this.attrs        = Parser.convertGhettoToAttrs(required, optional);
        this.platformList = platformList;
    }
    
    public String getName(){
        return "platform";
    }
    
    public XmlAttr[] getAttributes(){ 
        return this.attrs;
    }
    
    public XmlTagInfo[] getSubTags(){
        return (XmlTagInfo[])
            ArrayUtil.merge(super.getSubTags(), 
                new XmlTagInfo[] {
                    new XmlTagInfo(new PlatformIPAddrsTag(this.platVal),
                                        XmlTagInfo.OPTIONAL),
                    new XmlTagInfo(new PlatformIPTag(this.platVal),
                                        XmlTagInfo.ZERO_OR_MORE),
                    new XmlTagInfo(new AgentConnTag(this.platVal),
                                        XmlTagInfo.REQUIRED),
                    new XmlTagInfo(new ServerTag(this.platVal),
                                        XmlTagInfo.ZERO_OR_MORE),
                }, new XmlTagInfo[0]);
    }

    public void handleAttribute(int idx, String value)
        throws XmlAttrException
    {
        try {
            this.platVal.setValue(this.attrs[idx].getName(),
                                  value);
        } catch(XmlInvalidAttrException exc){
            throw new XmlAttrException(exc.getMessage());
        }
    }

    public void enter(){
        if(this.platVal != null)
            throw new IllegalStateException("Invalid platform state");

        this.platVal = new XmlPlatformValue();
        this.setEntityVal(this.platVal);
    }

    public void exit()
        throws XmlTagException
    {
        this.platformList.add(this.platVal);
        this.platVal = null;
        this.setEntityVal(this.platVal);
    }
}
