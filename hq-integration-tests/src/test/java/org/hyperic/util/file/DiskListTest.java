package org.hyperic.util.file;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.Iterator;

import junit.framework.TestCase;

public class DiskListTest extends TestCase
{
    private static final int RECSIZE  = 1024;
    private static final long MAXSIZE = 50 * 1024 * 1024; // 100MB
    private static final long CHKSIZE = 10 * 1024 * 1024;  // 20MB
    private static final int CHKPERC  = 50; // Only allow < 50% free
    private static final int MAXRECS = 2000;

    public DiskListTest() {}
    
    public void setUp() {
    }
        
    public void testReadWriteFile() throws Exception {

        DiskListDataHolder holder = null;

        try {

            try {
                holder = new DiskListDataHolder();
            } catch (Exception e) {
                e.printStackTrace();
                fail(e.toString());
            }

            for (long i = 0; i < MAXRECS; ++i) {
                String toPut = String.valueOf(i);
                holder.list.addToList(toPut);                
            }

            Iterator it = holder.list.getListIterator();
            // Check that we can read the proper number of records back
            long i = 0;
            while (it.hasNext()) {
                it.next();
                i++;
            }
            assertTrue(i == MAXRECS);

            holder.list.close();

            // Check that we can read the proper number after close/reopen, and that they can be cleanly deleted
            holder.list = new DiskList(holder.dataFile,
                                       RECSIZE,
                                       CHKSIZE,
                                       CHKPERC,
                                       MAXSIZE);
            it = holder.list.getListIterator();
            i = 0;
            while (it.hasNext()) {
                it.next();
                it.remove();

                i++;
            }

            assertTrue(i == MAXRECS);
            
        } finally {
            
            holder.dispose();
            
        }
    }
    
    public void testFreeListWithNoInserts() throws Exception {

        DiskListDataHolder holder = null;

        try {

            try {
                holder = new DiskListDataHolder();
            } catch (Exception e) {
                e.printStackTrace();
                fail(e.toString());
            }
                        
            holder.list.close();
            
            // Check that we can read the proper number after close/reopen, and that they can be cleanly deleted
            holder.list = new DiskList(holder.dataFile,
                                       RECSIZE,
                                       CHKSIZE,
                                       CHKPERC,
                                       MAXSIZE);
            
            assertTrue(holder.list.freeList.size() == 0);
            
        } finally {
            
            holder.dispose();
            
        }
    }    
    
    public void testFillAndReopen() throws Exception {

        DiskListDataHolder holder = null;

        try {

            try {
                holder = new DiskListDataHolder();
            } catch (Exception e) {
                e.printStackTrace();
                fail(e.toString());
            }
            
            String toPut = String.valueOf("dummystring");
            
            // Insert until we *almost* spill over
            long nRecs = 0;
            while (holder.list.dataFile.length() < MAXSIZE) {
                holder.list.addToList(toPut);
                nRecs++;
            }
                                    
            holder.list.close();
            
            // Check that we can read the proper number after close/reopen, and that they can be cleanly deleted
            holder.list = new DiskList(holder.dataFile,
                                       RECSIZE,
                                       CHKSIZE,
                                       CHKPERC,
                                       MAXSIZE);
            
            assertTrue(holder.list.freeList.size() == 0);
            
            Iterator it = holder.list.getListIterator();
            long nIterated = 0;
            while (it.hasNext()) {
                it.next();
                nIterated++;
            }

            assertTrue("Expected " + nRecs + " records, got " + nIterated,
                       nIterated == nRecs);
            
        } finally {
            
            holder.dispose();
            
        }
    }    
    
    public void testFillAndDeleteAllAndReopen() throws Exception {

        DiskListDataHolder holder = null;

        try {

            try {
                holder = new DiskListDataHolder();
            } catch (Exception e) {
                e.printStackTrace();
                fail(e.toString());
            }
            
            String toPut = String.valueOf("dummystring");
            
            // Insert until we *almost* spill over
            while (holder.list.dataFile.length() < MAXSIZE) {
                holder.list.addToList(toPut);
            }
            
            // Iterate and delete
            Iterator it = holder.list.getListIterator();
            while (it.hasNext()) {
                it.next();
                it.remove();
            }
                                    
            holder.list.close();
            
            // Check that we can read the proper number after close/reopen, and that they can be cleanly deleted
            holder.list = new DiskList(holder.dataFile,
                                       RECSIZE,
                                       CHKSIZE,
                                       CHKPERC,
                                       MAXSIZE);
            
            it = holder.list.getListIterator();
            // This is current behavior, but bad behavior
            assertNull(it);
            
        } finally {
            
            holder.dispose();
            
        }
    }    
    
