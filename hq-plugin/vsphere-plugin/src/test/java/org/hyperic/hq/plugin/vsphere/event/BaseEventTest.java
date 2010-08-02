package org.hyperic.hq.plugin.vsphere.event;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hyperic.hq.plugin.vsphere.VSphereCollector;
import org.hyperic.hq.plugin.vsphere.VSphereUtil;
import org.hyperic.hq.product.PluginException;
import org.hyperic.util.config.ConfigResponse;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;

/**
 * BaseEventTest
 *
 * @author Helena Edelson
 */

abstract public class BaseEventTest {

    protected final Log logger = LogFactory.getLog(this.getClass().getName());

    protected VSphereUtil vSphereUtil;


    @Before
    public void before() throws PluginException {
        /* Depending on which env we want to connect to */
        this.vSphereUtil = VSphereUtil.getInstance(configResponse("https://localhost/sdk", "administrator", "password123", "resgroup-v327"));
        Assert.assertNotNull(vSphereUtil);
    }

    @After
    public void after() {
        if (this.vSphereUtil != null) {
            VSphereUtil.dispose(this.vSphereUtil);
            this.vSphereUtil = null;
        }
    }

    private ConfigResponse configResponse(String url, String user, String pass, String morId) {
        final String MOR = "mor";

        ConfigResponse pluginConfig = new ConfigResponse();
        pluginConfig.setValue(VSphereCollector.PROP_URL, url);
        pluginConfig.setValue(VSphereCollector.PROP_USERNAME, user);
        pluginConfig.setValue(VSphereCollector.PROP_PASSWORD, pass);
        pluginConfig.setValue(MOR, morId);

        return pluginConfig;
    }
}
