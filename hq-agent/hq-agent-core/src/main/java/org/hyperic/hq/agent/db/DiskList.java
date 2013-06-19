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

package org.hyperic.hq.agent.db;

import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.EOFException;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.ArrayList;
import java.util.Collection;
import java.util.ConcurrentModificationException;
import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.Random;
import java.util.SortedSet;
import java.util.TreeSet;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hyperic.hq.agent.stats.AgentStatsCollector;

/**
 * A DiskList is a representation of a list on disk.  The basic
 * usage is to add members to the end of the list, and remove 
 * entries from it.  The iterators returned support the remove()
 * operation, but will throw a ConcurrentModificationException 
 * and fail-fast if an update is detected while iteration is occuring.
 *
 * The storage is contained in 2 files, one housing the data, and
 * the other housing the index.  
 *
 * The format of the data file is as follows:
 *
 * [Record]
 *      data     - recordSize bytes containing the raw data
 *
 * The format of the index file is as follows:
 *
 * [Idx]
 *      boolean  - Indicates whether the record is in use or not
 *      long int - Index of the previous record
 *      long int - Index of the next record
 */

public class DiskList {
    private static final int IDX_REC_LEN = 1 + 8 + 8;
    private static final Log log = LogFactory.getLog(DiskList.class.getName());

    private final String           fileName;
    private final String           idxFileName;
    private final RandomAccessFile indexFile;
    protected RandomAccessFile dataFile;
    private int              recordSize;  // Size of each record
    private long             firstRec;    // IDX of first record
    private long             lastRec;     // IDX of last record
    private final byte[]           padBytes;    // Utility array for padding
    protected SortedSet      freeList;    // Set(Long) of free rec idxs
    private int              modNum;      // Modification random number
    private final long             checkSize;   // Start to check for unused blocks
                                          // when the datafile reaches this 
                                          // size in bytes
    private final int              checkPerc;   // Max percent (0-100) of free space
                                          // allowed in the data file.  Only
                                          // significant when datafile size is
                                          // greated than checkSize
    private final long             maxLength;   // Max file size in bytes
    private final Random           rand;         
    private boolean          closed;
    private static final AgentStatsCollector statsCollector = AgentStatsCollector.getInstance();
    private static final String DISK_LIST_DISK_ITERATOR_REMOVE_TIME = AgentStatsCollector.DISK_LIST_DISK_ITERATOR_REMOVE_TIME;
    private static final String DISK_LIST_READ_RECORD_TIME = AgentStatsCollector.DISK_LIST_READ_RECORD_TIME;
    private static final String DISK_LIST_ADD_TO_LIST_TIME = AgentStatsCollector.DISK_LIST_ADD_TO_LIST_TIME;
    private static final String DISK_LIST_DELETE_ALL_RECORDS_TIME = AgentStatsCollector.DISK_LIST_DELETE_ALL_RECORDS_TIME;
    static {
        statsCollector.register(DISK_LIST_ADD_TO_LIST_TIME);
        statsCollector.register(DISK_LIST_READ_RECORD_TIME);
        statsCollector.register(DISK_LIST_DISK_ITERATOR_REMOVE_TIME);
        statsCollector.register(DISK_LIST_DELETE_ALL_RECORDS_TIME);
    }

    /**
     * Construct a new DiskList
     */
    public DiskList(File dataFile, int recordSize, long checkSize, int checkPerc) throws IOException {
        this(dataFile, recordSize, checkSize, checkPerc, Long.MAX_VALUE);
    }

    /**
     * Construct a new DiskList
     *
     * @param dataFile   the base location for the datafile.  The index
     *                   file will be the same, with .idx appended
     * @param recordSize The maximum size for any record within the 
     *                   data file.  All records in the list will
     *                   occupy this many bytes worth of data
     * @param checkSize  Size in to bytes to start checking for
     *                   unused blocks
     * @param checkPerc  Maximum percentage of free blocks allowed when
     *                   data file is greater than checkSize.
     */
    public DiskList(File dataFile, int recordSize, long checkSize, int checkPerc, long maxLength) throws IOException {
        File idxFile;
        idxFile          = new File(dataFile + ".idx");
        this.fileName    = dataFile.getName();
        this.idxFileName = idxFile.getName();
        this.rand        = new Random();
        this.dataFile    = new RandomAccessFile(dataFile, "rw");
        this.recordSize  = recordSize;
        this.padBytes    = new byte[Math.max(recordSize, IDX_REC_LEN)];
        this.modNum      = this.rand.nextInt();
        this.checkSize   = checkSize;
        this.checkPerc   = checkPerc;
        if (log.isDebugEnabled()) {
            log.debug("Setting max length for " + this.fileName + " to " + maxLength + " bytes");
        }
        this.maxLength   = maxLength;
        this.indexFile = new RandomAccessFile(idxFile, "rw");
        this.genFreeList(idxFile);
        this.closed    = false;
    }

