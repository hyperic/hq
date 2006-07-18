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

package org.hyperic.image.widget;

import java.awt.image.BufferedImage;
import java.util.Vector;
import org.hyperic.image.ImageUtil;
import org.hyperic.util.data.ITreeNode;
import org.hyperic.util.data.IResourceTreeNode;

public class ResourceTreeNode extends TreeNode implements IResourceTreeNode
{
    private int m_type;

    //**************** Constructors *************
    public ResourceTreeNode(String name, String desc) {
        this(name, desc, false, NONE);
    }
    public ResourceTreeNode(String name, int type) {
        this(name, null, false, type);
    }

    public ResourceTreeNode(String name, String desc, int type) {
        this(name, desc, false, type);
    }
    
    public ResourceTreeNode(String name, String desc, boolean selected) {
        this(name, desc, selected, NONE);
    }
    
    public ResourceTreeNode(String name, String desc, boolean selected,
                            int type)
    {
        super(name, desc, selected);
        
        if(this.isValidType(type) == false)
            throw new IllegalArgumentException("Invalid Type: " + type);
            
        m_type = type;
    }

    //************ ITreeNode Methods ************
    /**
     * @see org.hyperic.util.data.ITreeNode#getUpChildren()
     */
    public ITreeNode[] getUpChildren() {
        return (IResourceTreeNode[])m_upChildren.toArray(
                                        new IResourceTreeNode[0]);
    }
    
    /**
     * @see org.hyperic.util.data.ITreeNode#getDownChildren()
     */
    public ITreeNode[] getDownChildren() {
        return (IResourceTreeNode[])m_downChildren.toArray(
                                        new IResourceTreeNode[0]);
    }

    /**
     * @see org.hyperic.util.data.ITreeNode#getType() 
     */
    public int getType() { return m_type; }

    
    //******** IResourceTreeNode Methods ********

    //************ Public Methods ***************
    public String toString () {
        StringBuffer buf = new StringBuffer();
        buf.append("[")
            .append(" name=").append(getName())
            .append(" desc=").append(getDescription())
            .append(" type=").append(getType())
            .append(" up-children: ").append(getUpChildren())
            .append(" down-chilren: ").append(getDownChildren())
            .append(" ]");
        return buf.toString();
    }
    
    //************* Private Methods *************
    private boolean isValidType(int type) {
        return (type >= NONE && type <= CLUSTER); 
    }
}

// EOF
