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

import java.awt.Rectangle;
import java.awt.image.BufferedImage;
import java.io.Serializable;
import java.util.Iterator;
import java.util.Vector;
import org.hyperic.util.data.ITreeNode;

public class TreeNode implements ITreeNode, Serializable
{
    protected Vector  m_upChildren = new Vector();
    protected Vector  m_downChildren = new Vector();
    private   String  m_desc;
    private   String  m_name;
    private   Vector  m_rects;
    private   boolean m_selected;
    
    public TreeNode(String name, String desc) {
        this(name, desc, false);
    }
    
    public TreeNode(String name, String desc, boolean selected) {
        m_name     = name;
        m_desc     = desc;
        m_selected = selected;
    }

    /**
     * Adds a single child to the up children list.
     * @param child An object that implements the IResourceTreeNode interface.
     * @see org.hyperic.util.data.IResourceTreeNode
     */
    public void addUpChild(ITreeNode child) {
        m_upChildren.add(child);    
    }

    /**
     * Add an array of children to the up children list.
     * @param children An array of objects that implement the IResourceTreeNode
     *                 interface.
     * @see org.hyperic.util.data.IResourceTreeNode
     */
    public void addUpChildren(ITreeNode[] children) {
        for(int i = 0;i < children.length;i++)
            m_upChildren.add(children[i]);
    }
    
    /**
     * @see org.hyperic.util.data.ITreeNode#getUpChildren()
     */
    public ITreeNode[] getUpChildren() {
        return (ITreeNode[])m_upChildren.toArray(new ITreeNode[0]);
    }

    /**
     * @return The number of up children for this node.
     */    
    public int getUpChildrenCount() {
        return m_upChildren.size();
    }
    
    /**
     * @return Whether the node has up children.
     */    
    public boolean hasUpChildren() {
        return (this.getUpChildrenCount() > 0);
    }
    
    /**
     * Adds a single child to the down children list.
     * @param child An object that implements the IResourceTreeNode interface.
     * @see org.hyperic.util.data.IResourceTreeNode
     */
    public void addDownChild(ITreeNode child) {
        m_downChildren.add(child);    
    }

    /**
     * Add an array of children to the down children list.
     * @param children An array of objects that implement the IResourceTreeNode
     *                 interface.
     * @see org.hyperic.util.data.IResourceTreeNode
     */
    public void addDownChildren(ITreeNode[] children) {
        for(int i = 0;i < children.length;i++)
            m_downChildren.add(children[i]);
    }
    
    /**
     * @see org.hyperic.util.data.ITreeNode#getDownChildren()
     */
    public ITreeNode[] getDownChildren() {
        return (ITreeNode[])m_downChildren.toArray(new ITreeNode[0]);
    }

    /**
     * @return The number of down children for this node.
     */    
    public int getDownChildrenCount() {
        return m_downChildren.size();
    }
    
    /**
     * @return Whether the node has down children.
     */    
    public boolean hasDownChildren() {
        return (this.getDownChildrenCount() > 0);
    }
    
    /**
     * Empties the list of both up and down children.
     */
    public void clear() {
        m_upChildren.clear();
        m_downChildren.clear();
    }
    
    /**
     * @see org.hyperic.util.data.ITreeNode#getDescription()
     */
    public String getDescription() {
        return m_desc;
    }

    /**
     * @see org.hyperic.util.data.ITreeNode#getImage()
     */
    public BufferedImage getImage() {
        return null;
    }
    
    /**
     * @see org.hyperic.util.data.ITreeNode#getName()
     */
    public String getName() {
        return m_name;
    }
    
    /**
     * @see org.hyperic.util.data.ITreeNode#getRectangles()
     */
    public Rectangle[] getRectangles() {
        return (m_rects == null) ? null
                 : (Rectangle[])m_rects.toArray(new Rectangle[m_rects.size()]);
    }
    
    /**
     * @see org.hyperic.util.data.ITreeNode#isSelected()
     */
    public boolean isSelected() {
        return m_selected;
    }
    
    /**
     * @see org.hyperic.util.data.ITreeNode#addRectangle()
     */
    public void addRectangle(int x, int y, int cx, int cy) {
        if(m_rects == null)
            m_rects = new Vector();
            
        m_rects.add(new Rectangle(x, y, cx, cy));
    }
    
    /**
     * Clears the internal state of the node. The list of rectangles must be
     * cleared at a minumum. This method is called by the ResourceTree.reset()
     * method.
     * 
     * @see org.hyperic.image.widget.ResourceTree#reset()
     */
    public void reset() {
        m_rects = null;    
    }
}
