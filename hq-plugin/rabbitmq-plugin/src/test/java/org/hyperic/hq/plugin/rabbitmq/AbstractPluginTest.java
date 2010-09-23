package org.hyperic.hq.plugin.rabbitmq;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hyperic.hq.plugin.rabbitmq.product.RabbitProductPlugin;
import org.hyperic.hq.product.PluginException;
import org.hyperic.util.config.ConfigResponse;
import org.junit.After;
import org.junit.Before; 

import static org.junit.Assert.*;

import java.util.Properties;

/**
 * AbstractPluginTest 
 * @author Helena Edelson
 */
public abstract class AbstractPluginTest {

    protected final Log logger = LogFactory.getLog(this.getClass().getName());

    protected RabbitProductPlugin productPlugin = new RabbitProductPlugin();

    protected Properties properties = new Properties();
 
    @Before
    public void doBefore() throws PluginException {
        this.productPlugin.configure(createConfigResponse());
        assertNotNull(productPlugin);
        this.properties = productPlugin.getConfig().toProperties();
        assertNotNull(properties);
    }

    @After
    public void doAfter() {
        /** ToDo cleanup */
    }

    protected ConfigResponse createConfigResponse() {
        ConfigResponse pluginConfig = new ConfigResponse();
        pluginConfig.setValue("host", "localhost");
        pluginConfig.setValue("username", "guest");
        pluginConfig.setValue("password", "guest");
        
        return pluginConfig;
    }

}
