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

package org.hyperic.hq.agent.server;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.security.GeneralSecurityException;
import java.security.KeyStore;
import java.security.KeyStore.PrivateKeyEntry;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.UnrecoverableEntryException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.StringTokenizer;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hyperic.hq.agent.AgentKeystoreConfig;
import org.hyperic.hq.agent.db.DiskList;
import org.hyperic.util.file.FileUtil;
import org.hyperic.util.security.KeystoreConfig;
import org.hyperic.util.security.KeystoreManager;
import org.hyperic.util.security.MarkedStringEncryptor;
import org.hyperic.util.security.SecurityUtil;
import org.jasypt.encryption.StringEncryptor;

public class AgentDListProvider implements AgentStorageProvider {
    private static final int RECSIZE  = 4000;
    private static final int OLD_RECSIZE = 1024;
    private static final long MAXSIZE = 50 * 1024 * 1024; // 50MB
    private static final long CHKSIZE = 10 * 1024 * 1024;  // 10MB
    private static final int CHKPERC  = 50; // Only allow < 50% free
    private static final String DEFAULT_PRIVATE_KEY_KEY = "hq";

    private final Log      log;        // da logger
    private HashMap  keyVals;    // The key-value pairs
    private HashMap  lists;      // Names onto DiskList objects
    private HashMap  overloads;
    private File     writeDir;   // Dir to write stuff to
    private File     keyValFile;
    private File     keyValFileBackup;
    
    // Dirty flag for when writing to keyvals.  Set to true at startup
    // to force an initial flush.
    private boolean  keyValDirty = true;      // Dirty flag for when writing to keyvals

    private long      maxSize = MAXSIZE;
    private long      chkSize = CHKSIZE;
    private int       chkPerc = CHKPERC;

    private StringEncryptor encryptor = null;
    
    public AgentDListProvider(){
        this.log     = LogFactory.getLog(AgentDListProvider.class);
        this.keyVals = null;
        this.lists   = null;
    }

    /**
     * Get a description of this storage provider.
     *
     * @return A string describing the functionality of the object.
     */
    public String getDescription(){
        return "Agent D-list provider.  Data is written to data/idx files " +
                "for lists, and a single file for key/values";
    }

    private DiskList intrCreateList(String name, int recSize) throws IOException {
        long _maxSize = maxSize;
        long _chkSize = chkSize;
        int _chkPerc = chkPerc;
        ListInfo info = (ListInfo) overloads.get(name);
        if (info != null) {
            _maxSize = info.maxSize;
            _chkSize = info.chkSize;
            _chkPerc = info.chkPerc;
        }
        return new DiskList(new File(this.writeDir, name),
                recSize, _chkSize, _chkPerc, _maxSize);
    }
    


    /**
     * Create a list of non-standard record size.
     */
    public void createList(String name, int recSize)
            throws AgentStorageException
            {
        try {
            DiskList dList = intrCreateList(name, recSize);
            this.lists.put(name, dList);
        } catch (IOException e) {
            throw new AgentStorageException("Unable to create DiskList: " + 
                    e.getMessage());
        }
            }

    private ListInfo parseInfo(String info) throws AgentStorageException {
        StringTokenizer st = new StringTokenizer(info, ":");
        if (st.countTokens() != 4) {
            throw new AgentStorageException(info + " is an invalid agent " +
                    "disklist configuration");
        }
        String s = st.nextToken().trim();
        long factor;
        if ("m".equalsIgnoreCase(s)) {
            factor = 1024 * 1024;
        } else if ("k".equalsIgnoreCase(s)) {
            factor = 1024;
        } else {
            throw new AgentStorageException(info + " is an invalid agent " +
                    "disklist configuration");
        }
        ListInfo listInfo = new ListInfo();
        try {
            listInfo.maxSize = Long.parseLong(st.nextToken().trim()) * factor;
            listInfo.chkSize = Long.parseLong(st.nextToken().trim()) * factor;
            listInfo.chkPerc = Integer.parseInt(st.nextToken().trim());
        } catch (NumberFormatException e) {
            throw new AgentStorageException("Invalid agent disklist " +
                    "configuration: " + e);
        }
        return listInfo;
    }

