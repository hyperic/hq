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

/*
 * MonitoringConfigForm.java
 *
 * Created on April 14, 2003, 1:44 PM
 */

package org.hyperic.hq.ui.action.resource.common.monitor.config;

import javax.servlet.http.HttpServletRequest;

import org.apache.commons.lang3.ArrayUtils;
import org.apache.struts2.dispatcher.mapper.ActionMapping;
import org.hyperic.hq.ui.Constants;
import org.hyperic.hq.ui.action.resource.ResourceFormNG;
import org.hyperic.hq.ui.util.ImageButtonBean;

/**
 * Form for setting the collection interval for metrics in
 * resource/monitoring/configuration areas of the application, and for adding
 * metrics to a resource.
 * 
 */
public class MonitoringConfigFormNG extends ResourceFormNG {

	/** Holds value of property mids (MetricIds). */
	private Integer[] mids;

	/** Holds value of property collectionInterval */
	private Long collectionInterval;

	/** Holds value of property collectionUnit. */
	private long collectionUnit;

	private ImageButtonBean indBtn;

	private String clickedType = "";

	/** Creates new MonitoringConfigForm */
	public MonitoringConfigFormNG() {
		super();
		indBtn = new ImageButtonBean();
	}

	/**
	 * Derived property based on collectionInterval and collectionUnit, return
	 * the time as a long
	 */
	public long getIntervalTime() {
		return collectionInterval.longValue() * collectionUnit;
	}

	public void reset(ActionMapping mapping, HttpServletRequest request) {
		this.collectionUnit = Constants.MINUTES;
		this.collectionInterval = null;
		this.mids = new Integer[0];
		super.reset(mapping, request);
	}

	public Long getCollectionInterval() {
		return collectionInterval;
	}

	/**
	 * Getter for property mids.
	 * 
	 * @return Value of property mids.
	 * 
	 */
	public Integer[] getMids() {
		return this.mids;
	}

	/**
	 * Setter for property mids.
	 * 
	 * @param mids
	 *            New value of property mids.
	 * 
	 */
	public void setMids(Integer[] mids) {
		this.mids = mids;
	}

	/**
	 * Setter for property collectionInterval.
	 * 
	 * @param collectionInterval
	 *            New value of property collectionInterval.
	 * 
	 */
	public void setCollectionInterval(Object collectionInterval) {

		if (collectionInterval instanceof Long) {
			this.collectionInterval = (Long) collectionInterval;
		} else if (collectionInterval instanceof String[]
				&& !ArrayUtils.isEmpty((String[]) collectionInterval)) {
			try{
				this.collectionInterval = Long
						.parseLong(((String[]) collectionInterval)[0]);
			}catch(Exception e){
				this.collectionInterval = -1l;
			}
		} else {
			this.collectionInterval = -1l;
		}
	}

	/**
	 * Getter for property collectionUnit.
	 * 
	 * @return Value of property collectionUnit.
	 * 
	 */
	public long getCollectionUnit() {
		return this.collectionUnit;
	}

	/**
	 * Setter for property collectionUnit.
	 * 
	 * @param collectionUnit
	 *            New value of property collectionUnit.
	 * 
	 */
	public void setCollectionUnit(long collectionUnit) {
		this.collectionUnit = collectionUnit;
	}

	public ImageButtonBean getIndBtn() {
		return indBtn;
	}

	public void setIndBtn(ImageButtonBean indBtn) {
		this.indBtn = indBtn;
	}

	public boolean isIndSelected() {
		return this.indBtn != null && this.indBtn.isSelected();
	}

	public String getClickedType() {
		return clickedType;
	}

	public void setClickedType(String clickedType) {
		this.clickedType = clickedType;
	}

}
