package org.hyperic.tools.ant;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

import org.apache.tools.ant.BuildException;
import org.junit.Test;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
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
        Properties serverConfig = upgrader.upgradeServerConfig(input);
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
        Properties serverConfig = upgrader.upgradeServerConfig(input);
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
            "jdbc:postgresql://127.0.0.1:9342/hqdb?protocolVersion=2");

        Properties serverConfig = upgrader.upgradeServerConfig(input);
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