    public void addOverloadedInfo(String listName, String info) {
        try {
            overloads.put(listName, parseInfo(info));
        } catch (AgentStorageException ex) {
            //use default values
            log.error(ex);
        }
    }
    /**
     * Sets a value within the storage object.  
     *
     * @param key    Key of the value to set.
     * @param value  Value to set for 'key'.
     */
    public void setValue(String key, String value){
        if(value == null){
            this.log.debug("Removing '" + key + "' from storage");
            synchronized(this.keyVals){
                this.keyVals.remove(key);
            }
        } else {
            this.log.debug("Setting '" + key + "' to '" + value + "'");
            synchronized(this.keyVals){
                this.keyVals.put(key, value);
            }
        }

        // After call to setValue() set dirty flag for flush to storage
        this.keyValDirty = true;
    }

    /**
     * Gets a value from the storage object.
     *
     * @param key  Key of the value to get.
     *
     * @return The value associated with the key for the subsystem.
     */
    public String getValue(String key){
        String res;

        synchronized(this.keyVals){
            res = (String)this.keyVals.get(key);
        }
        if(log.isDebugEnabled()){
            this.log.debug("Got " + key + "='" + res + "'");
        }
        return res;
    }

    public Set getKeys(){ 
        //copy keys to avoid possible ConcurrentModificationException
        HashSet set = new HashSet();
        synchronized(this.keyVals){
            set.addAll(this.keyVals.keySet()); 
        }
        return set;
    }

    protected String getKeyvalsPass() throws KeyStoreException, IOException, NoSuchAlgorithmException, UnrecoverableEntryException {
        KeystoreConfig keystoreConfig = new AgentKeystoreConfig();
        KeyStore keystore = KeystoreManager.getKeystoreManager().getKeyStore(keystoreConfig);
        KeyStore.Entry e = keystore.getEntry(DEFAULT_PRIVATE_KEY_KEY,
                new KeyStore.PasswordProtection(keystoreConfig.getFilePassword().toCharArray()));
        byte[] pk = ((PrivateKeyEntry)e).getPrivateKey().getEncoded();
        ByteBuffer encryptionKey = Charset.forName("US-ASCII").encode(ByteBuffer.wrap(pk).toString());
        return encryptionKey.toString();
    }
    
    //synchronized because concurrent threads cannot
    //have this.keyValFile open for writing on win32
    public synchronized void flush()
            throws AgentStorageException
            {
        BufferedOutputStream bOs;
        FileOutputStream fOs = null;
        DataOutputStream dOs;

        if (! this.keyValDirty)
            return;

        try {
            fOs = new FileOutputStream(this.keyValFile);
            bOs = new BufferedOutputStream(fOs);
            dOs = new DataOutputStream(bOs);
            if (this.encryptor==null) {
                this.encryptor = new MarkedStringEncryptor(SecurityUtil.DEFAULT_ENCRYPTION_ALGORITHM,getKeyvalsPass());
            }
            synchronized(this.keyVals){
                dOs.writeLong(this.keyVals.size());
                
                for(Iterator i=this.keyVals.entrySet().iterator(); 
                        i.hasNext();
                        )
                {
                    Map.Entry ent = (Map.Entry)i.next();

                    String encryptedKey = SecurityUtil.encrypt(this.encryptor, (String)ent.getKey());
                    String encryptedVal =  SecurityUtil.encrypt(this.encryptor, (String)ent.getValue());

                    dOs.writeUTF(encryptedKey);
                    dOs.writeUTF(encryptedVal);
                }

                dOs.flush();

                // After successful write, clear dirty flag.
                this.keyValDirty = false;
            }
        } catch(IOException exc){
            this.log.error("Error flushing data", exc);
            throw new AgentStorageException("Error flushing data: " +
                    exc.getMessage());
        } catch (GeneralSecurityException e) {
            throw new AgentStorageException(e.getMessage());
        } finally {
            try {if(fOs != null)fOs.close();} catch(IOException exc){}
        }

        // After successful flush, update backup copy
        try {
            synchronized(this.keyVals){
                FileUtil.copyFile(this.keyValFile, this.keyValFileBackup);
            }
        } catch (FileNotFoundException e) {
            // Should never happen
        } catch (IOException e) {
            this.log.error("Error backing up keyvals", e);
            throw new AgentStorageException("Error backing up keyvals: " +
                    e.getMessage());
        }
            }

