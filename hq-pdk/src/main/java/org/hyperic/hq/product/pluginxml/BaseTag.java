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

package org.hyperic.hq.product.pluginxml;

import java.util.List;
import java.util.Properties;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hyperic.util.ArrayUtil;
import org.hyperic.util.StringUtil;
import org.hyperic.util.xmlparser.XmlAttr;
import org.hyperic.util.xmlparser.XmlAttrException;
import org.hyperic.util.xmlparser.XmlAttrHandler;
import org.hyperic.util.xmlparser.XmlFilterHandler;
import org.hyperic.util.xmlparser.XmlTagEntryHandler;
import org.hyperic.util.xmlparser.XmlTagException;
import org.hyperic.util.xmlparser.XmlTagExitHandler;
import org.hyperic.util.xmlparser.XmlTagHandler;
import org.hyperic.util.xmlparser.XmlTagInfo;

//simple wrapper for XmlParser interfaces
public abstract class BaseTag
    implements XmlTagHandler,
               XmlTagEntryHandler,
               XmlTagExitHandler,
               XmlFilterHandler,
               XmlAttrHandler {

    static final String ATTR_NAME        = "name";
    static final String ATTR_TYPE        = "type";
    static final String ATTR_VERSION     = "version";
    static final String ATTR_DESCRIPTION = "description";
    static final String ATTR_INCLUDE     = "include";
    static final String ATTR_VALUE       = "value";
    static final String ATTR_PLATFORM    = "platform";
    static final String ATTR_CLASS       = "class";
    static final String ATTR_RECURSIVE   = "recursive";
    static final String ATTR_PATH        = "path";
    static final String ATTR_FILTER      = "filter";

    static final String TYPE_GLOBAL      = "global";

    private static final String[] EMPTY_LIST = new String[0];

    private static final String[] EMPTY_ATTRS =
        new String[0];
    private static final XmlAttr[] EMTPY_JATTRS = 
        new XmlAttr[0];
    private static final XmlTagInfo[] EMPTY_TAG_INFO =
        new XmlTagInfo[0];
    
    private XmlAttr[] attrs = EMTPY_JATTRS;
    Properties props = null;
    BaseTag parent;
    PluginData data;
    boolean collectMetrics = true;
    boolean collectHelp = true;
    
    BaseTag() { }
    
    BaseTag(BaseTag parent) {
        this.parent = parent;
        this.data = parent.data;
        this.collectMetrics = parent.collectMetrics;
        this.collectHelp    = parent.collectHelp;
    }
    
    public abstract String getName();

    protected Log getLog() {
        return LogFactory.getLog(this.getClass().getName());
    }

    boolean isGlobalType() {
        return TYPE_GLOBAL.equals(getAttribute(ATTR_TYPE));
    }

    boolean isResourceParent() {
        if (this.parent == null) {
            return false;
        }
        return this.parent instanceof ResourceTag;
    }
    
    String getAttribute(String name, String defval) {
        if (this.props == null) {
            return defval;
        }
        return this.props.getProperty(name, defval);
    }
    
    String getAttribute(String name) {
        return getAttribute(name, null);
    }

    void setAttribute(String name, String value) {
        if (this.props == null) {
            this.props = new Properties();
        }
        
        this.props.setProperty(name, value);        
    }

    public String[] getOptionalAttributes() {
        return EMPTY_ATTRS;
    }
    
    public String[] getRequiredAttributes() {
        return EMPTY_ATTRS;
    }
    
    public XmlAttr[] getAttributes() { 
        String[] optional = getOptionalAttributes();
        String[] required = getRequiredAttributes();

        if ((optional.length == 0) &&
            (required.length == 0))
        {
            return EMTPY_JATTRS;
        }

        int idx = 0;
        this.attrs = new XmlAttr[required.length + optional.length];

        for (int i=0; i<required.length; i++) {
            this.attrs[idx++] =
                new XmlAttr(required[i], XmlAttr.REQUIRED);
        }

        for (int i=0; i<optional.length; i++) {
            this.attrs[idx++] =
                new XmlAttr(optional[i], XmlAttr.OPTIONAL);
        }

        return this.attrs;
    }
    
    public void handleAttribute(int idx, String value)
        throws XmlAttrException
    {
        if (idx >= this.attrs.length) {
            throw new XmlAttrException(idx + " out of range " +
                                            this.attrs.length);
        }
        handleAttribute(this.attrs[idx].getName(), value);
    }
    
    public void handleAttribute(String name, String value)
        throws XmlAttrException
    {
        setAttribute(name, value);
    }
    
    public XmlTagInfo[] getSubTags() {
        return EMPTY_TAG_INFO;
    }

    public XmlTagInfo[] getMergedSubTags(XmlTagInfo[] base,
                                         XmlTagInfo add) {
        return getMergedSubTags(base, new XmlTagInfo[] { add });
    }
    
    public XmlTagInfo[] getMergedSubTags(XmlTagInfo[] base,
                                         XmlTagInfo[] add) {
        return (XmlTagInfo[])
            ArrayUtil.merge(base, add, new XmlTagInfo[0]); 
    }
    
    public String filterAttrValue(XmlTagHandler tag,
                                  String name,
                                  String value)
    {
        return this.data.applyFilters(value);
    }

    public void enter() {
        startTag();
    }

    void startTag() {
    }
    
    public void exit() throws XmlTagException {
        endTag();
        if (this.props != null) {
            this.props.clear();
        }
    }
    
    void endTag() throws XmlTagException {
    }
    
    Object getScratch(String name) {
        return this.data.scratch.get(getName() + name);
    }
    
    void putScratch(String name, Object o) {
        this.data.scratch.put(getName() + name, o);
    }
    
    String[] getList(String s) {
        if (s == null) {
            return EMPTY_LIST;
        }
        List list = StringUtil.explode(s, ",");
        return (String[])list.toArray(EMPTY_LIST);
    }
    
    String[] getAttributeList(String name) {
        return getList(getAttribute(name));
    }
}
