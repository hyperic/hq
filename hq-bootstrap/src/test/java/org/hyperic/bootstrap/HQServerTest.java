package org.hyperic.bootstrap;

import static org.junit.Assert.assertEquals;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import org.easymock.EasyMock;
import org.hyperic.sigar.OperatingSystem;
import org.hyperic.sigar.SigarException;
import org.junit.Before;
import org.junit.Test;
/**
 * Unit test of {@link HQServer}
 * @author jhickey
 *
 */
public class HQServerTest {

    private HQServer server;
    private ServerConfigurator serverConfigurator;
    private ProcessManager processManager;
    private EngineController engineController;
    private EmbeddedDatabaseController embeddedDatabaseController;
    private String serverHome = "/Applications/HQ5/server-5.0.0";
    private String serverPidFile = "/Applications/HQ5/server-5.0.0/logs/hq-server.pid";
    private OperatingSystem osInfo;

    @Before
    public void setUp() {
        this.serverConfigurator = EasyMock.createMock(ServerConfigurator.class);
        this.processManager = EasyMock.createMock(ProcessManager.class);
        this.engineController = EasyMock.createMock(EngineController.class);
        this.embeddedDatabaseController = EasyMock.createMock(EmbeddedDatabaseController.class);
        this.osInfo = org.easymock.classextension.EasyMock.createMock(OperatingSystem.class);
        this.server = new HQServer(serverHome, serverPidFile, processManager,
            embeddedDatabaseController, serverConfigurator, engineController, osInfo);
    }

    @Test
    public void testGetJavaOptsSunJava64() {
        Properties testProps = new Properties();
        testProps.put("server.java.opts",
            "-XX:MaxPermSize=192m -Xmx512m -Xms512m -XX:+HeapDumpOnOutOfMemoryError");
        final List<String> expectedOpts = new ArrayList<String>();
        expectedOpts.add("-XX:MaxPermSize=192m");
        expectedOpts.add("-Xmx512m");
        expectedOpts.add("-Xms512m");
        expectedOpts.add("-XX:+HeapDumpOnOutOfMemoryError");
        expectedOpts.add("-Dserver.home=" + serverHome);
        expectedOpts.add("-d64");
        EasyMock.expect(serverConfigurator.getServerProps()).andReturn(testProps);
        org.easymock.classextension.EasyMock.expect(osInfo.getName()).andReturn("SunOS");
        org.easymock.classextension.EasyMock.expect(osInfo.getArch()).andReturn(
            "64-bit something or other");
        replay();
        List<String> javaOpts = server.getJavaOpts();
        verify();
        assertEquals(expectedOpts, javaOpts);
    }

    @Test
    public void testStart() throws Exception {
        EasyMock.expect(processManager.getPidFromPidFile(serverPidFile)).andReturn(-1l);
        serverConfigurator.configure();
        EasyMock.expect(embeddedDatabaseController.shouldUse()).andReturn(true);
        EasyMock.expect(embeddedDatabaseController.startBuiltInDB()).andReturn(true);
        EasyMock.expect(
            processManager.executeProcess(EasyMock
                .aryEq(new String[] { serverHome + "/bin/ant",
                                     "--noconfig",
                                     "-q",
                                     "-Dserver.home=" + serverHome,
                                     "-logger",
                                     "org.hyperic.tools.ant.installer.InstallerLogger",
                                     "-f",
                                     serverHome + "/data/db-upgrade.xml",
                                     "upgrade" }), EasyMock.eq(serverHome), EasyMock.eq(true)))
            .andReturn(0);
        Properties testProps = new Properties();
        testProps.put("server.java.opts",
            "-XX:MaxPermSize=192m -Xmx512m -Xms512m -XX:+HeapDumpOnOutOfMemoryError");
        testProps.put("server.webapp.port", "7080");
        final List<String> expectedOpts = new ArrayList<String>();
        expectedOpts.add("-XX:MaxPermSize=192m");
        expectedOpts.add("-Xmx512m");
        expectedOpts.add("-Xms512m");
        expectedOpts.add("-XX:+HeapDumpOnOutOfMemoryError");
        expectedOpts.add("-Dserver.home=" + serverHome);
        EasyMock.expect(serverConfigurator.getServerProps()).andReturn(testProps);
        org.easymock.classextension.EasyMock.expect(osInfo.getName()).andReturn("Mac OS X");
        EasyMock.expect(engineController.start(expectedOpts)).andReturn(0);
        EasyMock.expect(serverConfigurator.getServerProps()).andReturn(testProps);
        EasyMock.expect(processManager.isPortInUse(7080, 90)).andReturn(true);
        replay();
        int exitCode = server.start();
        verify();
        assertEquals(0, exitCode);
    }