    /**
     * Get the precentage of free space available in the datafile rounded
     * to the nearest whole number. (0-100)
     */
    private long getDataFileFreePercentage() throws IOException {
        double dataBytes = this.dataFile.length();
        double freeBytes = (this.freeList.size() * this.recordSize);

        return Math.round((freeBytes * 100) / dataBytes);
    }

    /**
     * Do maintenance on the data and index files.  If the datafile size and 
     * the free block percentage exceed the defined thresholds, the extra
     * free blocks will be removed by truncating the data and index files.
     *
     * Since truncation is used, some times it will be possible that even
     * though the criteria are met, we won't be able to delete the free space.
     * This is a recoverable situation though, since new blocks will be
     * inserted at the beginning of the data file.
     */
    private void doMaintenence() throws IOException {
        long lastData = this.dataFile.length()/this.recordSize;
        long lastFree = ((Long)this.freeList.last()).longValue();

        // Nothing we can do if the last block in the 
        // file is not free
        if (lastData != (lastFree + 1)) {
            return;
        }
        
        // Simple iteration of the list.  May be faster to do a
        // binary search using freeList.size() to determine if all
        // blocks are free, but this is more readable, and we are
        // dealing with small numbers (< a few million)
        long firstFree = lastFree;
        while (this.freeList.contains(new Long(firstFree - 1))) {
            firstFree--;
        }
        
        synchronized(this.dataFile) {
            // Truncate the data file
            this.dataFile.setLength(firstFree * this.recordSize);
            
            // Truncate the index file.
            this.indexFile.setLength(firstFree * IDX_REC_LEN);

            // Remove the free blocks deleted from the freelist.
            SortedSet subset = this.freeList.headSet(new Long(firstFree - 1));
            // Must create a new TreeSet, since the sorted set imposes
            // restrictions on maximum and minimum key values.
            this.freeList = new TreeSet(subset);
        }
        
        long num = (lastFree - firstFree) + 1;
        this.log.info("Deleted " + (num * this.recordSize) +
                      " bytes from " + this.fileName + 
                      " (" + num  + " blocks)");
    }

    /**
     * A quick routine, which simply zips through the index file,
     * pulling out information about which records are free.
     *
     * We open up the file seperately here, so we can use the
     * buffered input stream, which makes our initial startup much
     * faster, if there is a lot of data sitting in the list.
     */
    private void genFreeList(File idxFile)
        throws IOException
    {
        BufferedInputStream bIs;
        FileInputStream fIs = null;
        DataInputStream dIs;

        this.firstRec = -1;
        this.lastRec  = -1;

        // TreeSet is used here to ensure a natural ordering of
        // the elements.
        this.freeList = new TreeSet();
        
        try {
            fIs = new FileInputStream(idxFile);

            bIs = new BufferedInputStream(fIs);
            dIs = new DataInputStream(bIs);

            for(long idx=0; ; idx++){
                boolean used;
                long prev, next;
                
                try {
                    used = dIs.readBoolean();
                } catch(EOFException exc){
                    break;
                }
                
                prev = dIs.readLong(); 
                next = dIs.readLong(); 
                
                if(used == false){
                    this.freeList.add(new Long(idx));
                } else {
                    if (prev == -1) {
                        this.firstRec = idx;
                    }
                
                    if (next == -1) {
                        this.lastRec = idx;
                    }
                }
            }
        } catch(FileNotFoundException exc){
            return;
        } finally {
            try {
                if (fIs != null) {
                    fIs.close();
                }
            } catch (IOException exc) {
            }
        }
    }

