/*
 * NOTE: This copyright does *not* cover user programs that use HQ
 * program services by normal system calls through the application
 * program interfaces provided as part of the Hyperic Plug-in Development
 * Kit or the Hyperic Client Development Kit - this is merely considered
 * normal use of the program, and does *not* fall under the heading of
 * "derived work".
 * 
 * Copyright (C) [2009], SpringSource, Inc.
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
 
package org.hyperic.hq.plugin.websphere;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Properties;

import junit.framework.TestCase;

import org.hyperic.hq.product.ProductPluginManager;

/**
 * Tests various approaches to finding the profile information needed by the WebSphere plugin.
 * 
 * @author trader
 *
 */
public class ProfilesDirectoryTest extends TestCase {

    private File tmpFile;

    private static final String INSTALL_ROOT = "opt" + File.separator + "WebSphere";
    private static final String PROPS_DIR = "properties"; 
    private static final String PROFILES_DIR = "profiles"; 
    private static final String SOAP_PROPS = "soap.client.props";
    private static final String LOOK_FOR = PROPS_DIR + File.separator + SOAP_PROPS;
    private static final String SERVER_ROOT_DIR = "serverRoot" + File.separator + "wasprofiles";
    
    public void setUp() throws Exception {
        super.setUp();
        tmpFile = new File(System.getProperty("java.io.tmpdir"));
        File sigarBin = new File(getClass().getResource("/libsigar-sparc64-solaris.so").getFile()).getParentFile();
		System.setProperty("org.hyperic.sigar.path",sigarBin.getAbsolutePath());
    }

    public void test60Structure() throws Exception {
        
        File defaultProfilePropsDir = null;
        File profilesDir = null;
        File prof1Props = null;
        File prof2Props = null;
        File defProfileSoapProps = null;
        File prof2SoapProps = null;
        File installPropsFor60DefProfile = null;
        File installPropsDefProfile = null;
        File installPropsFor60Profile2 = null;
        File installPropsProfile2 = null;
        
        try {
            // opt/WebSphere/profiles/default/properties structure
            defaultProfilePropsDir =
                createDirectory(INSTALL_ROOT + File.separator +
                                PROFILES_DIR + File.separator +
                                "default" + File.separator + PROPS_DIR);
            assertNotNull(defaultProfilePropsDir);
            assertPath(defaultProfilePropsDir, "opt" + File.separator +
                                               "WebSphere" + File.separator +
                                               "profiles" + File.separator +
                                               "default" + File.separator + "properties");
            
            // /opt/WebSphereprofiles/default/properties/soap.client.props
            defProfileSoapProps = new File(defaultProfilePropsDir, SOAP_PROPS);
            assertFalse(defProfileSoapProps.exists());
            boolean created = defProfileSoapProps.createNewFile();
            assertTrue(created);
            assertPath(defProfileSoapProps, "opt" + File.separator +
                                            "WebSphere" + File.separator +
                                            "profiles" + File.separator +
                                            "default" + File.separator +
                                            "properties" + File.separator + "soap.client.props");

            WebsphereProductPlugin plugin =
                new StubbedWebsphereProductPlugin();
            String installDir =
                defaultProfilePropsDir.getParentFile().getParentFile().getParent();
            assertTrue(installDir.endsWith("opt" + File.separator + "WebSphere"));

            // properties/soap.client.props
            installPropsFor60DefProfile =
                plugin.getInstallPropertiesDirForWAS60(installDir, LOOK_FOR);
            assertEquals(defProfileSoapProps, installPropsFor60DefProfile);

            installPropsDefProfile =
                plugin.getInstallPropertiesDir(installDir, LOOK_FOR);
            assertEquals(defProfileSoapProps, installPropsDefProfile);
            
            // Test with a single alternate profile, moving the props file out
            // of default
            profilesDir = defaultProfilePropsDir.getParentFile().getParentFile();
            assertPath(profilesDir, "opt" + File.separator +
                                    "WebSphere" + File.separator + "profiles");
            prof1Props = new File(profilesDir, "profile1" + File.separator + "properties");
            assertTrue(prof1Props.mkdirs());
            assertPath(prof1Props, "opt" + File.separator +
                                   "WebSphere" + File.separator +
                                   "profiles" + File.separator +
                                   "profile1" + File.separator + "properties");
            prof2Props = new File(profilesDir, "profile2" + File.separator + "properties");
            assertTrue(prof2Props.mkdirs());
            assertPath(prof2Props, "opt" + File.separator +
                                   "WebSphere" + File.separator +
                                   "profiles" + File.separator +
                                   "profile2" + File.separator + "properties");
            
            // Move the soap file to profiles2
            prof2SoapProps = new File(prof2Props, SOAP_PROPS);
            assertTrue(defProfileSoapProps.renameTo(prof2SoapProps));
            assertPath(prof2SoapProps, "opt" + File.separator +
                                       "WebSphere" + File.separator +
                                       "profiles" + File.separator +
                                       "profile2" + File.separator +
                                       "properties" + File.separator + "soap.client.props");
            
            // Verify...
            installPropsFor60Profile2 =
                plugin.getInstallPropertiesDirForWAS60(installDir, LOOK_FOR);
            assertEquals(prof2SoapProps, installPropsFor60Profile2);
            installPropsProfile2 =
                plugin.getInstallPropertiesDir(installDir, LOOK_FOR);
            assertEquals(prof2SoapProps, installPropsProfile2);            
            
            File installPropsFor61 =
                plugin.getInstallPropertiesDirForWAS61(LOOK_FOR);
            assertNull(installPropsFor61);
        } finally {
            if (defProfileSoapProps != null && defProfileSoapProps.exists()) {
                defProfileSoapProps.delete();
            }
            
            if (prof2SoapProps != null && prof2SoapProps.exists()) {
                prof2SoapProps.delete();
            }
            
            // The following is only necessary if the test fails on the "assertEquals"
            if (installPropsFor60DefProfile != null && installPropsFor60DefProfile.exists()) {
                installPropsFor60DefProfile.delete();
            }
            
            if (installPropsDefProfile != null && installPropsDefProfile.exists()) {
                installPropsDefProfile.delete();
            }
            
            if (prof1Props != null && prof1Props.exists()) {
                prof1Props.delete();
                prof1Props.getParentFile().delete();
            }
            
            if (prof2Props != null && prof2Props.exists()) {
                prof2Props.delete();
                prof2Props.getParentFile().delete();
            }
            
            // Delete all directories made.
            if (defaultProfilePropsDir != null && defaultProfilePropsDir.exists()) {
                File toDelete = defaultProfilePropsDir;
                for (int i = 0; i < 5; ++i) {
                    toDelete.delete();
                    toDelete = toDelete.getParentFile();
                }
            }
            
            // Verify cleanup
            assertTrue(tmpFile.exists());
            assertFalse(new File(tmpFile, "opt").exists());
        }
    }

