/*
 * NOTE: This copyright does *not* cover user programs that use HQ
 * program services by normal system calls through the application
 * program interfaces provided as part of the Hyperic Plug-in Development
 * Kit or the Hyperic Client Development Kit - this is merely considered
 * normal use of the program, and does *not* fall under the heading of
 * "derived work".
 * 
 * Copyright (C) [2004, 2005, 2006], Hyperic, Inc.
 * This file is part of HQ.
 * 
 * HQ is free software; you can redistribute it and/or modify
 * it under the terms version 2 of the GNU General Public License as
 * published by the Free Software Foundation. This program is distributed
 * in the hope that it will be useful, but WITHOUT ANY WARRANTY; without
 * even the implied warranty of MERCHANTABILITY or FITNESS FOR A
 * PARTICULAR PURPOSE. See the GNU General Public License for more
 * details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307
 * USA.
 */

package org.hyperic.hq.install;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.sql.SQLException;
import java.util.Properties;
import java.util.StringTokenizer;

import org.hyperic.sigar.Sigar;
import org.hyperic.sigar.SigarException;

import org.hyperic.util.InetPortPinger;
import org.hyperic.util.JDK;
import org.hyperic.util.StringUtil;
import org.hyperic.util.config.ConfigResponse;
import org.hyperic.util.config.ConfigSchema;
import org.hyperic.util.config.EarlyExitException;
import org.hyperic.util.config.EnumerationConfigOption;
import org.hyperic.util.config.HiddenConfigOption;
import org.hyperic.util.config.InvalidOptionValueException;
import org.hyperic.util.config.IpAddressConfigOption;
import org.hyperic.util.config.PortConfigOption;
import org.hyperic.util.config.StringConfigOption;
import org.hyperic.util.config.YesNoConfigOption;
import org.hyperic.util.jdbc.DBUtil;
import org.hyperic.util.jdbc.DriverLoadException;

public class ServerConfig extends BaseConfig {

    public static final String ctx = ServerConfig.class.getName();

    // convenience, PN for "product name"
    public static final String PN = PRODUCT;

    // database names that appear in the select list
    public static final String DBC_ORA8    = "Oracle 8";
    public static final String DBC_ORA9    = "Oracle 9i/10g";
    public static final String DBC_PGSQL   = "PostgreSQL";
    public static final String DBC_BUILTIN = "HQ Built-in Database";
    public static final String DBC_MYSQL   = "MySQL 5.x";
    
    // database names we need to use internally
    public static final String DB_ORA8  = "Oracle8";
    public static final String DB_ORA9  = "Oracle9i";
    public static final String DB_PGSQL = "PostgreSQL";
    public static final String DB_MYSQL = "MySQL";
    
    // Required for postgresql
    public static final String PGSQL_PROTOCOL = "?protocolVersion=2";

