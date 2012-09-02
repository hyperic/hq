/* **********************************************************************
/*
 * NOTE: This copyright does *not* cover user programs that use Hyperic
 * program services by normal system calls through the application
 * program interfaces provided as part of the Hyperic Plug-in Development
 * Kit or the Hyperic Client Development Kit - this is merely considered
 * normal use of the program, and does *not* fall under the heading of
 * "derived work".
 *
 * Copyright (C) [2004-2012], VMware, Inc.
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
package org.hyperic.tools.dbmigrate;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.Statement;
import java.util.Hashtable;
import java.util.List;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingDeque;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Future;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.zip.GZIPOutputStream;

import org.apache.tools.ant.Project;
import org.apache.tools.ant.Task;
import org.hyperic.tools.dbmigrate.Forker.ForkContext;
import org.hyperic.tools.dbmigrate.Forker.ForkWorker;
import org.hyperic.tools.dbmigrate.TableExporter.Worker;
import org.hyperic.tools.dbmigrate.TableProcessor.Table;
import org.hyperic.util.MultiRuntimeException;

/**
 * Database Exporter, streaming tables into files in serialization format concurrently 
 */
public class TableExporter extends TableProcessor<Worker> {
    
    private static final String HQ_SERVER_CONF_RELATIVE_PATH = "/source-artifacts/conf/hq-server.conf";
    
    /**
     * If the table is an instance of {@link BigTable}, the instance is clone for as many partitions defined in 
     * the {@link BigTable#noOfPartitions} value and added to the sink
     */
    @Override
    protected final void addTableToSink(final LinkedBlockingDeque<Table> sink, final Table table) {
        
        if(table instanceof BigTable) { 
            final BigTable bigTable = (BigTable) table ;
            final int noOfPartitions = bigTable.noOfPartitions ; 
            for (int i=0; i < noOfPartitions; i++) { 
                sink.add(bigTable.clone(i)) ;
            }//EO while there are more partitions
        }else { 
            super.addTableToSink(sink, table) ;
        }//EO else if table was not a big table  
        
    }//EOM 
    
    /**
     * Ensures that the data directory is empty prior to the export to avoid stale data from previous runs 
     */
    @Override
    protected final void clearResources(final File stagingDir) { 
        final File dataDir = new File(stagingDir, DATA_RELATIVE_DIR);
        Utils.deleteDirectory(dataDir);
    }//EOM
    
    @Override
    @SuppressWarnings("rawtypes")
    protected Connection getConnection(final Hashtable env) throws Throwable {
        return Utils.getSourceConnection(env);
    }//EOM 
    
    /**
     * Extracts the values of {@link Utils#DB_SCHEMA_VERSION_KEY} and  {@link Utils#HQ_BUILD_VERSION_KEY} from EAM_CONFIG_PROPS and 
     * injects them into the hq-server.conf file) 
     */
    @Override
    protected final <Y, Z extends Callable<Y[]>> void afterFork(final ForkContext<Y, Z> context,
            final List<Future<Y[]>> workersResponses, final LinkedBlockingDeque<Y> sink) throws Throwable {      
        
        MultiRuntimeException thrown = null;
        
        try{
            super.afterFork(context, workersResponses, sink);
        }catch(Throwable t) {
            thrown = new MultiRuntimeException(t);
        }finally {
            
            if(!isDisabled) {
                Statement stmt = null;
                ResultSet rs = null;
                FileOutputStream fos = null;
                Connection conn = null;
                
                try {
                    this.log("-----------------  Commencing hq-server.conf version embedding") ;
                    
                    final Project project = this.getProject() ; 
                    @SuppressWarnings("rawtypes")
                    final Hashtable env = project.getProperties();
                    conn = this.getConnection(env);
                    
                    final StringBuilder statementBuilder = new StringBuilder("SELECT PROPKEY, PROPVALUE FROM EAM_CONFIG_PROPS WHERE PROPKEY IN (");
                    
                    boolean missingProps = false;
                    if(!env.contains(Utils.DB_SCHEMA_VERSION_KEY)){
                        missingProps = true;
                        statementBuilder.append("'").append(Utils.DB_SCHEMA_VERSION_KEY).append("'");
                    }//EO if no database build version was in env 
                    
                    if(!env.contains(Utils.HQ_BUILD_VERSION_KEY)) {
                        missingProps = true;
                        statementBuilder.append(",'").append(Utils.HQ_BUILD_VERSION_KEY).append("')");
                    }//EO if no hq build version was in env 
                    
                    if(missingProps) {
                        
                        final String stagingServerConf = (String) context.getEnv().get(Utils.STAGING_DIR);
                        final File hqServerConfigFile = new File(stagingServerConf + HQ_SERVER_CONF_RELATIVE_PATH) ;
                        
                        fos = new FileOutputStream(hqServerConfigFile, true);
                        
                        stmt = conn.createStatement();
                        rs = stmt.executeQuery(statementBuilder.toString());
                        
                        String propKey = null;
                        String propValue = null;
                        
                        while(rs.next()) {
                            propKey = rs.getString(1);
                            propValue = rs.getString(2);
                            fos.write((new StringBuilder()).append("\n").append(propKey).append("=").append(propValue).toString().getBytes());
                            //put the property in the env for future use 
                            project.setProperty(propKey, propValue)  ;
                        }//EO while there are more records 

                    }//EO if there were missing properties
                    
                    this.log("-----------------  Finished embedding versions into hq-server.conf") ; 
                    
                }catch(Throwable t2) {
                    thrown = MultiRuntimeException.newMultiRuntimeException(thrown, t2);
                }finally {
                    Utils.close(new Object[] { fos, rs, stmt, conn });
                }//EO catch block 
            }//EO if not disabled 
            
            if(thrown != null) throw thrown;
        }//EO catch block  
    }//EOM 

