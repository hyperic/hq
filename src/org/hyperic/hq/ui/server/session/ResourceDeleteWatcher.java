package org.hyperic.hq.ui.server.session;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hyperic.hq.zevents.ZeventListener;
import org.hyperic.hq.appdef.server.session.ResourceDeletedZevent;
import org.hyperic.hq.ui.shared.DashboardManagerLocal;

import java.util.Set;
import java.util.HashSet;
import java.util.List;

public class ResourceDeleteWatcher implements ZeventListener {

    private Set _opts = new HashSet(); //The set of user prefs to watch
    private static final Object OBJLOCK = new Object();

    private static final String[] DEFAULT_OPTS = {
        ".userPref.recent.resources"
    };

    private static Log _log = LogFactory .getLog(ResourceDeleteWatcher.class);

    private static final ResourceDeleteWatcher _instance =
        new ResourceDeleteWatcher();

    private ResourceDeleteWatcher() {
        for (int i = 0; i < DEFAULT_OPTS.length; i++) {
            _opts.add(DEFAULT_OPTS[i]);
        }
    }

    void addPreference(String pref) {
        synchronized(OBJLOCK) {
            _opts.add(pref);
        }
    }

    void removePreference(String pref) {
        synchronized(OBJLOCK) {
            _opts.remove(pref);
        }
    }

    static ResourceDeleteWatcher getInstance() {
        return _instance;
    }

    public void processEvents(List events) {

        String[] ids = new String[events.size()];
        for (int i = 0; i < events.size(); i++ ) {
            ResourceDeletedZevent e = (ResourceDeletedZevent)events.get(i);
            ids[i] = e.getAppdefEntityID().getAppdefKey();
        }

        DashboardManagerLocal dm = DashboardManagerEJBImpl.getOne();
        dm.handleResourceDelete(_opts, ids);
    }
}