    public static final String Q_POSTGRESQL_PORT
        = "What port should HQ's built-in database use?";
    public static final String Q_PRODUCT_TYPE
        = "What kind of " + PN + " server should be installed?";
    public static final String Q_CLUSTER_BINDADDR
        ="What IP address should the cluster server bind to?";
    public static final String Q_MULTICAST_ADDR
        = "What is the cluster's multicast address?";
    public static final String Q_MULTICAST_PORT
        = "What is the cluster's multicast port?";
    public static final String Q_OVERWRITE
        = "Should we overwrite the existing "+PN+" server installation?";
    public static final String Q_OVERWRITE_DB
        = "An HQ server database already exists at the JDBC connection URL.\n"
        + "What should be done with this database?";
    public static final String DB_CHOICE_UPGRADE 
        = "Upgrade the HQ server database";
    public static final String DB_CHOICE_OVERWRITE
        = "Overwrite the HQ server database (ERASE all existing data)";
    public static final String DB_CHOICE_CANCEL
        = "Exit the installer";
    public static final String[] DB_CHOICES
        = { DB_CHOICE_UPGRADE, DB_CHOICE_OVERWRITE, DB_CHOICE_CANCEL };
    public static final String Q_CREATE_DB
        = "Should we create the "+PN+" server database?  If this is "
        + "an upgrade, answer no.  Otherwise, answer yes for a new install.";
    public static final String Q_PORT_WEBAPP
        = "What port should the "+PN+" server's web-based GUI listen "
        + "on for http communication?";
    public static final String Q_PORT_WEBAPP_SECURE
        = "What port should the "+PN+" server's web-based GUI listen "
        + "on for secure https communication?";
    public static final String Q_PORT_JNP
        = "What port should the "+PN+" server use for the jnp service?";
    public static final String Q_PORT_MBEAN
        = "What port should the "+PN+" server use for the mbean server?";
    public static final String Q_WEBAPP_URL
        = "Enter the base URL for the "+PN+" server's web-based GUI";
    public static final String Q_MAIL_HOST
        = "Enter the fully qualified domain name of the SMTP server that " + PN
        + " will use to send email messages";
    public static final String Q_MAIL_FROM
        = "Enter the email address that "+PN+" will use as the sender for "
        + "email messages";
    public static final String Q_DATABASE
        = "What backend database should the "+PN+" server use?";
    public static final String Q_JDBC_URL
        = "Enter the JDBC connection URL for the %%DBNAME%% database";
    public static final String Q_JDBC_USER
        = "Enter the username to use to connect to the database";
    public static final String Q_JDBC_PASSWORD
        = "Enter the password to use to connect to the database";
    public static final String Q_ADMIN_USER
        = "What should the username be for the initial admin user?";
    public static final String Q_ADMIN_PASSWORD
        = "What should the password be for the initial admin user?";
    public static final String Q_ADMIN_EMAIL
        = "What should the email address be for the initial admin user?";

    private static final String SERVER_DATABASE_UPGRADE_CHOICE
        = "server.database.upgrade.choice";

    // convenience constants
    private static final String nl = System.getProperty("line.separator");
    
    public ServerConfig () {
        super("server");
    }

    public String getName () { return PN + " server"; }

