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