    protected final Worker newWorkerInner(final ForkContext<Table,Worker> context, final Connection conn, final File stagingDir) {
        return new Worker(context.getSemaphore(), conn, context.getSink(), stagingDir);
    }//EOM 
    
    /**
     * {@link Table} entities exporter 
     */
    public final class Worker extends ForkWorker<Table> {

        private final File outputDir;
        private FileStreamer fileStreamer ; 
        
        Worker(final CountDownLatch countdownSemaphore, final Connection conn, final BlockingDeque<Table> sink, final File outputDir) {
            super(countdownSemaphore, conn, sink, Table.class);
            this.outputDir = outputDir;
            try{ 
                this.conn.setTransactionIsolation(Connection.TRANSACTION_READ_UNCOMMITTED) ;
                this.conn.setReadOnly(true) ; 
            }catch(Throwable t) { 
                throw (t instanceof RuntimeException ? (RuntimeException) t : new RuntimeException(t)) ; 
            }//EO catch block 
             
            this.fileStreamer = new FileStreamer(TableExporter.this) ; 
        }//EOM 
        
        @Override
        protected final void callInner(final Table table) throws Throwable {
            
            String traceMsgPrefix = null ; 
            Statement stmt = null;
            ResultSet rs = null;
            try{
                String tableName = table.name ; 
                String sql = "SELECT * FROM "  ;
                int partitionNo = 0 ; 
                
                //if a big table add the partitioning clause so as to select only the records pertaining to the 
                //current modulo remainder 
                if(table instanceof BigTable) { 
                    final BigTable bigTable = (BigTable) table ; 
                    final StringBuilder statementBuilder = new StringBuilder(sql).append(tableName).append(" WHERE ") ; 
                   
                    sql = enumDatabaseType.appendModuloClause(bigTable.partitionColumn,bigTable.noOfPartitions, statementBuilder).
                            append(" = ").append(bigTable.partitionNumber).toString() ;
                    
                    partitionNo = bigTable.partitionNumber ; 
                }else{ 
                    sql = sql + tableName ; 
                }//EO else if not a big table 
                
                final File tableParentDir = new File(this.outputDir, tableName);
                
                traceMsgPrefix = "----------------- [Table["+tableName+"];Partition["+partitionNo+"]]: " ;
                final String loopTraceMsgSuffix = " records so far." ;
                TableExporter.this.log(traceMsgPrefix + "Commencing the export into " + tableParentDir) ;
                this.fileStreamer.setName(traceMsgPrefix) ; 
                
                stmt = this.conn.createStatement(java.sql.ResultSet.TYPE_FORWARD_ONLY, java.sql.ResultSet.CONCUR_READ_ONLY) ; 
              
                //delegate the fetch size setting to the current database strategy  
                enumDatabaseType.setFetchSize(batchSize, (table instanceof BigTable)/*bigTable*/, stmt) ;                 
                stmt.setQueryTimeout(queryTimeoutSecs) ; 
                rs = stmt.executeQuery(sql) ; 
              
                final ResultSetMetaData rsmd = rs.getMetaData();
                final int columnCount = rsmd.getColumnCount();
                
                final DBDataType[] columns = new DBDataType[columnCount];

                int recordCount = 0;
                int batchCounter = 0;
                
                while (rs.next()) {
                    
                    if(this.fileStreamer.exception != null) throw this.fileStreamer.exception ;  
                    
                    //create the table parent directory if not yet created 
                    if(recordCount == 0 && !tableParentDir.exists()) tableParentDir.mkdir();
                    
                    if (recordCount == maxRecordsPerTable) break;
                    
                    //flush and close the current file (if not th first one) and create a new one 
                    //if the batch size threshold was reached 
                    if (recordCount % batchSize == 0) {
                        TableExporter.this.log(traceMsgPrefix + "exported " + recordCount + loopTraceMsgSuffix ) ;   
                        this.fileStreamer.newFile(tableParentDir, tableName + "_" + partitionNo + "_" + batchCounter++ + ".out");
                    }//EO if batch threshold was reached 

                    //stream the data from the resultset into the file using the DBDataType strategy corersponding to the 
                    //datatype 
                    for (int i = 0; i < columnCount; i++) {
                      if (columns[i] == null) columns[i] = DBDataType.reverseValueOf(rsmd.getColumnType(i + 1));
                      columns[i].serialize(this.fileStreamer, rs, i + 1);
                      
                    }//EO while there are more columns 

                    recordCount++;
                }//EO while there are more records 
                
                //flush and close the last file if exists
                String recordCountMsgPart = null ; 
                if(recordCount == 0) { 
                    recordCountMsgPart = "No Records where exported" ; 
                }else { 
                    recordCountMsgPart = "Finished exporting " + recordCount + " records" ;
                    //ous.write(ObjectOutputStream.TC_MAX) ;
                    this.fileStreamer.EOF() ; 
                }//EO if records were exported to file 
                
                final int totalNumberOfRecordsSofar = table.noOfProcessedRecords.addAndGet(recordCount) ;
                TableExporter.this.log(traceMsgPrefix + recordCountMsgPart +  
                        " for this partition Total records exported so far: " + totalNumberOfRecordsSofar) ;
            }catch(Throwable t){ 
                Utils.printStackTrace(t, traceMsgPrefix) ;
                throw t ; 
            }finally {
              Utils.close(new Object[] { rs, stmt });
            }//EO catch block 
        }//EOM 

