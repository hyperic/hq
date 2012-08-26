/*
 * NOTE: This copyright does *not* cover user programs that use Hyperic
 * program services by normal system calls through the application
 * program interfaces provided as part of the Hyperic Plug-in Development
 * Kit or the Hyperic Client Development Kit - this is merely considered
 * normal use of the program, and does *not* fall under the heading of
 * "derived work".
 *
 * Copyright (C) [2004-2011], VMware, Inc.
 * This file is part of Hyperic.
 *
 * Hyperic is free software; you can redistribute it and/or modify
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
/**
 * Custom ant task responsible for the encryption of one or more table column data.<br> 
 * Encryption is done using a standard PBEWithMD5AndDES algorithm and the database password 
 * Encryption password.<br>
 * 
 * The columnar data encryption process is heavyweight due to the possible size of
 * the dataset as well as the complex nature of the actual values encyption.<br> 
 * To speed up the process, the logic partitions the database into pages and spawns workers to 
 * process the former.<br>  
 * 
 * <b>Note:</b> At the moment there are max of 4 workers (less if there are less partitions) <br>
 * as a small environment would probably have that many CPUs as well as a possible local <br>
 * database (more would max out the CPU utilization).<br>
 * <br>
 * <b>Note:</b> Each partition operation is atomic (committed separately). failure in one<br>
 * would not rollack other partitions commits  
 * 
 * @author guys
 */
package org.hyperic.tools.ant.dbupgrade;

import java.io.PrintStream;
import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Project;
import org.hyperic.tools.ant.utils.DatabaseType;
import org.hyperic.util.security.SecurityUtil;
import org.jasypt.encryption.pbe.PBEStringEncryptor;


public class SST_ColumnEncyptor extends SchemaSpecTask{
    
    private String table ; 
    private String pkColumn ; 
    private int noOfUpdateColumns ;
    private String columnsClause;
    private String updateColumnsClause ; 
    private int batchSize ; 
    private int iNoOfchunks ; 
    private PBEStringEncryptor encryptor;
    private DatabaseType enumDatabaseType ; 
     
    private static AtomicInteger pages ;
    private static AtomicInteger totalnoOfRecords = new AtomicInteger(0) ;
    private static final int DEFUALT_BATCH_SIZE = 1000 ;  
    
    public SST_ColumnEncyptor(){
    }//EOM 
    
    public final void setTable(final String table) { 
        this.table = table ; 
    }//EOM 
    
    public final void setPkColumn(final String pkColumn) { 
        this.pkColumn = pkColumn; 
    }//EOM 
    
    public final void setBatchSize(final String batchSize) { 
        this.batchSize = (batchSize == null ? DEFUALT_BATCH_SIZE : Integer.parseInt(batchSize)) ;  
    }//EOM 
    
    /**
     * Processes the columns list and weeds out duplicates. 
     * 
     * @param sColumns Comma delimited columns list. 
     */
    public final void setColumns(final String sColumns) {
        
        if(sColumns == null || sColumns.length() == 0) return ; 
        final String[] arrColumns = sColumns.split(",") ; 
        final int iOrigSize = arrColumns.length ; 
        
        if(iOrigSize == 1) { 
            
            this.columnsClause = sColumns ; 
            this.updateColumnsClause = sColumns + "=?" ; 
            this.noOfUpdateColumns = 1 ; 
            
        }else { 
            final HashSet<String> columns = new HashSet<String>(arrColumns.length) ; 
            
            final StringBuilder updateStatementBuilder = new StringBuilder() ; 
            final StringBuilder selectStatementbuilder = new StringBuilder() ;
                 
            for(final String column : arrColumns) {
                if(column.length() == 0 || columns.contains(columns)) continue ;
                //else 
                columns.add(column) ;
                selectStatementbuilder.append(column).append(',') ; 
                updateStatementBuilder.append(column).append("=?,") ; 
            }//EO while there are more columns
            
            selectStatementbuilder.deleteCharAt(selectStatementbuilder.length()-1) ; 
            updateStatementBuilder.deleteCharAt(updateStatementBuilder.length()-1) ; 
            
            this.columnsClause = selectStatementbuilder.toString() ; 
            this.updateColumnsClause = updateStatementBuilder.toString() ; 
            
            this.noOfUpdateColumns = columns.size() ;
        }//EO if multiple columns 
        
    }//EOM 
    