    protected ConfigSchema getUpgradeSchema (ConfigResponse previous,
                                             int iterationCount)
        throws EarlyExitException {
        ConfigSchema schema = super.getUpgradeSchema(previous, iterationCount);
        if (schema == null) schema = new ConfigSchema();
        switch (iterationCount) {
        case 0:
            schema.addOption
                (new HiddenConfigOption("server.overwrite",
                                        YesNoConfigOption.NO));
                return schema;
        case 1:
            return schema;
        default: return null;
        }
    }
    protected ConfigSchema getInstallSchema (ConfigResponse previous,
                                             int iterationCount) 
        throws EarlyExitException {

        ConfigSchema schema = super.getInstallSchema(previous, iterationCount);
        String portChoice;
        String fqdn;
        String domain;
        String senderChoice;
        String dbChoiceStr;
        String dbChoice;
        StringConfigOption usernameOption;
        StringConfigOption passwordOption;
        String serverInstallDir;

        InstallMode installMode =
            new InstallMode(getProjectProperty("install.mode"));

        // Do we have an builtin-postgresql packaged with us?
        boolean haveBuiltinDB = getReleaseHasBuiltinDB();

        switch ( iterationCount ) {
        case 0:
            break;

        case 1:
            schema.addOption
                (new HiddenConfigOption("server.multicast.addr", "227.0.0.1"));
            schema.addOption
                (new HiddenConfigOption("server.multicast.port", "3030"));
            schema.addOption
                (new HiddenConfigOption("server.ha.bind_addr", 
                                        IpAddressConfigOption.DEFAULT_ADDR));
            break;

        case 2:
            // Is there in fact an installation we should worry about?
            serverInstallDir = previous.getValue("server.installdir");
            if ( serverAlreadyInstalled(serverInstallDir) ) {
                schema.addOption
                    (new YesNoConfigOption("server.overwrite",
                                           Q_OVERWRITE, 
                                           YesNoConfigOption.NO));
            } else {
                // No server installed, just assume the answer is no,
                // which will be a 'safe bet'
                schema.addOption
                    (new HiddenConfigOption("server.overwrite",
                                            YesNoConfigOption.NO));
            }
            break;

        case 3:
            // If they chose not to overwrite, and the server exists, bail out
            serverInstallDir = previous.getValue("server.installdir");
            if ( serverAlreadyInstalled(serverInstallDir) 
                 && "No".equals(previous.getValue("server.overwrite")) ) {
                throw new EarlyExitException("Exiting setup: "+PN+" server "
                                             + "already installed in " 
                                             + serverInstallDir);
            }
            
            if (installMode.isQuick()) {
                schema.addOption
                    (new HiddenConfigOption("server.webapp.port", "7080"));
                
                schema.addOption
                    (new HiddenConfigOption("server.webapp.secure.port",
                                            "7443"));
                
                schema.addOption
                    (new HiddenConfigOption("hq-engine.jnp.port", "2099"));
                
                schema.addOption
                    (new HiddenConfigOption("hq-engine.server.port", "9093"));
            } else {
                schema.addOption
                    (new PortConfigOption("server.webapp.port",
                                          Q_PORT_WEBAPP,
                                          new Integer(7080)));
                
                schema.addOption
                    (new PortConfigOption("server.webapp.secure.port",
                                          Q_PORT_WEBAPP_SECURE,
                                          new Integer(7443)));
                
                schema.addOption
                    (new PortConfigOption("hq-engine.jnp.port",
                                          Q_PORT_JNP,
                                          new Integer(2099)));
                
                schema.addOption
                    (new PortConfigOption("hq-engine.server.port",
                                          Q_PORT_MBEAN,
                                          new Integer(9093)));
            }
            break;

        case 4:
            portChoice = previous.getValue("server.webapp.port");
            fqdn = computeFQDN();
            domain = computeDomain(fqdn);

            // only collect baseurl property if we're initially
            // creating the database, since we don't yet run dbsetup
            // for upgrades
            String computedBaseUrl = computeHTTPBaseUrl(fqdn, portChoice);
            if (installMode.isQuick()) {
                schema.addOption
                    (new HiddenConfigOption("server.webapp.baseurl",
                                            computedBaseUrl));
            } else {
                schema.addOption
                    (new StringConfigOption("server.webapp.baseurl",
                                            Q_WEBAPP_URL,
                                            computedBaseUrl));
            }

            // Do we have a local MTA?
            if ( haveLocalMTA() ) {
                schema.addOption(new HiddenConfigOption("server.mail.host",
                                                        "127.0.0.1"));
            } else {
                schema.addOption(new StringConfigOption("server.mail.host",
                                                        Q_MAIL_HOST,
                                                        fqdn));
            }

            if (installMode.isQuick()) {
                schema.addOption(new HiddenConfigOption("server.mail.sender",
                                                        "hqadmin@" + domain));
            } else {
                schema.addOption(new StringConfigOption("server.mail.sender",
                                                        Q_MAIL_FROM,
                                                        "hqadmin@" + domain));
            }
            break;

        case 5:
            if (installMode.isOracle()) {
                String defaultDB = DBC_ORA9;
                String[] dbs = new String[] { DBC_ORA8, DBC_ORA9 };
                schema.addOption(
                    new EnumerationConfigOption("server.database.choice",
                                                Q_DATABASE,
                                                defaultDB,
                                                dbs));
            } else if (installMode.isPostgres()) {
                schema.addOption(
                    new HiddenConfigOption("server.database.choice",
                                           DBC_PGSQL));
            } else if (installMode.isMySQL()) {
                schema.addOption(
                    new HiddenConfigOption("server.database.choice",
                                           DBC_MYSQL));
            } else if (installMode.isQuick() && haveBuiltinDB) {
                schema.addOption(
                    new HiddenConfigOption("server.database.choice",
                                           DBC_BUILTIN));
            } else {
                String defaultDB = haveBuiltinDB ? DBC_BUILTIN : DBC_ORA9;
                String[] dbs = haveBuiltinDB
                    ? new String[] { DBC_BUILTIN, DBC_ORA8, DBC_ORA9, 
                                     DBC_PGSQL, DBC_MYSQL }
                    : new String[] { DBC_ORA8, DBC_ORA9, DBC_PGSQL, DBC_MYSQL };
                schema.addOption(
                    new EnumerationConfigOption("server.database.choice",
                                                Q_DATABASE,
                                                defaultDB,
                                                dbs));
            }
            break;

        case 6:
            // determine server.database from server.database.choice...
            dbChoiceStr = previous.getValue("server.database.choice");
            if (dbChoiceStr.equals(DBC_ORA8))
                dbChoice = DB_ORA8;
            else if (dbChoiceStr.equals(DBC_ORA9))
                dbChoice = DB_ORA9;
            else if (dbChoiceStr.startsWith(DBC_PGSQL))
                dbChoice = DB_PGSQL;
            else if (dbChoiceStr.startsWith(DBC_MYSQL))
                dbChoice = DB_MYSQL;
            else if (dbChoiceStr.equals(DBC_BUILTIN)) {
                dbChoice = DB_PGSQL;
                schema.addOption(new HiddenConfigOption("using.builtin.db",
                                                        "true"));
            }
            else throw new IllegalStateException("Invalid database: " 
                                                 + dbChoiceStr);

            schema.addOption(new HiddenConfigOption("server.database",
                                                    dbChoice));

            if (dbChoice.equals(DB_ORA8) || dbChoice.equals(DB_ORA9)) {
                schema.addOption(new StringConfigOption("server.database-url",
                        StringUtil.replace(Q_JDBC_URL, "%%DBNAME%%",
                                           dbChoiceStr),
                        "jdbc:oracle:thin:@localhost:1521:HYPERIC_" + PRODUCT));
                schema.addOption(new HiddenConfigOption(
                        "server.database-driver",
                        "oracle.jdbc.driver.OracleDriver"));

                schema.addOption(new HiddenConfigOption(
                        "server.quartzDelegate",
                        "org.quartz.impl.jdbcjobstore.oracle.OracleDelegate"));

            } else if ( dbChoiceStr.startsWith(DBC_PGSQL) ) {
                schema.addOption(new StringConfigOption("server.database-url",
                        StringUtil.replace(Q_JDBC_URL, "%%DBNAME%%",
                                           dbChoiceStr),
                        "jdbc:postgresql://localhost:5432/" + PRODUCT
                                + PGSQL_PROTOCOL));
                schema.addOption(new HiddenConfigOption(
                        "server.database-driver", "org.postgresql.Driver"));
                schema.addOption(new HiddenConfigOption(
                        "server.quartzDelegate",
                        "org.quartz.impl.jdbcjobstore.PostgreSQLDelegate"));
            } else if ( dbChoice.equals(DB_MYSQL) ) {
                schema.addOption(new StringConfigOption("server.database-url",
                        StringUtil.replace(Q_JDBC_URL, "%%DBNAME%%",
                                           dbChoiceStr),
                        "jdbc:mysql://localhost:3306/" + PRODUCT));
                schema.addOption(new HiddenConfigOption(
                        "server.database-driver", "com.mysql.jdbc.Driver"));
                schema.addOption(new HiddenConfigOption(
                        "server.quartzDelegate",
                        "org.quartz.impl.jdbcjobstore.StdJDBCDelegate"));
            } else {
                if (!installMode.isQuick()) {
                    // In "full" mode, we even let them pick the pgsql port
                    schema.addOption
                        (new PortConfigOption("server.postgresql.port",
                                              Q_POSTGRESQL_PORT,
                                              new Integer(9432)));
                } else {
                    schema.addOption
                        (new HiddenConfigOption("server.postgresql.port",
                                                "9432"));
                }
                    
                schema.addOption(new HiddenConfigOption
                                 ("server.database-driver",
                                  "org.postgresql.Driver"));
                schema.addOption
                    (new HiddenConfigOption
                     ("server.quartzDelegate",
                      "org.quartz.impl.jdbcjobstore.PostgreSQLDelegate"));
            }
            
            if (dbChoiceStr.equals(DBC_BUILTIN)) {
                schema.addOption(new HiddenConfigOption
                                 ("server.database-user", "hqadmin"));
                schema.addOption(new HiddenConfigOption
                                 ("server.database-password", "hqadmin"));
            }
            else {
                schema.addOption(new StringConfigOption("server.database-user",
                                                        Q_JDBC_USER));
                passwordOption = new StringConfigOption(
                        "server.database-password", Q_JDBC_PASSWORD);
                passwordOption.setSecret(true);
                schema.addOption(passwordOption);
            }

            senderChoice = previous.getValue("server.mail.sender");
            // dont ask about admin username if this is an HA node
            // this should have already been set up
            if (installMode.isQuick()) {
                schema.addOption(new HiddenConfigOption(
                        "server.admin.username", "hqadmin"));
                schema.addOption(new HiddenConfigOption(
                        "server.admin.password", "hqadmin"));
                schema.addOption(new HiddenConfigOption("server.admin.email",
                        senderChoice));
            } else {
                usernameOption = new AdminUsernameConfigOption
                    ("server.admin.username", Q_ADMIN_USER, "hqadmin");
                schema.addOption(usernameOption);

                passwordOption = new StringConfigOption
                    ("server.admin.password", Q_ADMIN_PASSWORD, null);
                passwordOption.setSecret(true);
                passwordOption.setMinLength(6);
                passwordOption.setMaxLength(40);
                schema.addOption(passwordOption);

                schema.addOption
                    (new StringConfigOption("server.admin.email",
                                            Q_ADMIN_EMAIL,
                                            senderChoice));
            }
            break;

        case 7:
            // For servers using the builtinDB we have only gotten the port at 
            // this point.  Now we setup the url based on the port selection
            dbChoiceStr = previous.getValue("server.database.choice");
            if (dbChoiceStr.equals(DBC_BUILTIN)) {
                String pgport = previous.getValue("server.postgresql.port");
                schema.addOption(new HiddenConfigOption
                                 ("server.database-url",
                                  "jdbc:postgresql://127.0.0.1:" + pgport +
                                  "/hqdb" + PGSQL_PROTOCOL));
            }
            break;

        case 8:
            // Now that they have made their jdbc selections, do a sanity check:
            // If we are in "quick" mode and the database already exists,
            // then STOP ask the user what to do.
            dbChoiceStr = previous.getValue("server.database.choice");
            if (!dbChoiceStr.equals(DBC_BUILTIN)) {
                if (databaseExists(previous)) {
                    // Bug 9722 only check for db upgrade if this isnt an HA node
                    schema.addOption
                        (new EnumerationConfigOption(SERVER_DATABASE_UPGRADE_CHOICE,
                                                     Q_OVERWRITE_DB,
                                                     DB_CHOICE_CANCEL,
                                                     DB_CHOICES,
                                                     DB_CHOICE_OVERWRITE));
                } else {
                    schema.addOption(new HiddenConfigOption(SERVER_DATABASE_UPGRADE_CHOICE,
                                                            DB_CHOICE_OVERWRITE));
                }
            } else {
                //Built-in DB, overwrite the db
                schema.addOption(new HiddenConfigOption(SERVER_DATABASE_UPGRADE_CHOICE,
                                                        DB_CHOICE_OVERWRITE));
            }
            break;

        case 9:
            String dbUpgradeChoice = previous.getValue(SERVER_DATABASE_UPGRADE_CHOICE);
            if (dbUpgradeChoice.equals(DB_CHOICE_OVERWRITE)) {
                schema.addOption
                    (new HiddenConfigOption("server.database.create",
                                            YesNoConfigOption.YES));
            } else if (dbUpgradeChoice.equals(DB_CHOICE_UPGRADE)) {
                schema.addOption
                    (new HiddenConfigOption("server.database.upgrade",
                                            YesNoConfigOption.YES));
            } else {
                throw new EarlyExitException
                    ("No modifications made to existing database.  " +
                     "Exiting installer.");
            }
            break;

        default:
            return null;
        }

        return schema;
    }

