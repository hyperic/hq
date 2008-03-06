/*
 * NOTE: This copyright does *not* cover user programs that use HQ
 * program services by normal system calls through the application
 * program interfaces provided as part of the Hyperic Plug-in Development
 * Kit or the Hyperic Client Development Kit - this is merely considered
 * normal use of the program, and does *not* fall under the heading of
 * "derived work".
 * 
 * Copyright (C) [2004 - 2008], Hyperic, Inc.
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
package org.hyperic.ui.tapestry.components.panel;

import org.apache.tapestry.BaseComponent;
import org.apache.tapestry.annotations.Parameter;

public abstract class MessagePanel extends BaseComponent {
    
    public static final String MSG_ERROR = "Error";
    public static final String MSG_WARING = "Warning";
    public static final String MSG_INFO = "Info";
    public static final String MSG_CONFIRM = "Confirm";
    
    public static final String MSG_ICON_SMALL = "small";
    public static final String MSG_ICON_LARGE = "large";
    
    /**
     * The message to display
     */
    @Parameter(name = "message", required = true)
    public abstract String getMessage();
    public abstract void setMessage(String message);
    
    /**
     * The type of message
     */
    @Parameter(name = "type", defaultValue = "@org.hyperic.ui.tapestry.components.panel.MessagePanel@MSG_ERROR")
    public abstract String getType();
    public abstract void setType(String type);
    
    /**
     * Should the message be alowed to contain html 
     */
    @Parameter(name = "raw", defaultValue = "false")
    public abstract boolean getIsRaw();
    public abstract void setIsRaw(boolean raw);
    
    /**
     * The dom node ID. Specified to differentiate between panels
     */
    @Parameter(name = "nodeID", defaultValue = "")
    public abstract String getNodeId();
    public abstract void setNodeId(String nodeId);
    
    /**
     * Should the panel be hidden on initial render?
     */
    @Parameter(name = "hidden")
    public abstract boolean getIsHidden();
    public abstract void setIsHidden(boolean defaultHidden);
    
    /**
     * Small (16x16) or Large (32x32) icon
     */
    @Parameter(name = "iconSize", defaultValue = MSG_ICON_SMALL )
    public abstract boolean getIsLargeIcon();
    public abstract void setIsLargeIcon(boolean isLargeIcon);
 
    public String getStyle(){
	if(getIsHidden())
	    return "display:none;";
	else
	    return "";
    }
}