    /**
     * For WAS 6.1, all the WAS 6.0 test scenarios apply, but there is an additional
     * scenario whereby the profile is in some other location, relative to the server
     * root.
     * 
     * @throws Exception
     */
    public void test61Structure() throws Exception {

        File installDir = null;
        File serverRootDir = null;
        File prof1Props = null;
        File prof2Props = null;
        File prof3Props = null;
        File soapPropsFile = null;
        
        List servers = new ArrayList();
        
        WebSphereProcess p1 = new WebSphereProcess();
        p1.serverRoot = tmpFile.getAbsolutePath() + File.separator +
                        "serverRoot" + File.separator +
                        "wasprofiles" + File.separator +
                        "profile1";
        p1.installRoot = tmpFile.getAbsolutePath() + File.separator +
                         INSTALL_ROOT;
        servers.add(p1);
        
        WebSphereProcess p2 = new WebSphereProcess();
        p2.serverRoot = tmpFile.getAbsolutePath() + File.separator +
                        "serverRoot" + File.separator +
                        "wasprofiles" + File.separator +
                        "profile2";
        p2.installRoot = tmpFile.getAbsolutePath() + File.separator +
                         INSTALL_ROOT;
        servers.add(p2);
        
        WebSphereProcess p3 = new WebSphereProcess();
        p3.serverRoot = tmpFile.getAbsolutePath() + File.separator +
                        "serverRoot" + File.separator +
                        "wasprofiles" + File.separator +
                        "profile3";
        p3.installRoot = tmpFile.getAbsolutePath() + File.separator +
                         INSTALL_ROOT;
        servers.add(p3);
        
        // Instantiate the fake plugin with the server descriptors created
        WebsphereProductPlugin plugin =
            new StubbedWebsphereProductPlugin(servers);
        
        try {
            installDir =
                createDirectory(INSTALL_ROOT + File.separator + "install");
            assertNotNull(installDir);
            assertPath(installDir, "opt" + File.separator +
                       "WebSphere" + File.separator + "install");
            
            serverRootDir =
                createDirectory(SERVER_ROOT_DIR);
            assertNotNull(serverRootDir);
            assertTrue(serverRootDir.exists());
            
            prof1Props = new File(serverRootDir, "profile1" + File.separator + PROPS_DIR);
            assertFalse(prof1Props.exists());
            assertTrue(prof1Props.mkdirs());
            assertPath(prof1Props, "serverRoot" + File.separator +
                       "wasprofiles" + File.separator +
                       "profile1" + File.separator + "properties");
            
            prof2Props = new File(serverRootDir, "profile2" + File.separator + PROPS_DIR);
            assertFalse(prof2Props.exists());
            assertTrue(prof2Props.mkdirs());
            assertPath(prof2Props, "serverRoot" + File.separator +
                       "wasprofiles" + File.separator +
                       "profile2" + File.separator + "properties");
            
            prof3Props = new File(serverRootDir, "profile3" + File.separator + PROPS_DIR);
            assertFalse(prof3Props.exists());
            assertTrue(prof3Props.mkdirs());
            assertPath(prof3Props, "serverRoot" + File.separator +
                       "wasprofiles" + File.separator +
                       "profile3" + File.separator + "properties");
            
            // Put the soap props file in profile2
            soapPropsFile = new File(prof2Props, SOAP_PROPS);
            assertFalse(soapPropsFile.exists());
            assertTrue(soapPropsFile.createNewFile());
            
            // Verify
            File foundPropsFile =
                plugin.getInstallPropertiesDirForWAS61(LOOK_FOR);
            assertEquals(soapPropsFile, foundPropsFile);
            
            foundPropsFile =
                plugin.getInstallPropertiesDir(installDir.getAbsolutePath(), LOOK_FOR);
            assertEquals(soapPropsFile, foundPropsFile);
            
        } finally {
            
            installDir.delete();
            installDir.getParentFile().delete();
            installDir.getParentFile().getParentFile().delete();
            
            soapPropsFile.delete();
            prof1Props.delete();
            prof1Props.getParentFile().delete();
            prof2Props.delete();
            prof2Props.getParentFile().delete();
            prof3Props.delete();
            prof3Props.getParentFile().delete();
            
            serverRootDir.delete();
            serverRootDir.getParentFile().delete();
            
            // Verify cleanup
            assertTrue(tmpFile.exists());
            assertFalse(new File(tmpFile, "opt").exists());
            assertFalse(new File(tmpFile, "serverRoot").exists());
        }
    }
    
