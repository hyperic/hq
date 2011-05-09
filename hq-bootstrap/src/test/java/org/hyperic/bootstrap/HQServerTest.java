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

import static org.junit.Assert.assertEquals;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
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
    private EngineController engineController;
    private EmbeddedDatabaseController embeddedDatabaseController;
    private String serverHome = "/Applications/HQ5/server-5.0.0";
    private OperatingSystem osInfo;
    private Connection connection;
    private Statement statement;
    private ResultSet resultSet;

    @Before
    public void setUp() {
        this.serverConfigurator = EasyMock.createMock(ServerConfigurator.class);
        this.engineController = EasyMock.createMock(EngineController.class);
        this.embeddedDatabaseController = EasyMock.createMock(EmbeddedDatabaseController.class);
        this.connection = EasyMock.createMock(Connection.class);
        this.statement = EasyMock.createMock(Statement.class);
        this.resultSet = EasyMock.createMock(ResultSet.class);
        this.osInfo = org.easymock.classextension.EasyMock.createMock(OperatingSystem.class);
        this.server = new HQServer(serverHome, embeddedDatabaseController, serverConfigurator,
            engineController, osInfo);
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
        EasyMock.expect(engineController.isEngineRunning()).andReturn(false);
        serverConfigurator.configure();
        EasyMock.expect(embeddedDatabaseController.shouldUse()).andReturn(true);
        EasyMock.expect(embeddedDatabaseController.startBuiltInDB()).andReturn(true);
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
        replay();
        server.start();
        verify();
    }

    @Test
    public void testStartServerAlreadyRunning() throws SigarException {
        EasyMock.expect(engineController.isEngineRunning()).andReturn(true);
        replay();
        server.start();
        verify();
    }

    @Test
    public void testStartServerUnableToTellIfRunning() throws SigarException {
        EasyMock.expect(engineController.isEngineRunning()).andThrow(new SigarException());
        replay();
        server.start();
        verify();
    }

    @Test
    public void testStartErrorConfiguring() throws Exception {
        EasyMock.expect(engineController.isEngineRunning()).andReturn(false);
        serverConfigurator.configure();
        EasyMock.expectLastCall().andThrow(new NullPointerException());
        EasyMock.expect(embeddedDatabaseController.shouldUse()).andReturn(false);

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
        replay();
        server.start();
        verify();
    }

    @Test
    public void testStartErrorStartingDB() throws Exception {
        EasyMock.expect(engineController.isEngineRunning()).andReturn(false);
        serverConfigurator.configure();
        EasyMock.expect(embeddedDatabaseController.shouldUse()).andReturn(true);
        EasyMock.expect(embeddedDatabaseController.startBuiltInDB()).andThrow(
            new NullPointerException());
        replay();
        server.start();
        verify();
    }

    @Test
    public void testStartStartDBFailed() throws Exception {
        EasyMock.expect(engineController.isEngineRunning()).andReturn(false);
        serverConfigurator.configure();
        EasyMock.expect(embeddedDatabaseController.shouldUse()).andReturn(true);
        EasyMock.expect(embeddedDatabaseController.startBuiltInDB()).andReturn(false);
        replay();
        server.start();
        verify();
    }

    @Test
    public void testStop() throws Exception {
        EasyMock.expect(engineController.stop()).andReturn(true);
        EasyMock.expect(embeddedDatabaseController.shouldUse()).andReturn(true);
        EasyMock.expect(embeddedDatabaseController.stopBuiltInDB()).andReturn(true);
        replay();
        server.stop();
        verify();
    }

    @Test
    public void testStopErrorStoppingEngine() throws Exception {
        EasyMock.expect(engineController.stop()).andThrow(new SigarException());
        replay();
        server.stop();
        verify();
    }

    @Test
    public void testStopErrorStoppingBuiltInDB() throws Exception {
        EasyMock.expect(engineController.stop()).andReturn(true);
        EasyMock.expect(embeddedDatabaseController.shouldUse()).andReturn(true);
        EasyMock.expect(embeddedDatabaseController.stopBuiltInDB()).andThrow(new SigarException());
        replay();
        server.stop();
        verify();
    }

    private void replay() {
        EasyMock.replay(engineController, serverConfigurator, embeddedDatabaseController,
            connection, statement, resultSet);
        org.easymock.classextension.EasyMock.replay(osInfo);
    }

    private void verify() {
        EasyMock.verify(engineController, serverConfigurator, embeddedDatabaseController,
            connection, statement, resultSet);
        org.easymock.classextension.EasyMock.verify(osInfo);
    }

}
