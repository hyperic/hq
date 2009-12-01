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
package org.hyperic.hq.bizapp.explorer;

import java.util.List;

/**
 * Represents a single item in an explorer tree.
 */
public interface ExplorerItem {
    /**
     * A unique identifier for the item.  This is used to find instances of
     * the item when scanning a tree.
     */
    String getCode();
    
    /**
     * Get the display label for the item.  This label should be localized.
     */
    String getLabel();
    
    /**
     * Get the type that created this item.
     */
    ExplorerItemType getType();

    /**
     * Get the item's direct children.  
     * 
     * Note that if you are attempting to get _all_ of an item's children, you
     * should use ExplorerManager.getAllChildren, as that will include any
     * adopted items.
     * 
     * @return a list of {@link ExplorerItem}s
     */
    List getChildren();

    ExplorerItem getParent();
}