    /**
     * DList info string is a series of properties seperated by '|'
     * Three properties are expected.
     *
     * Directory to place the data files
     * Size in MB to start checking for unused blocks
     * Maximum percentage of free blocks allowed
     *
     * Default is 'data|20|50'
     */
    public void init(String info)
            throws AgentStorageException 
            {
        BufferedInputStream bIs;
        FileInputStream fIs = null;
        DataInputStream dIs;
        long nEnts;

        // Parse out configuration
        StringTokenizer st = new StringTokenizer(info, "|");
        if (st.countTokens() != 5) {
            throw new AgentStorageException(info + " is an invalid agent" +
                    " storage provider configuration");
        }

        this.keyVals    = new HashMap();
        this.lists      = new HashMap();
        overloads       = new HashMap();
        String dir      = st.nextToken();
        this.writeDir   = new File(dir);
        this.keyValFile = new File(writeDir, "keyvals");
        this.keyValFileBackup = new File(writeDir, "keyvals.backup");

        String s = st.nextToken().trim();
        long factor;
        if ("m".equalsIgnoreCase(s)) {
            factor = 1024 * 1024;
        } else if ("k".equalsIgnoreCase(s)) {
            factor = 1024;
        } else {
            throw new AgentStorageException(info + " is an invalid agent" +
                    " storage provider configuration");
        }
        try {
            maxSize = Long.parseLong(st.nextToken().trim()) * factor;
            chkSize = Long.parseLong(st.nextToken().trim()) * factor;
            chkPerc = Integer.parseInt(st.nextToken().trim());
        } catch (NumberFormatException e) {
            throw new AgentStorageException("Invalid agent storage provider " +
                    "configuration: " + e);
        }

        if(this.writeDir.exists() == false){
            // Try to create it
            this.writeDir.mkdir();
        }

        if(this.writeDir.isDirectory() == false){
            throw new AgentStorageException(dir + " is not a directory");
        }

        try {
            fIs = new FileInputStream(this.keyValFile);
            bIs = new BufferedInputStream(fIs);
            dIs = new DataInputStream(bIs);

            nEnts = dIs.readLong();

            if (this.encryptor==null) {
                this.encryptor = new MarkedStringEncryptor(SecurityUtil.DEFAULT_ENCRYPTION_ALGORITHM,getKeyvalsPass());
            }
            while(nEnts-- != 0){
                String key = dIs.readUTF();
                String val = dIs.readUTF();
                String decryptedKey = SecurityUtil.isMarkedEncrypted(key)?SecurityUtil.decrypt(encryptor, key):key;
                String decryptedVal = SecurityUtil.isMarkedEncrypted(val)?SecurityUtil.decrypt(encryptor, val):val;
                this.keyVals.put(decryptedKey, decryptedVal);
            }
        } catch(FileNotFoundException exc){
            // Normal when it doesn't exist
        } catch (GeneralSecurityException e) {
            this.log.error(e.getMessage());
            throw new AgentStorageException(e.getMessage());
 	} catch(IOException exc){
            this.log.error("Error reading " + this.keyValFile + " loading " +
                    "last known good version");

            // Close old stream
            try {if(fIs != null)fIs.close();} catch(IOException e){}

            // Fall back to last known good keyvals file
            try {
                fIs = new FileInputStream(this.keyValFileBackup);
                bIs = new BufferedInputStream(fIs);
                dIs = new DataInputStream(bIs);

                nEnts = dIs.readLong();
                if (this.encryptor==null) {
                    this.encryptor = new MarkedStringEncryptor(SecurityUtil.DEFAULT_ENCRYPTION_ALGORITHM,getKeyvalsPass());
                }
                while(nEnts-- != 0) {
                    String denryptedKey = SecurityUtil.encrypt(this.encryptor, dIs.readUTF());
                    String decryptedVal = SecurityUtil.encrypt(this.encryptor, dIs.readUTF());
                    this.keyVals.put(denryptedKey, decryptedVal);
                }
            } catch (FileNotFoundException e) {
                // Already checked this before, shouldn't happen
            } catch (IOException e) {
                // Throw original error
                throw new AgentStorageException("Error reading " + 
                        this.keyValFile + ": " +
                        e.getMessage());
            
            } catch(GeneralSecurityException e){
                // Throw original error
                throw new AgentStorageException(e.getMessage());
            }
    } finally {
            try {if(fIs != null)fIs.close();} catch(IOException exc){}
        }
            }

