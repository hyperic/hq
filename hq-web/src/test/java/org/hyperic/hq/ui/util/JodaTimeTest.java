package org.hyperic.hq.ui.util;

import static org.junit.Assert.*;

import java.util.Calendar;
import java.util.GregorianCalendar;

import org.hyperic.hq.ui.action.resource.common.monitor.alerts.ListAlertAction;
import org.joda.time.DateTime;
import org.junit.Test;


public class JodaTimeTest {
	
	@Test
	public void testAddingOneDayAnRemovingOneSecond() {
        GregorianCalendar cal = new GregorianCalendar();
        cal.set(Calendar.HOUR_OF_DAY, 0);
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.SECOND, 0);
        
        final DateTime begin = new DateTime(cal);
        final DateTime end = new ListAlertAction(null, null, null, null).addAlmostOneDay(begin);
        
        assertEquals(0, begin.getHourOfDay());
        assertEquals(0, begin.getMinuteOfHour());
        assertEquals(0, begin.getSecondOfMinute());
        
        assertEquals(23, end.getHourOfDay());
        assertEquals(59, end.getMinuteOfHour());
        assertEquals(59, end.getSecondOfMinute());
        
        assertEquals(begin.getDayOfMonth(), end.getDayOfMonth());
	}

}
