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

import javax.ejb.SessionBean;
import javax.ejb.SessionContext;

import org.hyperic.dao.DAOFactory;
import org.hyperic.hq.common.SystemException;
import org.hyperic.hq.common.server.session.Calendar;
import org.hyperic.hq.common.server.session.CalendarEntry;
import org.hyperic.hq.common.server.session.WeekEntry;
import org.hyperic.hq.common.shared.CalendarManagerLocal;
import org.hyperic.hq.common.shared.CalendarManagerUtil;


/**
 * @ejb:bean name="CalendarManager"
 *      jndi-name="ejb/common/CalendarManager"
 *      local-jndi-name="LocalCalendarManager"
 *      view-type="local"
 *      type="Stateless"
 * @ejb:util generate="physical"
 * @ejb:transaction type="REQUIRED"
 */
public class CalendarManagerEJBImpl implements SessionBean {
    private final CalendarDAO _calDAO = 
        new CalendarDAO(DAOFactory.getDAOFactory());
    private final CalendarEntryDAO _entryDAO = 
        new CalendarEntryDAO(DAOFactory.getDAOFactory());
    
    /**
     * Create a calendar with the specified name.  This name is only used
     * to distinguish between calendars and must be unique, however it
     * will be changed in the future to be used in the UI.
     * 
     * @ejb:interface-method
     */
    public Calendar createCalendar(String name) {
        Calendar c = new Calendar(name);
        
        _calDAO.save(c);
        return c;
    }
    
    /**
     * Find all calendars in the system
     * 
     * @return {@link Calendar}s
     * @ejb:interface-method
     */
    public Collection findAll() {
        return _calDAO.findAll();
    }
    
    /** 
     * Delete a calendar and all of its entries
     * 
     * @ejb:interface-method
     */
    public void remove(Calendar c) {
        _calDAO.remove(c);
    }
    
    /**
     * Add a weekly entry to a calendar.  
     * 
     * @param weekDay    Day of the week (0 to 6)
     * @param startTime  # of minutes since midnight
     * @param endTime    # of minutes since midnight
     *  
     * @ejb:interface-method
     */
    public WeekEntry addWeekEntry(Calendar c, int weekDay, int startTime,
                                  int endTime) 
    {
        WeekEntry res = c.addWeekEntry(weekDay, startTime, endTime);
        
        _entryDAO.save(res); // In order to pick up an ID
        return res;
    }
    
    /**
     * Remove a calendar entry from a calendar
     *  
     * @ejb:interface-method
     */
    public void removeEntry(Calendar c, CalendarEntry ent) {
        if (c.removeEntry(ent) == false) {
            throw new IllegalArgumentException("Entry was not a part of the " +
                                               "calendar");
        }
    }

    /** 
     * @ejb:interface-method
     */
    public Calendar findCalendarById(int id) {
        return _calDAO.findById(new Integer(id));
    }

    /** 
     * @ejb:interface-method
     */
    public CalendarEntry findEntryById(int id) {
        return _entryDAO.findById(new Integer(id));
    }
    
    public static CalendarManagerLocal getOne() {
        try {
            return CalendarManagerUtil.getLocalHome().create();
        } catch(Exception e) {
            throw new SystemException(e);
        }
    }

    public void ejbCreate() { }
    public void ejbRemove() { }
    public void ejbActivate() { }
    public void ejbPassivate() { }
    public void setSessionContext(SessionContext c) {}
}
