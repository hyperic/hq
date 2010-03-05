package org.hyperic.bootstrap;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertFalse;
import java.io.IOException;

import org.easymock.EasyMock;
import org.hyperic.sigar.OperatingSystem;
import org.hyperic.sigar.SigarException;
import org.junit.Before;
import org.junit.Test;

/**
 * Unit test of the {@link PostgresEmbeddedDatabaseController}
 * @author jhickey
 * 
 */
public class PostgresEmbeddedDatabaseControllerTest {

    private PostgresEmbeddedDatabaseController embeddedDBController;
    private String serverHome = "/fake/server/home";
    private ProcessManager processManager;
    private OperatingSystem osInfo;

    @Before
    public void setUp() {
        this.processManager = EasyMock.createMock(ProcessManager.class);
        this.osInfo = org.easymock.classextension.EasyMock.createMock(OperatingSystem.class);
        this.embeddedDBController = new PostgresEmbeddedDatabaseController(serverHome,
            processManager, osInfo);
    }

    @Test
    public void testStartBuiltInDB() throws Exception {
        EasyMock.expect(processManager.getPidFromPidFile(serverHome + "/hqdb/data/postmaster.pid"))
            .andReturn(-1l);
        EasyMock.expect(osInfo.getName()).andReturn("Mac OS X");
        EasyMock.expect(
            processManager.executeProcess(EasyMock.aryEq(new String[] { serverHome +
                                                                        "/bin/db-start.sh" }),
                EasyMock.eq(serverHome), EasyMock.eq(false), EasyMock
                    .eq(PostgresEmbeddedDatabaseController.DB_PROCESS_TIMEOUT))).andReturn(0);
        EasyMock.expect(processManager.isPortInUse(5432l, 10)).andReturn(true);
        replay();
        boolean dbStarted = embeddedDBController.startBuiltInDB();
        verify();
        assertTrue(dbStarted);
    }

    @Test
    public void testStartAlreadyRunning() throws SigarException, IOException {
        EasyMock.expect(processManager.getPidFromPidFile(serverHome + "/hqdb/data/postmaster.pid"))
            .andReturn(-5678l);
        replay();
        boolean dbStarted = embeddedDBController.startBuiltInDB();
        verify();
        assertTrue(dbStarted);
    }

    @Test(expected = UnsupportedOperationException.class)
    public void testStartOnWindows() throws SigarException, IOException {
        EasyMock.expect(processManager.getPidFromPidFile(serverHome + "/hqdb/data/postmaster.pid"))
            .andReturn(-1l);
        EasyMock.expect(osInfo.getName()).andReturn("Win32");
        replay();
        embeddedDBController.startBuiltInDB();
        verify();
    }

    @Test
    public void testStartPortNotBound() throws Exception {
        EasyMock.expect(processManager.getPidFromPidFile(serverHome + "/hqdb/data/postmaster.pid"))
            .andReturn(-1l);
        EasyMock.expect(osInfo.getName()).andReturn("Mac OS X");
        EasyMock.expect(
            processManager.executeProcess(EasyMock.aryEq(new String[] { serverHome +
                                                                        "/bin/db-start.sh" }),
                EasyMock.eq(serverHome), EasyMock.eq(false), EasyMock
                    .eq(PostgresEmbeddedDatabaseController.DB_PROCESS_TIMEOUT))).andReturn(0);
        EasyMock.expect(processManager.isPortInUse(5432l, 10)).andReturn(false);
        replay();
        boolean dbStarted = embeddedDBController.startBuiltInDB();
        verify();
        assertFalse(dbStarted);
    }

    @Test
    public void testStartErrorDeterminingIfPortBound() throws Exception {
        EasyMock.expect(processManager.getPidFromPidFile(serverHome + "/hqdb/data/postmaster.pid"))
            .andReturn(-1l);
        EasyMock.expect(osInfo.getName()).andReturn("Mac OS X");
        EasyMock.expect(
            processManager.executeProcess(EasyMock.aryEq(new String[] { serverHome +
                                                                        "/bin/db-start.sh" }),
                EasyMock.eq(serverHome), EasyMock.eq(false), EasyMock
                    .eq(PostgresEmbeddedDatabaseController.DB_PROCESS_TIMEOUT))).andReturn(0);
        EasyMock.expect(processManager.isPortInUse(5432l, 10)).andThrow(new NullPointerException());
        replay();
        boolean dbStarted = embeddedDBController.startBuiltInDB();
        verify();
        assertFalse(dbStarted);
    }

