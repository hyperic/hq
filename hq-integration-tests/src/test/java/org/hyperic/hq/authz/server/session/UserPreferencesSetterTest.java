/**
 * NOTE: This copyright does *not* cover user programs that use Hyperic
 * program services by normal system calls through the application
 * program interfaces provided as part of the Hyperic Plug-in Development
 * Kit or the Hyperic Client Development Kit - this is merely considered
 * normal use of the program, and does *not* fall under the heading of
 *  "derived work".
 *
 *  Copyright (C) [2010], VMware, Inc.
 *  This file is part of Hyperic.
 *
 *  Hyperic is free software; you can redistribute it and/or modify
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
package org.hyperic.hq.authz.server.session;

import java.util.ArrayList;
import java.util.List;

import org.hyperic.hq.test.BaseInfrastructureTest;
import org.hyperic.util.config.ConfigResponse;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.annotation.DirtiesContext;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * Integration test of {@link UserPreferencesSetter}
 * @author jhickey
 * 
 */
@DirtiesContext
public class UserPreferencesSetterTest
    extends BaseInfrastructureTest {

    @Autowired
    private UserPreferencesSetter userPreferencesSetter;

    @Test
    public void testSetPrefs() throws Exception {
        AuthzSubject who = authzSubjectManager.getOverlordPojo();
        AuthzSubject user = authzSubjectManager.createSubject(who, "jen", true, "jen", "dept",
            "jen@jen.com", "Jen", "Hickey", "1111111", "sms", true);
        flushSession();
        ConfigResponse prefs = new ConfigResponse();
        prefs.setValue("meat", false);
        List<UserPreferencesUpdatedEvent> events = new ArrayList<UserPreferencesUpdatedEvent>(1);
        events.add(new UserPreferencesUpdatedEvent(who.getId(), user.getId(), prefs));
        userPreferencesSetter.processEvents(events);
        flushSession();
        ConfigResponse actualPrefs = authzSubjectManager.getUserPrefs(who, user.getId());
        assertEquals(prefs, actualPrefs);
    }

    /**
     * Validate that prefs are not saved when the user making the change does
     * not exist
     * @throws Exception
     */
    @Test
    public void testSetPrefsInvalidWho() throws Exception {
        AuthzSubject user = authzSubjectManager.createSubject(
            authzSubjectManager.getOverlordPojo(), "jen", true, "jen", "dept", "jen@jen.com",
            "Jen", "Hickey", "1111111", "sms", true);
        flushSession();
        ConfigResponse prefs = new ConfigResponse();
        prefs.setValue("meat", false);
        List<UserPreferencesUpdatedEvent> events = new ArrayList<UserPreferencesUpdatedEvent>(1);
        events.add(new UserPreferencesUpdatedEvent(345, user.getId(), prefs));
        userPreferencesSetter.processEvents(events);
        flushSession();
        assertTrue(authzSubjectManager.getUserPrefs(authzSubjectManager.getOverlordPojo(),
            user.getId()).toProperties().isEmpty());
    }

    /**
     * Validate that Exception is just logged if setting prefs for a
     * non-existent user
     * @throws Exception
     */
    @Test
    public void testSetPrefsInvalidSubject() throws Exception {
        ConfigResponse prefs = new ConfigResponse();
        prefs.setValue("meat", false);
        List<UserPreferencesUpdatedEvent> events = new ArrayList<UserPreferencesUpdatedEvent>(1);
        events.add(new UserPreferencesUpdatedEvent(authzSubjectManager.getOverlordPojo().getId(),
            345, prefs));
        userPreferencesSetter.processEvents(events);
        flushSession();
    }

}