    public void testFillAndDeleteAllButLastAndReopen() throws Exception {

        DiskListDataHolder holder = null;

        try {

            try {
                holder = new DiskListDataHolder();
            } catch (Exception e) {
                e.printStackTrace();
                fail(e.toString());
            }
            
            String toPut = String.valueOf("dummystring");
            
            // Insert until we *almost* spill over
            long nRecs = 0;
            while (holder.list.dataFile.length() < MAXSIZE) {
                holder.list.addToList(toPut);
                nRecs++;
            }
            
            // Iterate and delete all but the last record.  By not deleting the last record,
            // we prevent maintenance from happening.
            Iterator it = holder.list.getListIterator();
            long nDeleted = 0;
            while (it.hasNext()) {
                it.next();
                if (++nDeleted < nRecs) {
                    it.remove();
                }
            }
                                    
            holder.list.close();
            
            // Check that we can read the proper number after close/reopen, and that they can be cleanly deleted
            holder.list = new DiskList(holder.dataFile,
                                       RECSIZE,
                                       CHKSIZE,
                                       CHKPERC,
                                       MAXSIZE);
            
            it = holder.list.getListIterator();
            while (it.hasNext()) {
                it.next();
            }
            
        } finally {
            
            holder.dispose();
            
        }
    }    
    
    public void testFreeListWithInsertsAndNoDeletes() throws Exception {

        DiskListDataHolder holder = null;
        if (holder == null) return;

        try {

            try {
                holder = new DiskListDataHolder();
            } catch (Exception e) {
                e.printStackTrace();
                fail(e.toString());
            }
            
            for (long i = 0; i < MAXRECS; ++i) {
                String toPut = String.valueOf(i);
                holder.list.addToList(toPut);                
            }
            
            holder.list.close();
            
            // Check that we can read the proper number after close/reopen, and that they can be cleanly deleted
            holder.list = new DiskList(holder.dataFile,
                                       RECSIZE,
                                       CHKSIZE,
                                       CHKPERC,
                                       MAXSIZE);
            
            assertTrue(holder.list.freeList.size() == 0);
                        
        } finally {
            
            holder.dispose();
            
        }
    }    
    
    public void testFreeListWithInsertsAndDeletes() throws Exception {

        DiskListDataHolder holder = null;

        try {

            try {
                holder = new DiskListDataHolder();
            } catch (Exception e) {
                e.printStackTrace();
                fail(e.toString());
            }
            
            for (long i = 0; i < MAXRECS; ++i) {
                String toPut = String.valueOf(i);
                holder.list.addToList(toPut);                
            }
            
            holder.list.close();
            
            // Check that we can read the proper number after close/reopen, and that they can be cleanly deleted
            holder.list = new DiskList(holder.dataFile,
                                       RECSIZE,
                                       CHKSIZE,
                                       CHKPERC,
                                       MAXSIZE);
            Iterator it = holder.list.getListIterator();
            int nDeleted = 0;
            while (it.hasNext()) {
                for (int i = 0; i < 5; ++i) {
                    it.next();
                }
                it.remove();
                nDeleted++;
            }
            
            assertTrue(holder.list.freeList.size() == nDeleted);
            
        } finally {
            
            holder.dispose();
            
        }
    }

    public void testFreeListAfterTruncation() throws Exception {

        DiskListDataHolder holder = null;
        long epsilon = 50;
        
        try {

            try {
                holder = new DiskListDataHolder();
            } catch (Exception e) {
                e.printStackTrace();
                fail(e.toString());
            }

            String toPut = String.valueOf("dummystring");
            
            // Insert until we *almost* spill over
            while (holder.list.dataFile.length() + epsilon < MAXSIZE) {
                holder.list.addToList(toPut);                
            }
            
            assertTrue(holder.list.freeList.size() == 0);

            // Now insert until we spill over, expect a log message from this line
            while (holder.list.dataFile.length() > MAXSIZE - epsilon) {
                holder.list.addToList(toPut);                
            }
            
            assertTrue(holder.list.freeList.size() == 0);
            
            // After truncation, there should be no records, add one back and then delete it
            holder.list.addToList(toPut);
            Iterator it = holder.list.getListIterator();
            int nIterated = 0;
            while (it.hasNext()) {
                it.next();
                
                // Each remove should create one spot in the free list
                it.remove();
                nIterated++;
            }
            
            assertTrue(nIterated == 1);
            assertTrue(holder.list.freeList.size() == 1);
            
        } finally {
            
            holder.dispose();
            
        }
    }
    
