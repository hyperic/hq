package org.hyperic.hq.plugin.vsphere.event;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import org.hyperic.hq.product.PluginException;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

import com.vmware.vim25.Event;
import com.vmware.vim25.EventFilterSpec;
import com.vmware.vim25.ManagedObjectReference; 
import com.vmware.vim25.mo.EventManager;

import java.util.Calendar;

/**
 * EventHandlerTest
 * ToDo add mockito
 *
 * @author Helena Edelson
 */
@Ignore("This test shouldn't run in automated suite until we mock out vCenter connection")
public class EventHandlerTest extends BaseEventTest {

    private EventHandler eventHandler;

    private EventManager eventManager;

    private ManagedObjectReference rootFolder;

    @Before
    public void doBefore() throws PluginException {
        this.eventHandler = new DefaultEventHandler();

        this.eventManager = vSphereUtil.getEventManager();
        assertNotNull(eventManager);

        this.rootFolder = vSphereUtil.getRootFolder().getMOR();
        assertNotNull(rootFolder);
 
    }
 
    @Test
    public void handleEvents() throws Exception {
        Calendar pendingQuery = Calendar.getInstance();
        Calendar lastQuery = Calendar.getInstance();
        lastQuery.roll(Calendar.HOUR, false);

        EventFilterSpec eventFilter = EventFilterBuilder.buildEventFilters(rootFolder, lastQuery, pendingQuery);

        Event[] events = eventManager.queryEvents(eventFilter);
        assertNotNull(events);
        assertTrue(events.length > 0);

        eventHandler.handleEvents(events);
    }
 

}