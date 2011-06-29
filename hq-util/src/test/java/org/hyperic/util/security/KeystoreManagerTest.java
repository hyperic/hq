/**
 * NOTE: This copyright does *not* cover user programs that use Hyperic
 * program services by normal system calls through the application
 * program interfaces provided as part of the Hyperic Plug-in Development
 * Kit or the Hyperic Client Development Kit - this is merely considered
 * normal use of the program, and does *not* fall under the heading of
 *  "derived work".
 *
 *  Copyright (C) [2010], VMware, Inc.
 *  This file is part of Hyperic.
 *
 *  Hyperic is free software; you can redistribute it and/or modify
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
package org.hyperic.util.security;

import static org.junit.Assert.*;

import java.io.File;
import java.io.IOException;
import java.security.KeyStore;
import java.security.KeyStoreException;

import org.junit.Test;

/**
 * @author achen
 *
 */
public class KeystoreManagerTest {

    /**
     *  1. test create keystore (hq default)
           pre-condition: no keystore
           post-cond: keystore("test_HQ","testKeystore","111111") is created     
     */
    @Test
    public void testCreateDefaultKeystore() {
        KeystoreManager tester = KeystoreManager.getKeystoreManager();

        KeystoreConfig config = new KeystoreConfig("test_HQ",
            "testKeystore","111111",true);
        try {
            tester.getKeyStore(config);
            File key = new File(config.getFilePath());
            if(key.exists()){
                assertTrue("keystore is created",true);
                key.delete();
            }else{
                fail("File does not exist");
            }
        } catch (KeyStoreException e) {
            e.printStackTrace();
            fail("KeystoreException");
        } catch (IOException e) {
            e.printStackTrace();
            fail("IOException");
        }
    }
    /**
     * 2. test custom config keystore
       pre-condition: no keystore
       post-cond: IOException - can't find the keystore 
     */
    @Test
    public void testCustomConfigKeystore() {    
        KeystoreManager tester = KeystoreManager.getKeystoreManager();
        KeystoreConfig config = new KeystoreConfig("test_HQ",
            "testKeystore","111111",false);
        
        try {
            tester.getKeyStore(config);
            File key = new File(config.getFilePath());
            if(key.exists()){
                key.delete();
            }
            fail("Should get IOException, but doesn't get any Exception");
            
        } catch (KeyStoreException e) {
            e.printStackTrace();
            fail("Should get IOException, but get KeystoreException");
        } catch (IOException e) {
            assertTrue("Should get IOException",true);
        }
    }
    /**
     *  1. test create keystore (hq default)
           pre-condition: keystore("test_HQ","testKeystore","111111")
           post-cond: keystore("test_HQ","testKeystore","111111"), no exception thrown    
     */
    @Test
    public void testCreateExistingDefaultKeystore() {
        KeystoreManager tester = KeystoreManager.getKeystoreManager();

        KeystoreConfig config = new KeystoreConfig("test_HQ",
            "testKeystore","111111",true);
        try {
            tester.getKeyStore(config);//keystore is created here
            tester.getKeyStore(config);
            //TODO verify the password.

        } catch (KeyStoreException e) {
            e.printStackTrace();
            fail("KeystoreException");
        } catch (IOException e) {
            e.printStackTrace();
            fail("IOException");
        } finally{
            File key = new File(config.getFilePath());
            if(key.exists()){
                key.delete();
            }else{
                fail("File does not exist");
            }
        }
    }
}