    @Override
    public final void initialize(final Connection conn, final DBUpgrader upgrader) {
        super.initialize(conn, upgrader);
        try{
            this.encryptor = upgrader.getEncryptor() ; 
            
            this._conn.setAutoCommit(false) ;
            
            //initialize the database type strategy. 
            this.enumDatabaseType = DatabaseType.valueOf(conn.getMetaData().getDatabaseProductName()) ;
            
        }catch(Throwable t) { 
            throw new BuildException(t) ; 
        }//EO catch block 
    }//EOM 
    
    /**
     * Invoked from the {@link SchemaSpec#execute()}. 
     * 
     * Partitions the dataset into logical pages by dividing the number of records by the defined 
     * batchSize<br> and spawns workers to handle individual pages in a separate thread of 
     * execution<br> 
     * 
     * <b>Note:</b> at the moment there are max of 4 workers (less if there are less partitions)<br>
     * as a small environment would probably have that many CPUs as well as a possible local<br>
     * database (more would max out the CPU utilization).<br>
     *  
     * <b>Note:</b> As there might be more partitions than there are worker instances,<br>
     *  the latter will keep consuming page processing requests until non are left (multiple <br>
     *  per worker instance).
     *  
     * <b>Note:</b> Each partition operation is atomic (committed separately). failure in one<br>
     * would not rollack other partitions commits  
     *   
     * <br> 
     * Main thread awaits the completion of all consumer threads before terminating. 
     */
    @Override
    public final void execute() throws BuildException { 
        
        PreparedStatement ps = null ; 
        ResultSet rs = null ;
        NestedBuildException thrownExcpetion = null ; 
        ExecutorService executorPool = null ; 

        try{ 
            //ensure batchsize is set if non was defined in the xml.
            if(this.batchSize == 0) this.batchSize = DEFUALT_BATCH_SIZE ; 
            
            final long before = System.currentTimeMillis() ; 
            
            //determine the dataset size first. 
            ps = this._conn.prepareStatement("select count("+this.pkColumn+") from " + this.table) ; 
            rs = ps.executeQuery() ;
            rs.next() ; 
            final int iNoOfExistingRecords = rs.getInt(1) ;
            //if the table is empty abort. 
            if(iNoOfExistingRecords == 0) return ;
            //calculate the number of partitions taking into account the remainder...
            this.iNoOfchunks =  (iNoOfExistingRecords+this.batchSize-1)/this.batchSize ;
            
            this.log("[SST_ColumnEncryptor.execute()]: No of records: " + iNoOfExistingRecords + " No of chunks: " + iNoOfchunks, Project.MSG_WARN);
            
            rs.close() ; 
            ps.close();   
            rs = null ; 
            ps = null ; 
            
            //initialize the decrementing shemaphore (waitgate) and the consumer buffer. 
            final CountDownLatch inverseSemaphore = new CountDownLatch(iNoOfchunks) ; 
            pages = new AtomicInteger(iNoOfchunks-1) ; 

            Connection conn = null ; 
            
            int iNoOfWorkers = 4 ; 
            if(iNoOfWorkers  > iNoOfchunks) iNoOfWorkers = iNoOfchunks ;
            
            this.log("[SST_ColumnEncryptor.execute()]: Starting update"); 
            
            //construct the paginated select statement for the given database product using 
            //the databaseType strategy. 
            final String selectStatement = this.enumDatabaseType.generatePagedQuery(
                    this.table, 
                    this.columnsClause, 
                    this.pkColumn) ;  
            
            //construct the update statement for the given database product using 
            //the databaseType strategy. 
            final String updateStatement = this.enumDatabaseType.generateUpdateQuery(  
                    this.table, 
                    this.updateColumnsClause,
                    this.pkColumn) ; 
            
            final List<Future<String>> workersFutures = new ArrayList<Future<String>>(iNoOfWorkers) ; 
            executorPool = Executors.newFixedThreadPool(iNoOfWorkers) ;
            Future<String> workerFuture = null ; 
            
            //spawn the workers ensuring each gets its own database connection and encryptor 
            //instances so as to minimize concurrency friction
            for(int i=0 ; i < iNoOfWorkers; i++) { 
                conn = this.getNewConnection() ;  
                conn.setAutoCommit(false) ; 
                workerFuture = executorPool.submit(new Worker(inverseSemaphore, conn, selectStatement, 
                        updateStatement, this.newEncryptor())) ;
                workersFutures.add(workerFuture) ; 
            }//EO while there are more exeuctors 
            
            //wait until the countdown latch reaches 0 (all workers are finished) before 
            //terminating 
            inverseSemaphore.await() ;
            
            //now verify that there no exceptions were returned (thrown) from the workers 
            for(Future<String> workerResponse : workersFutures) { 
                //should throw an exceptions if one was thrown from a worker thread 
                workerResponse.get() ; 
            }//EO while there are more worker responses 

            this.log("[SST_ColumnEncryptor.execute()]: after all workers are finished encrypting " + totalnoOfRecords.get() + " records in an overall time in millis: " + (System.currentTimeMillis()-before));
            
        }catch(Throwable t) {
            //must keep record of the exception as more can occur during the finally block 
            thrownExcpetion = new NestedBuildException(t) ; 
        }finally{
            try{ 
                //ensure all threads are killed 
                if(executorPool != null) executorPool.shutdown() ;
                
                if(rs != null) rs.close() ; 
                if(ps != null) ps.close() ;
                
            }catch(Throwable t){
                //if an exception was previously thrown, add this one as a nested otherwise create 
                //a new one 
                if(thrownExcpetion == null) { 
                    thrownExcpetion = new NestedBuildException(t) ; 
                }else { 
                    thrownExcpetion.addThrowable(t) ; 
                }//EO if an exception was already thrown 
            }//EO catch block
            
            //if an error had occurred, throw the exception (might contain multiple nested 
            //exceptions) 
            if(thrownExcpetion != null) {
                log(thrownExcpetion, Project.MSG_ERR) ; 
                throw thrownExcpetion ;  
            }//EO if there was an error 
        }//EO catch block
    }//EOM 
    
