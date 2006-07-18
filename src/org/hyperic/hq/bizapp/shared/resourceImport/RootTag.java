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

import org.hyperic.util.xmlparser.XmlFilterHandler;
import org.hyperic.util.xmlparser.XmlTagEntryHandler;
import org.hyperic.util.xmlparser.XmlTagException;
import org.hyperic.util.xmlparser.XmlTagExitHandler;
import org.hyperic.util.xmlparser.XmlTagHandler;
import org.hyperic.util.xmlparser.XmlTagInfo;

import org.hyperic.util.StringUtil;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.Map;
import java.util.Properties;

class RootTag
    implements XmlTagHandler, XmlTagEntryHandler,
               XmlTagExitHandler, XmlFilterHandler
{
    private BatchImportData batchData;
    private ArrayList       applications;
    private ArrayList       platforms;
    private ArrayList       groups;
    private Properties      importProps;

    RootTag(BatchImportData batchData){
        this.batchData   = batchData;
    }

    public String getName(){
        return "hq";
    }

    public XmlTagInfo[] getSubTags(){
        return new XmlTagInfo[] {
            new XmlTagInfo(new PlatformTag(this.platforms), 
                                XmlTagInfo.ZERO_OR_MORE),
            new XmlTagInfo(new ApplicationTag(this.applications), 
                                XmlTagInfo.ZERO_OR_MORE),
            new XmlTagInfo(new GroupTag(this.groups),
                                XmlTagInfo.ZERO_OR_MORE),
            new XmlTagInfo(new PropertyTag(this.importProps), 
                                XmlTagInfo.ZERO_OR_MORE),
        };
    }

    public String filterAttrValue(XmlTagHandler tag, String attrName,
                                  String attrValue)
    {
        return replaceMulti(this.importProps, attrValue,
                            PropertyTag.VAR_START);
    }

    public void enter(){
        this.applications = new ArrayList();
        this.platforms    = new ArrayList();
        this.groups       = new ArrayList();
        this.importProps  = new Properties();
    }

    public void exit()
        throws XmlTagException
    {
        for(Iterator i=this.applications.iterator(); i.hasNext(); ){
            this.batchData.addApplication((XmlApplicationValue)i.next());
        }

        for(Iterator i=this.platforms.iterator(); i.hasNext(); ){
            this.batchData.addPlatform((XmlPlatformValue)i.next());
        }

        for(Iterator i=this.groups.iterator(); i.hasNext(); ){
            this.batchData.addGroup((XmlGroupValue)i.next());
        }

        this.applications = null;
        this.platforms    = null;
        this.groups       = null;
        this.importProps  = null;
    }

    private static String replaceMulti(Properties props, String in,
                                       String varStart)
    {
        if (in.indexOf(varStart) == -1) {
            return in;
        }
    
        for (Iterator i=props.entrySet().iterator(); i.hasNext();) {
            Map.Entry ent = (Map.Entry)i.next();
    
            in = StringUtil.replace(in, (String)ent.getKey(),
                                    (String)ent.getValue());
        }

        return in;
    }
}
