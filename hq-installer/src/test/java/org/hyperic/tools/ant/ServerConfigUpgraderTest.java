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

package org.hyperic.tools.ant;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

import org.apache.tools.ant.BuildException;
import org.jasypt.properties.PropertyValueEncryptionUtils;
import org.junit.Test;

/**
 * Unit test of {@link ServerConfigUpgrader}
 * @author jhickey
 * 
 */
public class ServerConfigUpgraderTest {

    /**
     * Verifies that username/pw are successfully extracted from the default
     * JBoss mail service config
     */
    @Test
    public void testParseDefaultMailConfig() {
        ServerConfigUpgrader upgrader = new ServerConfigUpgrader();
        InputStream input = ServerConfigUpgrader.class.getClassLoader().getResourceAsStream(
            "default-mail-config.xml");
        Properties serverConfig = new Properties();
        Properties expectedConfig = new Properties();
        expectedConfig.put("mail.user", "EAM Application");
        expectedConfig.put("mail.password", "password");
        upgrader.parseMailConfig(input, serverConfig);
        assertEquals(expectedConfig, serverConfig);
    }

    /**
     * Verifies that all possibly changed SMTP props (if configuring secure SMTP
     * as we advertise on the wiki) are exported from JBoss mail service config
     * properly
     */
    @Test
    public void testParseMailConfigWithTLSAuth() {
        ServerConfigUpgrader upgrader = new ServerConfigUpgrader();
        InputStream input = ServerConfigUpgrader.class.getClassLoader().getResourceAsStream(
            "auth-tls-mail-config.xml");
        Properties serverConfig = new Properties();
        Properties expectedConfig = new Properties();
        expectedConfig.put("mail.user", "jen");
        expectedConfig.put("mail.password", "password");
        expectedConfig.put("mail.smtp.auth", "true");
        expectedConfig.put("mail.smtp.port", "587");
        expectedConfig.put("mail.smtp.starttls.enable", "true");
        expectedConfig.put("mail.smtp.socketFactory.port", "465");
        expectedConfig.put("mail.smtp.socketFactory.fallback", "false");
        expectedConfig.put("mail.smtp.socketFactory.class", "javax.net.ssl.SSLSocketFactory");
        upgrader.parseMailConfig(input, serverConfig);
        assertEquals(expectedConfig, serverConfig);
    }

    /**
     * Verifies that the proper validation query is added to props on upgrade
     * @throws IOException
     */
    @Test
    public void testUpgradeConfigNonOracleDB() throws IOException {
        ServerConfigUpgrader upgrader = new ServerConfigUpgrader();
        InputStream expectedInput = ServerConfigUpgrader.class.getClassLoader()
            .getResourceAsStream("hq-server-to-upgrade.conf");
        Properties expectedConfig = new Properties();
        expectedConfig.load(expectedInput);
        expectedConfig.remove("hq-engine.jnp.port");
        expectedConfig.remove("hq-engine.server.port");
        expectedInput.close();

        InputStream input = ServerConfigUpgrader.class.getClassLoader().getResourceAsStream(
            "hq-server-to-upgrade.conf");
        expectedConfig.put("server.connection-validation-sql", "select 1");
        expectedConfig.put("server.hibernate.dialect",
            "org.hyperic.hibernate.dialect.MySQL5InnoDBDialect");
        expectedConfig.put("server.encryption-key", "defaultkey");
        expectedConfig.put("tomcat.maxthreads", "500");
        expectedConfig.put("tomcat.minsparethreads", "50");
        expectedConfig.put("server.jms.usejmx", "false");
        expectedConfig.put("server.jms.jmxport", "1099");
        expectedConfig.remove("server.database-password");

        Properties serverConfig = upgrader.upgradeServerConfig(input);
        String encryptedPw = (String) serverConfig.remove("server.database-password");
        assertTrue(PropertyValueEncryptionUtils.isEncryptedValue(encryptedPw));
        assertEquals(expectedConfig, serverConfig);
    }