    /**
     * Asynchronous worker responsible for the encryption of one or more columnar dataset 
     * partitions.<br>
     * Inner instance class (so that it would have access to outer class instance members). 
     */
    private class Worker implements Callable<String> { 
        
        private final CountDownLatch countdownSemaphore  ;
        private final Connection conn ;
        private final PBEStringEncryptor encryptor ; 
        private final String selectStatement ; 
        private final String updateStatement ; 
        
        Worker(final CountDownLatch countdownSemaphore, final Connection conn,      
                    final String selectStatement, final String updateStatement, 
                    PBEStringEncryptor encryptor) { 
            this.countdownSemaphore = countdownSemaphore ; 
            this.conn = conn ;
            this.encryptor = encryptor ; 
            this.selectStatement = selectStatement ;
            this.updateStatement = updateStatement ; 
        }//EOM 
        
        /**
         * Encrypts one or more columnar dataset partitions.<br> 
         * The method acts as a consumer to the {@link SST_ColumnEncyptor#pages} buffer.<br> 
         * <br>
         * it Iterates over the buffer and processs dataset partitions base on the buffer 
         * value.<br>
         * <br>
         * Each partition processing is an atomic operation which would be committed separately.<br> 
         * <br> 
         * Update is performed by selecting the records from the table using the calculated<br> 
         * pagination information and for each record, create an update batch statement. 
         * <br> encryption would only occur IFF the value was not already encrypted.      
         */
        public String call() throws Exception { 
            
            final String msgPrefix = "[Encryptor Worker ("+Thread.currentThread().getName()+")]: " ;
            
            ResultSet rs = null ;
            PreparedStatement selectStatement = null, updateStatement = null ; 
            NestedBuildException thrownExcpetion = null ; 
           
            final DatabaseType enumDatabaseType = SST_ColumnEncyptor.this.enumDatabaseType ;
            final int iNoOfEncryptableColumns = SST_ColumnEncyptor.this.noOfUpdateColumns ; 

            int iCurrentPageNumber = 0 ; 
            final int iBatchSize = SST_ColumnEncyptor.this.batchSize ; 
            final int iNoOfChunks = SST_ColumnEncyptor.this.iNoOfchunks ; 
            String colVal = null ; 
            
            try{ 
                //iterate over the partitions buffer and process until there are non (buffer < 0) 
                //Note: cannot use the countDownLatch as the buffer as the countDown & getCount 
                //are not bound as one atomic operations.  
                while((iCurrentPageNumber = pages.getAndDecrement()) >= 0) { 
                    
                    long total = 0 ; 
    
                    try{
                        long before = System.currentTimeMillis() ; 
                               
                        long beforeSelect = System.currentTimeMillis() ; 
                       
                        selectStatement = this.conn.prepareStatement(this.selectStatement) ;
                        enumDatabaseType.bindPageInfo(selectStatement, iCurrentPageNumber, iBatchSize, iNoOfChunks) ; 
                        rs = selectStatement.executeQuery() ; 
                        rs.setFetchSize(iBatchSize) ;
                        
                        long afterSelect = (System.currentTimeMillis()-beforeSelect) ; 
                       
                        long beforeBatch = System.currentTimeMillis() ;
                        updateStatement = conn.prepareStatement(this.updateStatement) ;
                        
                        long beforeLoop= System.currentTimeMillis() ;
						boolean isDirty = false ; 
                        while(true) {
                            
                            long beforeSingleLoop = System.currentTimeMillis() ;
                            if(!rs.next()) break ; 
                            long afterSingleLoop = (System.currentTimeMillis()-beforeSingleLoop) ;
                            
                            //index starts from 2
                            for(int i=1; i <= iNoOfEncryptableColumns; i++) { 
                                colVal = rs.getString(i+1) ; 
                                
                                if(!SecurityUtil.isMarkedEncrypted(colVal)){  
                                    colVal = encryptor.encrypt(colVal) ;
									updateStatement.setString(i, colVal) ;
									isDirty = true ; 
                                }//EO if should encrypt
                                
                                
                            }//EO while there are more columns to encrypt 
                            
							if(isDirty) { 
		                        //set the where clause binding param to the next binding param index 
        	                    updateStatement.setString(iNoOfEncryptableColumns+1, rs.getString(1)) ; 
        	                    
        	                    updateStatement.addBatch() ; 
							}//EO if dirty 

							isDirty = false ; 
                           
                        }///EO while there are more records
                        long afterLoop = (System.currentTimeMillis()-beforeLoop) ;
                        
                        long beforeExecuteBatch= System.currentTimeMillis() ;
                        final int[] arrResults = updateStatement.executeBatch() ; 
                        long afterExecuteBatch = (System.currentTimeMillis()-beforeExecuteBatch) ;

						final int iLength = arrResults.length ; 
						for(int i=0; i<iLength; i++) { 
							if(arrResults[i] == PreparedStatement.EXECUTE_FAILED) { 
								log(msgPrefix + " Failed batch sequence: " + i) ; 
							}//EO if failure 
						}//EO while there are more results 
                        
                        long beforeCommit = System.currentTimeMillis() ;
                        this.conn.commit() ;
                        long afterCommit = (System.currentTimeMillis()-beforeCommit) ;
                        long afterBatch = (System.currentTimeMillis()-beforeBatch) ;
                        
                        rs.close() ; 
                        selectStatement.close() ; 
                        updateStatement.close() ;
                                
                        total += (System.currentTimeMillis()-before) ;
                        
                        totalnoOfRecords.addAndGet(iLength) ; 
                        
                        log(msgPrefix +  "Batch No: " + iCurrentPageNumber + " No of Records: " + iLength + " Total millis: " + (total)  + " select: " + afterSelect + " batch update: " + afterBatch + " commit time: " + afterCommit + " execute Batch: " + afterExecuteBatch + " loop: " + afterLoop ) ;
                    }catch(Throwable t) {
                        //must keep record of the exception as more can occur during the finally block
                        thrownExcpetion = new NestedBuildException(msgPrefix, t) ; 
        
                        try{ 
                            this.conn.rollback() ;
                        }catch(Throwable innerT) { 
                            thrownExcpetion.addThrowable(t) ; 
                        }//EO catch block 
                    }finally{
                        try{ 
                            if(rs != null) rs.close() ; 
                            if(selectStatement != null) selectStatement.close() ; 
                            
                            if(updateStatement != null) updateStatement.close() ; 
        
                        }catch(Throwable t){
                            //if an exception was previously thrown, add this one as a nested 
                            //otherwise create a new one 
                            if(thrownExcpetion == null) { 
                                thrownExcpetion = new NestedBuildException(msgPrefix, t) ; 
                            }else { 
                                thrownExcpetion.addThrowable(t) ; 
                            }//EO if an exception was already thrown 
                        }//EO catch block
                    
                        //decrement the gate semaphore 
                        this.countdownSemaphore.countDown() ;
                        log(msgPrefix +" after chunk countdown " + this.countdownSemaphore.getCount());
                        
                        if(thrownExcpetion != null) { 
                            log(thrownExcpetion, Project.MSG_ERR) ; 
                            throw thrownExcpetion ;  
                        }//EO if there was an error 
                    }//EO catch block
                    
                }//EO while there are more pages to work on
            
            }finally{ 
                this.conn.close() ; 
            }//EO catch block 
            
            log(msgPrefix +" exiting with chunks left: " + this.countdownSemaphore.getCount()) ;
            return null;
        }//EOM 
    }//EO inner class Worker
    
