/*
 * NOTE: This copyright does *not* cover user programs that use HQ
 * program services by normal system calls through the application
 * program interfaces provided as part of the Hyperic Plug-in Development
 * Kit or the Hyperic Client Development Kit - this is merely considered
 * normal use of the program, and does *not* fall under the heading of
 * "derived work".
 * 
 * Copyright (C) [2004-2008], Hyperic, Inc.
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

package org.hyperic.hq.events;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.ResourceBundle;

import org.hyperic.hq.appdef.shared.AppdefEntityID;
import org.hyperic.hq.measurement.shared.ResourceLogEvent;
import org.hyperic.hq.product.LogTrackPlugin;
import org.hyperic.hq.product.TrackEvent;
import org.hyperic.util.StringUtil;

/**
 * Class to track cloning events
 */
public class CloningEvent extends ResourceLogEvent
{
    private static final String BUNDLE = "org.hyperic.hq.events.Resources";

    private AppdefEntityID master;
    private Collection clonedResources = new ArrayList();
    private MessageFormat messageFormat;
    
    public CloningEvent(AppdefEntityID id) {
    	super(new TrackEvent(id,
                             System.currentTimeMillis(),
                             LogTrackPlugin.LOGLEVEL_INFO, "", ""));

        master = id;
        ResourceBundle bundle = ResourceBundle.getBundle(BUNDLE);
        setSource(bundle.getString("event.clone.platform.subject"));
        setMessage(bundle.getString("event.clone.platform.message.start"));
        messageFormat = new MessageFormat(bundle.getString("event.clone.platform.message.end"));
    }
    
    public void addClonedResource(AppdefEntityID clonedId) {
    	clonedResources.add(clonedId);
    	logMessage();
    }
    
    public Collection getClonedResources() {
    	return Collections.unmodifiableCollection(clonedResources);
    }
    
    public void logMessage() {
    	StringBuffer sb = new StringBuffer();
    	messageFormat.format(
    					new String[] {
    							master.getAppdefKey(), 
    							Integer.toString(getClonedResources().size())},
    					sb, null);
    	
    	setTimestamp(System.currentTimeMillis());
    	setMessage(sb.toString());
    }
    
    public String toString() {
    	StringBuffer sb = new StringBuffer();
    	if ((getMessage() != null) && (getMessage().length() > 0)) {
    		sb.append(getMessage());
    	} else {
    		sb.append("CloningEvent[");
    		sb.append("master=");
    		sb.append(master.getAppdefKey());
    		sb.append("]");
    	}
    	return sb.toString();
    }
}
