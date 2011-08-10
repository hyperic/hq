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
 * 
 */

/**
 * @author Adar Margalit
 */

package org.hyperic.hq.product.pluginxml;

import org.hyperic.util.xmlparser.XmlTagInfo;

public class MonitoredTag
    extends BaseTag {
    private static final String TAG_NAME = "monitored";

    public MonitoredTag() {
    }

    public MonitoredTag(BaseTag parent) {
        super(parent);
    }

    /* (non-Javadoc)
     * @see org.hyperic.hq.product.pluginxml.BaseTag#getName()
     */
    @Override
    public String getName() {
        return TAG_NAME;
    }

    public XmlTagInfo[] getSubTags() {
        return new XmlTagInfo[] {
           new XmlTagInfo(new FolderTag(this),
                    XmlTagInfo.ONE_OR_MORE),
           new XmlTagInfo(new MonitoredPropertiesTag(this),
                   XmlTagInfo.ZERO_OR_MORE)
        };
    }
}