    private String computeBaseUrl(String scheme, String fqdn, String port,
                                  String defaultPort) {
        if (fqdn == null) {
            return null;
        }

        StringBuffer buf = new StringBuffer(scheme);
        buf.append("://");
        buf.append(fqdn);
        if (port != null && ! port.equals(defaultPort)) {
            buf.append(":");
            buf.append(port.toString());
        }
        buf.append("/");

        return buf.toString();
    }

    protected String computeHTTPBaseUrl(String fqdn, String port) {
        return computeBaseUrl("http", fqdn, port, "80");
    }

    protected String computeDomain(String fqdn) {
        String domainname;

        StringTokenizer token = new StringTokenizer(fqdn, ".");

        if (token.countTokens() > 2) {
            int index = fqdn.indexOf('.');
            domainname = fqdn.substring(++index);
        }
        else {
            domainname = fqdn;
        }
        return domainname;
    }

    protected String computeFQDN() {
        String fqdn = null;
        try {
            fqdn = new Sigar().getFQDN();
        }
        catch (SigarException e) {
            // this machine is seriously misconfigured. that's
            // ok, you just don't get a default. have fun
            // typing!
        }
        // Actually, just assume localhost if no fqdn
        if (fqdn == null || fqdn.length() == 0) fqdn = "localhost";
        return fqdn;
    }

