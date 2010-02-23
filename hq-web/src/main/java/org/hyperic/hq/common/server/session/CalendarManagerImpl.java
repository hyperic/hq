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

package org.hyperic.hq.common.server.session;

import java.util.Collection;

import org.hyperic.hq.common.shared.CalendarManager;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class CalendarManagerImpl implements CalendarManager {
	private CalendarDAO calendarDAO;
	private CalendarEntryDAO calendarEntryDAO;

	@Autowired
	public CalendarManagerImpl(CalendarDAO calendarDAO,
			CalendarEntryDAO calendarEntryDAO) {
		this.calendarDAO = calendarDAO;
		this.calendarEntryDAO = calendarEntryDAO;
	}

	/**
	 * Create a calendar with the specified name. This name is only used to
	 * distinguish between calendars and must be unique, however it will be
	 * changed in the future to be used in the UI.
	 * 
	 */
	public Calendar createCalendar(String name) {
		Calendar c = new Calendar(name);

		calendarDAO.save(c);

		return c;
	}

	/**
	 * Find all calendars in the system
	 * 
	 * @return {@link Calendar}s
	 */
    @Transactional(readOnly=true)
	public Collection<Calendar> findAll() {
		return calendarDAO.findAll();
	}

	/**
	 * Delete a calendar and all of its entries
	 * 
	 */
	public void remove(Calendar c) {
		calendarDAO.remove(c);
	}

	/**
	 * Add a weekly entry to a calendar.
	 * 
	 * @param weekDay
	 *            Day of the week (0 to 6)
	 * @param startTime
	 *            # of minutes since midnight
	 * @param endTime
	 *            # of minutes since midnight
	 * 
	 */
	public WeekEntry addWeekEntry(Calendar c, int weekDay, int startTime,
			int endTime) {
		WeekEntry res = c.addWeekEntry(weekDay, startTime, endTime);

		calendarEntryDAO.save(res); // In order to pick up an ID

		return res;
	}

	/**
	 * Remove a calendar entry from a calendar
	 * 
	 */
	public void removeEntry(Calendar c, CalendarEntry ent) {
		if (!c.removeEntry(ent)) {
			throw new IllegalArgumentException(
					"Entry was not a part of the calendar");
		}
	}

	/**
	 * Remove calendar entries from a calendar
	 * 
	 */
	public void removeEntries(Calendar c) {
		calendarDAO.removeEntries(c);
	}

	/**
	 * 
	 */
    @Transactional(readOnly=true)
	public Calendar findCalendarById(int id) {
		return calendarDAO.findById(new Integer(id));
	}

    /**
     * 
     */
    @Transactional(readOnly=true)
	public CalendarEntry findEntryById(int id) {
		return calendarEntryDAO.findById(new Integer(id));
	}
}
