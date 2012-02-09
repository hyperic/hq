/**
 * NOTE: This copyright does *not* cover user programs that use HQ
 * program services by normal system calls through the application
 * program interfaces provided as part of the Hyperic Plug-in Development
 * Kit or the Hyperic Client Development Kit - this is merely considered
 * normal use of the program, and does *not* fall under the heading of
 *  "derived work".
 *
 *  Copyright (C) [2010], VMware, Inc.
 *  This file is part of HQ.
 *
 *  HQ is free software; you can redistribute it and/or modify
 *  it under the terms version 2 of the GNU General Public License as
 *  published by the Free Software Foundation. This program is distributed
 *  in the hope that it will be useful, but WITHOUT ANY WARRANTY; without
 *  even the implied warranty of MERCHANTABILITY or FITNESS FOR A
 *  PARTICULAR PURPOSE. See the GNU General Public License for more
 *  details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program; if not, write to the Free Software
 *  Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307
 *  USA.
 *
 */

package org.hyperic.bootstrap;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

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
    private final String serverHome = "/fake/server/home";
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

    @Test
    public void testStartOnWindows() throws Exception {
        EasyMock.expect(processManager.getPidFromPidFile(serverHome + "/hqdb/data/postmaster.pid"))
            .andReturn(-1l);
        EasyMock.expect(osInfo.getName()).andReturn("Win32");
        EasyMock.expect(
            processManager.executeProcess(EasyMock.aryEq(new String[] { serverHome +
                                                                        "/bin/db-start.bat" }),
                EasyMock.eq(serverHome), EasyMock.eq(false), EasyMock
                    .eq(PostgresEmbeddedDatabaseController.DB_PROCESS_TIMEOUT))).andReturn(0);
        EasyMock.expect(processManager.isPortInUse(5432l, 10)).andReturn(true);
        replay();
        boolean dbStarted = embeddedDBController.startBuiltInDB();
        verify();
        assertTrue(dbStarted);
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

    @Test
    public void testStopOnWindows() throws Exception {
        EasyMock.expect(processManager.getPidFromPidFile(serverHome + "/hqdb/data/postmaster.pid"))
            .andReturn(1l);
        EasyMock.expect(osInfo.getName()).andReturn("Win32");
        EasyMock.expect(
            processManager.executeProcess(EasyMock.aryEq(new String[] { serverHome +
                                                                        "/bin/db-stop.bat" }),
                EasyMock.eq(serverHome), EasyMock.eq(false), EasyMock
                    .eq(PostgresEmbeddedDatabaseController.DB_PROCESS_TIMEOUT))).andReturn(0);
        replay();
        boolean dbStopped = embeddedDBController.stopBuiltInDB();
        verify();
        assertTrue(dbStopped);
    }

    /*
     * As the stopBuiltinDb() no longer checks the stop operation success the method is obsolete
     */
    //@Test
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

    /*
     * As the stopBuiltinDb() no longer checks the stop operation success the method is obsolete  
     */
    //@Test
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