    protected boolean serverAlreadyInstalled (String dir) {
        String serverFile
            = dir + File.separator
            + "server-" + getProjectProperty("version") + File.separator
            + "hq-engine";
        File f = new File(serverFile);
        return f.exists();
    }
    
    public boolean haveLocalMTA() {
        InetPortPinger ip = new InetPortPinger("127.0.0.1", 25, 10);
        return ip.check();
    }

    public static final String[] MARKER_FILES
        = { "bin/hq-server.sh", "bin/hq-server.exe", "bin/hq-server.bat" };

    protected String[] getMarkerFiles () {
        return MARKER_FILES;
    }

    public void canUpgrade (String dir) throws InvalidOptionValueException {
        // If the 'dir' points to a pointbase-backed installation, we can't 
        // upgrade it.

        // First we look in conf/hq-server.conf if it exists.  If this is an
        // older release, we look in hq-ds.xml file for the <connection-url> 
        // element.
        File confFile = new File(dir, 
                                 StringUtil.normalizePath
                                 ("conf/hq-server.conf"));
        if (confFile.exists()) {
            Properties props = new Properties();
            FileInputStream fi = null;
            try {
                fi = new FileInputStream(confFile.getAbsolutePath());
                props.load(fi);
            } catch (IOException e) {
                throw new InvalidOptionValueException
                    ("Error reading hq-server.conf file: " 
                     + confFile.getAbsolutePath()
                     + e.getMessage());
            } finally {
                if (fi != null) {
                    try { fi.close(); } catch (IOException e) {}
                }
            }
            String driverClass = props.getProperty("server.database-driver");
            if (driverClass == null) {
                throw new InvalidOptionValueException
                    ("No server.database-driver property found in: " 
                     + confFile.getAbsolutePath());
            }
            if (driverClass.indexOf("com.pointbase.") != -1) {
                throw new InvalidOptionValueException
                    ("Cannot upgrade HQ server: upgrade not supported for "
                     + "HQ servers that use the PointBase database");
            }
            return;
        } else {
            throw new EarlyExitException
                ("__ll__\nCannot upgrade HQ server\n(file not found: "
                 + confFile.getAbsolutePath()
                 + ")\n\nOnly versions 1.3.x and higher can "
                 + "be upgraded using the installer.\nTo upgrade earlier "
                 + "versions, follow the instructions in README.txt\n\n__ll__");
        }
    }