        /**
         * Closes and existing {@link ObjectOutputStream} and opens a new one wrapped in a GZIPOutputStream to reduce disk size
         * @param tableName 
         * @param batchNo
         * @param partitionNumber
         * @param parentDir
         * @param existingOus {@link ObjectOutputStream} to close
         * @return new {@link ObjectOutputStream} directed to the file with the followingi format: <<table_name>>_<<partition_no>>_<<batch_no>>.out
         * @throws Throwable
         */
        private final ObjectOutputStream newOutputFile(final String tableName, final int batchNo, final int partitionNumber, 
                        final File parentDir, final ObjectOutputStream existingOus) throws Throwable {
            if(existingOus != null) {
                //write the EOF marker 
                existingOus.write(ObjectOutputStream.TC_MAX) ;
                existingOus.flush();
                existingOus.close();
            }//EO if existing output stream exists 
            
            final File outputFile = new File(parentDir, tableName + "_" + partitionNumber + "_" + batchNo + ".out");
            final FileOutputStream fos = new FileOutputStream(outputFile);
            final GZIPOutputStream gzos = new GZIPOutputStream(fos) ; 
            return new UTFNullHandlerOOS(gzos);
            //return new ObjectOutputStream(fos);
        }//EOM 

        /**
         * Deletes the content of the corresponding directory as no data is better than partial 
         * @param tableName
         * @param reason
         */
        @Override
        protected final void rollbackEntity(final Table table, final Throwable reason) {
            log("Failed to export table " + table.name + ", will disacrd all export data for it; Reason: " + reason, Project.MSG_ERR);
            this.fileStreamer.EOF() ; 
            final File tableDir = new File(outputDir, table.name);
            Utils.deleteDirectory(tableDir);
        }//EOM 
        
        @Override
        protected void dispose(MultiRuntimeException thrown) throws Throwable {
            try{ 
                this.fileStreamer.close() ; 
            }catch(Throwable t) { 
                MultiRuntimeException.newMultiRuntimeException(thrown, t);
            }//EO catch block 
            super.dispose(thrown);
        }//EOM 
         
    }//EO inner class Worker
    