    /**
     * Add the string to the list of data being stored in the DiskList.
     *
     * @param data Data to add to the end of the list
     */
    public void addToList(String data) throws IOException {
        if(this.closed){
            throw new IOException("Datafile already closed");
        }
        ByteArrayOutputStream bOs = new ByteArrayOutputStream(this.recordSize);
        DataOutputStream dOs = new DataOutputStream(bOs);
        dOs.writeUTF(data);
        if(bOs.size() > this.recordSize){
            throw new IOException("Data length(" + bOs.size() + ") exceeds " +
                                  "maximum record length(" + this.recordSize + ")");
        }
        final long start = now();
        bOs.write(this.padBytes, 0, this.recordSize - bOs.size());
        byte[] bytes = bOs.toByteArray();
        
        synchronized(this.dataFile){
            Long firstFreeL;
            long firstFree;

            this.modNum = this.rand.nextInt();

            try {
                firstFreeL = (Long)this.freeList.first();
                firstFree = firstFreeL.longValue();
                this.freeList.remove(firstFreeL);
            } catch(NoSuchElementException exc){
                // Else we're adding to the end
                firstFree = this.indexFile.length() / IDX_REC_LEN;
            }

            // Write the record to the data file
            this.dataFile.seek(firstFree * this.recordSize);
            this.dataFile.write(bytes);

            bOs.reset();
            dOs.writeBoolean(true);      // Is Used
            dOs.writeLong(this.lastRec); // Previous record idx
            dOs.writeLong(-1);           // Next record idx
            
            // Write the index for the record we just made
            this.indexFile.seek(firstFree * IDX_REC_LEN);
            bytes = bOs.toByteArray();
            this.indexFile.write(bytes, 0, bytes.length);

            // Update the previous 'last' record to point to us
            if(this.lastRec != -1){
                this.indexFile.seek((this.lastRec * IDX_REC_LEN) + 1 + 8);
                this.indexFile.writeLong(firstFree);
            } 

            this.lastRec = firstFree;
            if(this.firstRec == -1){
                this.firstRec = firstFree;
            }
        }

        if (this.dataFile.length() > this.maxLength) {
            this.log.error("Maximum file size for data file: " +
                           this.fileName + " reached (" +
                           this.maxLength + " bytes), truncating.");
            deleteAllRecords();
        }
        long duration = now() - start;
        statsCollector.addStat(duration, DISK_LIST_ADD_TO_LIST_TIME);
    }

    private static long now() {
        return System.currentTimeMillis();
    }

    private static class Record {
        private boolean isUsed;
        private long    prevIdx;
        private long    nextIdx;
        private String  data;
    }

    private Record readRecord(long recNo)
        throws IOException
    {
        Record res = new Record();

        if(recNo < 0){
            throw new IllegalArgumentException("IDX must be positive");
        }
                                               
        final long start = now();
        synchronized(this.dataFile){
            this.dataFile.seek(recNo * this.recordSize);
            res.data = this.dataFile.readUTF();

            this.indexFile.seek(recNo * IDX_REC_LEN);
            res.isUsed  = this.indexFile.readBoolean();
            res.prevIdx = this.indexFile.readLong();
            res.nextIdx = this.indexFile.readLong();
        }
        final long duration = now() - start;
        statsCollector.addStat(duration, DISK_LIST_READ_RECORD_TIME);

        return res;
    }

    /**
     * Delete all the records from storage. 
     */
    public void deleteAllRecords() throws IOException {
        IOException sExc = null;

        if(this.closed){
            throw new IOException("Datafile already closed");
        }
        final long start = now();

        synchronized(this.dataFile){
            this.modNum   = this.rand.nextInt();
            this.firstRec = -1;
            this.lastRec  = -1;

            try {
                this.indexFile.setLength(0);
            } catch(IOException exc){
                this.log.error("IOException while truncating file " + idxFileName);
                if (this.log.isDebugEnabled()) {
                    this.log.debug(exc);
                }
                sExc = exc;
            }

            try {
                this.dataFile.setLength(0);
            } catch(IOException exc){
                this.log.error("IOException while truncating file " + fileName);
                if (this.log.isDebugEnabled()) {
                    this.log.debug(exc);
                }
                if(sExc != null){
                    sExc = exc;
                }
            }
            this.freeList.clear();
        }
        final long duration = now() - start;
        statsCollector.addStat(duration, DISK_LIST_DELETE_ALL_RECORDS_TIME);
        
        if(sExc != null){
            throw sExc;
        }
    }

