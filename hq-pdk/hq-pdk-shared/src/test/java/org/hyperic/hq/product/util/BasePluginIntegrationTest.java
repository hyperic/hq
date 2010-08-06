package org.hyperic.hq.product.util;

import java.io.File;
import java.io.IOException;
import java.util.Properties;

import org.apache.tools.ant.Project;
import org.apache.tools.ant.taskdefs.Jar;
import org.hyperic.hq.product.AutoinventoryPluginManager;
import org.hyperic.hq.product.ConfigTrackPluginManager;
import org.hyperic.hq.product.ControlPluginManager;
import org.hyperic.hq.product.LiveDataPluginManager;
import org.hyperic.hq.product.LogTrackPluginManager;
import org.hyperic.hq.product.MeasurementPluginManager;
import org.hyperic.hq.product.PluginException;
import org.hyperic.hq.product.ProductPluginManager;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.springframework.core.io.DefaultResourceLoader;

/**
 * Base class for driving local tests of plugins. Takes care of setting sigar
 * home (assuming you have unpacked sigar-libs into
 * target/generated-test-resources), creates a tmp plugin jar from your plugin
 * source code, and registers that plugin jar with the PluginManagers, thus
 * fully creating your plugin as would be done in the agent (including
 * initialization of PluginData from the plugin XML)
 * @author jhickey
 * 
 */
abstract public class BasePluginIntegrationTest {

    protected ProductPluginManager productPluginManager;
    protected AutoinventoryPluginManager autoInventoryPluginManager;
    protected MeasurementPluginManager measurementPluginManager;
    protected ControlPluginManager controlPluginManager;
    protected LiveDataPluginManager liveDataPluginManager;
    protected ConfigTrackPluginManager configTrackPluginManager;
    protected LogTrackPluginManager logTrackPluginManager;
    protected static File pluginJar;
    static final String HQ_IP = "agent.setup.camIP";
    static final String HQ_PORT = "agent.setup.camPort";
    static final String HQ_USER = "agent.setup.camLogin";
    static final String HQ_PASS = "agent.setup.camPword";

    @Before
    public void registerPlugin() throws PluginException {
        Properties agentProps = new Properties();
        agentProps.put(HQ_IP, "127.0.0.1");
        agentProps.put(HQ_PORT, "8080");
        agentProps.put(HQ_USER,"hqadmin");
        agentProps.put(HQ_PASS, "hqadmin");
        this.productPluginManager = new ProductPluginManager(agentProps);
        this.productPluginManager.setRegisterTypes(false);
        this.productPluginManager.init();
        this.autoInventoryPluginManager = this.productPluginManager.getAutoinventoryPluginManager();
        this.measurementPluginManager = this.productPluginManager.getMeasurementPluginManager();
        this.controlPluginManager = this.productPluginManager.getControlPluginManager();
        this.liveDataPluginManager = this.productPluginManager.getLiveDataPluginManager();
        this.configTrackPluginManager = this.productPluginManager.getConfigTrackPluginManager();
        this.logTrackPluginManager = this.productPluginManager.getLogTrackPluginManager();
        productPluginManager.registerPluginJar(pluginJar.getAbsolutePath());
    }

    /**
     * Creates a plugin jar in the tmp dir containing everything in
     * target/classes and using the project name to name the jar
     * @throws IOException
     */
    @BeforeClass
    public static void createPluginJar() throws IOException {
        File testClassesDir = new DefaultResourceLoader().getResource("classpath:/").getFile();
        String pluginName = new File(new File(testClassesDir.getParent()).getParent()).getName();
        pluginJar = new File(System.getProperty("java.io.tmpdir") + "/" + pluginName + ".jar");
        Jar jarTask = new Jar();
        jarTask.setDestFile(pluginJar);
        jarTask.setBasedir(new File(testClassesDir.getParent(), "classes"));
        jarTask.setProject(new Project());
        jarTask.execute();
    }

    /**
     * Registers the sigar path as the location in the classpath to which the
     * sigar libs have been unpacked
     * @throws IOException
     */
    @BeforeClass
    public static void registerSigarLib() throws IOException {
        File sigarBin = new File(new DefaultResourceLoader().getResource(
            "classpath:/libsigar-sparc64-solaris.so").getFile().getParent());
        System.setProperty("org.hyperic.sigar.path", sigarBin.getAbsolutePath());
    }

    @AfterClass
    public static void deletePluginJar() {
        if (pluginJar != null) {
            pluginJar.delete();
        }
    }

}