    public void dispose(){
        try {
            this.flush();
        } catch(Exception exc){
            this.log.error("Error flushing key/vals storage", exc);
        }

        for(Iterator i=this.lists.entrySet().iterator(); i.hasNext(); ){
            Map.Entry ent = (Map.Entry)i.next();
            DiskList dl = (DiskList)ent.getValue();

            try {
                dl.close();
            } catch(Exception exc){
                this.log.error("Unable to dispose of disk list '" +
                        ent.getKey() + "'", exc);
            }
        }

    }

    /*** LIST FUNCTIONALITY ***/

    public void addToList(String listName, String value)
            throws AgentStorageException
            {
        DiskList dList = getDiskList(listName);
        if (null == dList) {
            log.error("Error adding data , cannot read list '" + listName + "' " +
                    "from storage");
            return;
        }
        try {
            dList.addToList(value);
        } catch(IOException exc){
            this.log.error("Error adding to list '" + listName + "'", exc);
            throw new AgentStorageException("Error adding data to list: " +
                    exc.getMessage());
        }
            }

    public void removeFromList(String listName, long recNumber) throws AgentStorageException {
        DiskList dList = getDiskList(listName);
        if (null == dList) {
            log.error("Error removing data , cannot read list '" + listName + "' " +
                    "from storage");
            return;
        }
        try {
            dList.removeRecord(recNumber);
        } catch(IOException exc){
            this.log.error("Error deleting from list '" + listName + "'", exc);
            throw new AgentStorageException("Error deleting data from list: " +
                    exc.getMessage());
        }
    }

    public void deleteList(String listName) {
        DiskList dList = getDiskList(listName);
        if (null == dList) {
            return ;
        }
        try {
            dList.deleteAllRecords();
        } catch(IOException exc){
            this.log.error("Error deleting all records", exc);
        }
    }

    public Iterator<String> getListIterator(String listName) {
        DiskList dList = getDiskList(listName);
        if (null == dList) {
            return null;
        }
        return dList.getListIterator();
    }
    

    public void convertListToCurrentRecordSize(String listName) throws IOException{
        DiskList dList = getDiskList(listName);
        if (null == dList) {
            return ;
        }
        dList.convertListToCurrentRecordSize(OLD_RECSIZE);
    }

    private DiskList getDiskList(String listName) {
        DiskList dList;

        synchronized(this.lists){
            dList = (DiskList)this.lists.get(listName);

            if(dList == null){
                try {
                    dList = intrCreateList(listName, RECSIZE);
                } catch(IOException exc){
                    this.log.error("Error loading disk list", exc);
                    return null; // XXX
                }
                this.lists.put(listName, dList);
            }
        }
        return dList;
    }
    
    private static class ListInfo {
        long      maxSize;
        long      chkSize;
        int       chkPerc;
    }


}
