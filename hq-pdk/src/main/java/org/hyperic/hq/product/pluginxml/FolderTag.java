/*
 * NOTE: This copyright does *not* cover user programs that use HQ
 * program services by normal system calls through the application
 * program interfaces provided as part of the Hyperic Plug-in Development
 * Kit or the Hyperic Client Development Kit - this is merely considered
 * normal use of the program, and does *not* fall under the heading of
 * "derived work".
 * 
 * Copyright (C) [2004-2011], Hyperic, Inc.
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

/**
 * @author Adar Margalit
 */

package org.hyperic.hq.product.pluginxml;

import org.hyperic.hq.product.MonitoredFolderConfig;
import org.hyperic.util.xmlparser.XmlAttrException;
import org.hyperic.util.xmlparser.XmlEndAttrHandler;
import org.hyperic.util.xmlparser.XmlTagInfo;

public class FolderTag
    extends BaseTag implements XmlEndAttrHandler{


    private static final String[] OPTIONAL_ATTRS =
            { ATTR_FILTER };

    private static final String[] REQUIRED_ATTRS =
        { ATTR_RECURSIVE, ATTR_PATH };

    private static final String TAG_NAME = "folder";
    
    private MonitoredFolderConfig monitoredFolderConfig;

    private MonitoredFolderConfig parentFolder = null;

    /**
     * @param parent
     */
    public FolderTag(BaseTag parent) {
        super(parent);
        if (parent instanceof FolderTag)
            parentFolder = ((FolderTag)parent).getMonitoredFolderConfig();
    }

    /* (non-Javadoc)
     * @see org.hyperic.hq.product.pluginxml.BaseTag#getName()
     */
    @Override
    public String getName() {
        return TAG_NAME;
    }

    public String[] getOptionalAttributes() {
        return OPTIONAL_ATTRS;
    }
    
    public String[] getRequiredAttributes() {
        return REQUIRED_ATTRS;
    }

    public void endAttributes() throws XmlAttrException {
        final String recStr = getAttribute(ATTR_RECURSIVE);
        final boolean recursive = recStr == null || recStr.trim().length() <= 0 ? false : Boolean.valueOf(getAttribute(ATTR_RECURSIVE)).booleanValue();    
        final String path = getAttribute(ATTR_PATH);    
        final String filter = getAttribute(ATTR_FILTER);
        this.monitoredFolderConfig = new MonitoredFolderConfig(path, filter, recursive);
    }
    
    void endTag() {
        if (parentFolder != null)
            parentFolder.addSubFolder(this.monitoredFolderConfig);
        else
            this.data.addMonitoredConfig(this.monitoredFolderConfig);
    }
    
    public MonitoredFolderConfig getMonitoredFolderConfig() {
        return monitoredFolderConfig;
    }
    
    public XmlTagInfo[] getSubTags() {
        return new XmlTagInfo[] {
            new XmlTagInfo(new FolderTag(this),
                           XmlTagInfo.ZERO_OR_MORE),
        };
    }
}
