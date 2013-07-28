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
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.io.UTFDataFormatException;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.security.KeyStore;
import java.security.KeyStore.PrivateKeyEntry;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.UnrecoverableEntryException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map.Entry;
import java.util.Set;
import java.util.StringTokenizer;
import java.util.concurrent.atomic.AtomicBoolean;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hyperic.hq.agent.AgentKeystoreConfig;
import org.hyperic.hq.agent.db.DiskList;
import org.hyperic.hq.agent.stats.AgentStatsCollector;
import org.hyperic.hq.common.SystemException;
import org.hyperic.util.file.FileUtil;
import org.hyperic.util.security.KeystoreConfig;
import org.hyperic.util.security.KeystoreManager;
import org.hyperic.util.security.SecurityUtil;
import org.jasypt.encryption.pbe.PooledPBEStringEncryptor;

public class AgentDListProvider implements AgentStorageProvider {
    private static final Log log = LogFactory.getLog(AgentDListProvider.class);
    private static final int RECSIZE  = 4000;
    private static final int OLD_RECSIZE = 1024;
    private static final long MAXSIZE = 50 * 1024 * 1024; // 50MB
    private static final long CHKSIZE = 10 * 1024 * 1024;  // 10MB
    private static final int CHKPERC  = 50; // Only allow < 50% free

    private final AgentStatsCollector agentStatsCollector = AgentStatsCollector.getInstance();
    private final AtomicBoolean shutdown = new AtomicBoolean(false);
    private HashMap<EncVal, EncVal>  keyVals;
    private HashMap<String, DiskList>  lists;
    private HashMap<String, ListInfo> overloads;
    private File writeDir;
    private File keyValFile;
    private File keyValFileBackup; 
    
    // Dirty flag for when writing to keyvals.  Set to true at startup
    // to force an initial flush.
    private final AtomicBoolean keyValDirty = new AtomicBoolean(true);      // Dirty flag for when writing to keyvals

    private long maxSize = MAXSIZE;
    private long chkSize = CHKSIZE;
    private int chkPerc = CHKPERC;

    private final PooledPBEStringEncryptor encryptor;

    public AgentDListProvider() {
        keyVals = null;
        lists   = null;
        try {
            encryptor = createEncryptor();
        } catch (Exception e) {
            throw new SystemException(e);
        }
    }

    protected PooledPBEStringEncryptor createEncryptor() throws KeyStoreException, NoSuchAlgorithmException, UnrecoverableEntryException, IOException {
        PooledPBEStringEncryptor encryptor = new PooledPBEStringEncryptor();
        encryptor.setPoolSize(1);
        encryptor.setAlgorithm(SecurityUtil.DEFAULT_ENCRYPTION_ALGORITHM);
        encryptor.setPassword(getKeyvalsPass());
        return encryptor; 
    }
    
    /**
     * Get a description of this storage provider.
     *
     * @return A string describing the functionality of the object.
     */
    public String getDescription(){
        return "Agent D-list provider.  Data is written to data/idx files for lists, and a single file for key/values";
    }

    private DiskList intrCreateList(String name, int recSize) throws IOException {
        long _maxSize = maxSize;
        long _chkSize = chkSize;
        int _chkPerc = chkPerc;
        ListInfo info = overloads.get(name);
        if (info != null) {
            _maxSize = info.maxSize;
            _chkSize = info.chkSize;
            _chkPerc = info.chkPerc;
        }
        return new DiskList(new File(this.writeDir, name), recSize, _chkSize, _chkPerc, _maxSize);
    }

    /**
     * Create a list of non-standard record size.
     */
    public void createList(String name, int recSize) throws AgentStorageException {
        try {
            DiskList dList = intrCreateList(name, recSize);
            lists.put(name, dList);
        } catch (IOException e) {
            AgentStorageException toThrow = new AgentStorageException("Unable to create DiskList: " +  e);
            toThrow.initCause(e);
            throw toThrow;
        }
    }

