package org.hyperic.hq.appdef.server.session;

import java.util.Properties;

import org.hyperic.hq.appdef.shared.AppdefEntityValue;
import org.hyperic.hq.appdef.shared.CPropManager;
import org.hyperic.testSuite.BaseInfrastructureTest;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
/**
 * Integration test of {@link CPropManagerImpl}
 * @author jhickey
 *
 */
public class CPropManagerTest
    extends BaseInfrastructureTest {

    @Autowired
    private CPropManager cPropManager;

    private PlatformType platformType;

    private Platform platform;

    @Before
    public void setUp() throws Exception {
        createAgent("127.0.0.1", 2144, "authToken", "agent123", "5.0");
        this.platformType = createPlatformType("FakeLinux", "test plugin");
        this.platform = createPlatform("agent123", "FakeLinux", "platform1","platform1");
        flushSession();
    }

    @Test
    public void testSetValueFirstTime() throws Exception {
        cPropManager.addKey(platformType, "userName", "A user name");
        flushSession();
        cPropManager.setValue(platform.getEntityId(), platformType.getId(), "userName", "bob");
        AppdefEntityValue appdefVal = new AppdefEntityValue(platform.getEntityId(),
            authzSubjectManager.getOverlordPojo());
        String val = cPropManager.getValue(appdefVal, "userName");
        assertEquals("bob", val);
    }

    @Test
    public void testSetValueUpdate() throws Exception {
        cPropManager.addKey(platformType, "userName", "A user name");
        flushSession();
        AppdefEntityValue appdefVal = new AppdefEntityValue(platform.getEntityId(),
            authzSubjectManager.getOverlordPojo());

        cPropManager.setValue(platform.getEntityId(), platformType.getId(), "userName", "bob");
        String val = cPropManager.getValue(appdefVal, "userName");
        assertEquals("bob", val);
        cPropManager.setValue(platform.getEntityId(), platformType.getId(), "userName", "joe");
        String updatedVal = cPropManager.getValue(appdefVal, "userName");
        assertEquals("joe", updatedVal);
    }

    @Test
    public void testSetValueUpdateToNull() throws Exception {
        cPropManager.addKey(platformType, "userName", "A user name");
        flushSession();
        AppdefEntityValue appdefVal = new AppdefEntityValue(platform.getEntityId(),
            authzSubjectManager.getOverlordPojo());

        cPropManager.setValue(platform.getEntityId(), platformType.getId(), "userName", "bob");
        String val = cPropManager.getValue(appdefVal, "userName");
        assertEquals("bob", val);
        cPropManager.setValue(platform.getEntityId(), platformType.getId(), "userName", null);
        String updatedVal = cPropManager.getValue(appdefVal, "userName");
        assertNull(updatedVal);
    }

    @Test
    public void testSetValueToNullNoChange() throws Exception {
        cPropManager.addKey(platformType, "userName", "A user name");
        flushSession();
        AppdefEntityValue appdefVal = new AppdefEntityValue(platform.getEntityId(),
            authzSubjectManager.getOverlordPojo());

        cPropManager.setValue(platform.getEntityId(), platformType.getId(), "userName", null);
        String val = cPropManager.getValue(appdefVal, "userName");
        assertNull(val);
        cPropManager.setValue(platform.getEntityId(), platformType.getId(), "userName", null);
        String updatedVal = cPropManager.getValue(appdefVal, "userName");
        assertNull(updatedVal);
    }

    @Test
    public void testSetValueUpdateNoChange() throws Exception {
        cPropManager.addKey(platformType, "userName", "A user name");
        flushSession();
        AppdefEntityValue appdefVal = new AppdefEntityValue(platform.getEntityId(),
            authzSubjectManager.getOverlordPojo());

        cPropManager.setValue(platform.getEntityId(), platformType.getId(), "userName", "bob");
        String val = cPropManager.getValue(appdefVal, "userName");
        assertEquals("bob", val);
        cPropManager.setValue(platform.getEntityId(), platformType.getId(), "userName", "bob");
        String updatedVal = cPropManager.getValue(appdefVal, "userName");
        assertEquals("bob", updatedVal);
    }
    
    @Test
    public void testGetEntries() throws Exception {
        cPropManager.addKey(platformType, "something", "A thing");
        flushSession();
        cPropManager.setValue(platform.getEntityId(), platformType.getId(), "something", "nothing");
       
        Properties props = cPropManager.getEntries(platform.getEntityId());
        Properties expected = new Properties();
        expected.setProperty("something", "nothing");
        assertEquals(expected,props);
    }
    
    @Test
    public void testGetEntriesMultipleChunks() throws Exception {
        cPropManager.addKey(platformType, "something", "A thing");
        cPropManager.addKey(platformType, "securityPolicy", "A policy");
        flushSession();
        final StringBuffer giantString = new StringBuffer();
        for(int i=0;i < 1003;i++)  {
            giantString.append('a');
        }
        cPropManager.setValue(platform.getEntityId(), platformType.getId(), "something", giantString.toString());
        cPropManager.setValue(platform.getEntityId(), platformType.getId(), "securityPolicy", "none");
        Properties props = cPropManager.getEntries(platform.getEntityId());
        Properties expected = new Properties();
        expected.setProperty("something", giantString.toString());
        expected.setProperty("securityPolicy", "none");
        assertEquals(expected,props);
    }
    
    @Test
    public void testGetDescEntries() throws Exception {
        cPropManager.addKey(platformType, "something", "A thing");
        cPropManager.addKey(platformType, "securityPolicy", "A policy");
        flushSession();
        final StringBuffer giantString = new StringBuffer();
        for(int i=0;i < 1003;i++)  {
            giantString.append('a');
        }
        cPropManager.setValue(platform.getEntityId(), platformType.getId(), "something", giantString.toString());
        cPropManager.setValue(platform.getEntityId(), platformType.getId(), "securityPolicy", "none");
        Properties props = cPropManager.getDescEntries(platform.getEntityId());
        Properties expected = new Properties();
        expected.setProperty("A thing", giantString.toString());
        expected.setProperty("A policy", "none");
        assertEquals(expected,props);
    }
    
    @Test
    public void testDeleteValues() throws Exception {
        cPropManager.addKey(platformType, "something", "A thing");
        flushSession();
        cPropManager.setValue(platform.getEntityId(), platformType.getId(), "something", "nothing");
        cPropManager.deleteValues(platform.getEntityId().getType(), platform.getEntityId().getID());
        assertTrue(cPropManager.getEntries(platform.getEntityId()).isEmpty());
    }
   
}
