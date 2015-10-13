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

package org.hyperic.hq.ui.action.resource.common.monitor.alerts.config;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hyperic.hq.ui.action.resource.ResourceFormNG;

public class SyslogActionFormNG extends ResourceFormNG {
	   private Log log = LogFactory.getLog(SyslogActionFormNG.class);

	    // -------------------------------------instance variables

	    // control action properties
	    private Integer id; // nullable
	    private Integer ad; // nullable
	    private String metaProject;
	    private String project;
	    private String version;
	    private boolean shouldBeRemoved;

	    // -------------------------------------constructors

	    public SyslogActionFormNG() {
	        // do nothing
	    }

	    // -------------------------------------public methods

	    public Integer getId() {
	        return this.id;
	    }

	    public void setId(Integer id) {
	        this.id = id;
	    }

	    public Integer getAd() {
	        return this.ad;
	    }

	    public void setAd(Integer ad) {
	        this.ad = ad;
	    }

	    public String getMetaProject() {
	        return this.metaProject;
	    }

	    public void setMetaProject(String metaProject) {
	        this.metaProject = metaProject;
	    }

	    public String getProject() {
	        return this.project;
	    }

	    public void setProject(String project) {
	        this.project = project;
	    }

	    public String getVersion() {
	        return this.version;
	    }

	    public void setVersion(String version) {
	        this.version = version;
	    }

	    public boolean getShouldBeRemoved() {
	        return this.shouldBeRemoved;
	    }

	    public void setShouldBeRemoved(boolean shouldBeRemoved) {
	        this.shouldBeRemoved = shouldBeRemoved;
	    }

	    public void reset( ) {
	        super.reset( );
	        metaProject = null;
	        project = null;
	        version = null;
	        shouldBeRemoved = false;
	    }
}