    private static class FileStreamer extends Thread implements FileStream{ 
        
        //private ArrayBlockingQueue<Object> fileStreamingSink ;
        private BlockingOnConcurrentLinkedQueue fileStreamingSink ; 
        private boolean isTerminated ; 
        private Task logger ; 
        
        final static Byte NEW_CONFIGURATION_INSTR = (byte)0x7A;
        final static Byte EOS_INSTR = (byte)0x7B ; 
        final static Byte EOF_INSTR = ObjectOutputStream.TC_MAX ;
        final static Object NULL_STRING = new Object() ;
        final static Object NULL_OBJECT = ObjectOutputStream.TC_NULL ;   

        private Throwable exception; 
        
        public FileStreamer(final Task logger) { 
            this.logger = logger ; 
            this.fileStreamingSink = new BlockingOnConcurrentLinkedQueue<Object>() ; // new ArrayBlockingQueue<Object>(1000000) ;
            this.setDaemon(true) ; 
            this.start() ; 
        }//EOM
        
        final void newFile(final File parentDir, final String fileName) { 
            this.fileStreamingSink.add(NEW_CONFIGURATION_INSTR) ;
            this.fileStreamingSink.add(new File(parentDir, fileName)) ; 
        }//EOM 
        
        public final void write(final Object object) throws IOException{ 
            this.fileStreamingSink.add(object == null ? NULL_OBJECT : object) ; 
        }//EOM
        
        public final void writeUTF(final String value) throws IOException { 
            this.fileStreamingSink.add(value == null ? NULL_STRING : value) ; 
        }//EOM 
        
        final void EOF() { 
            this.fileStreamingSink.add(EOF_INSTR)  ;
        }//EOM 
        
        final void close() throws Throwable{ 
            this.fileStreamingSink.add(EOS_INSTR) ; 
            this.join() ; 
            if(this.exception != null) throw this.exception ; 
        }//EOM 
        
        @Override
        public void run() {
            
            logger.log(this.getName() + "  File streamer starting") ; 

            ObjectOutputStream ous = null;
            try{ 
                Object ovalue = null ; 
                File outputFile = null ; 
                while(!this.isTerminated) { 
                    
                    //ovalue = this.fileStreamingSink.take() ; 
                    ovalue = this.fileStreamingSink.poll() ;
                    if(ovalue == EOS_INSTR || ovalue == EOF_INSTR) { 
                        this.EOF(ous) ;
                        ous = null ;
                        if(ovalue == EOS_INSTR) this.isTerminated = true ; 
                    }else if (ovalue == NEW_CONFIGURATION_INSTR) { 
                       outputFile = (File) this.fileStreamingSink.poll() ; 
                       ous = this.newOutputFile(outputFile, ous) ; 
                    }else { 
                        
                        if(ovalue == NULL_STRING) ous.writeUTF(null) ; 
                        else if(ovalue instanceof String) { 
                            ous.writeUTF((String)ovalue) ; 
                        }else{ 
                            if(ovalue == NULL_OBJECT) ovalue = null ;
                            ous.writeUnshared(ovalue) ; 
                        }//EO else if not string 
                    }//EO if actual value 
                    
                }//EO while not terminated 
                
                logger.log(this.getName() + "File streamer finished succesfully") ; 
            }catch(Throwable t) { 
                Utils.printStackTrace(t, this.getName() + "An Exception Had occured during streaming to file") ; 
                this.exception = t ; 
                this.isTerminated = true ; 
            }finally{ 
                Utils.close(ous) ; 
            }//EO catch block 
            
        }//EOM 
        
        private final void EOF(final ObjectOutputStream existingOus) throws Throwable{ 
            if(existingOus != null) {
                //write the EOF marker 
                existingOus.write(ObjectOutputStream.TC_MAX) ;
                existingOus.flush();
                existingOus.close();
            }//EO if existingOus != null 
        }//EOM 
        
        private final ObjectOutputStream newOutputFile(final File newFile, final ObjectOutputStream existingOus) throws Throwable {
           
            if(existingOus != null) {
                this.EOF(existingOus) ; 
            }//EO if existing output stream exists 
            
            final FileOutputStream fos = new FileOutputStream(newFile);
            final GZIPOutputStream gzos = new GZIPOutputStream(fos) ; 
            return new UTFNullHandlerOOS(gzos);
            //return new ObjectOutputStream(fos);
        }//EOM 
        
    }//EO inner class FileStreamer ;
    
