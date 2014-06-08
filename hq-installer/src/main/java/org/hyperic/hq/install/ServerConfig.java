/*
 * NOTE: This copyright does *not* cover user programs that use Hyperic
 * program services by normal system calls through the application
 * program interfaces provided as part of the Hyperic Plug-in Development
 * Kit or the Hyperic Client Development Kit - this is merely considered
 * normal use of the program, and does *not* fall under the heading of
 * "derived work".
 *
 * Copyright (C) [2004-2010], VMware, Inc.
 * This file is part of Hyperic.
 *
 * Hyperic is free software; you can redistribute it and/or modify
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

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.sql.SQLException;
import java.util.Properties;
import java.util.StringTokenizer;
import java.util.UUID;

import org.hyperic.sigar.Sigar;
import org.hyperic.sigar.SigarException;
import org.hyperic.util.InetPortPinger;
import org.hyperic.util.JDK;
import org.hyperic.util.StringUtil;
import org.hyperic.util.config.ConfigOptionDisplay;
import org.hyperic.util.config.ConfigResponse;
import org.hyperic.util.config.ConfigSchema;
import org.hyperic.util.config.EarlyExitException;
import org.hyperic.util.config.EnumerationConfigOption;
import org.hyperic.util.config.HiddenConfigOption;
import org.hyperic.util.config.InstallConfigOption;
import org.hyperic.util.config.InvalidOptionValueException;
import org.hyperic.util.config.IpAddressConfigOption;
import org.hyperic.util.config.PortConfigOption;
import org.hyperic.util.config.ReturnStepsException;
import org.hyperic.util.config.StringConfigOption;
import org.hyperic.util.config.YesNoConfigOption;
import org.hyperic.util.jdbc.DriverLoadException;
import org.hyperic.util.security.SecurityUtil;

public class ServerConfig
extends BaseConfig {

	public static final String ctx = ServerConfig.class.getName();

	// convenience, PN for "product name"
	public static final String PN = PRODUCT;

	//environments names for selection as an installation profile
	public static final String ENV_SMALL = "small";
	public static final String ENV_MEDIUM = "medium";
	public static final String ENV_LARGE = "large";

	public static final String ENV_SMALL_DESC = " (less than 50 platforms)";
	public static final String ENV_MEDIUM_DESC = " (50-250 platforms)";
	public static final String ENV_LARGE_DESC = " (larger than 250 platforms)";    

	// database names that appear in the select list
	public static final String DBC_PGSQL = "PostgreSQL";
	public static final String DBC_BUILTIN = "HQ Built-in Database";

	// database names we need to use internally
	public static final String DB_PGSQL = "PostgreSQL";

	// Required for postgresql
	public static final String PGSQL_PROTOCOL = "?protocolVersion=2";

	public static final String Q_POSTGRESQL_PORT = "What port should HQ's built-in database use?";
	public static final String Q_PRODUCT_TYPE = "What kind of " + PN +
			" server should be installed?";
	public static final String Q_CLUSTER_BINDADDR = "What IP address should the cluster server bind to?";
	public static final String Q_MULTICAST_ADDR = "What is the cluster's multicast address?";
	public static final String Q_MULTICAST_PORT = "What is the cluster's multicast port?";
	public static final String Q_OVERWRITE = "Should we overwrite the existing " + PN +
			" server installation?";
	public static final String Q_OVERWRITE_DB = "An HQ server database already exists at the JDBC connection URL.\n"
			+ "What should be done with this database?";
	public static final String DB_CHOICE_UPGRADE = "Upgrade the HQ server database";
	public static final String DB_CHOICE_OVERWRITE = "Overwrite the HQ server database (ERASE all existing data)";
	public static final String DB_CHOICE_CANCEL = "Exit the installer";
	public static final String[] DB_CHOICES = { DB_CHOICE_UPGRADE,
		DB_CHOICE_OVERWRITE,
		DB_CHOICE_CANCEL };
	public static final String Q_CREATE_DB = "Should we create the " + PN +
			" server database?  If this is " +
			"an upgrade, answer no.  Otherwise, answer yes for a new install.";
	public static final String Q_PORT_WEBAPP = "What port should the " + PN +
			" server's web-based GUI listen " +
			"on for http communication?";
	public static final String Q_PORT_WEBAPP_SECURE = "What port should the " + PN +
			" server's web-based GUI listen " +
			"on for secure https communication?";
	public static final String Q_WEBAPP_URL = "Enter the base URL for the " + PN +
			" server's web-based GUI";
	public static final String Q_MAIL_HOST = "Enter the fully qualified domain name of the SMTP server that " +
			PN + " will use to send email messages";
	public static final String Q_MAIL_FROM = "Enter the email address that " + PN +
			" will use as the sender for " + "email messages";
	public static final String Q_DATABASE = "The " + PN + " built-in database is provided for EVALUATION PURPOSES ONLY. " +
			"For production purposes use vPosgreSQL. " + 
			"What backend database should the " + PN +
			" server use?";
	public static final String Q_DB_HOSTNAME = "Enter the vPostgres DB hostname";
	public static final String Q_DB_PORT = "Enter the vPostgres DB port";
	public static final String Q_DB_NAME = "Enter the vPostgres DB name";
	public static final String Q_JDBC_URL = "Override the JDBC connection URL for the %%DBNAME%% database";
	public static final String Q_JDBC_USER = "Enter the username to use to connect to the database";
	public static final String Q_JDBC_PASSWORD = "Enter the password to use to connect to the database.";
	public static final String Q_ENCRYPTION_KEY_CREATE = "Would you like to use an auto generated encryption key to encrypt the database password?";
	public static final String Q_ENCRYPTION_KEY = "Enter an encryption key to use to encrypt the database password.";
	public static final String Q_ADMIN_USER = "What should the username be for the initial admin user?";
	public static final String Q_ADMIN_PASSWORD = "What should the password be for the initial admin user?";
	public static final String Q_ADMIN_EMAIL = "What should the email address be for the initial admin user?";
	public static final String Q_USE_CUSTOM_KEYSTORE = "Would you like to use your own java keystore?";
	public static final String Q_SERVER_KEYSTORE_PATH = "What is the file path to your java keystore?";
	public static final String Q_SERVER_KEYSTORE_PASSWORD = "What is the password to your java keystore?";
	private static final String SERVER_DATABASE_UPGRADE_CHOICE = "server.database.upgrade.choice";
	public static final String Q_PROFILE = 	"What is the installation profile?";

	// convenience constants
	private static final String nl = System.getProperty("line.separator");

	public ServerConfig() {
		super("server");
	}

	@Override
	public String getName() {
		return PN + " server";
	}

	@Override
	protected ConfigSchema getUpgradeSchema(ConfigResponse previous, int iterationCount)
			throws EarlyExitException {
		ConfigSchema schema = super.getUpgradeSchema(previous, iterationCount);
		if (schema == null)
			schema = new ConfigSchema();

		// TODO Remove this code once we no longer support HQ version less than 4.6
		//      This is solely to maintain backwards compatibility with older HQ agents
		//      that don't handle SSL communication correctly.
		//      For the upgrade case, we want to automatically import unverified certificates
		schema.addOption(new HiddenConfigOption("accept.unverified.certificates", Boolean.TRUE.toString()));

		switch (iterationCount) {
		case 0:
			schema.addOption(new HiddenConfigOption("server.overwrite", YesNoConfigOption.NO));
			return schema;
		case 1:
			return schema;
		default:
			return null;
		}
	}

	@Override
	protected ConfigSchema getInstallSchema(ConfigResponse previous, int iterationCount)
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
		String dbHost;
		String dbPort;
		String dbName;
		InstallMode installMode = new InstallMode(getProjectProperty("install.mode"));
		// ...this property allows us to change installer behavior so that it's
		// .org or EE specific,
		// currently the eula is only shown for EE, so it's a pretty good
		// indicator right now...
		boolean isEEInstall = Boolean.parseBoolean(getProjectProperty("eula.present"));

		// Do we have an builtin-postgresql packaged with us?
		//Remove embedded DB on win system
		boolean haveBuiltinDB=false;
		if (JDK.IS_WIN32) {
			haveBuiltinDB = false;
		}
		else{
			 haveBuiltinDB = getReleaseHasBuiltinDB();
		}
	

		// TODO Remove this code once we no longer support HQ version less than 4.6
		//      This is solely to maintain backwards compatibility with older HQ agents
		//      that don't handle SSL communication correctly
		//      For the new install case, we do not want to automatically import unverified certificates
		schema.addOption(new HiddenConfigOption("accept.unverified.certificates", Boolean.FALSE.toString()));

		switch (iterationCount) {
		case 0:
			break;

		case 1:
			schema.addOption(new HiddenConfigOption("server.multicast.addr", "227.0.0.1"));
			schema.addOption(new HiddenConfigOption("server.multicast.port", "3030"));
			schema.addOption(new HiddenConfigOption("server.ha.bind_addr",
					IpAddressConfigOption.DEFAULT_ADDR));
			break;

		case 2:
			// Is there in fact an installation we should worry about?
			serverInstallDir = previous.getValue("server.installdir");

			if (serverAlreadyInstalled(serverInstallDir)) {
				schema.addOption(new YesNoConfigOption("server.overwrite", Q_OVERWRITE,
						YesNoConfigOption.NO));
			} else {
				// No server installed, just assume the answer is no,
				// which will be a 'safe bet'
				schema.addOption(new HiddenConfigOption("server.overwrite",
						YesNoConfigOption.NO));
			}

			break;

		case 3:
			// If they chose not to overwrite, and the server exists, bail
			// out
			serverInstallDir = previous.getValue("server.installdir");
			if (serverAlreadyInstalled(serverInstallDir) &&
					"No".equals(previous.getValue("server.overwrite"))) {
				throw new EarlyExitException("Exiting setup: " + PN + " server " +
						"already installed in " + serverInstallDir);
			}

			if (installMode.isQuick()) {
				schema.addOption(new HiddenConfigOption("server.webapp.port", "7080"));

				schema.addOption(new HiddenConfigOption("server.webapp.secure.port", "7443"));

			} else {
				schema.addOption(new PortConfigOption("server.webapp.port", Q_PORT_WEBAPP,
						new Integer(7080)));

				schema.addOption(new PortConfigOption("server.webapp.secure.port",
						Q_PORT_WEBAPP_SECURE, new Integer(7443)));

			}

			if (installMode.isQuick()) {
				schema.addOption(new HiddenConfigOption("server.use.custom.keystore", YesNoConfigOption.NO));
			} else {
				schema.addOption(new YesNoConfigOption("server.use.custom.keystore", Q_USE_CUSTOM_KEYSTORE, YesNoConfigOption.NO));
			}


			break;
		case 4:
			String useCustomKeystore = previous.getValue("server.use.custom.keystore");

			schema.addOption(new StringConfigOption("server.keystore.path", Q_SERVER_KEYSTORE_PATH, ""));
			if (YesNoConfigOption.YES.equals(useCustomKeystore)) {   //yes implies it's not a quick install (default is no)
				schema.addOption(new StringConfigOption("server.keystore.password", Q_SERVER_KEYSTORE_PASSWORD, ""));                	
			} else {
				// TODO not sure if there's a cleaner way to do this.  The problem is we technically don't know the real install path bc 
				// the archive hasn't been unzipped at this point.  So we use a token and replace it later in the ant script with the real path
				schema.addOption(new HiddenConfigOption("server.keystore.path", "../../conf/hyperic.keystore"));
				schema.addOption(new HiddenConfigOption("server.keystore.password", "hyperic"));
			}

			break;

		case 5:
			portChoice = previous.getValue("server.webapp.port");
			fqdn = computeFQDN();
			domain = computeDomain(fqdn);

			// only collect baseurl property if we're initially
			// creating the database, since we don't yet run dbsetup
			// for upgrades
			String computedBaseUrl = computeHTTPBaseUrl(fqdn, portChoice);
			if (installMode.isQuick()) {
				schema.addOption(new HiddenConfigOption("server.webapp.baseurl",
						computedBaseUrl));
			} else {
				schema.addOption(new StringConfigOption("server.webapp.baseurl", Q_WEBAPP_URL,
						computedBaseUrl));
			}

			// Do we have a local MTA?
			if (haveLocalMTA()) {
				schema.addOption(new HiddenConfigOption("server.mail.host", "127.0.0.1"));
			} else {
				schema.addOption(new StringConfigOption("server.mail.host", Q_MAIL_HOST, fqdn));
			}

			// We always ask for username and password now per HQ-3627, 
			// so probably shouldn't auto enter the email value
			schema.addOption(new StringConfigOption("server.mail.sender", Q_MAIL_FROM, "hqadmin@" + domain));

			ConfigOptionDisplay smallOption = new ConfigOptionDisplay(ENV_SMALL, ENV_SMALL_DESC);
			ConfigOptionDisplay mediumOption = new ConfigOptionDisplay(ENV_MEDIUM, ENV_MEDIUM_DESC);
			ConfigOptionDisplay largeOption = new ConfigOptionDisplay(ENV_LARGE, ENV_LARGE_DESC);
			ConfigOptionDisplay[] envs = {smallOption, mediumOption, largeOption};

			if(!installMode.isQuick() && isEEInstall)
				schema.addOption(new InstallConfigOption("install.profile", Q_PROFILE, smallOption, envs));
			else
				schema.addOption(new HiddenConfigOption("install.profile", ENV_SMALL));
			break;

		case 6:
			boolean usingSmallEnv = previous.getValue("install.profile").contentEquals(ENV_SMALL);
			if (installMode.isPostgres()) {
				schema.addOption(new HiddenConfigOption("server.database.choice", DBC_PGSQL));
			} 
			else if (installMode.isQuick() && haveBuiltinDB) {
				schema.addOption(new HiddenConfigOption("server.database.choice", DBC_BUILTIN));
			} 
			else {

				if (!haveBuiltinDB) {
					schema.addOption(new HiddenConfigOption("server.database.choice", DBC_PGSQL));
				}
				else {
					ConfigOptionDisplay builtInOption = new ConfigOptionDisplay(DBC_BUILTIN);
					if (!usingSmallEnv) {
						builtInOption = new ConfigOptionDisplay(DBC_BUILTIN + "(not recommended since you have selected " + previous.getValue("install.profile") +
								" installation profile and the build in DB should be used only with small profiles)");
					}

					ConfigOptionDisplay postgresOption = new ConfigOptionDisplay(DBC_PGSQL);
					ConfigOptionDisplay defaultDB =  builtInOption;
					ConfigOptionDisplay[] dbs =   new ConfigOptionDisplay[] { builtInOption, postgresOption};
					schema.addOption(new InstallConfigOption("server.database.choice", Q_DATABASE,
							defaultDB, dbs));
				}
			}
			break;

		case 7:
			// determine server.database from server.database.choice...
			dbChoiceStr = previous.getValue("server.database.choice");

			if (dbChoiceStr.startsWith(DBC_PGSQL)){
				dbChoice = DB_PGSQL;
			}
			else if (dbChoiceStr.startsWith(DBC_BUILTIN)) {
				dbChoice = DB_PGSQL;
				schema.addOption(new HiddenConfigOption("using.builtin.db", "true"));
			} 
			else{
				throw new IllegalStateException("Invalid database: " + dbChoiceStr);
			}
			schema.addOption(new HiddenConfigOption("server.database", dbChoice));
			if(!dbChoiceStr.startsWith(DBC_BUILTIN)) {
				schema.addOption(new StringConfigOption("server.database.host", Q_DB_HOSTNAME, "localhost"));
			}
			if (dbChoiceStr.startsWith(DBC_PGSQL) && !dbChoiceStr.startsWith(DBC_BUILTIN)) {
				schema.addOption(new PortConfigOption("server.database.port", Q_DB_PORT, 5432));
			} 
			if(!dbChoiceStr.startsWith(DBC_BUILTIN)) {
				schema.addOption(new StringConfigOption("server.database.name", Q_DB_NAME, PRODUCT));
			}
			break;
			
		case 8:
			dbChoiceStr = previous.getValue("server.database.choice");
			dbChoice = previous.getValue("server.database");
			dbHost = previous.getValue("server.database.host");
			dbPort = previous.getValue("server.database.port");
			dbName = previous.getValue("server.database.name");
			if (dbChoiceStr.startsWith(DBC_PGSQL)) {
				schema.addOption(new StringConfigOption("server.database-url", StringUtil
						.replace(Q_JDBC_URL, "%%DBNAME%%", dbChoiceStr),
						"jdbc:postgresql://" + dbHost + ":" + dbPort + "/" + dbName + PGSQL_PROTOCOL));
				schema.addOption(new HiddenConfigOption("server.database-driver",
						"org.postgresql.Driver"));
				schema.addOption(new HiddenConfigOption("server.quartzDelegate",
						"org.quartz.impl.jdbcjobstore.PostgreSQLDelegate"));
				schema.addOption(new HiddenConfigOption("server.hibernate.dialect",
						"org.hyperic.hibernate.dialect.PostgreSQLDialect"));
				schema.addOption(new HiddenConfigOption("server.connection-validation-sql",
						"select 1"));
			} 
			else {
				if (!installMode.isQuick()) {
					// In "full" mode, we even let them pick the pgsql port
					schema.addOption(new PortConfigOption("server.postgresql.port",
							Q_POSTGRESQL_PORT, new Integer(9432)));
				} else {
					schema.addOption(new HiddenConfigOption("server.postgresql.port", "9432"));
				}

				schema.addOption(new HiddenConfigOption("server.database-driver",
						"org.postgresql.Driver"));
				schema.addOption(new HiddenConfigOption("server.quartzDelegate",
						"org.quartz.impl.jdbcjobstore.PostgreSQLDelegate"));
				schema.addOption(new HiddenConfigOption("server.hibernate.dialect",
						"org.hyperic.hibernate.dialect.PostgreSQLDialect"));
				schema.addOption(new HiddenConfigOption("server.connection-validation-sql",
						"select 1"));
			}

			if (dbChoiceStr.startsWith(DBC_BUILTIN)) {
				schema.addOption(new HiddenConfigOption("server.database-user", "hqadmin"));
				schema.addOption(new HiddenConfigOption("server.database-password", "hqadmin"));
			} else {
				schema.addOption(new StringConfigOption("server.database-user", Q_JDBC_USER));
				passwordOption = new StringConfigOption("server.database-password",
						Q_JDBC_PASSWORD);
				passwordOption.setSecret(true);
				schema.addOption(passwordOption);
			}
			schema.addOption(new YesNoConfigOption("server.encryption-key.auto", Q_ENCRYPTION_KEY_CREATE,
					YesNoConfigOption.YES));
			break;

		case 9:
			dbChoiceStr = previous.getValue("server.database.choice");
			if(!dbChoiceStr.startsWith(DBC_BUILTIN)) {
				try {
			       	 FileWriter fstream = new FileWriter("/tmp/out.txt");
			       	  BufferedWriter out = new BufferedWriter(fstream);
			       	
			           	 out.write(previous.getValue("server.database-url") + "\n");
			           	out.write(previous.getValue("server.database-user") + "\n");
			           	out.write(previous.getValue("server.database-password") + "\n");
			      	  out.close();

			       	}
			       	catch (Exception e) {
							// TODO: handle exception
						}
				boolean exists = InstallDBUtil.checkConnectionExists(previous.getValue("server.database-url"), previous.getValue("server.database-user"),
						previous.getValue("server.database-password"));
				if (!exists) {
					//If we cannot connect the data base we need to return to step 7 
					throw new ReturnStepsException("Error connecting to the database, please enter the database information again - ", 7);
				}
			}
			if("yes".equalsIgnoreCase(previous.getValue("server.encryption-key.auto"))) {
				//Create an auto generated random key
				String encryptionKey = UUID.randomUUID().toString().substring(0, 13).replaceAll("-", "");
				schema.addOption(new HiddenConfigOption("server.encryption-key", encryptionKey));
				break;
			}
			StringConfigOption encryptionKeyOption = new StringConfigOption(
					"server.encryption-key", Q_ENCRYPTION_KEY);
			encryptionKeyOption.setMinLength(8);
			schema.addOption(encryptionKeyOption);
			break;

		case 10:
			// Get encryption key
			String encryptionKey = previous.getValue("server.encryption-key");

			// Encrypt database password
			String encryptedPw = encryptPassword("PBEWithMD5AndDES", encryptionKey, previous
					.getValue("server.database-password")).replaceAll("\\r|\\n", "");
			schema.addOption(new HiddenConfigOption("server.encryption-key", encryptionKey));
			//Make this optional for non-interactive installers so the default value created here will be used instead
			HiddenConfigOption encryptedPwOption = new HiddenConfigOption("server.database-password-encrypted",
					encryptedPw.toString());
			encryptedPwOption.setOptional(true);
			schema.addOption(encryptedPwOption);


			senderChoice = previous.getValue("server.mail.sender");
			// dont ask about admin username if this is an HA node
			// this should have already been set up
			// We always ask for username and password now per HQ-3627
			usernameOption = new AdminUsernameConfigOption("server.admin.username", Q_ADMIN_USER, "hqadmin");

			schema.addOption(usernameOption);

			passwordOption = new StringConfigOption("server.admin.password", Q_ADMIN_PASSWORD, null);

			passwordOption.setSecret(true);
			passwordOption.setMinLength(6);
			passwordOption.setMaxLength(40);
			schema.addOption(passwordOption);

			// probably shouldn't auto enter the email value, since we're asking for username...
			schema.addOption(new StringConfigOption("server.admin.email", Q_ADMIN_EMAIL, senderChoice));

			break;

		case 11:
			// For servers using the builtinDB we have only gotten the port
			// at
			// this point. Now we setup the url based on the port selection
			dbChoiceStr = previous.getValue("server.database.choice");
			if (dbChoiceStr.startsWith(DBC_BUILTIN)) {
				String pgport = previous.getValue("server.postgresql.port");
				schema.addOption(new HiddenConfigOption("server.database-url",
						"jdbc:postgresql://127.0.0.1:" + pgport + "/hqdb" + PGSQL_PROTOCOL));
			}
			break;

		case 12:
			// Now that they have made their jdbc selections, do a sanity
			// check:
			// If we are in "quick" mode and the database already exists,
			// then STOP ask the user what to do.
			dbChoiceStr = previous.getValue("server.database.choice");
			if (!dbChoiceStr.startsWith(DBC_BUILTIN)) {
				if (installMode.isQuick())
				{
					// Like Built-in DB, overwrite the db - in case of silent install with exists db 
					schema.addOption(new HiddenConfigOption(SERVER_DATABASE_UPGRADE_CHOICE,
							DB_CHOICE_OVERWRITE));

				}else{
					if (databaseExists(previous)) {
						// Bug 9722 only check for db upgrade if this isnt an HA
						// node
						schema.addOption(new EnumerationConfigOption(
								SERVER_DATABASE_UPGRADE_CHOICE, Q_OVERWRITE_DB, DB_CHOICE_CANCEL,
								DB_CHOICES, DB_CHOICE_OVERWRITE));
					} else {
						schema.addOption(new HiddenConfigOption(SERVER_DATABASE_UPGRADE_CHOICE,
								DB_CHOICE_OVERWRITE));
					}
				}
			} else {
				// Built-in DB, overwrite the db
				schema.addOption(new HiddenConfigOption(SERVER_DATABASE_UPGRADE_CHOICE,
						DB_CHOICE_OVERWRITE));
			}

			break;

		case 13:
			String dbUpgradeChoice = previous.getValue(SERVER_DATABASE_UPGRADE_CHOICE);
			if (dbUpgradeChoice.equals(DB_CHOICE_OVERWRITE)) {
				schema.addOption(new HiddenConfigOption("server.database.create",
						YesNoConfigOption.YES));
			} else if (dbUpgradeChoice.equals(DB_CHOICE_UPGRADE)) {
				schema.addOption(new HiddenConfigOption("server.database.upgrade",
						YesNoConfigOption.YES));
			} else {
				throw new EarlyExitException("No modifications made to existing database.  "
						+ "Exiting installer.");
			}
			break;    

		case 14:
			if(isEEInstall) {
				schema.addOption(new HiddenConfigOption("accept.eula",YesNoConfigOption.NO));
			}
			break;
		default:
			return null;
		}

		return schema;
	}

	private String computeBaseUrl(String scheme, String fqdn, String port, String defaultPort) {
		if (fqdn == null) {
			return null;
		}

		StringBuffer buf = new StringBuffer(scheme);
		buf.append("://");
		buf.append(fqdn);
		if (port != null && !port.equals(defaultPort)) {
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
		} else {
			domainname = fqdn;
		}
		return domainname;
	}

	protected String computeFQDN() {
		String fqdn = null;
		try {
			fqdn = new Sigar().getFQDN();
		} catch (SigarException e) {
			// this machine is seriously misconfigured. that's
			// ok, you just don't get a default. have fun
			// typing!
		}
		// Actually, just assume localhost if no fqdn
		if (fqdn == null || fqdn.length() == 0)
			fqdn = "localhost";
		return fqdn;
	}

	protected boolean serverAlreadyInstalled(String dir) {
		String serverFile = dir + File.separator + "server-" + getProjectProperty("version") +
				File.separator + "hq-engine";
		if (Boolean.parseBoolean(getProjectProperty("eula.present"))) {
			serverFile = dir + File.separator + "server-" + getProjectProperty("version") + "-EE" +
					File.separator + "hq-engine";
		}
		File f = new File(serverFile);
		return f.exists();
	}

	public boolean haveLocalMTA() {
		InetPortPinger ip = new InetPortPinger("127.0.0.1", 25, 10);
		return ip.check();
	}

	public static final String[] MARKER_FILES = { "bin/hq-server.sh",
		"bin/hq-server.exe",
		"bin/hq-server.bat",
		"bin/ams-server.sh",
	"bin/ams-server.exe" };

	@Override
	protected String[] getMarkerFiles() {
		return MARKER_FILES;
	}

	@Override
	public void canUpgrade(String dir) throws InvalidOptionValueException {
		// If the 'dir' points to a pointbase-backed installation, we can't
		// upgrade it.

		// First we look in conf/hq-server.conf if it exists. If this is an
		// older release, we look in hq-ds.xml file for the <connection-url>
		// element.
		File confFile = new File(dir, StringUtil.normalizePath("conf/hq-server.conf"));
		if (confFile.exists()) {
			Properties props = new Properties();
			FileInputStream fi = null;
			try {
				fi = new FileInputStream(confFile.getAbsolutePath());
				props.load(fi);
			} catch (IOException e) {
				throw new InvalidOptionValueException("Error reading hq-server.conf file: " +
						confFile.getAbsolutePath() + e.getMessage());
			} finally {
				if (fi != null) {
					try {
						fi.close();
					} catch (IOException e) {
					}
				}
			}
			String driverClass = props.getProperty("server.database-driver");
			if (driverClass == null) {
				throw new InvalidOptionValueException(
						"No server.database-driver property found in: " + confFile.getAbsolutePath());
			}
			if (driverClass.indexOf("com.pointbase.") != -1) {
				throw new InvalidOptionValueException(
						"Cannot upgrade HQ server: upgrade not supported for "
								+ "HQ servers that use the PointBase database");
			}
			return;
		} else {
			throw new EarlyExitException("__ll__\nCannot upgrade HQ server\n(file not found: " +
					confFile.getAbsolutePath() +
					")\n\nOnly versions 1.3.x and higher can " +
					"be upgraded using the installer.\nTo upgrade earlier " +
					"versions, follow the instructions in README.txt\n\n__ll__");
		}
	}

	@Override
	public String getProductInstallDir(ConfigResponse config) {
		String installDir = getInstallDir(config);
		if (Boolean.parseBoolean(getProjectProperty("eula.present"))) {
			return installDir + getBaseName() + "-" + getProjectProperty("version") + "-EE" +
					File.separator;
		} else {
			return installDir + getBaseName() + "-" + getProjectProperty("version") +
					File.separator;
		}
	}

	@Override
	public String getCompletionText(ConfigResponse config) {
		StringBuffer s = new StringBuffer();
		String sp = File.separator;
		String startup = getProductInstallDir(config);
		startup += "bin" + sp + PN.toLowerCase() + "-server" + getExtension();
		s.append("__ll__").append(getServerStartupText(startup).toString());
		if (isUpgrade()) {
			s.append(nl).append(nl).append(" Your HQ server has been successfully upgraded.")
			.append(nl).append(nl).append(
					" Once you start up your HQ server, you can log in using any of your existing")
					.append(nl).append(" HQ user accounts.");
		} else if (isDBUpgrade(config)) {
			s.append(nl).append(nl).append(
					" Once the HQ server reports that it has successfully started, you can log in")
					.append(nl).append(" to your HQ server at: ").append(nl).append(nl).append("  ")
					.append(config.getValue("server.webapp.baseurl")).append(nl).append(nl).append(
							" You can log in using any of your existing HQ user accounts.");

		} else {
			// A new/fresh install
			s
			.append(nl)
			.append(nl)
			.append(
					" Once the HQ server reports that it has successfully started, you can log in")
					.append(nl)
					.append(" to your HQ server at: ")
					.append(nl)
					.append(nl)
					.append("  ")
					.append(config.getValue("server.webapp.baseurl"))
					.append(nl)
					.append("  username: ")
					.append(config.getValue("server.admin.username"))
					.append(nl)
					.append(nl)
					.append(
							" To change your password, log in to the HQ server, click the \"Administration\"")
							.append(nl).append(
									" link, choose \"List Users\", then click on the \"hqadmin\" user.");
		}
		s.append(nl).append("__ll__");
		return s.toString();
	}

	private StringBuffer getServerStartupText(String startup) {
		StringBuffer s = new StringBuffer();
		s.append(nl);
		if (JDK.IS_WIN32) {
			s
			.append(
					" You should now install the HQ server as a Windows Service using this command:")
					.append(nl)
					.append(nl)
					.append("  ")
					.append(startup)
					.append(" install")
					.append(nl)
					.append(nl)
					.append(
							" You can then use the Service Control Manager (Control Panel->Services) to ")
							.append(nl)
							.append(
									" start the HQ server.  Note that the first time the HQ server starts up it may")
									.append(nl)
									.append(
											" take several minutes to initialize.  Subsequent startups will be much faster.");

		} else {
			s
			.append(nl)
			.append(" You can now start your HQ server by running this command:")
			.append(nl)
			.append(nl)
			.append("  ")
			.append(startup)
			.append(" start")
			.append(nl)
			.append(nl)
			.append(
					" Note that the first time the HQ server starts up it may take several minutes")
					.append(nl).append(" to initialize.  Subsequent startups will be much faster.");
		}
		return s;
	}

	private String encryptPassword(String algorithm, String encryptionKey, String clearTextPassword) {
		return SecurityUtil.encrypt(algorithm, encryptionKey, clearTextPassword);
	}

	protected boolean databaseExists(ConfigResponse config) throws EarlyExitException {

		String user = config.getValue("server.database-user");
		String password = config.getValue("server.database-password");
		String url = config.getValue("server.database-url");

		try {
			return InstallDBUtil.checkTableExists(url, user, password, "EAM_CONFIG_PROPS");
		} catch (DriverLoadException e) {
			throw new EarlyExitException("Error connecting to database " + "(" + url + "): " +
					e.getMessage());
		} catch (SQLException e) {
			throw new EarlyExitException("Error checking for existing " + "database: " +
					e.getMessage());
		}
	}

	private boolean isDBUpgrade(ConfigResponse config) {
		String dbUpgrade = config.getValue("server.database.upgrade");
		return (dbUpgrade != null && dbUpgrade.equals(YesNoConfigOption.YES));
	}

	protected boolean getReleaseHasBuiltinDB() {
		File hqdbDir = new File(getProjectProperty("install.dir") + File.separator + "data" +
				File.separator + "hqdb");
		return (hqdbDir.exists() && hqdbDir.isDirectory() && hqdbDir.canRead());
	}

	private class InstallMode {
		boolean _postgresQuickMode = false;
		boolean _quickMode = false;

		InstallMode(String mode) {
			_postgresQuickMode = mode.equals(INSTALLMODE_POSTGRESQL);
			_quickMode = mode.equals(INSTALLMODE_QUICK);
		}

		boolean isPostgres() {
			return _postgresQuickMode;
		}

		
		boolean isQuick() {
			return _postgresQuickMode || _quickMode;
		}
	}

}