    public String getCompletionText (ConfigResponse config) {
        StringBuffer s = new StringBuffer();
        String sp = File.separator;
        String startup = getProductInstallDir(config);
        startup += "bin" + sp + PN.toLowerCase() + "-server" + getExtension();
        s.append("__ll__")
            .append(getServerStartupText(startup).toString());
        if (isUpgrade()) {
            s.append(nl)
                .append(nl).append(" Your HQ server has been successfully upgraded.").append(nl)
                .append(nl).append(" Once you start up your HQ server, you can log in using any of your existing")
                .append(nl).append(" HQ user accounts.");
        } else if (isDBUpgrade(config)) {
            s.append(nl)
                .append(nl).append(" Once the HQ server reports that it has successfully started, you can log in")
                .append(nl).append(" to your HQ server at: ")
                .append(nl)
                .append(nl).append("  ").append(config.getValue("server.webapp.baseurl"))
                .append(nl)
                .append(nl).append(" You can log in using any of your existing HQ user accounts.");
            
        } else {
            // A new/fresh install
            s.append(nl)
                .append(nl).append(" Once the HQ server reports that it has successfully started, you can log in")
                .append(nl).append(" to your HQ server at: ")
                .append(nl)
                .append(nl).append("  ").append(config.getValue("server.webapp.baseurl"))
                .append(nl).append("  username: ").append(config.getValue("server.admin.username"))
                .append(nl).append("  password: ").append(config.getValue("server.admin.password"))
                .append(nl)
                .append(nl).append(" To change your password, log in to the HQ server, click the \"Administration\"")
                .append(nl).append(" link, choose \"List Users\", then click on the \"hqadmin\" user.");
        }
        s.append(nl).append("__ll__");
        return s.toString();
    }