    public static final class BlockingOnConcurrentLinkedQueue<E> extends ConcurrentLinkedQueue<E> { 
        
        AtomicInteger count = new AtomicInteger() ; 
        final Object lock = new Object() ;
        
        @Override
        public boolean offer(E e) {
            final int currCount = count.getAndIncrement() ;  
            final boolean returnVal = super.offer(e) ; 
            if(currCount == 0) {
                synchronized(this.lock) { 
                    this.lock.notifyAll() ;
                }//EO sync
            }//EO if empty 
            return returnVal  ;
        }//EOM 
        
        @Override
        public E poll() {
            E value = null ; 
            
            while( (value = super.poll()) == null) { 
                try {
                    synchronized(this.lock) { 
                        this.lock.wait(1000) ;
                    }
                }catch(InterruptedException e) {
                    e.printStackTrace();
                }//EO catch block 
            }// while no value

            count.decrementAndGet() ; 
            return value ; 
        }//EOM 
    }//inner class BlockingOnConcurrentLinkedQueue
    
    /**
     * Specialized {@link ObjectOutputStream} supporting null UTF strings 
     */
    public static final class UTFNullHandlerOOS extends ObjectOutputStream { 
        
        public UTFNullHandlerOOS(final OutputStream out) throws IOException{ super(out) ; }//EOM 

        /**
         * Method to be used for any object other than string (e.g. blob byte[]) 
         */
        @Override
        public final void writeUnshared(final Object obj) throws IOException{
            //must prefix with the marked object for symmetry as the input handler always reads a single instruction 
            //byte prior to the actual value 
            this.writeByte(TC_OBJECT) ; 
            super.writeObject(obj) ;
        }//EOM 
        
        /**
         * Writes the UTF string to string replacing marking nulls with TC_NULL instruction byte. 
         * String values are prefixed by TC_STRING instruction byte 
         * @param str - string to read, might be null 
         */
        @Override
        public final void writeUTF(final String str) throws IOException {
            if(str == null) { 
                this.writeByte(TC_NULL) ;
            }else { 
                this.writeByte(TC_STRING) ; 
                super.writeUTF(str);
            }//EO else if not null 
        }//EOM 
        
    }//EO inner class UTFNullHandlerOOS
     
    //*****************************************************************************************************
    //DEBUG DEBUG DEBUG DEBUG DEBUG DEBUG DEBUG DEBUG DEBUG DEBUG DEBUG DEBUG DEBUG DEBUG DEBUG DEBUG DEBUG  
    //*****************************************************************************************************
    
    public static void main(String args[]) throws Throwable {
        Connection conn = null;
        Statement stmt = null;
        ResultSet rs = null;
        try
        {
            //String tablesClause = "EAM_CONFIG_RESPONSE,EAM_MEASUREMENT,EAM_MEASUREMENT_BL,EAM_SERVICE,EAM_SERVER,EAM_PLATFORM,EAM_AGENT";
            File file = new File("/work/workspaces/master-complete/hq/dist/installer/src/main/resources/data/sql/migrationScripts/import-scripts.sql");
            
            FileInputStream fis = new FileInputStream(file);
            byte arrBytes[] = new byte[fis.available()];
            fis.read(arrBytes);
            String sql = new String(arrBytes);
            System.out.println((new StringBuilder()).append("sql:\n").append(sql).toString());
            fis.close();
            conn = Utils.getPostgresConnection();
            conn.setAutoCommit(false);
            conn.commit();
            stmt = conn.createStatement();
            stmt.execute(sql);
            //stmt.executeQuery("select fmigrationPostConfigure('EAM_CONFIG_RESPONSE,EAM_MEASUREMENT,EAM_MEASUREMENT_BL,EAM_SERVICE,EAM_SERVER,EAM_PLATFORM,EAM_AGENT');");
            
            //stmt.executeQuery("select fToggleIndices('EAM_CONFIG_RESPONSE,EAM_MEASUREMENT,EAM_MEASUREMENT_BL,EAM_SERVICE,EAM_SERVER,EAM_PLATFORM,EAM_AGENT',true);");
            conn.commit();
            System.out.println("CREATED FUNCTION");
        }catch(Throwable t) {
            Utils.printStackTrace(t);
            t.printStackTrace();
        }finally {
            Utils.close(new Object[] { rs, stmt, conn });
        }//EO catch block 
    }//EOM 
    
    
    
    
}//EO class 