    /**
     * Exception container which delegates to its nested exceptions
     */
    private static final class NestedBuildException extends BuildException { 
        
        private final List<Throwable> nestedExcpetions ; 
        
        public NestedBuildException(Throwable t) {
            super() ; 
            this.nestedExcpetions = new ArrayList<Throwable>() ; 
            this.nestedExcpetions.add(t) ; 
        }//EOC 
        
        public NestedBuildException(final String message, Throwable t) {
            super(message) ; 
            this.nestedExcpetions = new ArrayList<Throwable>() ; 
            this.nestedExcpetions.add(t) ; 
        }//EOC 
        
        public final NestedBuildException addThrowable(final Throwable t) { 
            this.nestedExcpetions.add(t) ; 
            return this ;
        }//EOM 
        
        @Override
        public String getMessage() {
            String origMsg = (super.getMessage() == null ? "" : super.getMessage() + "\n") ;  
            
            final StringBuilder builder = new StringBuilder(origMsg).
                    append(this.nestedExcpetions.size()).append(" Excpetions had occured:") ;
            for(Throwable nested : this.nestedExcpetions) { 
                builder.append("\n--- Nested Exception ---\n").append(nested.getMessage()) ; 
            }//EO while there are more exceptions
            return builder.toString() ; 
        }//EOM 
        