    /**
     * Verifies that the proper validation query is added to props on upgrade
     * @throws IOException
     */
    @Test
    public void testUpgradeConfigOracleDB() throws IOException {
        ServerConfigUpgrader upgrader = new ServerConfigUpgrader();
        InputStream expectedInput = ServerConfigUpgrader.class.getClassLoader()
            .getResourceAsStream("hq-oracle-server-to-upgrade.conf");
        Properties expectedConfig = new Properties();
        expectedConfig.load(expectedInput);
        expectedConfig.remove("hq-engine.jnp.port");
        expectedConfig.remove("hq-engine.server.port");
        expectedInput.close();

        InputStream input = ServerConfigUpgrader.class.getClassLoader().getResourceAsStream(
            "hq-oracle-server-to-upgrade.conf");
        expectedConfig.put("server.connection-validation-sql", "select 1 from dual");
        expectedConfig.put("server.hibernate.dialect",
            "org.hyperic.hibernate.dialect.Oracle9Dialect");
        expectedConfig.put("server.encryption-key", "defaultkey");
        expectedConfig.put("tomcat.maxthreads", "500");
        expectedConfig.put("tomcat.minsparethreads", "50");
        expectedConfig.put("server.jms.usejmx", "false");
        expectedConfig.put("server.jms.jmxport", "1099");
        expectedConfig.remove("server.database-password");
        Properties serverConfig = upgrader.upgradeServerConfig(input);
        String encryptedPw = (String) serverConfig.remove("server.database-password");
        assertTrue(PropertyValueEncryptionUtils.isEncryptedValue(encryptedPw));
        assertEquals(expectedConfig, serverConfig);
    }

    /**
     * Verifies that protocolVersion is added to old embedded database URLs (if
     * missing) on upgrade
     * @throws IOException
     */
    @Test
    public void testUpgradeConfigEmbeddedDB() throws IOException {
        ServerConfigUpgrader upgrader = new ServerConfigUpgrader();
        InputStream expectedInput = ServerConfigUpgrader.class.getClassLoader()
            .getResourceAsStream("hq-embedded-db-server-to-upgrade.conf");
        Properties expectedConfig = new Properties();
        expectedConfig.load(expectedInput);
        expectedConfig.remove("hq-engine.jnp.port");
        expectedConfig.remove("hq-engine.server.port");
        expectedInput.close();

        InputStream input = ServerConfigUpgrader.class.getClassLoader().getResourceAsStream(
            "hq-embedded-db-server-to-upgrade.conf");
        expectedConfig.put("server.connection-validation-sql", "select 1");
        expectedConfig.put("server.database-url",
            "jdbc:postgresql://127.0.0.1:9432/hqdb?protocolVersion=2");
        expectedConfig.put("server.hibernate.dialect",
            "org.hyperic.hibernate.dialect.PostgreSQLDialect");
        expectedConfig.put("server.encryption-key", "defaultkey");
        expectedConfig.put("tomcat.maxthreads", "500");
        expectedConfig.put("tomcat.minsparethreads", "50");
        expectedConfig.put("server.jms.usejmx", "false");
        expectedConfig.put("server.jms.jmxport", "1099");
        expectedConfig.remove("server.database-password");
        Properties serverConfig = upgrader.upgradeServerConfig(input);
        String encryptedPw = (String) serverConfig.remove("server.database-password");
        assertTrue(PropertyValueEncryptionUtils.isEncryptedValue(encryptedPw));
        assertEquals(expectedConfig, serverConfig);
    }

    @Test(expected = BuildException.class)
    public void testLoadNonExistentConfigFile() {
        ServerConfigUpgrader upgrader = new ServerConfigUpgrader();
        upgrader.setExisting("/foo/bar/hq-server.conf");
        upgrader.upgradeServerConfig();
    }

    @Test(expected = BuildException.class)
    public void testExportNonExistentDir() {
        ServerConfigUpgrader upgrader = new ServerConfigUpgrader();
        upgrader.setNew("/foo/bar2/hq-server.conf");
        upgrader.exportConfig(new Properties());
    }

    /**
     * If upgrading after 4.3, it would be normal for the JBoss mail service
     * config file to be missing This should just return gracefully
     */
    @Test
    public void testParseMailConfigMissingFile() {
        ServerConfigUpgrader upgrader = new ServerConfigUpgrader();
        upgrader.setUpgradeDir("/foo/bar");
        Properties props = new Properties();
        upgrader.parseMailConfig(props);
        assertTrue(props.isEmpty());
    }

}
