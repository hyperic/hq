package org.hyperic.hq.appdef.server.session;

import junit.framework.TestCase;

import org.hyperic.hq.appdef.shared.ServerValue;

public class ServerTest extends TestCase {

    private static final String NAME = "Fred";
    private static final String DESC = "Caveman";
    private static final String LOC = "Bedrock";
    private static final String INST_PATH = "/down/the/street";
    private static final String AID = "Flintstone";
    
    public void testNullServerAndNullServerValue() throws Exception {
        
        Server s = new Server(new Integer(1));
        ServerValue sv = new ServerValue();
        
        assertTrue(s.matchesValueObject(sv));
    }
    
    public void testNameMatch() throws Exception {
        Server s = new Server(new Integer(1));
        s.setName(NAME);
        ServerValue sv = new ServerValue();
        assertFalse(s.matchesValueObject(sv));
        
        sv.setName(NAME);
        assertTrue(s.matchesValueObject(sv));
        
        s.setName(null);
        assertFalse(s.matchesValueObject(sv));
        
        sv.setName(null);
        assertTrue(s.matchesValueObject(sv));
    }
    
    public void testDescriptionMatch() throws Exception {
        Server s = new Server(new Integer(1));
        s.setDescription(DESC);
        ServerValue sv = new ServerValue();
        assertFalse(s.matchesValueObject(sv));
        
        sv.setDescription(DESC);
        assertTrue(s.matchesValueObject(sv));
        
        s.setDescription(null);
        assertFalse(s.matchesValueObject(sv));
        
        sv.setDescription(null);
        assertTrue(s.matchesValueObject(sv));
    }
    
    public void testLocationMatch() throws Exception {
        Server s = new Server(new Integer(1));
        s.setLocation(LOC);
        ServerValue sv = new ServerValue();
        assertFalse(s.matchesValueObject(sv));
        
        sv.setLocation(LOC);
        assertTrue(s.matchesValueObject(sv));
        
        s.setLocation(null);
        assertFalse(s.matchesValueObject(sv));
        
        sv.setLocation(null);
        assertTrue(s.matchesValueObject(sv));
    }
    
    public void testRuntimeDiscoveryMatch() throws Exception {
        Server s = new Server(new Integer(1));
        s.setRuntimeAutodiscovery(false);
        ServerValue sv = new ServerValue();
        sv.setRuntimeAutodiscovery(false);
        assertTrue(s.matchesValueObject(sv));
        
        s.setRuntimeAutodiscovery(true);
        assertFalse(s.matchesValueObject(sv));
        
        sv.setRuntimeAutodiscovery(true);
        assertTrue(s.matchesValueObject(sv));
        
        s.setRuntimeAutodiscovery(false);
        assertFalse(s.matchesValueObject(sv));
    }
    
    public void testInstallPathMatch() throws Exception {
        Server s = new Server(new Integer(1));
        s.setInstallPath(INST_PATH);
        ServerValue sv = new ServerValue();
        assertFalse(s.matchesValueObject(sv));
        
        sv.setInstallPath(INST_PATH);
        assertTrue(s.matchesValueObject(sv));
        
        s.setInstallPath(null);
        assertFalse(s.matchesValueObject(sv));
        
        sv.setInstallPath(null);
        assertTrue(s.matchesValueObject(sv));
    }
    
    public void testAutoInventoryIdentifierMatch() throws Exception {
        Server s = new Server(new Integer(1));
        s.setAutoinventoryIdentifier(AID);
        ServerValue sv = new ServerValue();
        assertFalse(s.matchesValueObject(sv));
        
        sv.setAutoinventoryIdentifier(AID);
        assertTrue(s.matchesValueObject(sv));
        
        s.setAutoinventoryIdentifier(null);
        assertFalse(s.matchesValueObject(sv));
        
        sv.setAutoinventoryIdentifier(null);
        assertTrue(s.matchesValueObject(sv));
    }
    
    public void testAllMatching() throws Exception {
        Server s = new Server(new Integer(1));
        s.setName(NAME);
        ServerValue sv = new ServerValue();
        sv.setName(NAME);
        assertTrue(s.matchesValueObject(sv));
        
        s.setDescription(DESC);
        sv.setDescription(DESC);
        assertTrue(s.matchesValueObject(sv));
        
        s.setLocation(LOC);
        sv.setLocation(LOC);
        assertTrue(s.matchesValueObject(sv));
        
        s.setRuntimeAutodiscovery(false);
        sv.setRuntimeAutodiscovery(false);
        assertTrue(s.matchesValueObject(sv));
        
        s.setRuntimeAutodiscovery(true);
        sv.setRuntimeAutodiscovery(true);
        assertTrue(s.matchesValueObject(sv));
        
        s.setInstallPath(INST_PATH);
        sv.setInstallPath(INST_PATH);
        assertTrue(s.matchesValueObject(sv));
        
        s.setAutoinventoryIdentifier(AID);
        sv.setAutoinventoryIdentifier(AID);
        assertTrue(s.matchesValueObject(sv));
    }
}
