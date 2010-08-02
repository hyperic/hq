package org.hyperic.hq.plugin.vsphere.event;


import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import org.hyperic.hq.plugin.vsphere.event.DefaultEventHandler;
import org.hyperic.hq.plugin.vsphere.event.EventHandler;
import org.hyperic.hq.plugin.vsphere.event.EventService;
import org.hyperic.hq.plugin.vsphere.event.EventServiceImpl;
import org.hyperic.hq.product.PluginException;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.springframework.core.task.SimpleAsyncTaskExecutor;
import org.springframework.core.task.TaskExecutor;
import org.springframework.util.ClassUtils;

import com.vmware.vim25.Event;
import com.vmware.vim25.ManagedObjectReference;
import com.vmware.vim25.ServiceContent;
import com.vmware.vim25.VimPortType;
import com.vmware.vim25.mo.EventManager;
import com.vmware.vim25.mo.ServerConnection;

/**
 * EventServiceTest
 * TToDo add mockito
 *
 * @author Helena Edelson
 */
@Ignore("This test shouldn't run in automated suite until we mock out vCenter connection")
public class EventServiceTest extends BaseEventTest {

    private TaskExecutor taskExecutor;

    private EventManager eventManager;

    private EventService eventService;

     /** Default thread name prefix: "EventServiceImpl-". */
	public static final String DEFAULT_THREAD_NAME_PREFIX = ClassUtils.getShortName(EventServiceImpl.class) + "-";

    protected TaskExecutor createDefaultTaskExecutor() {
		return new SimpleAsyncTaskExecutor(DEFAULT_THREAD_NAME_PREFIX);
	}

    @Before
    public void doBefore(){
        /*this.scheduler = Executors.newScheduledThreadPool(1);
        this.taskScheduler = new ConcurrentTaskScheduler(scheduler);*/

        this.eventManager = vSphereUtil.getEventManager();
        assertNotNull(eventManager);
        this.eventService = new EventServiceImpl();
    }

    /** Really we need a scheduler */
    @Test
    public void asyncExecuteQueries() throws Exception {
        final EventManager eventManager = vSphereUtil.getEventManager();
        assertNotNull(eventManager);
        final ManagedObjectReference rootFolder = vSphereUtil.getRootFolder().getMOR();
        assertNotNull(rootFolder);
        final EventService eventService = new EventServiceImpl();
        final EventHandler eventHandler = new DefaultEventHandler();

        this.taskExecutor = createDefaultTaskExecutor();

        try {

            taskExecutor.execute(new Runnable() {
                public void run() {
                    try {
                        Event[] events = eventService.queryEvents(eventManager, rootFolder);
                        eventHandler.handleEvents(events);
                    } catch (Exception e) {
                        logger.error("", e);
                    }
                }
            });

            Thread.sleep(1000);
        }
        catch (Exception e) {
            logger.error(e);
        } 
    }
    @Test
    public void queryEvents() throws Exception {
        ManagedObjectReference rootFolder = vSphereUtil.getRootFolder().getMOR();
        assertNotNull(rootFolder);

        Event[] events = eventService.queryEvents(eventManager, rootFolder);
        assertNotNull(events);
        assertTrue(events.length > 0);
        new DefaultEventHandler().handleEvents(events);
    }

    @Test
    public void queryHistoricalEvents() throws Exception {
        ManagedObjectReference rootFolder = vSphereUtil.getRootFolder().getMOR();
        assertNotNull(rootFolder);

        Event[] events = eventService.queryHistoricalEvents(eventManager, rootFolder);
        assertNotNull(events);
        assertTrue(events.length > 0);
        new DefaultEventHandler().handleEvents(events);
    }

    @Test
    public void getLatestEvent(){
        Event event = eventService.getLatestEvent(eventManager);
        assertNotNull(event); 
    }
 
    @Test
    public void compare() throws PluginException {
        ServiceContent serviceContent = vSphereUtil.getServiceContent();
        logger.debug("Testing info---" + serviceContent.getAbout().getFullName());
 
        ServerConnection conn = vSphereUtil.getServerConnection();
        ServerConnection c = eventManager.getServerConnection();
        assertEquals(conn, c);

        ManagedObjectReference rootFolder = vSphereUtil.getRootFolder().getMOR();
        assertEquals(serviceContent.getRootFolder(), rootFolder);

        VimPortType service = vSphereUtil.getServerConnection().getVimService();
        assertTrue(service.getWsc().getBaseUrl().toString().equalsIgnoreCase(eventManager.getServerConnection().getUrl().toString())); 
    }

    
}