    @Test
    public void testStartServerAlreadyRunning() throws SigarException {
        EasyMock.expect(processManager.getPidFromPidFile(serverPidFile)).andReturn(1234l);
        replay();
        int exitCode = server.start();
        verify();
        assertEquals(0, exitCode);
    }

    @Test
    public void testStartServerUnableToTellIfRunning() throws SigarException {
        EasyMock.expect(processManager.getPidFromPidFile(serverPidFile)).andThrow(
            new SigarException());
        replay();
        int exitCode = server.start();
        verify();
        assertEquals(1, exitCode);
    }

    @Test
    public void testStartErrorConfiguring() throws Exception {
        EasyMock.expect(processManager.getPidFromPidFile(serverPidFile)).andReturn(-1l);
        serverConfigurator.configure();
        EasyMock.expectLastCall().andThrow(new NullPointerException());
        EasyMock.expect(embeddedDatabaseController.shouldUse()).andReturn(false);
        EasyMock.expect(
            processManager.executeProcess(EasyMock
                .aryEq(new String[] { serverHome + "/bin/ant",
                                     "--noconfig",
                                     "-q",
                                     "-Dserver.home=" + serverHome,
                                     "-logger",
                                     "org.hyperic.tools.ant.installer.InstallerLogger",
                                     "-f",
                                     serverHome + "/data/db-upgrade.xml",
                                     "upgrade" }), EasyMock.eq(serverHome), EasyMock.eq(true)))
            .andReturn(0);
        Properties testProps = new Properties();
        testProps.put("server.java.opts",
            "-XX:MaxPermSize=192m -Xmx512m -Xms512m -XX:+HeapDumpOnOutOfMemoryError");
        testProps.put("server.webapp.port", "7080");
        final List<String> expectedOpts = new ArrayList<String>();
        expectedOpts.add("-XX:MaxPermSize=192m");
        expectedOpts.add("-Xmx512m");
        expectedOpts.add("-Xms512m");
        expectedOpts.add("-XX:+HeapDumpOnOutOfMemoryError");
        expectedOpts.add("-Dserver.home=" + serverHome);
        EasyMock.expect(serverConfigurator.getServerProps()).andReturn(testProps);
        org.easymock.classextension.EasyMock.expect(osInfo.getName()).andReturn("Mac OS X");
        EasyMock.expect(engineController.start(expectedOpts)).andReturn(0);
        EasyMock.expect(serverConfigurator.getServerProps()).andReturn(testProps);
        EasyMock.expect(processManager.isPortInUse(7080, 90)).andReturn(true);
        replay();
        int exitCode = server.start();
        verify();
        assertEquals(0, exitCode);
    }

    @Test
    public void testStartErrorStartingDB() throws Exception {
        EasyMock.expect(processManager.getPidFromPidFile(serverPidFile)).andReturn(-1l);
        serverConfigurator.configure();
        EasyMock.expect(embeddedDatabaseController.shouldUse()).andReturn(true);
        EasyMock.expect(embeddedDatabaseController.startBuiltInDB()).andThrow(
            new NullPointerException());
        replay();
        int exitCode = server.start();
        verify();
        assertEquals(1, exitCode);
    }

