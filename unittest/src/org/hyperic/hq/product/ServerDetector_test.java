package org.hyperic.hq.product;

import java.io.File;
import java.util.Properties;

import junit.framework.TestCase;

public class ServerDetector_test
    extends TestCase
{
    private File tmpDir;
    private File simpleFile;
    private File deepFile;

    protected void setUp() throws Exception {
        String tmpDirStr = System.getProperty("java.io.tmpdir");
        if (tmpDirStr != null) {
            try {
                tmpDir = new File(tmpDirStr);
                simpleFile = new File(tmpDir, "installFile");
                simpleFile.createNewFile();
                File d = new File(tmpDir, "deep/dir/structure");
                d.mkdirs();
                deepFile = new File(d, "otherInstallFile");
                deepFile.createNewFile();
            } catch (Exception e) {
                tmpDir = null;
                simpleFile = null;
                deepFile = null;
            }
        } else {
            tmpDir = null;
            simpleFile = null;
            deepFile = null;
        }
    }
    
    public void testDefault() throws Exception {
        assertNotNull(tmpDir);
        assertNotNull(simpleFile);
        
        Properties props = new Properties();
        props.setProperty(TestServerDetector.getVersionFile(), simpleFile.getName());
        TestServerDetector tsd = new TestServerDetector(props);

        assertTrue(tsd.isInstallTypeVersion(simpleFile.getParent()));
    }
    
    public void testMatch() throws Exception {
        assertNotNull(tmpDir);
        assertNotNull(simpleFile);
        
        // substring match
        Properties props = new Properties();
        props.setProperty(TestServerDetector.getVersionFile(), simpleFile.getName());
        String installPath = simpleFile.getParent();
        TestServerDetector tsd;
        if (installPath != null && installPath.length() > 2) {
            String substringMatch = installPath.substring(1, installPath.length() - 2);
            props.setProperty(TestServerDetector.getInstallPathMatch(), substringMatch);
            tsd = new TestServerDetector(props);

            assertTrue(tsd.isInstallTypeVersion(simpleFile.getParent()));
        }
        
        // full match
        props.setProperty(TestServerDetector.getInstallPathMatch(), installPath);
        tsd = new TestServerDetector(props);

        assertTrue(tsd.isInstallTypeVersion(installPath));
    }
    
    public void testNoMatch() throws Exception {
        assertNotNull(tmpDir);
        assertNotNull(simpleFile);
        
        // substring match
        Properties props = new Properties();
        props.setProperty(TestServerDetector.getVersionFile(), simpleFile.getName());
        String installPath = simpleFile.getParent();
        TestServerDetector tsd;
        if (installPath != null && installPath.length() > 2) {
            String substringMatch = installPath.substring(1, installPath.length() - 2);
            props.setProperty(TestServerDetector.getInstallPathNoMatch(), substringMatch);
            tsd = new TestServerDetector(props);

            assertFalse(tsd.isInstallTypeVersion(installPath));
        }
        
        // full match
        props.setProperty(TestServerDetector.getInstallPathNoMatch(), installPath);
        tsd = new TestServerDetector(props);

        assertFalse(tsd.isInstallTypeVersion(installPath));
    }
    
    public void testMatchAndNoMatchOnSameString() throws Exception {
        assertNotNull(tmpDir);
        assertNotNull(simpleFile);
        
        // substring match
        Properties props = new Properties();
        props.setProperty(TestServerDetector.getVersionFile(), simpleFile.getName());
        String installPath = simpleFile.getParent();
        TestServerDetector tsd;
        
        // full match and no match -- logical contradiction, should fail
        props.setProperty(TestServerDetector.getInstallPathMatch(), installPath);
        props.setProperty(TestServerDetector.getInstallPathNoMatch(), installPath);
        tsd = new TestServerDetector(props);

        assertFalse(tsd.isInstallTypeVersion(installPath));
    }
    
    public void testMatchAndNoMatchOnDifferentStrings() throws Exception {
        assertNotNull(tmpDir);
        assertNotNull(simpleFile);
        
        // substring match
        Properties props = new Properties();
        props.setProperty(TestServerDetector.getVersionFile(), simpleFile.getName());
        String installPath = simpleFile.getParent();
        String badPath = "gibberish";
        TestServerDetector tsd;
        
        props.setProperty(TestServerDetector.getInstallPathMatch(), installPath);
        props.setProperty(TestServerDetector.getInstallPathNoMatch(), badPath);
        tsd = new TestServerDetector(props);

        assertTrue(tsd.isInstallTypeVersion(installPath));
    }
    
    public void testRecursiveMatch() throws Exception {
        assertNotNull(tmpDir);
        assertNotNull(deepFile);
        
        // substring match
        Properties props = new Properties();
        props.setProperty(TestServerDetector.getVersionFile(), "**/" + deepFile.getName());
        String installPath = simpleFile.getParent();
        TestServerDetector tsd;
        
        // full match and no match -- logical contradiction, should fail
        props.setProperty(TestServerDetector.getInstallPathMatch(), installPath);
        tsd = new TestServerDetector(props);

        assertTrue(tsd.isInstallTypeVersion(installPath));
    }
    
    public void testRecursiveNoMatch() throws Exception {
        assertNotNull(tmpDir);
        assertNotNull(deepFile);
        
        // substring match
        Properties props = new Properties();
        props.setProperty(TestServerDetector.getVersionFile(), "**/" + deepFile.getName());
        String installPath = simpleFile.getParent();
        TestServerDetector tsd;
        
        // full match and no match -- logical contradiction, should fail
        props.setProperty(TestServerDetector.getInstallPathNoMatch(), "gobbledygook");
        tsd = new TestServerDetector(props);

        assertTrue(tsd.isInstallTypeVersion(installPath));
    }
    
    private static class TestServerDetector extends ServerDetector {
        
        private Properties props;

        public TestServerDetector(Properties props) {
            this.props = props;
        }
        
        public String getTypeProperty(String key) {
            return props.getProperty(key);
        }
        
        // Expose some protected constants
        public static String getVersionFile() {
            return VERSION_FILE;
        }
        
        public static String getInstallPathMatch() {
            return INSTALLPATH_MATCH;
        }
        
        public static String getInstallPathNoMatch() {
            return INSTALLPATH_NOMATCH;
        }
    }
}