    @Test
    public void testStopBuiltInDB() throws Exception {
        EasyMock.expect(processManager.getPidFromPidFile(serverHome + "/hqdb/data/postmaster.pid"))
            .andReturn(123l);
        EasyMock.expect(osInfo.getName()).andReturn("Mac OS X");
        EasyMock.expect(
            processManager.executeProcess(EasyMock.aryEq(new String[] { serverHome +
                                                                        "/bin/db-stop.sh" }),
                EasyMock.eq(serverHome), EasyMock.eq(false), EasyMock
                    .eq(PostgresEmbeddedDatabaseController.DB_PROCESS_TIMEOUT))).andReturn(0);
        EasyMock.expect(processManager.isPortInUse(5432l, 1)).andReturn(false);
        replay();
        boolean dbStopped = embeddedDBController.stopBuiltInDB();
        verify();
        assertTrue(dbStopped);
    }

    @Test
    public void testStopDBNotRunning() throws Exception {
        EasyMock.expect(processManager.getPidFromPidFile(serverHome + "/hqdb/data/postmaster.pid"))
            .andReturn(-1l);
        replay();
        boolean dbStopped = embeddedDBController.stopBuiltInDB();
        verify();
        assertTrue(dbStopped);
    }

    @Test(expected = UnsupportedOperationException.class)
    public void testStopOnWindows() throws SigarException, IOException {
        EasyMock.expect(processManager.getPidFromPidFile(serverHome + "/hqdb/data/postmaster.pid"))
            .andReturn(1l);
        EasyMock.expect(osInfo.getName()).andReturn("Win32");
        replay();
        embeddedDBController.stopBuiltInDB();
        verify();
    }

    @Test
    public void testStopPortStillUp() throws Exception {
        EasyMock.expect(processManager.getPidFromPidFile(serverHome + "/hqdb/data/postmaster.pid"))
            .andReturn(123l);
        EasyMock.expect(osInfo.getName()).andReturn("Mac OS X");
        EasyMock.expect(
            processManager.executeProcess(EasyMock.aryEq(new String[] { serverHome +
                                                                        "/bin/db-stop.sh" }),
                EasyMock.eq(serverHome), EasyMock.eq(false), EasyMock
                    .eq(PostgresEmbeddedDatabaseController.DB_PROCESS_TIMEOUT))).andReturn(0);
        EasyMock.expect(processManager.isPortInUse(5432l, 1)).andReturn(true);
        EasyMock.expect(processManager.isPortInUse(5432l, 1)).andReturn(true);
        replay();
        embeddedDBController.dbPortStopCheckTries = 2;
        boolean dbStopped = embeddedDBController.stopBuiltInDB();
        verify();
        assertFalse(dbStopped);
    }

    @Test
    public void testStopErrorDeterminingIfStopped() throws Exception {
        EasyMock.expect(processManager.getPidFromPidFile(serverHome + "/hqdb/data/postmaster.pid"))
            .andReturn(123l);
        EasyMock.expect(osInfo.getName()).andReturn("Mac OS X");
        EasyMock.expect(
            processManager.executeProcess(EasyMock.aryEq(new String[] { serverHome +
                                                                        "/bin/db-stop.sh" }),
                EasyMock.eq(serverHome), EasyMock.eq(false), EasyMock
                    .eq(PostgresEmbeddedDatabaseController.DB_PROCESS_TIMEOUT))).andReturn(0);
        EasyMock.expect(processManager.isPortInUse(5432l, 1)).andThrow(new NullPointerException());
        replay();
        boolean dbStopped = embeddedDBController.stopBuiltInDB();
        verify();
        assertFalse(dbStopped);
    }

    @Test
    public void testShouldUseNoDBFile() {
        assertFalse(embeddedDBController.shouldUse());
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
