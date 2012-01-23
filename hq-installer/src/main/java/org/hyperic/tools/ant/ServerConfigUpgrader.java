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

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashSet;
import java.util.Properties;
import java.util.Set;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Task;
import org.jasypt.encryption.pbe.StandardPBEStringEncryptor;
import org.jasypt.properties.PropertyValueEncryptionUtils;
import org.springframework.util.StringUtils;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 * Ant task responsible for applying any necessary transformations to an
 * existing hq-server.conf file during a server upgrade
 * @author jhickey
 * 
 */
public class ServerConfigUpgrader
    extends Task {

    private String existingConfigFile;
    private String newConfigFile;
    private String upgradeDir;

    /**
     * Mail server config properties to grab from the old
     * jboss-service-events.xml file and add to hq-server.conf
     */
    private static final Set<String> UPGRADE_MAIL_PROPERTIES = new HashSet<String>();
    static {
        // server.mail.host is already in hq-server.conf. Grab anything else
        // that may have been configured for mail
        UPGRADE_MAIL_PROPERTIES.add("mail.smtp.auth");
        UPGRADE_MAIL_PROPERTIES.add("mail.smtp.port");
        UPGRADE_MAIL_PROPERTIES.add("mail.smtp.starttls.enable");
        UPGRADE_MAIL_PROPERTIES.add("mail.smtp.socketFactory.port");
        UPGRADE_MAIL_PROPERTIES.add("mail.smtp.socketFactory.fallback");
        UPGRADE_MAIL_PROPERTIES.add("mail.smtp.socketFactory.class");
    }

    /**
     * hq-server.conf properties no longer used. While not harmful to leave them
     * in, we take them out to avoid clutter
     */
    private static final Set<String> UNUSED_PROPS = new HashSet<String>();
    static {
        UNUSED_PROPS.add("hq-engine.jnp.port");
        UNUSED_PROPS.add("hq-engine.server.port");
    }

    @Override
	public void execute() throws BuildException {
        Properties config = upgradeServerConfig();
        parseMailConfig(config);
        exportConfig(config);
    }

    void exportConfig(Properties config) {
        FileOutputStream fo = null;
        try {
            fo = new FileOutputStream(newConfigFile);
            config.store(fo, null);
        } catch (IOException e) {
            throw new BuildException("Error storing server config to " + newConfigFile, e);
        } finally {
            if (fo != null) {
                try {
                    fo.close();
                } catch (IOException e) {
                }
            }
        }
    }

    void parseMailConfig(Properties existingConfig) {
        FileInputStream mailConfigInputStream = null;
        File mailServiceConfig = new File(upgradeDir + "/conf/templates/jboss-service-events.xml");
        try {
            mailConfigInputStream = new FileInputStream(mailServiceConfig);
        } catch (FileNotFoundException e) {
            // if we are upgrading a server after 4.3, the JBoss MailService
            // config will not be present
            return;
        }
        log("Parsing jboss mail service configuration=" + mailServiceConfig.getAbsolutePath());
        try {
            parseMailConfig(mailConfigInputStream, existingConfig);
        } finally {
            if (mailConfigInputStream != null) {
                try {
                    mailConfigInputStream.close();
                } catch (IOException e) {
                }
            }
        }
    }

    void parseMailConfig(InputStream mailConfigInputStream, Properties serverConfig) {
        Document mailDocument = loadDocument(mailConfigInputStream);
        NodeList mbeans = mailDocument.getElementsByTagName("mbean");
        for (int i = 0; i < mbeans.getLength(); i++) {
            Node mbean = mbeans.item(i);
            String name = mbean.getAttributes().getNamedItem("name").getNodeValue();
            if ("jboss:service=SpiderMail".equals(name)) {
                NodeList mbeanElements = mbean.getChildNodes();
                for (int j = 0; j < mbeanElements.getLength(); j++) {
                    Node mbeanElement = mbeanElements.item(j);
                    if (mbeanElement.getAttributes() == null) {
                        continue;
                    }
                    String childName = mbeanElement.getAttributes().getNamedItem("name")
                        .getNodeValue();
                    if ("User".equals(childName)) {
                        String username = mbeanElement.getFirstChild().getNodeValue();
                        serverConfig.put("mail.user", username);
                    } else if ("Password".equals(childName)) {
                        String password = mbeanElement.getFirstChild().getNodeValue();
                        serverConfig.put("mail.password", password);
                    } else if ("Configuration".equals(childName)) {
                        parseMailConfigurationNode(mbeanElement, serverConfig);
                    }
                }
            }
        }
    }

    private void parseMailConfigurationNode(Node mbeanElement, Properties serverConfig) {
        NodeList childNodes = mbeanElement.getChildNodes();
        for (int i = 0; i < childNodes.getLength(); i++) {
            Node childNode = childNodes.item(i);
            if ("configuration".equals(childNode.getNodeName())) {
                NodeList configNodes = childNode.getChildNodes();
                for (int k = 0; k < configNodes.getLength(); k++) {
                    Node configNode = configNodes.item(k);
                    if ("property".equals(configNode.getNodeName())) {
                        String propName = configNode.getAttributes().getNamedItem("name")
                            .getNodeValue();
                        String propValue = configNode.getAttributes().getNamedItem("value")
                            .getNodeValue();
                        if (UPGRADE_MAIL_PROPERTIES.contains(propName)) {
                            serverConfig.put(propName, propValue);
                        }
                    }
                }
            }
        }

    }

    private Document loadDocument(InputStream input) throws BuildException {
        try {
            DocumentBuilder dom = DocumentBuilderFactory.newInstance().newDocumentBuilder();
            return dom.parse(input);
        } catch (Exception e) {
            throw new BuildException("Error parsing document", e);
        }
    }

    Properties upgradeServerConfig(InputStream confFile) throws IOException {
        Properties serverProps = new Properties();
        serverProps.load(confFile);
        removeOldProps(serverProps);
        addNewProps(serverProps);
        return serverProps;
    }

    private void removeOldProps(Properties serverProps) {
        // remove props no longer needed
        for (String unusedProp : UNUSED_PROPS) {
            if (serverProps.containsKey(unusedProp)) {
                serverProps.remove(unusedProp);
            }
        }
    }

    private void addNewProps(Properties serverProps) {
        // Add new properties that we've placed in hq-server.conf
        // Add protocol version to upgraded servers that use the embedded
        // database
        String jdbcUrl = serverProps.getProperty("server.database-url");
        if (jdbcUrl.startsWith("jdbc:postgresql:") && !jdbcUrl.endsWith("?protocolVersion=2")) {
            serverProps.setProperty("server.database-url", jdbcUrl + "?protocolVersion=2");
        }

        String dbProp = serverProps.getProperty("server.database");
        if (dbProp == null) {
            // this shouldn't happen, but return to avoid NPEs if it does
            return;
        }
        // Add new DB connection validation sql
        if (serverProps.getProperty("server.connection-validation-sql") == null) {
            String validationSQL = "select 1";
            if (dbProp.startsWith("Oracle")) {
                validationSQL += " from dual";
            }
            serverProps.setProperty("server.connection-validation-sql", validationSQL);
        }
        // Add new hibernate dialect property
        if (serverProps.getProperty("server.hibernate.dialect") == null) {
            if (dbProp.startsWith("Oracle")) {
                serverProps.setProperty("server.hibernate.dialect",
                    "org.hyperic.hibernate.dialect.Oracle9Dialect");
            } else if (dbProp.equals("MySQL")) {
                serverProps.setProperty("server.hibernate.dialect",
                    "org.hyperic.hibernate.dialect.MySQL5InnoDBDialect");
            } else {
                serverProps.setProperty("server.hibernate.dialect",
                    "org.hyperic.hibernate.dialect.PostgreSQLDialect");
            }
        }
        if (serverProps.getProperty("server.encryption-key") == null) {
            serverProps.setProperty("server.encryption-key", "defaultkey");
            serverProps.setProperty("server.database-password", encryptPassword("defaultkey",
                serverProps.getProperty("server.database-password")));
        }
        
        // Add new SSL properties for upgrade
        if (!StringUtils.hasText(serverProps.getProperty("accept.unverified.certificates"))) {
        	serverProps.setProperty("accept.unverified.certificates", "true");
        }

        if (!StringUtils.hasText(serverProps.getProperty("server.keystore.path"))) {
        	serverProps.setProperty("server.keystore.path", "../../conf/hyperic.keystore");
        }
        
        if (!StringUtils.hasText(serverProps.getProperty("server.keystore.password"))) {
        	serverProps.setProperty("server.keystore.password", "hyperic");
        }
        
        if (!StringUtils.hasText(serverProps.getProperty("tomcat.maxthreads"))) {
        	serverProps.setProperty("tomcat.maxthreads", "500");
        }
        
        if (!StringUtils.hasText(serverProps.getProperty("tomcat.minsparethreads"))) {
        	serverProps.setProperty("tomcat.minsparethreads", "50");
        }
        
        if (!StringUtils.hasText(serverProps.getProperty("server.jms.usejmx"))) {
        	serverProps.setProperty("server.jms.usejmx", "false");
        }
        
        if (!StringUtils.hasText(serverProps.getProperty("server.jms.jmxport"))) {
        	serverProps.setProperty("server.jms.jmxport", "1099");
        }
    }

    private String encryptPassword(String encryptionKey, String clearTextPassword) {
        // TODO: This needs to be refactored into a security utility class
        StandardPBEStringEncryptor encryptor = new StandardPBEStringEncryptor();
        encryptor.setPassword(encryptionKey);
        encryptor.setAlgorithm("PBEWithMD5AndDES");
        return PropertyValueEncryptionUtils.encrypt(clearTextPassword, encryptor);
    }

    Properties upgradeServerConfig() {
        Properties config;
        FileInputStream confFileInputStream = null;
        try {
            confFileInputStream = new FileInputStream(new File(existingConfigFile));
            config = upgradeServerConfig(confFileInputStream);
        } catch (IOException e) {
            throw new BuildException("Error loading existing config from " + existingConfigFile, e);
        } finally {
            if (confFileInputStream != null) {
                try {
                    confFileInputStream.close();
                } catch (IOException e) {
                }
            }
        }
        return config;
    }

    public void setExisting(String existingConfigFile) {
        this.existingConfigFile = existingConfigFile;
    }

    public void setNew(String newConfigFile) {
        this.newConfigFile = newConfigFile;
    }

    public void setUpgradeDir(String upgradeDir) {
        this.upgradeDir = upgradeDir;
    }

}
