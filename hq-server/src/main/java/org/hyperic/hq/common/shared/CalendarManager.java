/**
 * NOTE: This copyright does *not* cover user programs that use HQ
 * program services by normal system calls through the application
 * program interfaces provided as part of the Hyperic Plug-in Development
 * Kit or the Hyperic Client Development Kit - this is merely considered
 * normal use of the program, and does *not* fall under the heading of
 *  "derived work".
 *
 *  Copyright (C) [2009-2010], VMware, Inc.
 *  This file is part of HQ.
 *
 *  HQ is free software; you can redistribute it and/or modify
 *  it under the terms version 2 of the GNU General Public License as
 *  published by the Free Software Foundation. This program is distributed
 *  in the hope that it will be useful, but WITHOUT ANY WARRANTY; without
 *  even the implied warranty of MERCHANTABILITY or FITNESS FOR A
 *  PARTICULAR PURPOSE. See the GNU General Public License for more
 *  details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program; if not, write to the Free Software
 *  Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307
 *  USA.
 *
 */

package org.hyperic.hq.common.shared;

import java.util.Collection;

import org.hyperic.hq.common.server.session.Calendar;
import org.hyperic.hq.common.server.session.CalendarEntry;
import org.hyperic.hq.common.server.session.WeekEntry;

/**
 * Local interface for CalendarManager.
 */
public interface CalendarManager {
	/**
	 * Create a calendar with the specified name. This name is only used to
	 * distinguish between calendars and must be unique, however it will be
	 * changed in the future to be used in the UI.
	 */
	public Calendar createCalendar(String name);

	/**
	 * Find all calendars in the system
	 * 
	 * @return {@link Calendar}s
	 */
	public Collection<Calendar> findAll();

	/**
	 * Delete a calendar and all of its entries
	 */
	public void remove(Calendar c);

	/**
	 * Add a weekly entry to a calendar.
	 * 
	 * @param weekDay
	 *            Day of the week (0 to 6)
	 * @param startTime
	 *            # of minutes since midnight
	 * @param endTime
	 *            # of minutes since midnight
	 */
	public WeekEntry addWeekEntry(Calendar c, int weekDay, int startTime,
			int endTime);

	/**
	 * Remove a calendar entry from a calendar
	 */
	public void removeEntry(Calendar c, CalendarEntry ent);

	/**
	 * Remove calendar entries from a calendar
	 */
	public void removeEntries(Calendar c);

	public Calendar findCalendarById(int id);

	public CalendarEntry findEntryById(int id);
}
