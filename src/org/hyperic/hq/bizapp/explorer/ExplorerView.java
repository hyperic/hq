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

/**
 * An ExplorerView represents 1 (of possibly many) views of the selection 
 * on the left-hand side of the explorer.  
 * 
 * It describes 2 things:
 *    1 - The button used to select the view
 *    2 - The location of the view's content   
 */
public interface ExplorerView {
    /**
     * Get the name of this view.  Should be a unique string like
     * "criteriaEditor" or "alertCenter"
     */
    String getName();
    
    /**
     * Get the styleclass used to render the view buttons on the right-hand
     * side of the explorer. 
     */
    String getStyleClass();

    /**
     * Get a text description of this view:
     *   e.g. "Criteria Editor" 
     */
    String getText();
    
    /**
     * Get the type of view that this object is. 
     */
    ExplorerViewType getType();
}
