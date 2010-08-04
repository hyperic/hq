package org.hyperic.hq.plugin.vsphere.event;

import org.hyperic.hq.plugin.vsphere.event.EventFilterBuilder;
import com.vmware.vim25.*;
import com.vmware.vim25.mo.EventManager;
import org.hyperic.hq.product.PluginException;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

import java.util.Calendar;

import static org.junit.Assert.*;
/**
 * EventFilterTest
 * ToDo add mockito
 * @author hedelson
 */
@Ignore("This test shouldn't run in automated suite until we mock out vCenter connection")
public class EventFilterTest extends BaseEventTest {

    private EventManager eventManager;

    private ManagedObjectReference rootFolder;

    @Before
    public void doBefore() throws PluginException {
        this.eventManager = vSphereUtil.getEventManager();
        assertNotNull(eventManager);

        this.rootFolder = vSphereUtil.getRootFolder().getMOR();
        assertNotNull(rootFolder);
    }

    @Test
    public void buildFilter() throws Exception {
        Calendar pendingQuery = Calendar.getInstance();
        Calendar lastQuery = Calendar.getInstance();
        lastQuery.roll(Calendar.HOUR, false);

        EventFilterSpec eventFilter = EventFilterBuilder.buildEventFilters(rootFolder, lastQuery, pendingQuery);
        assertNotNull(eventFilter);
        assertTrue(eventFilter.getType().length > 0);
        assertTrue(eventFilter.entity instanceof EventFilterSpecByEntity);
        assertEquals(eventFilter.entity.getEntity(), rootFolder);
    }

     

}
