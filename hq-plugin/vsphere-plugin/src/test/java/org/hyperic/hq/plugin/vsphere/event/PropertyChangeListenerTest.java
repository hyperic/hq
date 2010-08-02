package org.hyperic.hq.plugin.vsphere.event;

import static org.junit.Assert.assertNotNull;

import org.hyperic.hq.plugin.vsphere.event.DefaultEventHandler;
import org.hyperic.hq.plugin.vsphere.event.PropertyChangeListener;
import org.hyperic.hq.product.PluginException;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

import com.vmware.vim25.ManagedObjectReference;
import com.vmware.vim25.mo.Folder;
import com.vmware.vim25.mo.PropertyCollector;
import com.vmware.vim25.mo.ViewManager;

/**
 * EventInvokerTest
 *
 * @author Helena Edelson
 */
@Ignore("Investigating.")
public class PropertyChangeListenerTest extends BaseEventTest {

    private Folder rootFolder;

    private ViewManager viewManager;

    private PropertyCollector pc;

    @Before
    public void doBefore(){
        this.pc = vSphereUtil.getPropertyCollector();
        assertNotNull(pc); 
        this.rootFolder = vSphereUtil.getRootFolder();
        assertNotNull(rootFolder);
        this.viewManager = vSphereUtil.getViewManager();
        assertNotNull(viewManager);
    }
 
    @Test
    public void waitForUpdates() throws Exception {
        final PropertyChangeListener invoker = new PropertyChangeListener();
        invoker.invoke(pc, rootFolder, viewManager, new DefaultEventHandler());
    }

    @Test
    public void propertyCollector() throws PluginException {
        ManagedObjectReference mof = vSphereUtil.getServiceContent().getPropertyCollector();
        PropertyCollector pc = vSphereUtil.getPropertyCollector();

        logger.debug("vSphereUtil.getPropertyCollector type/value=" + pc.getMOR().getType() + ", " + pc.getMOR().getVal());
        logger.debug("serviceContent.getPropertyCollector type/value=" + mof.getType() + ", " + mof.getVal());
    }
}