    private StringBuffer getServerStartupText (String startup) {
        StringBuffer s = new StringBuffer();
        s.append(nl);
        if (JDK.IS_WIN32) {
            s.append(" You should now install the HQ server as a Windows Service using this command:")
                .append(nl).append(nl).append("  ").append(startup).append(" -i")
                .append(nl).append(nl).append(" You can then use the Service Control Manager (Control Panel->Services) to ")
                .append(nl).append(" start the HQ server.  Note that the first time the HQ server starts up it may")
                .append(nl).append(" take several minutes to initialize.  Subsequent startups will be much faster.");
                     
        } else {
            s.append(nl).append(" You can now start your HQ server by running this command:")
                .append(nl).append(nl).append("  ").append(startup).append(" start")
                .append(nl).append(nl).append(" Note that the first time the HQ server starts up it may take several minutes")
                .append(nl).append(" to initialize.  Subsequent startups will be much faster.");
        }
        return s;
    }

    protected boolean databaseExists (ConfigResponse config) 
        throws EarlyExitException {

        String user     = config.getValue("server.database-user");
        String password = config.getValue("server.database-password");
        String url      = config.getValue("server.database-url");

        try {
            return DBUtil.checkTableExists(url, user, password,
                                           "EAM_CONFIG_PROPS");
        } catch (DriverLoadException e) {
            throw new EarlyExitException("Error connecting to database "
                                         + "(" + url + "): " + e.getMessage());
        } catch (SQLException e) {
            throw new EarlyExitException("Error checking for existing "
                                         + "database: " + e.getMessage());
        }
    }
    
    private boolean isDBUpgrade (ConfigResponse config) {
        String dbUpgrade = config.getValue("server.database.upgrade");
        return (dbUpgrade != null && dbUpgrade.equals(YesNoConfigOption.YES));
    }

    protected boolean getReleaseHasBuiltinDB () {
        File hqdbDir = new File(getProjectProperty("install.dir")
                                + File.separator
                                + "data"
                                + File.separator
                                + "hqdb");
        return (hqdbDir.exists() && hqdbDir.isDirectory() && hqdbDir.canRead());
    }
    
    private class InstallMode {
        boolean _oracleQuickMode   = false;
        boolean _postgresQuickMode = false;
        boolean _mysqlQuickMode    = false;
        boolean _quickMode         = false;
        
        InstallMode(String mode) {
            _oracleQuickMode   = mode.equals(INSTALLMODE_ORACLE);
            _postgresQuickMode = mode.equals(INSTALLMODE_POSTGRESQL);
            _mysqlQuickMode    = mode.equals(INSTALLMODE_MYSQL);
            _quickMode         = mode.equals(INSTALLMODE_QUICK);
        }
        
        boolean isOracle() {
            return _oracleQuickMode;
        }
        
        boolean isPostgres() {
            return _postgresQuickMode;
        }
        
        boolean isMySQL() {
            return _mysqlQuickMode;
        }
        
        boolean isQuick() {
            return _postgresQuickMode || _oracleQuickMode || _mysqlQuickMode ||
                   _quickMode;
        }
    }
}