    @Test
    public void testStartStartDBFailed() throws Exception {
        EasyMock.expect(processManager.getPidFromPidFile(serverPidFile)).andReturn(-1l);
        serverConfigurator.configure();
        EasyMock.expect(embeddedDatabaseController.shouldUse()).andReturn(true);
        EasyMock.expect(embeddedDatabaseController.startBuiltInDB()).andReturn(false);
        replay();
        int exitCode = server.start();
        verify();
        assertEquals(1, exitCode);
    }

    @Test
    public void testStartWebPortNotBound() throws Exception {
        EasyMock.expect(processManager.getPidFromPidFile(serverPidFile)).andReturn(-1l);
        serverConfigurator.configure();
        EasyMock.expect(embeddedDatabaseController.shouldUse()).andReturn(true);
        EasyMock.expect(embeddedDatabaseController.startBuiltInDB()).andReturn(true);
        EasyMock.expect(
            processManager.executeProcess(EasyMock
                .aryEq(new String[] { serverHome + "/bin/ant",
                                     "--noconfig",
                                     "-q",
                                     "-Dserver.home=" + serverHome,
                                     "-logger",
                                     "org.hyperic.tools.ant.installer.InstallerLogger",
                                     "-f",
                                     serverHome + "/data/db-upgrade.xml",
                                     "upgrade" }), EasyMock.eq(serverHome), EasyMock.eq(true)))
            .andReturn(0);
        Properties testProps = new Properties();
        testProps.put("server.java.opts",
            "-XX:MaxPermSize=192m -Xmx512m -Xms512m -XX:+HeapDumpOnOutOfMemoryError");
        testProps.put("server.webapp.port", "7080");
        final List<String> expectedOpts = new ArrayList<String>();
        expectedOpts.add("-XX:MaxPermSize=192m");
        expectedOpts.add("-Xmx512m");
        expectedOpts.add("-Xms512m");
        expectedOpts.add("-XX:+HeapDumpOnOutOfMemoryError");
        expectedOpts.add("-Dserver.home=" + serverHome);
        EasyMock.expect(serverConfigurator.getServerProps()).andReturn(testProps);
        org.easymock.classextension.EasyMock.expect(osInfo.getName()).andReturn("Mac OS X");
        EasyMock.expect(engineController.start(expectedOpts)).andReturn(0);
        EasyMock.expect(serverConfigurator.getServerProps()).andReturn(testProps);
        EasyMock.expect(processManager.isPortInUse(7080, 90)).andReturn(false);
        replay();
        int exitCode = server.start();
        verify();
        assertEquals(1, exitCode);
    }

    @Test
    public void testStartErrorDeterminingWebPortBound() throws Exception {
        EasyMock.expect(processManager.getPidFromPidFile(serverPidFile)).andReturn(-1l);
        serverConfigurator.configure();
        EasyMock.expect(embeddedDatabaseController.shouldUse()).andReturn(true);
        EasyMock.expect(embeddedDatabaseController.startBuiltInDB()).andReturn(true);
        EasyMock.expect(
            processManager.executeProcess(EasyMock
                .aryEq(new String[] { serverHome + "/bin/ant",
                                     "--noconfig",
                                     "-q",
                                     "-Dserver.home=" + serverHome,
                                     "-logger",
                                     "org.hyperic.tools.ant.installer.InstallerLogger",
                                     "-f",
                                     serverHome + "/data/db-upgrade.xml",
                                     "upgrade" }), EasyMock.eq(serverHome), EasyMock.eq(true)))
            .andReturn(0);
        Properties testProps = new Properties();
        testProps.put("server.java.opts",
            "-XX:MaxPermSize=192m -Xmx512m -Xms512m -XX:+HeapDumpOnOutOfMemoryError");
        testProps.put("server.webapp.port", "7080");
        final List<String> expectedOpts = new ArrayList<String>();
        expectedOpts.add("-XX:MaxPermSize=192m");
        expectedOpts.add("-Xmx512m");
        expectedOpts.add("-Xms512m");
        expectedOpts.add("-XX:+HeapDumpOnOutOfMemoryError");
        expectedOpts.add("-Dserver.home=" + serverHome);
        EasyMock.expect(serverConfigurator.getServerProps()).andReturn(testProps);
        org.easymock.classextension.EasyMock.expect(osInfo.getName()).andReturn("Mac OS X");
        EasyMock.expect(engineController.start(expectedOpts)).andReturn(0);
        EasyMock.expect(serverConfigurator.getServerProps()).andReturn(testProps);
        EasyMock.expect(processManager.isPortInUse(7080, 90)).andThrow(new NullPointerException());
        replay();
        int exitCode = server.start();
        verify();
        assertEquals(1, exitCode);
    }