    /**
     * Test that this does not blow up in a non-WAS environment.  The plugin as a whole should
     * not load, but this test only concerns the WebsphereProductPlugin directory sniffing
     * part.
     * 
     * @throws Exception
     */
    public void testClassPathInNonWASEnv() throws Exception {
        List emptyServers = new ArrayList();
        WebsphereProductPlugin stubbedPlugin = new StubbedWebsphereProductPlugin(emptyServers);
        String[] path = stubbedPlugin.getClassPath(new StubbedProductPluginManager());
        assertNotNull(path);
    }
    
    private File createDirectory(String pathName) throws Exception {
        
        File result = null;
        
        File toCreate = new File(tmpFile, pathName);
        if (toCreate.mkdirs()) {
            result = toCreate;
        }
        
        return result;
    }
    
    private void assertPath(File file, String path) {
        String fullPath = file.getAbsolutePath();
        String tmpFilePath = tmpFile.getAbsolutePath();
        String relPath = fullPath.substring(tmpFilePath.length() + 1);
        assertEquals(relPath, path);
    }
    
    private static class StubbedWebsphereProductPlugin extends WebsphereProductPlugin {
        
        private List serverProcessList;
        
        StubbedWebsphereProductPlugin() {
            super();
            serverProcessList = Collections.EMPTY_LIST;
        }
        
        StubbedWebsphereProductPlugin(List serverProcessList) {
            super();
            this.serverProcessList = serverProcessList;
        }
        
        List getServerProcessList() {
            return serverProcessList;
        }
    }
    
    private static class StubbedProductPluginManager extends ProductPluginManager {
        public Properties getProperties() {
            return new Properties();
        }
    }
}