    public void removeRecord(long recNo)
        throws IOException
    {
        if(recNo < 0){
            throw new IllegalArgumentException("IDX must be positive");
        }

        synchronized(this.dataFile){
            long prevIdx, nextIdx;

            this.modNum = this.rand.nextInt();

            // Handle all the individual cases, to improve disk I/O performance
            // while maintaining data integrity if someone kills us during
            // the operation
            if(recNo == this.firstRec){
                if(recNo == this.lastRec){
                    // It's the only record -- it's unused, and add to freeList
                    this.firstRec = -1;
                    this.lastRec  = -1;
                } else {
                    // It's the first in the list, but not the last
                    this.indexFile.seek((recNo * IDX_REC_LEN) + 1 + 8);
                    nextIdx = this.indexFile.readLong();
                    
                    // Set next->prev to -1
                    this.indexFile.seek((nextIdx * IDX_REC_LEN) + 1);
                    this.indexFile.writeLong(-1);
                    
                    this.firstRec = nextIdx;
                }
            } else if(recNo == this.lastRec){
                // It's the last in the list, but not the first
                this.indexFile.seek((recNo * IDX_REC_LEN) + 1);
                prevIdx = this.indexFile.readLong();
                
                // Set prev->next to -1
                this.indexFile.seek((prevIdx * IDX_REC_LEN) + 1 + 8);
                this.indexFile.writeLong(-1);
                
                this.lastRec = prevIdx;
            } else {
                // Otherwise, it's somewhere in the middle, so we have to
                // update both the previous and next
                this.indexFile.seek((recNo * IDX_REC_LEN) + 1);
                prevIdx = this.indexFile.readLong();
                nextIdx = this.indexFile.readLong();
                
                // Set prev->next = next
                this.indexFile.seek((prevIdx * IDX_REC_LEN) + 1 + 8);
                this.indexFile.writeLong(nextIdx);

                // Set next->prev = prev
                this.indexFile.seek((nextIdx * IDX_REC_LEN) + 1);
                this.indexFile.writeLong(prevIdx);
            }

            this.indexFile.seek(recNo * IDX_REC_LEN);
            this.indexFile.writeBoolean(false);

            this.freeList.add(new Long(recNo));
        }

        long length = this.dataFile.length();
        long percFree = this.getDataFileFreePercentage();
        if ((length > this.checkSize) &&
            (percFree > this.checkPerc)) {
            this.doMaintenence();
        }
    }

    /**
     * Close the DiskList.  All subsequent methods will
     * result in an IOException being thrown.
     */
    public void close()
        throws IOException
    {
        IOException sExc = null;

        if(this.closed){
            throw new IOException("Datafile already closed");
        }

        this.closed = true;

        try {
            this.dataFile.close();
        } catch(IOException exc){
            this.log.error("IOException while closing file " + fileName);
            if (this.log.isDebugEnabled()) {
                this.log.debug(exc);
            }
            sExc = exc; 
        }

        try {
            this.indexFile.close();
        } catch(IOException exc){
            this.log.error("IOException while closing file " + idxFileName);
            if (this.log.isDebugEnabled()) {
                this.log.debug(exc);
            }
            if(sExc == null){
                sExc = exc;
            }
        }

        if(sExc != null){
            throw sExc;
        }
    }
    
    /**
     * This method converts lists from the old record size to the current one -
     * it reads all the records from the list using the old size, deletes the list
     * and than saves all the records using the current record size. 
     * should be used when starting the first time after an upgrade. In version
     * 4.6.5 the default record size was changed from 1024 to 4000 and when we
     * will try to read the records with size 4000 we will get an exception because the
     * records size is 1024. This is a fix for Jira bug [HHQ-5387].
     * @param oldSize - the old size of the record
     * @throws IOException 
     */
    public void convertListToCurrentRecordSize(int oldSize) throws IOException {
        log.info("Converting list on file '" + this.fileName + "' from size " + oldSize + " to size " + this.recordSize);
        int realRecSize = this.recordSize;
        this.recordSize = oldSize;
        Collection<String> records = new ArrayList<String>();
        Iterator<String> iter = getListIterator();
        for (; (iter != null) && iter.hasNext();) {
            String data = iter.next();
            records.add(data);
        }
        log.info("Read " + records.size() + " records from file '" + this.fileName + "'");        
        deleteAllRecords();

        this.recordSize = realRecSize;
        for (String rec : records) {
            addToList(rec);
        }
    }