    private ListInfo parseInfo(String info) throws AgentStorageException {
        StringTokenizer st = new StringTokenizer(info, ":");
        if (st.countTokens() != 4) {
            throw new AgentStorageException(info + " is an invalid agent disklist configuration");
        }
        String s = st.nextToken().trim();
        long factor;
        if ("m".equalsIgnoreCase(s)) {
            factor = 1024 * 1024;
        } else if ("k".equalsIgnoreCase(s)) {
            factor = 1024;
        } else {
            throw new AgentStorageException(info + " is an invalid agent disklist configuration");
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
    public void setValue(String key, String value) {
        final boolean debug = log.isDebugEnabled();
        if(value == null) {
            if (debug) {
                log.debug("Removing '" + key + "' from storage");
            }
            synchronized(keyVals){
                keyVals.remove(key);
            }
        } else {
            if (debug) {
                log.debug("Setting '" + key + "' to '" + value + "'");
            }
            synchronized(keyVals){
                keyVals.put(new EncVal(encryptor, key), new EncVal(encryptor, value));
            }
        }
        // After call to setValue() set dirty flag for flush to storage
        keyValDirty.set(true);
    }

    /**
     * Gets a value from the storage object.
     *
     * @param key  Key of the value to get.
     *
     * @return The value associated with the key for the subsystem.
     */
    public String getValue(String key) {
        String res = null;
        synchronized(keyVals){
            EncVal encVal = keyVals.get(new EncVal(encryptor, key));
            res = encVal == null ? null : encVal.getVal();
        }
        if(log.isDebugEnabled()){
            log.debug("Got " + key + "='" + res + "'");
        }
        return res;
    }

    public Set<String> getKeys(){ 
        //copy keys to avoid possible ConcurrentModificationException
        Set<String> set = new HashSet<String>();
        synchronized(keyVals){
            for (EncVal v : keyVals.keySet()) {
                set.add(v.getVal());
            }
        }
        return set;
    }

    protected String getKeyvalsPass() throws KeyStoreException, IOException, NoSuchAlgorithmException, UnrecoverableEntryException {
        KeystoreConfig keystoreConfig = new AgentKeystoreConfig();
        KeyStore keystore = KeystoreManager.getKeystoreManager().getKeyStore(keystoreConfig);
        KeyStore.Entry e = keystore.getEntry(keystoreConfig.getAlias(), new KeyStore.PasswordProtection(keystoreConfig.getFilePassword().toCharArray()));
        if (e == null) { 
            throw new UnrecoverableEntryException("Encryptor password generation failure: No such alias") ; 
        }
// XXX scottmf - I'm a bit concerned about this.  I tested the upgrade path on the agent on the new code with the
// ByteBuffer and it doesn't work, the agent throws a org.jasypt.exceptions.EncryptionOperationNotPossibleException.
// When I put back the old code with the replaceAll() everything works.
//final String p = ((PrivateKeyEntry)e).getPrivateKey().toString();
//return p.replaceAll("[^a-zA-Z0-9]", "_");
        byte[] pk = ((PrivateKeyEntry)e).getPrivateKey().getEncoded();
        ByteBuffer encryptionKey = Charset.forName("US-ASCII").encode(ByteBuffer.wrap(pk).toString());
        return encryptionKey.toString();
    }
    
    public synchronized void flush() throws AgentStorageException {
        flush(false);
    }

    private synchronized void flush(boolean toShutdown) throws AgentStorageException {
        if (shutdown.get() && !toShutdown) {
            return;
        }
        final long start = System.currentTimeMillis();
        BufferedOutputStream bOs = null;
        FileOutputStream fOs = null;
        DataOutputStream dOs = null;
        if (!keyValDirty.get()) {
            return;
        }
        Entry<EncVal, EncVal> curr = null;
        try {
            fOs = new FileOutputStream(keyValFile);
            bOs = new BufferedOutputStream(fOs);
            dOs = new DataOutputStream(bOs);
            synchronized(keyVals){
                dOs.writeLong(keyVals.size());
                for (Entry<EncVal, EncVal> entry : keyVals.entrySet()) {
                    curr = entry;
                    String encKey = entry.getKey().getEnc();
                    String encVal = entry.getValue().getEnc();
                    dOs.writeUTF(encKey);
                    dOs.writeUTF(encVal);
                }
            }
        } catch(UTFDataFormatException e) {
            if (curr != null) {
                log.error("error writing key=" + curr.getKey().getVal() + ", value=" + curr.getValue().getVal(), e);
            } else {
                log.error(e,e);
            }
        } catch(IOException e) {
            log.error("Error flushing data", e);
            AgentStorageException toThrow = new AgentStorageException("Error flushing data: " + e);
            toThrow.initCause(e);
            throw toThrow;
        } finally {
            close(dOs);
            close(bOs);
            // After successful write, clear dirty flag.
            keyValDirty.set(false);
            close(fOs);
        }

        // After successful flush, update backup copy
        try {
            synchronized(keyVals){
                FileUtil.copyFile(this.keyValFile, this.keyValFileBackup);
            }
        } catch (FileNotFoundException e) {
            log.warn(e);
            log.debug(e,e);
        } catch (IOException e) {
            log.error("Error backing up keyvals", e);
            AgentStorageException toThrow = new AgentStorageException("Error backing up keyvals: " + e);
            toThrow.initCause(e);
            throw toThrow;
        }
        agentStatsCollector.addStat(System.currentTimeMillis() - start, AgentStatsCollector.DISK_LIST_KEYVALS_FLUSH_TIME);
    }

    private void close(OutputStream os) {
        try {
            if (os != null) {
                os.flush();
                os.close();
            }
        } catch (IOException e) {
            log.error(e,e);
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
    public void init(String info) throws AgentStorageException  {
        BufferedInputStream bIs;
        FileInputStream fIs = null;
        DataInputStream dIs;
        long nEnts;

        // Parse out configuration
        StringTokenizer st = new StringTokenizer(info, "|");
        if (st.countTokens() != 5) {
            throw new AgentStorageException(info + " is an invalid agent storage provider configuration");
        }

        keyVals = new HashMap<EncVal, EncVal>();
        lists = new HashMap<String, DiskList>();
        overloads = new HashMap<String, ListInfo>();
        String dir = st.nextToken();
        this.writeDir = new File(dir);
        this.keyValFile = new File(writeDir, "keyvals");
        this.keyValFileBackup = new File(writeDir, "keyvals.backup");

        String s = st.nextToken().trim();
        long factor;
        if ("m".equalsIgnoreCase(s)) {
            factor = 1024 * 1024;
        } else if ("k".equalsIgnoreCase(s)) {
            factor = 1024;
        } else {
            throw new AgentStorageException(info + " is an invalid agent storage provider configuration");
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
            while(nEnts-- != 0){
                String encKey = dIs.readUTF();
                String encVal = dIs.readUTF();
                String key = SecurityUtil.isMarkedEncrypted(encKey) ? SecurityUtil.decryptRecursiveUnmark(encryptor, encKey) : encKey;
                String val = SecurityUtil.isMarkedEncrypted(encVal) ? SecurityUtil.decryptRecursiveUnmark(encryptor, encVal) : encVal;
                this.keyVals.put(new EncVal(encryptor, key, encKey), new EncVal(encryptor, val, encVal));
            }
        } catch(FileNotFoundException exc) {
            // Normal when it doesn't exist
            log.debug("file not found (this is ok): " + exc);
        } catch(IOException exc){
            log.error("Error reading " + this.keyValFile + " loading " + "last known good version");
            // Close old stream
            close(fIs);
            // Fall back to last known good keyvals file
            try {
                fIs = new FileInputStream(this.keyValFileBackup);
                bIs = new BufferedInputStream(fIs);
                dIs = new DataInputStream(bIs);
                nEnts = dIs.readLong();
                while (nEnts-- != 0) {
                    String encKey = dIs.readUTF();
                    String encVal = dIs.readUTF();
                    String key = SecurityUtil.encrypt(this.encryptor, encKey);
                    String val = SecurityUtil.encrypt(this.encryptor, encVal);
                    this.keyVals.put(new EncVal(encryptor, key, encKey), new EncVal(encryptor, val, encVal));
                }
            } catch (FileNotFoundException e) {
                log.warn(e);
                log.debug(e,e);
            } catch (IOException e) {
                AgentStorageException toThrow = new AgentStorageException("Error reading " +  this.keyValFile + ": " + e);
                toThrow.initCause(e);
                throw toThrow;
            }
        } finally {
            close(fIs);
        }
    }

    public void addObjectToFolder(String folderName, Object obj, long createTime, int maxElementsInFolder) {
        File folder = new File(this.writeDir + System.getProperty("file.separator") + folderName);
        if (!folder.exists()) {
            folder.mkdir();
        }
        File[] files = folder.listFiles();
        int numberOfElementsInFolder = files.length;
        if (numberOfElementsInFolder >= maxElementsInFolder) {
            // sort the files by create time
            Arrays.sort(files, new Comparator<File>() {
                public int compare(File f1, File f2) {
                    return (Long.valueOf(f1.lastModified()).compareTo(f2.lastModified()));
                }
            });
            int i = 0;
            while (numberOfElementsInFolder >= maxElementsInFolder) {
                files[i].delete();
                numberOfElementsInFolder--;
                i++;
            }
        }

        ObjectOutputStream outputStream = null;
        try {
            outputStream = new ObjectOutputStream(new FileOutputStream(folder.getAbsolutePath()
                    + System.getProperty("file.separator") + createTime));
            outputStream.writeObject(obj);
        } catch (Exception ex) {
        } finally {
            try {
                if (outputStream != null) {
                    outputStream.flush();
                    outputStream.close();
                }
            } catch (IOException ex) {
            }
        }
    }

    public void deleteObjectsFromFolder(String folderName, String... objects) {
        String folder = this.writeDir + System.getProperty("file.separator") + folderName;
        for (String object : objects) {
            File file = new File(folder + System.getProperty("file.separator") + object);
            if (!file.exists()) {
                log.warn("Cannot find file '" + object + "' to delete");
                continue;
            }
            if (!file.delete()) {
                log.warn("Cannot delete '" + object);
            }
        }
    }

    @SuppressWarnings("unchecked")
    public <T> List<T> getObjectsFromFolder(String folderName, int maxNumOfObjects) {
        List<T> objects = new ArrayList<T>();
        File folder = new File(this.writeDir + System.getProperty("file.separator") + folderName);
        if (!folder.exists()) {
            return objects;
        }

        File[] files = folder.listFiles();
        // sort the files by create time
        Arrays.sort(files, new Comparator<File>() {
            public int compare(File f1, File f2) {
                return -(Long.valueOf(f1.lastModified()).compareTo(f2.lastModified()));
            }
        });

        for (final File fileEntry : files) {
            if (maxNumOfObjects <= 0) {
                break;
            }
            ObjectInputStream inputStream = null;
            try {
                inputStream = new ObjectInputStream(new FileInputStream(fileEntry));
                objects.add((T) inputStream.readObject());
                maxNumOfObjects--;
            } catch (Exception ex) {
                log.error("Cannot read objects from '" + folderName + "'" + ex.getMessage());
            } finally {
                try {
                    inputStream.close();
                } catch (IOException e) {
                    log.error(e.getMessage());
                }
            }

        }
        return objects;
    }

    public void deleteObject(String objectName) {
        File file = new File(this.writeDir + System.getProperty("file.separator") + objectName);
        if (file.exists()) {
            if (!file.delete()) {
                log.warn("Cannot delete '" + objectName + "'");
            }
        } else {
            log.warn("File does not exists '" + objectName + "'");
        }
    }

    public void saveObject(Object obj, String objectName) {
        File file = new File(this.writeDir + System.getProperty("file.separator") + objectName);
        ObjectOutputStream outputStream = null;
        try {
            outputStream = new ObjectOutputStream(new FileOutputStream(file));
            outputStream.writeObject(obj);
        } catch (Exception ex) {
            log.error("Cannot save object '" + objectName + "'", ex);
        } finally {
            try {
                if (outputStream != null) {
                    outputStream.flush();
                    outputStream.close();
                }
            } catch (IOException ex) {
                log.error(ex);
            }
        }
    }

    @SuppressWarnings("unchecked")
    public <T> T getObject(String objectName) {
        File file = new File(this.writeDir + System.getProperty("file.separator") + objectName);
        if (!file.exists()) {
            log.info("Did not find object '" + objectName + "' in the local storage");
            return null;
        }
        ObjectInputStream inputStream = null;
        try {
            inputStream = new ObjectInputStream(new FileInputStream(file));
            Object result = (inputStream.readObject());
            return (T) result;
        } catch (Exception ex) {
            log.error("Cannot read object '" + objectName + "'" + ex.getMessage());
        } finally {
            try {
                inputStream.close();
            } catch (IOException e) {
                log.error(e.getMessage());
            }
        }
        return null;
    }

    private void close(FileInputStream fIs) {
        try {
            if(fIs != null) {
                fIs.close();
            }
        } catch(IOException e){
            log.debug(e,e);
        }
    }

    public void dispose(){
        if (shutdown.get()) {
            return;
        }
        try {
            shutdown.set(true);
            flush(true);
        } catch(Exception exc){
            log.error("Error flushing key/vals storage", exc);
        }
        for (final Entry<String, DiskList> entry : lists.entrySet()) {
            try {
                DiskList dl = entry.getValue();
                dl.close();
            } catch(Exception exc){
                log.error("Unable to dispose of disk list '" + entry.getKey() + "'", exc);
            }
        }

    }

    /*** LIST FUNCTIONALITY ***/

    public void addToList(String listName, String value) throws AgentStorageException {
        if (shutdown.get()) {
            return;
        }
        DiskList dList = getDiskList(listName);
        if (null == dList) {
            log.error("Error adding data , cannot read list '" + listName + "' from storage");
            return;
        }
        try {
            if (log.isDebugEnabled()) {
                log.debug("adding value to list=" + listName + ", value=" + value);
            }
            dList.addToList(value);
        } catch(IOException exc){
            log.error("Error adding to list '" + listName + "'", exc);
            AgentStorageException toThrow = new AgentStorageException("Error adding data to list: " + exc);
            toThrow.initCause(exc);
            throw toThrow;
        }
    }

    public void removeFromList(String listName, long recNumber) throws AgentStorageException {
        if (shutdown.get()) {
            return;
        }
        DiskList dList = getDiskList(listName);
        if (null == dList) {
            log.error("Error removing data , cannot read list '" + listName + "' " +
                    "from storage");
            return;
        }
        try {
            dList.removeRecord(recNumber);
        } catch(IOException exc){
            log.error("Error deleting from list '" + listName + "'", exc);
            AgentStorageException t = new AgentStorageException("Error deleting data from list: " + exc);
            t.initCause(exc);
            throw t;
        }
    }

    public void deleteList(String listName) {
        if (shutdown.get()) {
            return;
        }
        DiskList dList = getDiskList(listName);
        if (null == dList) {
            return ;
        }
        try {
            dList.deleteAllRecords();
        } catch(IOException exc){
            log.error("Error deleting all records", exc);
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
            dList = this.lists.get(listName);

            if(dList == null){
                try {
                    dList = intrCreateList(listName, RECSIZE);
                } catch(IOException exc){
                    log.error("Error loading disk list", exc);
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

    private class EncVal {
        private final String val;
        private String encrypted = null;
        private final PooledPBEStringEncryptor encryptor;
        private EncVal(PooledPBEStringEncryptor encryptor, String val, String encrypted) {
            this.val = val;
            this.encrypted = SecurityUtil.isMarkedEncrypted(encrypted) ? encrypted : null;
            this.encryptor = encryptor;
        }
        private EncVal(PooledPBEStringEncryptor encryptor, String val) {
            this.val = val;
            this.encryptor = encryptor;
        }
        private String getVal() {
            return val;
        }
        private String getEnc() {
            if (encrypted == null) {
                encrypted = SecurityUtil.encrypt(encryptor, val);
            }
            return encrypted;
        }
        @Override
        public String toString() {
            return val;
        }
        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (o instanceof EncVal) {
                EncVal v = (EncVal) o;
                return val.equals(v.val);
            }
            return false;
        }
        @Override
        public int hashCode() {
            return val.hashCode();
        }
    }
    
}

