package org.hyperic.hq.plugin.vsphere.event;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hyperic.hq.plugin.vsphere.VSphereCollector;
import org.hyperic.hq.plugin.vsphere.VSphereUtil;
import org.hyperic.hq.product.PluginException;
import org.hyperic.util.config.ConfigResponse;
import org.junit.After;
import static org.junit.Assert.*;
import org.junit.Before;

/**
 * BaseEventTest
 *
 * @author Helena Edelson
 */ 
abstract public class BaseEventTest {

    protected final Log logger = LogFactory.getLog(this.getClass().getName());

    protected VSphereUtil vSphereUtil;

    /* ToDo https://localhost/sdk administrator password123 */
    private final String MOR = "mor";
     
    private final String URL = "https://vmc-ssrc-2k328.eng.vmware.com/sdk";

    private final String USER = "administrator";

    private final String PASS = "ca$hc0w";

    private final String MOR_ID = "resgroup-v327";

    @Before
    public void before() throws PluginException { 
        this.vSphereUtil = VSphereUtil.getInstance(configResponse());
        assertNotNull(vSphereUtil);
    }

    @After
    public void after() {
        if (this.vSphereUtil != null) {
            VSphereUtil.dispose(this.vSphereUtil);
            this.vSphereUtil = null;
        }
    }

    private ConfigResponse configResponse() {
        ConfigResponse pluginConfig = new ConfigResponse();
        pluginConfig.setValue(VSphereCollector.PROP_URL, URL);
        pluginConfig.setValue(VSphereCollector.PROP_USERNAME, USER);
        pluginConfig.setValue(VSphereCollector.PROP_PASSWORD, PASS);
        pluginConfig.setValue(MOR, MOR_ID);

        return pluginConfig;
    }
}