    public static class DiskListIterator 
        implements Iterator<String>
    {
        private final DiskList diskList;  // Pointer back to the creating DiskList
        private long     nextIdx;   // Next index to read (or -1)
        private long     curIdx;
        private boolean  calledNext;
        private int      modNum;

        private DiskListIterator(DiskList diskList, long nextIdx, 
                                 int modNum)
        {
            this.diskList   = diskList;
            this.nextIdx    = nextIdx;
            this.curIdx     = -1;
            this.calledNext = false;
            this.modNum     = modNum;
        }

        public boolean hasNext(){
            return this.nextIdx != -1;
        }

        public String next() throws NoSuchElementException {
            Record rec;

            if(this.nextIdx == -1){
                throw new NoSuchElementException();
            }

            this.curIdx = this.nextIdx;
            synchronized(this.diskList.dataFile){
                if(this.diskList.modNum != this.modNum){
                    throw new ConcurrentModificationException();
                }

                try {
                    rec = this.diskList.readRecord(this.curIdx);
                } catch(IOException e){
                    log.error("IOException while reading record");
                    if (log.isDebugEnabled()) {
                        log.debug("IOException while trying to read record number " + this.curIdx, e);
                    }
                    NoSuchElementException ex = new NoSuchElementException("Error getting next element: " + e);
                    ex.initCause(e);
                    throw ex;
                }
            }

            this.nextIdx    = rec.nextIdx;
            this.calledNext = true;
            return rec.data;
        }
        
        public void remove(){
            if(!this.calledNext){
                throw new IllegalStateException("remove() called without first calling next()");
            }

            this.calledNext = false;
            final long start = now();

            synchronized(this.diskList.dataFile){
                if(this.diskList.modNum != this.modNum){
                    throw new ConcurrentModificationException();
                }

                try {
                    this.diskList.removeRecord(this.curIdx);
                } catch(IOException exc){
                    log.error("IOException while removing record");
                    if (log.isDebugEnabled()) {
                        log.debug(exc, exc);
                    }
                    throw new IllegalStateException("Error removing record: " + exc, exc);
                }

                this.modNum = this.diskList.modNum;
            }
            final long duration = now() - start;
            statsCollector.addStat(duration, DISK_LIST_DISK_ITERATOR_REMOVE_TIME);
        }
    }

    public Iterator<String> getListIterator(){
        synchronized(this.dataFile){
            // XXX -- This is broken, and is used to satisfy a lame 
            // requirement I made on the AgentStorageProvider interface.. :-(
            if(this.firstRec == -1){
                if (log.isDebugEnabled()) {
                    log.debug("getListIterator() - list '" + this.fileName + "' has no elements");
                }
                return null;
            }

            return new DiskListIterator(this, this.firstRec, this.modNum);
        }
    }
    
    public static void main(String[] args)
        throws Exception
    {
        DiskList d;
        long NUM = 1024 * 128;
        long count;

        System.out.println("Creating DiskList..");
        d = new DiskList(new File("mydb"), 1024, 2 * 1024 * 1024, 10);

        // Fill the entire file with data
        System.out.println("Adding " + NUM + " records..");
        for(int i=0; i<NUM; i++){
            d.addToList("one " + i);
        }
        
        // Remove 1/2 of the data
        count = NUM/2;
        System.out.println("Removing " + (NUM/2) + " records..");
        for (Iterator i = d.getListIterator(); i.hasNext() && (count > 0); count--) {
            String val = (String)i.next();
            i.remove();
        }

        // Add back 1/4 of the data
        System.out.println("Adding " + (NUM/4) + " records..");
        for (int i = 0; i < (NUM / 4); i++) {
            d.addToList("two " + i);
        }

        // Remove 1/2 of the data
        count = NUM/2;
        System.out.println("Removing " + (NUM/2) + " records..");
        for (Iterator i = d.getListIterator(); i.hasNext() && (count > 0); count--) {
            String val = (String)i.next();
            i.remove();
        }

        // Add back 1/4 of the data
        System.out.println("Adding " + (NUM/4) + " records..");
        for (int i = 0; i < (NUM / 4); i++) {
            d.addToList("three " + i);
        }

        // Remove all data
        System.out.println("Removing all data..");
        for(Iterator i=d.getListIterator(); i.hasNext();){
            String val = (String)i.next();
            i.remove();
        }

        // Add back 1/16 of the data
        System.out.println("Adding " + (NUM/16) + " records..");
        for (int i = 0; i < (NUM / 4); i++) {
            d.addToList("three " + i);
        }

        // Remove all data
        System.out.println("Removing all data..");
        for(Iterator i=d.getListIterator(); i.hasNext();){
            String val = (String)i.next();
            i.remove();
        }

        d.close();
    }
}