        @Override
        public void printStackTrace(PrintStream ps) {
            synchronized (ps) {
                super.printStackTrace(ps);
                
                for(Throwable nested : this.nestedExcpetions){
                    ps.println("--- Nested Exception ---");
                    nested.printStackTrace(ps);
                }//while there are more nested exceptions
            }//EO sync block 
        }//EOM 

        /**
         * Prints the stack trace of this exception and any nested
         * exception to the specified PrintWriter.
         *
         * @param pw The PrintWriter to print the stack trace to.
         *           Must not be <code>null</code>.
         */
        @Override
        public void printStackTrace(PrintWriter pw) {
            synchronized (pw) {
                super.printStackTrace(pw);
                for(Throwable nested : this.nestedExcpetions){
                    pw.println("--- Nested Exception ---");
                    nested.printStackTrace(pw);
                }//while there are more nested exceptions
            }//EO sync block 
        }//EOM
        
    }//EO inner class NestedBuildException 
    
    
    
    
    public static void main(String[] args) throws Throwable {
        final int iNoOfExistingRecords = 950000 ; 
        final int iBatchSize = 10000 ; 
        final int iNoOfchunks =  (iNoOfExistingRecords+iBatchSize-1)/iBatchSize ;
        System.out.println(iNoOfchunks);
    }//EOM 

}//EOC 
