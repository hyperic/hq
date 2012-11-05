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

package org.hyperic.util.security;

import java.util.Random;

import org.jasypt.encryption.StringEncryptor;
import org.jasypt.encryption.pbe.StandardPBEStringEncryptor;
import org.jasypt.properties.PropertyValueEncryptionUtils;

public class SecurityUtil {
    public final static String DEFAULT_ENCRYPTION_ALGORITHM = "PBEWithMD5AndDES";
    public final static String ENC_MARK_PREFIX = "ENC(";
    public final static String ENC_MARK_POSTFIX = ")";
    
    /**
     * Generates a token of up to 100 chars of a (generally) random
     * token.
     */
    public static String generateRandomToken(){
        Random r;
        long rand1, rand2;

        r = new Random(System.currentTimeMillis());
        rand1 = Math.abs(r.nextLong());
        try { 
            Thread.sleep(rand1%100); 
        } catch(InterruptedException e){
        }
        
        rand2 = r.nextLong();
        return System.currentTimeMillis() + "-" +
            Math.abs(rand1) + "-" + Math.abs(rand2);
    }
    

    public static boolean isMarkedEncrypted(String str) {
        if (str==null) {
            return false;
        }
        String uStr = str.toUpperCase();
        return uStr.startsWith(ENC_MARK_PREFIX) && uStr.endsWith(ENC_MARK_POSTFIX);
    }
    
    public static String unmark(String str) {
        return str.substring(ENC_MARK_PREFIX.length(), str.length()-ENC_MARK_POSTFIX.length()); 
    }//EOM 
    
    public static String unmarkRecursive(String str) {
        
        while(str.startsWith(ENC_MARK_PREFIX)) { 
            str = str.substring(ENC_MARK_PREFIX.length(), str.length()-ENC_MARK_POSTFIX.length());
         }//EO while there are more parenthesis

        return str ; 
    }//EOM 
    
    public static String mark(String str) {
        return new StringBuilder().append(ENC_MARK_PREFIX).append(str).append(ENC_MARK_POSTFIX).toString(); 
    }

    public static StandardPBEStringEncryptor getStandardPBEStringEncryptor(String pbePass) {
        StandardPBEStringEncryptor encryptor =  new StandardPBEStringEncryptor();
        encryptor.setAlgorithm(SecurityUtil.DEFAULT_ENCRYPTION_ALGORITHM);
        encryptor.setPassword(pbePass);
        return encryptor;
    }
    
    /**
     * 
     * @param encryptor initialized encryptor
     * @param data
     * @return
     */
    public static String encrypt(StringEncryptor encryptor, String data) {
        return PropertyValueEncryptionUtils.encrypt(data,encryptor);
    }

    public static String encrypt(String encryptionAlgorithm, String encryptionKey, String data) {
        StandardPBEStringEncryptor encryptor = new StandardPBEStringEncryptor();
        encryptor.setPassword(encryptionKey);
        encryptor.setAlgorithm(encryptionAlgorithm);
        return encrypt(encryptor,data);
    }

    public static String decryptRecursiveUnmark(StringEncryptor encryptor, String data) {
        return encryptor.decrypt(unmarkRecursive(data.trim())) ;
    }
    
    public static String decrypt(StringEncryptor encryptor, String data) {
        return PropertyValueEncryptionUtils.decrypt(data,encryptor);
    }
    
    /**
     * 
     * @param encryptor initialized encryptor
     * @param data
     * @return
     */
    public static String decrypt(String encryptionAlgorithm, String encryptionKey, String data) {
        StandardPBEStringEncryptor encryptor = new StandardPBEStringEncryptor();
        
        encryptor.setPassword(encryptionKey);
        encryptor.setAlgorithm(encryptionAlgorithm);
        return decrypt(encryptor,data);
    }
    
}
