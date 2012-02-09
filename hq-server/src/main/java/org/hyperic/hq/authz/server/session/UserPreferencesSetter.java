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

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.annotation.PostConstruct;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hyperic.hq.authz.server.session.UserPreferencesUpdatedEvent.UserZeventPayload;
import org.hyperic.hq.authz.server.session.UserPreferencesUpdatedEvent.UserZeventSource;
import org.hyperic.hq.authz.shared.AuthzSubjectManager;
import org.hyperic.hq.zevents.Zevent;
import org.hyperic.hq.zevents.ZeventEnqueuer;
import org.hyperic.hq.zevents.ZeventListener;
import org.hyperic.util.config.ConfigResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * Dedicated listener for persisting user preferences. User prefs are stored as
 * a Crispo object, which uses Hibernate optimistic locking. So persisting user
 * prefs within a tx from the UI is vulnerable to StaleStateExceptions, since
 * multiple UI threads may be updating prefs. This thread ensures that the last
 * update wins without concurrent modification and speeds up UI operations by
 * persisting asynchronously.
 * @author jhickey
 * 
 */
@Component
public class UserPreferencesSetter implements ZeventListener<UserPreferencesUpdatedEvent> {
    private ZeventEnqueuer zEventManager;
    private AuthzSubjectManager authzSubjectManager;
    private Log log = LogFactory.getLog(UserPreferencesSetter.class);

    @Autowired
    public UserPreferencesSetter(ZeventEnqueuer zEventManager,
                                 AuthzSubjectManager authzSubjectManager) {
        this.zEventManager = zEventManager;
        this.authzSubjectManager = authzSubjectManager;
    }

    @PostConstruct
    public void subscribe() {
        Set<Class<? extends Zevent>> events = new HashSet<Class<? extends Zevent>>();
        events.add(UserPreferencesUpdatedEvent.class);
        zEventManager.addBufferedListener(events, this);
    }

    public void processEvents(List<UserPreferencesUpdatedEvent> events) {
        for (UserPreferencesUpdatedEvent event : events) {
            final Integer operatorId = ((UserZeventPayload) event.getPayload()).getOperatorId();
            final Integer subjectId = ((UserZeventSource) event.getSourceId()).getSubjectId();
            final ConfigResponse prefs = ((UserZeventPayload) event.getPayload()).getPrefs();
            try {
                authzSubjectManager.setUserPrefs(operatorId, subjectId, prefs);
            } catch (Exception e) {
                log.error("Error setting user prefs for user with id: " + subjectId, e);
            }
        }
    }

    public String toString() {
        return "UserPreferencesSetter";
    }
}
