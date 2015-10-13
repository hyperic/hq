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

package org.hyperic.hq.ui.action.resource.common.monitor.visibility;

import java.util.Date;
import java.util.List;
import java.util.Map;

import javax.servlet.ServletException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.tiles.AttributeContext;
import org.apache.tiles.context.TilesRequestContext;
import org.apache.tiles.preparer.ViewPreparer;
import org.hyperic.hq.ui.WebUser;
import org.hyperic.hq.ui.action.BaseActionNG;
import org.hyperic.hq.ui.util.MonitorUtilsNG;
import org.hyperic.hq.ui.util.RequestUtils;
import org.hyperic.util.config.InvalidOptionException;

/**
 * An <code>Action</code> that retrieves data from the BizApp to facilitate
 * display of the various pages that provide metrics summaries.
 */
public class MetricDisplayRangeFormPrepareActionNG extends BaseActionNG
		implements ViewPreparer {

	protected final Log log = LogFactory
			.getLog(MetricDisplayRangeFormPrepareActionNG.class.getName());

	private MetricRange getLastNRange(Integer lastN, Integer unit) {
		MetricRange range = new MetricRange();

		List<Long> timeframe = MonitorUtilsNG.calculateTimeFrame(
				lastN.intValue(), unit.intValue());
		if (timeframe != null) {
			range.setBegin((Long) timeframe.get(0));
			range.setEnd((Long) timeframe.get(1));
			return range;
		} else {
			return null;
		}
	}

	public void execute(TilesRequestContext tilesContext,
			AttributeContext attributeContext) {
		MetricDisplayRangeFormNG rangeForm = new MetricDisplayRangeFormNG();

		tilesContext.getRequestScope().put("rangeForm", doExecute(rangeForm));

	}

	protected MetricDisplayRangeFormNG doExecute(
			MetricDisplayRangeFormNG rangeForm) {
		try {
			WebUser user = RequestUtils.getWebUser(getServletRequest());
			Map<String, Object> pref = user.getMetricRangePreference(false);

			if (rangeForm.isResetClicked() || rangeForm.getRn() == null) {
				rangeForm.setRn((Integer) pref.get(MonitorUtilsNG.LASTN));
			}
			if (rangeForm.isResetClicked() || rangeForm.getRu() == null) {
				rangeForm.setRu((Integer) pref.get(MonitorUtilsNG.UNIT));
			}

			if (rangeForm.isResetClicked() || rangeForm.getA() == null) {
				Boolean readOnly = (Boolean) pref.get(MonitorUtilsNG.RO);
				if (readOnly.booleanValue()) {
					rangeForm.setA(MetricDisplayRangeFormNG.ACTION_DATE_RANGE);
				} else {
					rangeForm.setA(MetricDisplayRangeFormNG.ACTION_LASTN);
				}
			}

			// try to set date range using saved begin and end prefs
			Long begin = (Long) pref.get(MonitorUtilsNG.BEGIN);
			Long end = (Long) pref.get(MonitorUtilsNG.END);
			if (begin == null || end == null) {
				// default date range to lastN timeframe
				MetricRange range = getLastNRange(rangeForm.getRn(),
						rangeForm.getRu());
				if (range != null) {
					begin = range.getBegin();
					end = range.getEnd();
				} else {
					// finally, fall back to now
					begin = end = new Long(System.currentTimeMillis());
				}
			}
			rangeForm.populateStartDate(new Date(begin.longValue()),
					getServletRequest().getLocale());
			rangeForm.populateEndDate(new Date(end.longValue()),
					getServletRequest().getLocale());
		} catch (InvalidOptionException ioe) {
			log.error(ioe);
		} catch (ServletException e) {
			log.error(e);
		}

		// blank range number if it's set to 0
		if (rangeForm.getRn() != null && rangeForm.getRn().intValue() == 0) {
			rangeForm.setRn(null);
		}

		return rangeForm;
	}
}