    @Test
    public void testStop() throws SigarException, IOException {
        EasyMock.expect(engineController.stop()).andReturn(0);
        EasyMock.expect(processManager.getPidFromPidFile(serverPidFile)).andReturn(-1l);
        EasyMock.expect(embeddedDatabaseController.shouldUse()).andReturn(true);
        EasyMock.expect(embeddedDatabaseController.stopBuiltInDB()).andReturn(true);
        replay();
        int exitCode = server.stop();
        verify();
        assertEquals(0, exitCode);
    }
    
    @Test
    public void testStopErrorStoppingEngine() throws SigarException, IOException {
        EasyMock.expect(engineController.stop()).andThrow(new SigarException());
        replay();
        int exitCode = server.stop();
        verify();
        assertEquals(1, exitCode);
    }
    
    @Test
    public void testStopHaltEngine() throws SigarException, IOException {
        EasyMock.expect(engineController.stop()).andReturn(1);
        server.serverStopCheckRetries = 2;
        EasyMock.expect(processManager.getPidFromPidFile(serverPidFile)).andReturn(1234l).times(2);
        engineController.halt();
        EasyMock.expect(processManager.getPidFromPidFile(serverPidFile)).andReturn(-1l);
        EasyMock.expect(embeddedDatabaseController.shouldUse()).andReturn(true);
        EasyMock.expect(embeddedDatabaseController.stopBuiltInDB()).andReturn(true);
        replay();
        int exitCode = server.stop();
        verify();
        assertEquals(0, exitCode);
    }
    
    @Test
    public void testStopEvenHaltDoesntWork() throws SigarException, IOException {
        EasyMock.expect(engineController.stop()).andReturn(1);
        server.serverStopCheckRetries = 2;
        EasyMock.expect(processManager.getPidFromPidFile(serverPidFile)).andReturn(1234l).times(2);
        engineController.halt();
        EasyMock.expect(processManager.getPidFromPidFile(serverPidFile)).andReturn(1234l);
        replay();
        int exitCode = server.stop();
        verify();
        assertEquals(1, exitCode);
    }
    
    @Test
    public void testStopErrorDeterminingIfStopped() throws SigarException, IOException {
        EasyMock.expect(engineController.stop()).andReturn(1);
        EasyMock.expect(processManager.getPidFromPidFile(serverPidFile)).andThrow(new NullPointerException());
        replay();
        int exitCode = server.stop();
        verify();
        assertEquals(1, exitCode);
    }
    
    @Test
    public void testStopErrorStoppingBuiltInDB() throws Exception{
        EasyMock.expect(engineController.stop()).andReturn(0);
        EasyMock.expect(processManager.getPidFromPidFile(serverPidFile)).andReturn(-1l);
        EasyMock.expect(embeddedDatabaseController.shouldUse()).andReturn(true);
        EasyMock.expect(embeddedDatabaseController.stopBuiltInDB()).andThrow(new SigarException());
        replay();
        int exitCode = server.stop();
        verify();
        assertEquals(1, exitCode);
    }

    private void replay() {
        EasyMock.replay(engineController, serverConfigurator, processManager,
            embeddedDatabaseController);
        org.easymock.classextension.EasyMock.replay(osInfo);
    }

    private void verify() {
        EasyMock.verify(engineController, serverConfigurator, processManager,
            embeddedDatabaseController);
        org.easymock.classextension.EasyMock.verify(osInfo);
    }

}
