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
 * A factory which creates {@link ExplorerItem}s.
 */
public interface ExplorerItemType {
    /**
     * Get a short string identifying the item type.  Can be used when 
     * unregistering item types in the {@link ExplorerManager}
     */
    String getName();
    
    /**
     * Get children which this type thinks belong to the parent.
     * 
     * This is useful for plugins who wish to be displayed as children
     * of an item, without the item knowing about the child's existence
     * prior.
     * 
     * @see ExplorerManager#findAllChildren(ExplorerContext, ExplorerItem)
     */
    List getAdoptedChildren(ExplorerContext ctx, ExplorerItem parent);

    /**
     * Get a single child, by code.  Allows for an optimized searching when
     * looking for a single node (don't have to look everything up via
     * getChildren())  
     */
    ExplorerItem getChild(ExplorerContext ctx, ExplorerItem parent, 
                          String code);
}
