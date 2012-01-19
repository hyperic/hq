/**
 * NOTE: This copyright does *not* cover user programs that use Hyperic
 * program services by normal system calls through the application
 * program interfaces provided as part of the Hyperic Plug-in Development
 * Kit or the Hyperic Client Development Kit - this is merely considered
 * normal use of the program, and does *not* fall under the heading of
 *  "derived work".
 *
 *  Copyright (C) [2004-2012], VMware, Inc.
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
 */
package org.hyperic.util.security;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import java.io.File;
import java.net.URL;
import java.security.KeyStore;
import java.security.KeyStore.PasswordProtection;
import java.security.KeyStore.PrivateKeyEntry;
import java.util.Enumeration;

import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;


/**
 * @author guy
 */
public class KeystoreUtilsTest {

    private static final String KEYSTORE_RELATIVE_PATH = "test.keystore" ;
    private final char[] KEYSTORE_PASSWORD = "hyperic".toCharArray() ;
    
    private static String ksFileDir ;  
    private static String ksFullyqualifiedPath ;
    
    private KeyStore keystore ; 
     
    
    @BeforeClass
    public static final void setup() { 
        final URL keystorePathUrl = KeystoreUtilsTest.class.getResource(KEYSTORE_RELATIVE_PATH) ;
        ksFullyqualifiedPath = keystorePathUrl.getPath() ; 
        ksFileDir = new File(ksFullyqualifiedPath).getParent() ; 
    }//EOM 
    
    @Before
    public void methodSetup() { 
        try{ 
            this.keystore = KeyStoreUtils.loadKeyStore(ksFullyqualifiedPath, KEYSTORE_PASSWORD) ;
        }catch(Throwable t) { 
            fail("Keystore load failure.") ; 
            throw new RuntimeException(t) ;  
        }//EO catch block 
    }//EOM 
    
    @After
    public final void teardown() { 
        keystore = null ; 
    }//EOM 
    
    /**
     * Tests loading of an existing keystore into a keystore Object 
     */
    @Test
    public void testLoadKeyStoreIntoKeyStoreObject() { 
        //the before will actually test this 
    }//EOM
    
    /**
     * Tests loading of an existing keystore into a byte[]  
     */
    @Test 
    public void testLoadKeyStoreIntoByteArray() { 
        try{ 
            
            byte[] keystoreFileContent = KeyStoreUtils.loadKeystore(ksFullyqualifiedPath) ; 
            
            //convert the bytes into a keystore and compare to a loaded keystore 
            final KeyStore convertedKeystore = KeyStoreUtils.loadKeyStore(keystoreFileContent, KEYSTORE_PASSWORD) ; 
            compareKeystores(this.keystore, 
                    convertedKeystore,
                    "Byte[] converted keytstore is not the same as standardly loaded one"
                    ) ; 
            
        }catch(Throwable t) { 
            throw new RuntimeException(t) ;  
        }//EO catch block 
    }//EOM 
    
    @Test 
    public void testPersistKeyStoretoFile() { 
    
        final String sNewKeystoreFullyQualifiedPath = ksFileDir + "/persisted.version" ; 
        KeyStore origKeystore = this.keystore ;
        
        //ensure clean slate  
        final File persitedKeystoreFile = new File(sNewKeystoreFullyQualifiedPath) ; 
        if(persitedKeystoreFile.exists()) { 
            persitedKeystoreFile.delete() ; 
        }//EO if stale resources 
        
        try{ 
         
            //now persist 
            try{ 
                KeyStoreUtils.persistKeyStore(origKeystore, sNewKeystoreFullyQualifiedPath, KEYSTORE_PASSWORD) ;
            }catch(Throwable t) { 
                throw new RuntimeException(t) ;  
            }//EO catch block
          
            //now reload and compare to the original 
            try{  
               final KeyStore persitedKeystore =
                       KeyStoreUtils.loadKeyStore(sNewKeystoreFullyQualifiedPath, KEYSTORE_PASSWORD) ;
               
               this.compareKeystores(origKeystore, 
                       persitedKeystore, 
                       "Persited Keystore is not the same as original one") ;
                
            }catch(Throwable t) { 
                throw new RuntimeException(t) ;  
            }//EO catch block 
            
        }finally { 
            if(persitedKeystoreFile.exists()) { 
                persitedKeystoreFile.delete() ; 
            }//EO if stale resources  
        }//EO encapsulating catch block 
    }//EOM 

    @Test
    public void TestConvertKeyStoreToByteArray() { 
        
        byte[] keystoreFileContent = null ; 
        try{ 
            //convert to byte[] 
            keystoreFileContent = KeyStoreUtils.keyStoreToByteArray(this.keystore, KEYSTORE_PASSWORD) ;
        }catch(Throwable t) { 
            fail("Failed to convert to byte[].") ; 
            throw new RuntimeException(t) ;  
        }//EO catch block 
        
        //now convert back and compare 
        try{ 
            final KeyStore convetedKeystore = KeyStoreUtils.loadKeyStore(keystoreFileContent, KEYSTORE_PASSWORD) ; 
            this.compareKeystores(this.keystore, 
                    convetedKeystore, 
                    "converted Keystore is not the same as original one"
                    ) ;
        }catch(Throwable t) { 
            throw new RuntimeException(t) ;  
        }//EO catch block 
        
    }//EOM 
    
    private final void compareKeystores(final KeyStore templateKeystore, final KeyStore targetKeystore, 
            final String errorMsg) 
                                                                                                throws Throwable { 
        if(templateKeystore == null && targetKeystore != null) { 
            fail("Template keystore is null but target is not") ; 
        }else if(templateKeystore != null && targetKeystore == null) { 
            fail("target keystore is null but template is not") ;
        }//EO if target keystore is null but template is not
        
        final Enumeration<String> aliasesEnumeration = templateKeystore.aliases() ;
        String alias = null ;
        
        final PasswordProtection passwordMetadata = new PasswordProtection(KEYSTORE_PASSWORD) ; 
        PrivateKeyEntry templateKeystoreEntry = null, targetKeystoreEntry =  null ;  
        java.security.cert.Certificate templateCertificate = null, targetCertificate = null ; 
        
        while(aliasesEnumeration.hasMoreElements()) { 
            alias = aliasesEnumeration.nextElement() ;
            if(templateKeystore.isKeyEntry(alias)) { 
                templateKeystoreEntry = (PrivateKeyEntry) templateKeystore.getEntry(alias, passwordMetadata) ;
                templateCertificate = templateKeystoreEntry.getCertificate() ; 
                
                targetKeystoreEntry = (PrivateKeyEntry) targetKeystore.getEntry(alias, passwordMetadata) ;  
                
                if(targetKeystoreEntry == null) { 
                        fail("Could not find key entry for alias " + alias +    
                                " in target keystore") ; 
                }//EO if target keystore entry was null ; 
                
                targetCertificate = targetKeystore.getCertificate(alias) ; 
            }else { 
                templateCertificate = templateKeystore.getCertificate(alias) ;  
                targetCertificate = targetKeystore.getCertificate(alias) ; 
            }///EO else if not key entry 
            
            assertEquals(errorMsg + "with error:\n\t Target certificate with alias " + alias +
                    " is not the same as template one" , 
                    templateCertificate, 
                    targetCertificate) ; 
            
        }//EO while there are more aliases 
                
    }//EOM  
    
}//EOC 
