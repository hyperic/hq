package org.hyperic.hq.plugin.appha;

import org.easymock.EasyMock;
import org.hyperic.hq.plugin.appha.VCenterControlPlugin;
import org.hyperic.hq.plugin.appha.VSphereUtil;
import org.hyperic.hq.product.ControlPlugin;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.Ignore;
import org.junit.runner.RunWith;
//import org.powermock.core.classloader.annotations.PrepareForTest;
//import org.powermock.modules.junit4.PowerMockRunner;

import com.vmware.vim25.mo.EventManager;

//import static org.powermock.api.easymock.PowerMock.mockStatic;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.replay;
import static org.easymock.EasyMock.createMock;


@Ignore
//@RunWith(PowerMockRunner.class)
//@PrepareForTest(VSphereUtil.class)
public class VCenterControlPluginTest {

    VSphereUtil vim = createMock(VSphereUtil.class);
    VCenterControlPlugin vCenterControlPlugin = new VCenterControlPlugin();
    EventManager mockEventManager = createMock(EventManager.class);

    @Before
    public void setUp() {
       // mockStatic(VSphereUtil.class);
    }

    @Test
    public void test() {
        try {
            expect(VSphereUtil.getInstance(vCenterControlPlugin.getConfig())).andReturn(vim);
        }catch(Exception ex) {
            Assert.fail("Could not create instance of VSphereUtil with exception: " + ex.getMessage());
        }

        expect(vim.getEventManager()).andReturn(mockEventManager);
        //expect(mockEventManager.postEvent(eventToPost, taskInfo))
        replay(VSphereUtil.class);
        
        

        Assert.assertEquals(ControlPlugin.RESULT_SUCCESS, vCenterControlPlugin.getResult());
    }
}
