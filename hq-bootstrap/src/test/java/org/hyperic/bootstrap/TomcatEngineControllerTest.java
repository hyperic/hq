package org.hyperic.bootstrap;

import java.util.ArrayList;
import java.util.List;

import org.easymock.EasyMock;
import org.hyperic.sigar.OperatingSystem;
import org.hyperic.sigar.SigarException;
import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.assertEquals;
/**
 * Unit test of the {@link TomcatEngineController}
 * @author jhickey
 *
 */
public class TomcatEngineControllerTest {

    private ProcessManager processManager;
    private String serverHome = "/Applications/Evolution/server-5.0.0-EE";
    private String engineHome = "/Applications/Evolution/server-5.0.0-EE/hq-engine";
    private String serverPidFile = "/Applications/Evolution/server-5.0.0-EE/logs/hq-server.pid";
    private OperatingSystem osInfo;
    private TomcatEngineController tomcatEngineController;

    @Before
    public void setUp() {
        this.processManager = EasyMock.createMock(ProcessManager.class);
        this.osInfo = org.easymock.classextension.EasyMock.createMock(OperatingSystem.class);
        this.tomcatEngineController = new TomcatEngineController(processManager, engineHome,
            serverHome, serverPidFile, osInfo);
    }

    @Test
    public void testStart() {
        final List<String> expectedOpts = new ArrayList<String>();
        expectedOpts.add("-XX:MaxPermSize=192m");
        expectedOpts.add("-Xmx512m");
        expectedOpts.add("-Xms512m");
        expectedOpts.add("-XX:+HeapDumpOnOutOfMemoryError");
        expectedOpts.add("-Dserver.home=" + serverHome);
        EasyMock.expect(osInfo.getName()).andReturn("Mac OS X");
        EasyMock
            .expect(
                processManager
                    .executeProcess(
                        EasyMock.aryEq(new String[] { engineHome + "/hq-server/bin/startup.sh" }),
                        EasyMock.eq(serverHome),
                        EasyMock.aryEq(new String[] { "JAVA_OPTS=",
                                      "CATALINA_OPTS=-XX:MaxPermSize=192m -Xmx512m -Xms512m -XX:+HeapDumpOnOutOfMemoryError -Dserver.home=" +
                                          serverHome +
                                          " -Dcatalina.config=file://" +
                                          engineHome +
                                          "/hq-server/conf/hq-catalina.properties -Dcom.sun.management.jmxremote",
                                      "CATALINA_PID=" + serverPidFile }), EasyMock.eq(true))).andReturn(0);
        replay();
        int exitCode = tomcatEngineController.start(expectedOpts);
        verify();
        assertEquals(0,exitCode);
    }
    
    @Test(expected=UnsupportedOperationException.class)
    public void testStartOnWindows() {
        EasyMock.expect(osInfo.getName()).andReturn("Win32");
        replay();
        tomcatEngineController.start(new ArrayList<String>());
        verify();
    }
    
    @Test
    public void testStop() throws SigarException {
        EasyMock.expect(processManager.getPidFromPidFile(serverPidFile)).andReturn(123l);
        processManager.kill(123l);
        replay();
        int exitCode = tomcatEngineController.stop();
        verify();
        assertEquals(0,exitCode);
    }
    
    @Test
    public void testStopNotRunning() throws SigarException {
        EasyMock.expect(processManager.getPidFromPidFile(serverPidFile)).andReturn(-1l);
        replay();
        int exitCode = tomcatEngineController.stop();
        verify();
        assertEquals(0,exitCode);
    }
    
    @Test
    public void testHalt() throws SigarException {
        EasyMock.expect(osInfo.getName()).andReturn("Mac OS X");
        EasyMock.expect( processManager.executeProcess(EasyMock.aryEq(new String[] { engineHome + "/hq-server/bin/shutdown.sh",
                                                        "-force" }), EasyMock.eq(serverHome),
                EasyMock.aryEq(new String[] { "CATALINA_PID=" + serverPidFile }), EasyMock.eq(true))).andReturn(0);
        replay();
        tomcatEngineController.halt();
        verify();
    }
    
    @Test(expected=UnsupportedOperationException.class)
    public void testHaltOnWindows() throws SigarException {
        EasyMock.expect(osInfo.getName()).andReturn("Win32");
        replay();
        tomcatEngineController.halt();
        verify();
    }

    private void replay() {
        EasyMock.replay(processManager);
        org.easymock.classextension.EasyMock.replay(osInfo);
    }

    private void verify() {
        EasyMock.verify(processManager);
        org.easymock.classextension.EasyMock.verify(osInfo);
    }
}
