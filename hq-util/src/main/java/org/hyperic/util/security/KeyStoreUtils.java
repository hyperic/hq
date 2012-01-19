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
 *
 */
package org.hyperic.util.security;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;

/**
 * Utility class for handling the marshalling & unmarshalling of keystores to/from various datastructures
 * @author guy
 */
public class KeyStoreUtils {

    /**
     * Loads a keystore from file into a {@link KeyStore} object 
     * @param ksFilePath Fully qualified path to the keystore.  
     * @param ksPassword Keystore password.
     * @return {@link KeyStore} instance
     * @throws KeyStoreException
     * @throws NoSuchAlgorithmException
     * @throws CertificateException
     * @throws IOException
     */
    public static final KeyStore loadKeyStore(final String ksFilePath, final char[] ksPassword) throws 
        KeyStoreException, NoSuchAlgorithmException, CertificateException, IOException { 
        
        java.io.FileInputStream fis = null;
        try{ 
            fis = new FileInputStream(new File(ksFilePath));
            return loadKeyStore(fis, ksPassword) ; 
        }finally{ 
            if (fis != null) {
                fis.close();
            }// EO if fis != null
        }//EO catch block 
    }//EOM 

    /**
     * Converts a byte[] representation of a keystore file into a {@link KeyStore} object.  
     * @param keystoreFileContent byte[] representation of the target keystore.   
     * @param ksPassword Keystore password. 
     * @return {@link KeyStore} instance
     * @throws KeyStoreException
     * @throws NoSuchAlgorithmException
     * @throws CertificateException
     * @throws IOException
     */
    public static final KeyStore loadKeyStore(final byte[] keystoreFileContent, char[] ksPassword)
            throws KeyStoreException, NoSuchAlgorithmException, CertificateException, IOException {
    
        final ByteArrayInputStream bis = new ByteArrayInputStream(keystoreFileContent);
        return loadKeyStore(bis, ksPassword) ; 
    }//EOM
        
    /**
     * Loads a keystore file into a byte[] 
     * @param ksFilePath Fully qualified path to the keystore. 
     * @return byte[] representation of the keystore file. 
     * @throws KeyStoreException
     * @throws NoSuchAlgorithmException
     * @throws CertificateException
     * @throws IOException
     */
    public static final byte[] loadKeystore(final String ksFilePath) throws 
            KeyStoreException, NoSuchAlgorithmException, CertificateException, IOException {
        
        java.io.FileInputStream fis = null;
        try{ 
            final File keystoreFile = new File(ksFilePath);
            fis = new FileInputStream(keystoreFile);
            
            final byte[] arrContent = new byte[(int) keystoreFile.length()];
            fis.read(arrContent);
            return arrContent;
            
        }finally{ 
            if (fis != null) {
                fis.close();
            }// EO if fis != null
        }//EO catch block 
    }//EOM
    
    /**
     * Stores a keystore into a file. 
     * @param ks {@link KeyStore} instance to store. 
     * @param ksFilePath Fully qualified path to the keystore. 
     * @param ksPassword Keystore file password. 
     * @throws KeyStoreException
     * @throws NoSuchAlgorithmException
     * @throws CertificateException
     * @throws IOException
     */
    public static final void persistKeyStore(final KeyStore ks, final String ksFilePath, final char[] ksPassword) 
                    throws KeyStoreException, NoSuchAlgorithmException, CertificateException, IOException {
        
        FileOutputStream fos = null;
        try {
            final File file = new File(ksFilePath);
            fos = new FileOutputStream(file);
            ks.store(fos, ksPassword);
        } finally {
            if (fos != null)
                fos.close();
        }// EO catch block
    }//EOM 
    
    /**
     * Converts a {@link KeyStore} instance into a byte[] representation. 
     * @param ks {@link KeyStore} instance to convert. 
     * @param ksPassword Keystore password.
     * @return byte[] representation of the formal argument's keystore.
     * @throws KeyStoreException
     * @throws NoSuchAlgorithmException
     * @throws CertificateException
     * @throws IOException
     */
    public static final byte[] keyStoreToByteArray(final KeyStore ks, final char[] ksPassword) throws 
        KeyStoreException, NoSuchAlgorithmException, CertificateException, IOException {
         
        ByteArrayOutputStream bos = null ;  
        bos = new ByteArrayOutputStream() ;
        ks.store(bos, ksPassword);
        return bos.toByteArray() ; 
    }//EOM 
    
    
    /*
     * Loads a keystore from an InputStream.  
     * @param is Keystore InputStream. 
     * @param ksPassword Keystore password.
     * @return Keystore instance.
     * @throws KeyStoreException
     * @throws NoSuchAlgorithmException
     * @throws CertificateException
     * @throws IOException
     */
    private static final KeyStore loadKeyStore(final InputStream is, final char[] ksPassword) throws KeyStoreException, 
        NoSuchAlgorithmException, CertificateException, IOException{
    
        final KeyStore ks = KeyStore.getInstance(KeyStore.getDefaultType());
        ks.load(is, ksPassword);
        return ks;
    }//EOM 
    
    
}//EOC 
