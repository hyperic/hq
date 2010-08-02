package org.hyperic.hq.plugin.vsphere.event;

import static org.junit.Assert.assertNotNull;

import org.hyperic.hq.plugin.vsphere.events.EventHandlerImpl;
import org.hyperic.hq.plugin.vsphere.events.EventInvoker;
import org.hyperic.hq.plugin.vsphere.events.EventInvokerImpl;
import org.junit.Ignore;
import org.junit.Test;

import com.vmware.vim25.mo.PropertyCollector;

/**
 * EventInvokerTest
 * ToDo Add easymock or mockito
 *
 * @author Helena Edelson
 */
@Ignore
public class EventInvokerTest extends BaseEventTest {

    @Test
    public void invoke() throws Exception {
        PropertyCollector pc = vSphereUtil.getPropertyCollector();
        assertNotNull(pc);

        EventInvoker invoker = new EventInvokerImpl();
        Object o = invoker.invoke(pc, new EventHandlerImpl(), 10);

        /* not completed yet */
    }

}
