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

package org.hyperic.hq.appdef.shared;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

import junit.framework.TestCase;

/**
 * Tests the AppdefEntityID class. 
 */
public class AppdefEntityID_test extends TestCase {

    /**
     * Creates an instance.
     *
     * @param name
     */
    public AppdefEntityID_test(String name) {
        super(name);
    }
    
    /**
     * To address HHQ-1456, we had to introduce custom serialization for 
     * AppdefEntityID instances to provide a migration path for instances 
     * serialized in an HQ 3.1.x instance and deserialized in an HQ 3.2 
     * instance. Let's make sure the serialization works for instances 
     * serialized and deserialized in HQ 3.2.
     */
    public void testObjectSerialization() throws Exception {
        int entityType = 1;
        int entityId = 27;
        
        AppdefEntityID appdefEntityID = new AppdefEntityID(entityType, entityId);
        
        byte[] serialized = serializeToByteArray(appdefEntityID);
        
        AppdefEntityID deserialized = deserializeFromByteArray(serialized);

        assertEquals(appdefEntityID, deserialized);
        
        
        // now let's try the edge case where entityId == 0
        entityId = 0;
        
        appdefEntityID = new AppdefEntityID(entityType, entityId);
        
        serialized = serializeToByteArray(appdefEntityID);
        
        deserialized = deserializeFromByteArray(serialized);

        assertEquals(appdefEntityID, deserialized);
    }
    
    private byte[] serializeToByteArray(AppdefEntityID appdefEntityID) 
        throws IOException {
        
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ObjectOutputStream serializer = new ObjectOutputStream(baos);
        serializer.writeObject(appdefEntityID);
        serializer.flush();
        return baos.toByteArray();
    }
    
    private AppdefEntityID deserializeFromByteArray(byte[] appdefEntityID) 
        throws IOException, ClassNotFoundException {
        
        ByteArrayInputStream bais = new ByteArrayInputStream(appdefEntityID);
        ObjectInputStream deserializer = new ObjectInputStream(bais);
        return (AppdefEntityID)deserializer.readObject();
    }
    

}
