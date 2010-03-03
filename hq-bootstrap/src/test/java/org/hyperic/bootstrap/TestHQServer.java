package org.hyperic.bootstrap;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import org.easymock.EasyMock;
import org.hyperic.bootstrap.EngineController;
import org.hyperic.bootstrap.HQServer;
import org.hyperic.bootstrap.ServerConfigurator;
import org.hyperic.sigar.OperatingSystem;
import org.hyperic.sigar.Sigar;
import org.hyperic.sigar.SigarException;
import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertEquals;

public class TestHQServer {

    private HQServer server;
    private ServerConfigurator serverConfigurator;
    
    @Before
    public void setUp() {
        this.serverConfigurator = EasyMock.createMock(ServerConfigurator.class);
        this.server = new HQServer("/Applications/Evolution/server-5.0.0-EE",
            "/Applications/Evolution/server-5.0.0-EE/logs/hq-server.pid", new ProcessManager(new Sigar()), serverConfigurator, EasyMock.createMock(EngineController.class),
        OperatingSystem.getInstance());
    }
    
    public void testIsServerAlreadyRunning() throws SigarException {
        //test when already running and not, also not running but with stale pid file
        assertFalse(server.isServerAlreadyRunning());
    }
    
   
    public void testStartBuiltInDB() throws SigarException, IOException {
        //DB already running, not running, stale pid file, can't find port after starting, can't get port from conf file, can't find conf file, no built-in DB
        server.startBuiltInDB();
    }
   
    public void testUpgradeDB() {
        server.upgradeDB();
    }
    
    @Test
    public void testGetJavaOpts() {
        Properties testProps = new Properties();
        testProps.put("server.java.opts","-XX:MaxPermSize=192m -Xmx512m -Xms512m -XX:+HeapDumpOnOutOfMemoryError");
        final List<String> expectedOpts = new ArrayList<String>();
        expectedOpts.add("-XX:MaxPermSize=192m");
        expectedOpts.add("-Xmx512m");
        expectedOpts.add("-Xms512m");
        expectedOpts.add("-XX:+HeapDumpOnOutOfMemoryError");
        expectedOpts.add("-Dserver.home=/Applications/Evolution/server-5.0.0-EE");
        EasyMock.expect(serverConfigurator.getServerProps()).andReturn(testProps);
        EasyMock.replay(serverConfigurator);
        List<String> javaOpts = server.getJavaOpts();
        EasyMock.verify(serverConfigurator);
        assertEquals(expectedOpts, javaOpts);
        //TODO test adding flag for Sun 64-bit arch by mocking SystemInfo
    }
    
    
   
}