    public void testFreeListAfterMaintenance() throws Exception {

        DiskListDataHolder holder = null;
        long epsilon = 50;
        
        try {

            try {
                holder = new DiskListDataHolder();
            } catch (Exception e) {
                e.printStackTrace();
                fail(e.toString());
            }

            String toPut = String.valueOf("dummystring");
            
            // Insert until we *almost* spill over
            long nInserted = 0;
            while (holder.list.dataFile.length() + epsilon < MAXSIZE) {
                holder.list.addToList(toPut);
                nInserted++;
            }
            
            int freeListSize = holder.list.freeList.size();
            assertTrue(freeListSize == 0);

            // Delete every fourth record as we buzz through the list.  Because CHKPERC is
            // 50%, deleting every fourth should NOT trigger maintenance
            int nIterated = 0;
            int nDeleted = 0;
            int counter = 0;
            Iterator it = holder.list.getListIterator();
            while (it.hasNext()) {
                it.next();
                
                if (++counter == 4) {
                    // Each remove should create one spot in the free list
                    it.remove();
                    nDeleted++;
                    counter = 0;
                }
                
                nIterated++;
            }
            
            assertTrue(nIterated == nInserted);
            freeListSize = holder.list.freeList.size();
            assertTrue(freeListSize == nDeleted);
            
            // Now delete the the same amount - 1: still should NOT trigger maintenance
            int nToDelete = nDeleted - 20;
            int nPreviouslyDeleted = nDeleted;
            nDeleted = 0;
            it = holder.list.getListIterator();
            while (it.hasNext() && nDeleted < nToDelete) {
                it.next();
                it.remove();
                nDeleted++;
            }
            
            assertTrue(nDeleted == nToDelete);
            freeListSize = holder.list.freeList.size();
            assertTrue(nDeleted + nPreviouslyDeleted == freeListSize);
            
            // Now try to trigger maintenance.  First: maintenance is only done if the last block is
            // free (maintenance truncates off the end only, no internal compacting), so make sure that
            // there is stuff to truncate
            int offTheEnd = 20;
            it = holder.list.getListIterator();
            for (int i = nDeleted + nPreviouslyDeleted; i < nInserted - offTheEnd; ++i) {
                it.next();
            }
            while (it.hasNext()) {
                it.next();
                it.remove();
            }
            
            // A few more deletes should trigger maintenance.  The calculation uses an integer
            // percentage rounded from doubles, so exactly (half - 1) deletes may not be enough.
            freeListSize = holder.list.freeList.size();
            int freeListPeakSize = freeListSize;
            long oldLength = holder.list.dataFile.length();
            int toTriggerMaintenance = ((int) (nInserted / 100)) / 2 + 1;
            it = holder.list.getListIterator();
            for (int i = 0; i < toTriggerMaintenance; ++i) {
                it.next();
                it.remove();
                freeListPeakSize = Math.max(freeListPeakSize, holder.list.freeList.size());
            }
            
            freeListSize = holder.list.freeList.size();
            long length = holder.list.dataFile.length();
            assertTrue("Expected free list size < " + freeListPeakSize + ", actual value was " + freeListSize,
                       freeListSize < freeListPeakSize);
            assertTrue("Expected file length < " + oldLength + ", actual value was " + length,
                       length < oldLength);
                        
        } finally {
            
            holder.dispose();
            
        }
    }
    
    private static class DiskListDataHolder {
        DiskList list;
        File dataFile;
        File indexFile;
        
        DiskListDataHolder() throws Exception {
            File tmpDirFile;
            
            String tmpDir = System.getProperty("java.io.tmpdir");
            if (tmpDir == null) {
                tmpDir = "/tmp";
            }
            
            tmpDirFile = new File(tmpDir);

            File dataFile = null;
            File indexFile = null;
            if (tmpDirFile.isDirectory() && tmpDirFile.canWrite()) {
                dataFile = new File(tmpDirFile, "datafile");
                indexFile = new File(tmpDirFile, "datafile.idx");
                dataFile.delete();
                indexFile.delete();
            } else {
                throw new IllegalStateException("Non-writeable directory!");
            }
            
            DiskList list = new DiskList(dataFile,
                                         RECSIZE,
                                         CHKSIZE,
                                         CHKPERC,
                                         MAXSIZE);
            
            this.list = list;
            this.dataFile = dataFile;
            this.indexFile = indexFile;
        }
        
        void dispose() throws Exception {
            list.close();
            dataFile.delete();
            indexFile.delete();
        }
    }
}
