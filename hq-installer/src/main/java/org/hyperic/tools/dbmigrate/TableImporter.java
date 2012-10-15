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

import static org.hyperic.tools.dbmigrate.Utils.COMMIT_INSTRUCTION_FLAG;
import static org.hyperic.tools.dbmigrate.Utils.ROLLBACK_INSTRUCTION_FLAG;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileFilter;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.StreamCorruptedException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingDeque;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Future;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.zip.GZIPInputStream;

import org.apache.tools.ant.Project;
import org.apache.tools.ant.Task;
import org.hyperic.tools.dbmigrate.Forker.ForkContext;
import org.hyperic.tools.dbmigrate.Forker.ForkWorker;
import org.hyperic.tools.dbmigrate.Forker.WorkerFactory;
import org.hyperic.tools.dbmigrate.TableImporter.Worker;
import org.hyperic.tools.dbmigrate.TableProcessor.Table;
import org.hyperic.util.MultiRuntimeException;

/**
 * Streams serialized data from file(s) concurrently into a target database (optimized for postgres)  
 * @author guy
 *
 */
public class TableImporter extends TableProcessor<Worker> {
	
    private static final int DEFAULT_FILE_INPUT_STREAMER_BUFFER_SIZE = 20000 ; 
    private int fisBufferSize = DEFAULT_FILE_INPUT_STREAMER_BUFFER_SIZE ; 
	private StringBuilder preImportActionsInstructions;
	private StringBuilder tablesList ; 
	private final String MIGRATION_FUNCTIONS_DIR = "/sql/migrationScripts/" ; 
	private final String IMPORT_SCRIPTS_FILE = MIGRATION_FUNCTIONS_DIR+ "import-scripts.sql" ; 
	
	private int noOfReindexers = 3 ; //no of workers handling the index recreations

	public TableImporter() {}//EOM
	
	/**
	 * @param Async fisBufferSize FileInputStreamReader buffer size  
	 */
	public final void setFisBufferSize(final int fisBufferSize) { 
	    this.fisBufferSize = fisBufferSize ; 
	}//EOM 
	
	/**
	 * @param iNoOfReindexers No of Workers handling the index recreations
	 */
	public final void setNoOfReindexers(final int iNoOfReindexers) {
	    this.noOfReindexers = iNoOfReindexers ; 
	}//EOM 
	
	/**
	 * Caches an additional pre-import comma delimited instructions list with the following format:<br/>
	 *     <<tableName>~<<0|1 truncation instruction>><~<0|1 index removal  instruction>><br/>
	 * and an additonal comma delimited table list for the post-import actions.
	 *      
	 * @param sink  
	 * @param table current table to add to the sink 
	 */
	@Override
	protected final void addTableToSink(final LinkedBlockingDeque<Table> sink, final Table table) {
	    super.addTableToSink(sink, table);
	    
	    if(this.tablesList == null) { 
	        this.preImportActionsInstructions = new StringBuilder() ;  
	        this.tablesList = new StringBuilder() ; 
	    }//EO if not yet initailized 
	    else { 
	        this.tablesList.append(",") ;
	        this.preImportActionsInstructions.append(",") ;
	    }//EO else if already initialized 
	    
	    this.tablesList.append(table.name) ; 
	    this.preImportActionsInstructions.append(table.name).append("~").append( table.shouldTruncate ? "1" : "0" ).
	      append("~").append(table instanceof BigTable ? "1" : "0") ; 
	    
	}//EOM 
	
	/**
	 * @param {@link ForkContext} 
	 * @param sink
	 */
	@Override
    protected final void beforeFork(final ForkContext<Table, Worker> context, final LinkedBlockingDeque<Table> sink) throws Throwable {
        MultiRuntimeException thrown = null;

        Connection conn = null;
        Statement stmt = null;
        try {
            final Project project = getProject();
            conn = this.getConnection(project.getProperties(), false);
            
            log("About to perform tables truncation, trigger disablement and indices drop with instructions: " + this.preImportActionsInstructions) ; 

            final File importScriptsFile = new File(project.getBaseDir(), IMPORT_SCRIPTS_FILE) ; 
            final String importFunctions = Utils.getFileContent(importScriptsFile) ;
            stmt = conn.createStatement();
            stmt.execute(importFunctions) ;  
                       
            stmt.executeQuery("select fmigrationPreConfigure('" + this.preImportActionsInstructions.toString() + "')") ;
            
        } catch (Throwable t) {
            System.err.println("An error had occured while loading the scripts file or executing the fmigrationPreConfigure scripts.") ;
            thrown = new MultiRuntimeException(t);
        } finally {
            Utils.close(thrown != null ? ROLLBACK_INSTRUCTION_FLAG : COMMIT_INSTRUCTION_FLAG, new Object[] { stmt, conn });
            
            if (thrown != null) throw thrown;
            else { 
                log("Pre import actions were sucessful.");
            }//EO else if success 
        }//EO catch block 
    }//EOM
	
	@Override
	protected final <Y, Z extends Callable<Y[]>> void afterFork(final ForkContext<Y, Z> context,
            final List<Future<Y[]>> workersResponses, final LinkedBlockingDeque<Y> sink) throws Throwable {      
	    
	    MultiRuntimeException thrown = null;
	    try{
	       super.afterFork(context, workersResponses, sink);
	    }catch(Throwable t){
	        thrown = new MultiRuntimeException(t);
	    }finally{
	        if(!isDisabled){
	            final Project project = getProject();
	            
	            final LinkedBlockingDeque<IndexRestorationTask> indexRestorationTasksSink = new LinkedBlockingDeque<IndexRestorationTask>() ;
                Connection conn = null;
                FileInputStream fis = null;
                Statement stmt = null;
                ResultSet rs = null ; 
                try{
                    conn = getConnection(project.getProperties());
                    
                    stmt = conn.createStatement();
                    rs = stmt.executeQuery((new StringBuilder()).append("select * from fmigrationPostConfigure('").
                                                            append(this.tablesList.toString()).append("')").toString());

                    String tableName = null, lastTableName = null, indexCreationStatement = null, indexName =null ; 
                    Table tableRef = null ; 
                    IndexRestorationTask indexRestorationTask = null ;
                    
                    while(rs.next()) {
                        indexName = rs.getString(1) ; 
                        indexCreationStatement = rs.getString(2) ;
                        tableName = rs.getString(3) ;
                        
                        if(!tableName.equals(lastTableName)) {
                            tableRef = this.tablesContainer.tables.get(tableName.toUpperCase()) ;
                            indexRestorationTask = new IndexRestorationTask(tableName, tableRef) ; 
                            indexRestorationTasksSink.add(indexRestorationTask) ;
                            lastTableName =  tableName ; 
                        }//EO if new table 
                        
                        indexRestorationTask.addIndex(indexName, indexCreationStatement) ; 
                           
                    }//EO while there are more indices to restore 
                }catch(Throwable t2) {
                    Utils.printStackTrace(t2);
                    thrown = MultiRuntimeException.newMultiRuntimeException(thrown, t2);
                }finally{
                    Utils.close(new Object[] { rs, stmt, conn, fis });
                }//EO catch block 
                
                //if there are index restoration tasks fork to perform them in parallel 
                this.restoreIndices(indexRestorationTasksSink) ; 
                
                try{ 
                    //get a new connection after the restoration 
                    conn = getConnection(project.getProperties());
                    
                    stmt = conn.createStatement();
                    log("About to drop the HQ_DROPPED_INDICES table") ; 
                    stmt.executeUpdate("drop table hq_dropped_indices cascade") ; 
                }catch(Throwable t3) {
                    Utils.printStackTrace(t3);
                    thrown = MultiRuntimeException.newMultiRuntimeException(thrown, t3);
                }finally{
                    Utils.close(new Object[] { stmt, conn });
                }//EO catch block 
                
            }//EO if not disabled 
            
	        if(thrown != null) throw thrown ; 
	    }//EO catch block 
	}//EOM

	private final void restoreIndices(final LinkedBlockingDeque<IndexRestorationTask> indexRestorationTasksSink) throws Throwable { 
	    final int iNoOfIndexRestorationTasks = indexRestorationTasksSink.size() ; 
        if(iNoOfIndexRestorationTasks > 0) {
            
            final String BIG_TABLE_PAUSE_LOCK_KEY = "btplk" ; 
           
            @SuppressWarnings("rawtypes")
            final Hashtable env = this.getProject().getProperties() ;
            
            final WorkerFactory<IndexRestorationTask,IndexRestorationWorker> indexRestorationTasksWorkerFactory = 
                    new WorkerFactory<IndexRestorationTask,IndexRestorationWorker>() {
                        public final IndexRestorationWorker newWorker(
                                final ForkContext<IndexRestorationTask,IndexRestorationWorker> paramForkContext) throws Throwable {
                            
                            final ReadWriteLock bigTablePauseLock = (ReadWriteLock) paramForkContext.get(BIG_TABLE_PAUSE_LOCK_KEY)  ;
                            final Connection conn = getConnection(env, false/*autoCommit*/);
                            return new IndexRestorationWorker(paramForkContext.getSemaphore(), conn, paramForkContext.getSink(), 
                                        paramForkContext.getAccumulatedErrorsSink(), bigTablePauseLock) ; 
                        }//EOM 
                
            };//EO anonymous class 
           
            //replace the worker factory with a new one creating an indexrestorationTaskWorker 
            final ForkContext<IndexRestorationTask,IndexRestorationWorker> indexRestorationContext = 
                    new ForkContext<IndexRestorationTask, IndexRestorationWorker>(
                            indexRestorationTasksSink, indexRestorationTasksWorkerFactory, env) ; 
            
            final ReadWriteLock bigTablePauseLock = new ReentrantReadWriteLock() ; 
            indexRestorationContext.put(BIG_TABLE_PAUSE_LOCK_KEY, bigTablePauseLock) ; 
            
            final List<Future<IndexRestorationTask[]>> indexRestorationTaskResponses = 
                    Forker.fork(iNoOfIndexRestorationTasks, this.noOfReindexers/*maxWorkers*/, indexRestorationContext) ;
            
            this.log("About to restore " + iNoOfIndexRestorationTasks + " primary keys and unique indices using " + 5 + " threads") ; 
            
            super.afterFork(indexRestorationContext, indexRestorationTaskResponses, indexRestorationTasksSink) ; 
        }//EO if there were index restoration tasks to perform 
	}//EOM 
	
	@Override
	@SuppressWarnings("rawtypes")
	protected final Connection getConnection(final Hashtable env) throws Throwable {
		return Utils.getDestinationConnection(env);
	}//EOM 

	@Override
	protected final Worker newWorkerInner(final ForkContext<Table, Worker> context, final Connection conn, final File stagingDir) {
		return new Worker(context.getSemaphore(), conn, context.getSink(), 
		        context.getAccumulatedErrorsSink(), stagingDir);
	}//EOM 
	
	private static final class TableBatchMetadata extends Table{
        final String insertSql;
        final String bindingParamsClause ;  
        final int columnCount;
        final DBDataType[] columnStrategies;
        final int[] columnTypes;
        File batchFile;

        public TableBatchMetadata(final String tableName, final String insertSql, final String bindingParamsClause, final int columnCount, final DBDataType[] columnStrategies,
                final int[] columnTypes, File batchFile, final AtomicInteger noOfProcessedRecords, final Map<String,Object> recordsPerFile) {
            super(tableName) ;
            this.insertSql = insertSql;
            this.bindingParamsClause = bindingParamsClause ; 
            this.columnCount = columnCount;
            this.columnStrategies = columnStrategies;
            this.columnTypes = columnTypes;
            this.batchFile = batchFile;
            this.noOfProcessedRecords = noOfProcessedRecords ;
            this.recordsPerFile = recordsPerFile ; 
        }//EOM 

        public TableBatchMetadata(final TableBatchMetadata copyConstructor, final File batchFile) {
            this(copyConstructor.name, copyConstructor.insertSql, copyConstructor.bindingParamsClause,
                    copyConstructor.columnCount,
                    copyConstructor.columnStrategies,
                    copyConstructor.columnTypes, batchFile, copyConstructor.noOfProcessedRecords, 
                    copyConstructor.recordsPerFile);
        }//EOM 
    }//EO inner class TableBatchMetadata 
	
	private static final class IndexRestorationTask  {
	    private List<IndexDetails> indexCreationStatementList ; 
	    private String tableName ; 
	    private Table tableMetadata ; 
	    
	    IndexRestorationTask(final String tableName, final Table tableRef) { 
	        this.tableName = tableName ; 
	        this.indexCreationStatementList = new ArrayList<IndexDetails>() ; 
	        this.tableMetadata = tableRef ; 
	    }//EOM 
	    
	    void addIndex(final String indexName, final String creationStatement) { 
	        this.indexCreationStatementList.add(new IndexDetails(indexName, creationStatement)) ; 
	    }//EOM

        @Override
        public final String toString() {
            return "IndexRestorationTask [indexCreationStatement=" + indexCreationStatementList + ", tableName="
                    + tableName + "]";
        }//EOM 
	    
        private static final class IndexDetails { 
            String indexName ; 
            String creationStatement ;  
           
            public IndexDetails(final String indexName, final String creationStatement) { 
                this.indexName = indexName ; 
                this.creationStatement = creationStatement ; 
            }//EOM 
            
            @Override
            public final String toString() {
                return "[indexName=" + indexName + ", creationStatement=" + creationStatement + "]";
            }//EOM 
            
        }//EO inner class IndexDetails 
        
	}//EO inner class IndexRestorationTask
	
	public final class IndexRestorationWorker extends ForkWorker<IndexRestorationTask> { 
	    
	    private final Lock smallTableLock ; 
	    private final Lock bigTableLock ; 
	    private static final int BIG_TABLE_THRESHOLD = 50000000 ; 
	    
	    IndexRestorationWorker(final CountDownLatch countdownSemaphore, final Connection conn, final BlockingDeque<IndexRestorationTask> sink, 
	            final MultiRuntimeException accumulatedErrors, final ReadWriteLock bigTablePauseLock) {  
            super(countdownSemaphore, conn, sink, IndexRestorationTask.class, accumulatedErrors);
            this.smallTableLock = bigTablePauseLock.readLock() ; 
            this.bigTableLock = bigTablePauseLock.writeLock() ; 
        }//EOM  

        @Override
        protected final void callInner(final IndexRestorationTask entity) throws Throwable {
           Statement stmt = null ; 
           
           final int noOfRecords = entity.tableMetadata.noOfProcessedRecords.get() ;  
           String msgSuffix = " lock for entity " + entity + ", no of Records " + noOfRecords ; 
           
           Lock currentLock = null ; 
           try{ 
               
                if(noOfRecords > BIG_TABLE_THRESHOLD) { 
                   log("Attempting to Acquire bigtable (writer)" + msgSuffix) ; 
                   currentLock = this.bigTableLock ; 
                   this.bigTableLock.lock() ;
                   log("-----> exited bigtable (writer)" + msgSuffix) ; 
               }else {
                   log("Attempting to Acquire smalltable (reader)" + msgSuffix) ;
                   currentLock = this.smallTableLock ; 
                   this.smallTableLock.lock() ;
                   log("exited smalltable (reader)" + msgSuffix) ; 
               }//EO else if small table 
                             
               final String logMsg = "[IndexRestorationWorker[" + entity.tableName + "; records "+ noOfRecords + "]: executing statements "  + entity.indexCreationStatementList ;   
               
               log(logMsg) ;  
        
               final long before = System.currentTimeMillis() ; 
               try{ 
                   stmt =  enumDatabaseType.createBatchStatementWithTimeout(this.conn, 0) ;
                   
                   for(org.hyperic.tools.dbmigrate.TableImporter.IndexRestorationTask.IndexDetails indexDetails : entity.indexCreationStatementList) {
                       stmt.addBatch(indexDetails.creationStatement) ;
                   }//EO while there are more statements
                        
                  stmt.executeBatch() ; 
                }finally{
                    Utils.close(new Object[] { stmt }); 
                    log(logMsg + " took: " + (System.currentTimeMillis() - before)) ;  
                  }//EO catch block 
           }finally{ 
               currentLock.unlock() ; 
           }//EO catch block 
        }//EOM 
	    
	}//EO inner class IndexRestorationWorker

    public final class Worker extends ForkWorker<Table> {
        private final File outputDir;
        private final FileInputStreamer fileInputStreamer ; 
        
        Worker(final CountDownLatch countdownSemaphore, final Connection conn, final BlockingDeque<Table> sink, final MultiRuntimeException accumulatedErrors, 
                final File outputDir){ 
            super(countdownSemaphore, conn, sink, Table.class, accumulatedErrors);
            this.outputDir = outputDir;
            
            //execute database specific logic to optimize the bulk import performance (see postgres database type)  
            enumDatabaseType.optimizeForBulkImport(this.conn) ; 
            
            //initialize the 1:1 async file input streamer (shall process multiple files) 
            this.fileInputStreamer = new FileInputStreamer(TableImporter.this) ;
        }//EOM
        
        protected final void callInner(Table entity) throws Throwable {
            if((entity.getClass() != TableBatchMetadata.class)) {
                final TableBatchMetadata synchronousBatch = this.distributeBatches(((Table)entity));
                if (synchronousBatch != null) entity = synchronousBatch;
            }//EO if entity was a string
            
            if((entity instanceof TableBatchMetadata)) { 
                this.processSingleBatch((TableBatchMetadata) entity);
            }//EO if entity was a TableBatchMetadata
        }//EOM 

        private final void processSingleBatch(final TableBatchMetadata batchMetadata) throws Throwable {
            
            PreparedStatement insertPs = null;
            try {
                //notify the streamer of a new file which requires processing 
                this.fileInputStreamer.newFile(batchMetadata.batchFile); 
                int recordCount = 0;

                final int columnCount = batchMetadata.columnCount;
                final DBDataType[] columnStrategies = batchMetadata.columnStrategies;
                final int[] columnTypes = batchMetadata.columnTypes;
                final String logMsgPrefix = "[" + batchMetadata.name + "]: ";
                
                //extract the number of records from the file name convension (last index of '_') 
                final String shortFileName = batchMetadata.batchFile.getName() ; 
                final String numberOfRecordsInFile = (String) batchMetadata.recordsPerFile.get(shortFileName) ;

                log(logMsgPrefix + " importing " + numberOfRecordsInFile + " records from file " + batchMetadata.batchFile);
                final int iNoOfExpectedRecords = Integer.parseInt(numberOfRecordsInFile) ; 
                
                //calculate the remainder to determine whether two separate insert statements are required: 
                //one for the batch size and the other for the remainder (if the batch size if bigger than the 
                //number of records then there would be a single statement for the remainder
                int noOfBatches = (iNoOfExpectedRecords / batchSize) ; 
                final int remainder = (iNoOfExpectedRecords % batchSize) ;
                
                String standardBatchInsertStatement = null, remainderBatchInsertStatement = null ; 
               
                if(noOfBatches > 0) { 
                    standardBatchInsertStatement = 
                            this.generateBindingParamsClauses(batchMetadata.insertSql, 
                                    batchMetadata.bindingParamsClause, batchSize) ; 
                }//EO if the batch size is smaller than the number of records 
                
                if(remainder > 0) { 
                    noOfBatches++ ; 
                    remainderBatchInsertStatement = this.generateBindingParamsClauses(batchMetadata.insertSql, 
                            batchMetadata.bindingParamsClause, remainder) ; 
                }//EO if there was a remainder 
                
                //start with the standard statement unless the batch size is bigger than the number of records 
                //in which case the remainder statement would be the only one (single batch)
                insertPs = this.conn.prepareStatement(
                        (standardBatchInsertStatement == null ? remainderBatchInsertStatement : standardBatchInsertStatement)
                        );
                
                Object oValue = null ; 
                Throwable fileStreamerException = null; 
                int bindingParamIndex = 1, batchCounter = 1 ; 
                //+1 for the EOF marker record 
                for(int j=0; j <=iNoOfExpectedRecords; j++) { 
                    
                    //if the Filestreamer had registered an exception,
                    if(this.fileInputStreamer.exception != null) { 
                        fileStreamerException = this.fileInputStreamer.exception ; 
                        throw fileStreamerException ;   
                    }//EO if the file streamer had an exception
                    
                    for (int i = 0; i < columnCount; i++) {
                        
                        oValue = this.fileInputStreamer.readObject();  
                        
                        if(oValue == Utils.EOF_PLACEHOLDER) break ; 
                        
                        columnStrategies[i].bindStatementParam(bindingParamIndex++, oValue, insertPs, columnTypes[i]);
                    }//EO while there are more columns
                    
                    if(oValue == Utils.EOF_PLACEHOLDER) break ;
                    
                    recordCount++;
                    
                    if (recordCount % batchSize == 0) {
                        insertPs.executeUpdate();
                        this.conn.commit();
                        
                        //if the batch counter indicates that the next batch is the last one 
                        //and there is a remainder statement replace the statement with the latter 
                        //else stick with the current 
                        if(batchCounter++ == noOfBatches-1 && remainderBatchInsertStatement != null) { 
                            Utils.close(new Object[] { insertPs });
                            insertPs = conn.prepareStatement(remainderBatchInsertStatement) ;
                        }//EO if last batch smaller than the batch size  
                        
                        log(logMsgPrefix + "Imported " + recordCount + " records so far from file " + shortFileName);
                        
                        //reset the bindingparamcounter 
                        bindingParamIndex = 1 ;
                    }//EO while the batchsize threshold was reached 
                 }//EO while there are more records to read 
               
                if (recordCount % batchSize != 0) {
                    insertPs.executeUpdate();
                    this.conn.commit();
                }//EO while there are more bytes available in the file 

                final int totalNumberOfRecordsSofar = batchMetadata.noOfProcessedRecords.addAndGet(recordCount) ; 
                log(logMsgPrefix + "Imported " + recordCount + " records overall from file " + batchMetadata.batchFile + "; Overall imoprted records for table " + batchMetadata.name + " so far: " + totalNumberOfRecordsSofar);
            }catch(Throwable t) {  
                this.fileInputStreamer.abortCurrentFilestreaming()  ;
                throw t ; 
            } finally {
                Utils.close(new Object[] { insertPs });
            }//EO catch block 
        }//EOM 
        
        private final String generateBindingParamsClauses(final String insertSql, final String singleBindingParamClauseTemplate, final int noOfRecords) { 

            //prepare the multi row sql statement
            final StringBuilder sqlBuilder = new StringBuilder(insertSql) ; 
            for(int j=0; j < noOfRecords; j++) {
                sqlBuilder.append(singleBindingParamClauseTemplate) ; 
                if(j < noOfRecords-1) sqlBuilder.append(",") ; 
            }//EO while there are more records 
            
            return sqlBuilder.toString() ; 
        }//EOM 
        
        private final TableBatchMetadata distributeBatches(Table table) throws Throwable {
          PreparedStatement selectPs = null;
    
          TableBatchMetadata singleBatchMetadata = null;
          final String tableName = table.name ; 
          try{
            final File parentDir = new File(this.outputDir, tableName);
            
            final File[] batchFiles = parentDir.listFiles(new FileFilter() {
                public final boolean accept(final File pathname) {
                    return !pathname.getName().endsWith(Utils.TABLE_METADATA_FILE_SUFFIX) ; 
                }//EOM 
            });//EO file filter 
              
            if (batchFiles == null) { 
                log("No import data was found for table " + tableName) ; 
                return singleBatchMetadata ; 
            }//EO else if there are no files for the given table 
            
            //read the columns clause from the metadata file and pass to the create insert statement 
            //this is required as db upgrades might leave tables with column order different than that
            //which would be generated by a clean install
            final Properties metadataProperties = this.readTableMetadataFile(parentDir, tableName) ;
            final String columnsClause = metadataProperties.getProperty(COLUMNS_KEY) ; 
              
            selectPs = this.conn.prepareStatement("SELECT "+columnsClause+" FROM " + tableName);
            selectPs.executeQuery();
            final ResultSetMetaData rsmd = selectPs.getMetaData();
    
            final int columnCount = rsmd.getColumnCount();
            final DBDataType[] columnStrategies = new DBDataType[columnCount];
            final int[] columnTypes = new int[columnCount];
            
            final String sqlParts[] = createInsertStatement(rsmd, tableName, columnStrategies, columnTypes);
             
            final int iNoOfBatches = batchFiles.length;
            log("Partitioning the import of table " + tableName + " into " + iNoOfBatches + " units") ; 
            
            @SuppressWarnings("unchecked")
            final TableBatchMetadata batchMetadataTemplate = new TableBatchMetadata(tableName, sqlParts[0], sqlParts[1], columnCount, columnStrategies, 
                    columnTypes, null, table.noOfProcessedRecords, (Map)metadataProperties);
            
            this.conn.commit();
            
            if (iNoOfBatches == 1) {
              singleBatchMetadata = batchMetadataTemplate;
              singleBatchMetadata.batchFile = batchFiles[0];
            }else {
                for (int i = 0; i < iNoOfBatches; i++) {
                this.sink.offer(new TableImporter.TableBatchMetadata(batchMetadataTemplate, batchFiles[i]));
              }//EO while there are more batches 
            }//EO else if more then one batch 
    
          }finally{ 
            Utils.close(new Object[] { selectPs }); 
          }//EO catch block 
    
          return singleBatchMetadata;
        }//EOM 
        
        private final String readTableMetadataFile1(final File parentDir, final String tableName) throws Throwable { 
            BufferedReader fr = null ; 
            String columnsClause = null ;
            try{ 
                fr = new BufferedReader(new FileReader(new File(parentDir, tableName + Utils.TABLE_METADATA_FILE_SUFFIX))) ;
                columnsClause = fr.readLine() ; 
                log(" ---- About to import " + fr.readLine() +  " records from table " + tableName) ; 
            }catch(Throwable t) { 
                log("Failed to read the columns clause for table " + tableName) ; 
                throw t ; 
            }finally{ 
                Utils.close(fr) ;
            }//EO catch block 
            
            return columnsClause ; 
        }//EOM 
        
        private final Properties readTableMetadataFile(final File parentDir, final String tableName) throws Throwable {
            final Properties metadataFile = new Properties() ; 
            BufferedReader fr = null ; 
            String columnsClause = null ;
            try{ 
                fr = new BufferedReader(new FileReader(new File(parentDir, tableName + Utils.TABLE_METADATA_FILE_SUFFIX))) ;
                metadataFile.load(fr) ; 
                log(" ---- About to import " + metadataFile.getProperty(TOTAL_REC_NO_KEY) +  " records from table " + tableName) ;
            }catch(Throwable t) { 
                log("Failed to read the columns clause for table " + tableName) ; 
                throw t ; 
            }finally{ 
                Utils.close(fr) ;
            }//EO catch block 
            
            return metadataFile; 
        }//EOM 

        final String[] createInsertStatement(final ResultSetMetaData rsmd, final String tableName, final DBDataType[] columnStrategies,
                    final int[] columnTypes) throws Throwable {
            
            final int iColumnCount = rsmd.getColumnCount();

            final StringBuilder bindingParamsBuilder = new StringBuilder();
            final StringBuilder statementBuilder = new StringBuilder("insert into ").append(tableName).append("(");

            for (int i = 1; i <= iColumnCount; i++) {
                int columnSqlType = rsmd.getColumnType(i);
                columnStrategies[(i - 1)] = DBDataType
                        .reverseValueOf(columnSqlType);
                columnTypes[(i - 1)] = columnSqlType;

                statementBuilder.append(rsmd.getColumnName(i));
                bindingParamsBuilder.append("?");
                if (i < iColumnCount) {
                    statementBuilder.append(","); 
                    bindingParamsBuilder.append(",");
                }//EO if last iteration 
            }//EO while there are more columns 

            return new String[] { statementBuilder.append(") values ").toString(), "(" + bindingParamsBuilder.append(")").toString() } ; 
        }//EOM 
        
        protected void dispose(MultiRuntimeException thrown) throws Throwable {
            try{
               //release the file streamer's resources and terminate its thread   
               this.fileInputStreamer.close() ; 
            }catch(Throwable t) { 
                MultiRuntimeException.newMultiRuntimeException(thrown, t);
            }//EO catch block 
            super.dispose(thrown);
        }//EOM 
    }//EO inner class Worker

    private final class FileInputStreamer extends FileStreamer<FileInputStreamerContext,ArrayBlockingQueue<Object>> { 
        
        private static final long WAIT_TIMEOUT_SECS = 30 ;
        
        private File inputFile ; 
        private FileInputStreamerContext context; 
        private String toStringRepresentation ; 
        private boolean abortCurrentFilestreaming ; 
        
        FileInputStreamer(final Task logger) { 
            super(logger) ;  
        }//EOM
        
        public final void abortCurrentFilestreaming() throws Throwable { 
            this.abortCurrentFilestreaming =  true ;
            this.exception = null ; 
        }//EOM 
        
        public final void newFile(final File inputFile) throws Throwable{ 
            final String toStringPrefix = this.getClass().getSimpleName() + " :[" + this.getId() + "]: " ; 
            //if the input file already exists, then there was some error 
            //on the client side which had aborted the parsing and new requests a new 
            //one. therefore prior to processing the new file all resources should be purged 
            //and a ready signal must be received from the streamer 
            synchronized(this) { 
                if(this.abortCurrentFilestreaming) {
                    this.logger.log(this.toStringRepresentation + " newFile() --> before waiting for previous operation to finish") ; 
                    this.interrupt() ; 
                    this.wait() ; 
                    this.logger.log(this.toStringRepresentation + " newFile() --> after waiting for previous operation to finish") ;
                }//EO if some file is still being processed 
                else if(this.isTerminated) { 
                    while(this.isTerminated) { 
                        this.logger.log(toStringPrefix + "newFile() --> waiting for thread to start") ;
                        this.wait(500) ; 
                        if(!this.isTerminated) this.logger.log(toStringPrefix + "newFile() --> thread started, configuring first file") ;
                    }//EO while terminated
                }//EO else if terminated
            }//EO sync block 
            
            final String inputFileName = inputFile.getName() ; 
            this.setName(inputFileName) ;
            this.inputFile = inputFile ; 
            this.toStringRepresentation = toStringPrefix + "[" + (this.inputFile == null ? "EMPTY" : inputFileName) + "]" ;
            FileInputStream fis = null ;
            GZIPInputStream zis = null ; 
            try{ 
                fis = new FileInputStream(this.inputFile) ; 
                zis = new GZIPInputStream(fis) ; 
                this.context.ois = new UTFNullHandlerOIS(zis);
                
                //notify this instance to commence handling the new file 
                //should have been put to wait in the streamOnEnity() if the EOF marker was encountered 
                synchronized(this) {
                    this.logger.log(this.toString() + " new file was received, waking up for processing") ;
                    this.notify() ;
                }//EO synchronized block
            }catch(Throwable t) { 
                Utils.printStackTrace(t, "context=" + (this.context == null ? "null" : this.context)) ; 
                Utils.close(fis, zis, this.context.ois) ; 
                throw t ; 
            }//EO catch block 
        }//EOM 
        
        public final Object readObject() throws InterruptedException{
            //this shall block until the queue is no longer empty for WAIT_TIMEOUT_SECS seconds and 
            //will throw an exception IllegalStateException if got a null value (i.e empty queue), taking into account the this.exception  
            //the exception would either be the this.exception if not null or IllegalStateException
            //if the value is NULL_OBJECT then the replace the value with actual null (required as 
            //null is used by the queue implementation to indicate empty queue) 
            final Object oValue = this.fileStreamingSink.poll(WAIT_TIMEOUT_SECS, TimeUnit.SECONDS) ;
            if(oValue == null) {
                 throw new IllegalStateException(this.toString() + " has been empty for over " + WAIT_TIMEOUT_SECS + " seconds, aborting reader", this.exception) ; 
            }//EO if the queue was empty for longer than WAIT_TIMEOUT_SECS 
            else return (oValue == NULL_OBJECT ? null : oValue) ; 
        }//EOM 
        
        private final void abortFileStreamingAndReset(final boolean clearSink) { 
            this.abortCurrentFilestreaming = false ; 
            this.inputFile = null ;
            this.exception = null ;  
            if(clearSink) this.fileStreamingSink.clear() ;
            Utils.close(context.ois) ;
            context.ois = null ; 
        }//EOM 
        
        @Override
        void close() throws Throwable{ 
            MultiRuntimeException thrown = null ;
            
            this.dispose(this.context) ;
           
            this.isTerminated = true ; 
            
            //invoke the interrupt on this instance in case its hung waiting for new files 
            try{ 
                synchronized(this) { 
                    this.notifyAll() ;
                }//EO sync block 
                
                this.logger.log(this.toStringRepresentation + ".close() --> about to interrupt") ; 
                
                this.interrupt() ;
            }catch(Throwable t) { 
                thrown = MultiRuntimeException.newMultiRuntimeException(null, t) ; 
            }//EO catch block 
            
            this.logger.log(this.toStringRepresentation + ".close() --> after interrupt") ;
            
            try{ 
                super.close(); 
            }catch(Throwable t) { 
                thrown = MultiRuntimeException.newMultiRuntimeException(thrown, t) ; 
            }//EO catch block 
            
            this.logger.log(this.toStringRepresentation + " at the end of the close()") ; 
            
            if(thrown != null) throw thrown ; 
        }//EOM 
        
        @Override
        protected final void dispose(final FileInputStreamerContext context) {
            //if already disposed exit 
            this.logger.log(this.toString() + " is Disposing") ; 
          
            //clear the sink and current file streaming resources
            this.abortFileStreamingAndReset(false/*clearSink*/) ; 
        }//EOM 
        
        @Override
        public final String toString() {
            return this.toStringRepresentation ;  
        }//EOM
        
        @Override
        protected final ArrayBlockingQueue<Object> newSink() {
            return new ArrayBlockingQueue<Object>(fisBufferSize) ; 
        }//EOM 
        
        @Override
        protected FileInputStreamerContext newFileStreamerContext() throws Throwable{
            this.context = new FileInputStreamerContext() ;
            //wait until the first file was configured before returning 
            synchronized (this) {
                this.isTerminated = false ; 
                this.notify() ; 
                this.wait() ;
                this.logger.log(this.toString() + " Commencing the processing of the first file") ; 
            }//EO synch block
            return this.context ; 
        }//EOM 
        
        @Override
        protected final void streamOneEntity(final FileInputStreamerContext context) throws Throwable {
            Object entity = null ; 
            try{ 
                if(this.abortCurrentFilestreaming){ 
                    throw new InterruptedException() ; 
                }//EO if should abort current file streaming 
                
                entity = context.ois.readUnshared() ;
            
                //if the entity is null replace it with NULL_OBJECT flag to be handled by the client consumer
                if(entity == null) entity = NULL_OBJECT ; 
                //this will wait until the buffer has space if full
                this.fileStreamingSink.put(entity) ;
                //if the entity is the EOF flag, dispose of current resources and await for new file   
                if(entity == Utils.EOF_PLACEHOLDER) { 
                    Utils.close(context.ois) ;
                    synchronized (this) {
                        this.logger.log(this.toString() + " EOF was encountered, waiting for a new file") ;
                       
                        this.wait() ;
                        this.logger.log(this.toString() + " new file was received, commencing processing") ;
                        
                    }//EO synch block 
                }//EO if EOF 
                
            }catch(InterruptedException ie) { 
                if(this.isTerminated) { 
                    this.logger.log(this.toString() + " a close request was intercepted aborting.") ;
                }else{ 
                    this.logger.log(this.toString() + " a request to abort the current file reading was intercepted, closing down resources ") ;
                    synchronized (this) {
                        this.abortFileStreamingAndReset(true/*clearSink*/) ; 
                        this.notify() ;
                        this.wait() ; 
                    }//EO sync block
                }//EO else if abort operation 

            }catch(Throwable t) { 
                //Allow the client (worker to decide on the failure action (i.e don't exit the while(True) loop.
                //this behaviour is required as this instance should be reusable 
                Utils.printStackTrace(t, this.toString()) ; 
                this.exception = t; 
            }//EO catch block
        }//EOM 
        
    }//EO inner class InputFileStreamer ; 
    
    public static final class FileInputStreamerContext extends FileStreamerContext { 
        ObjectInputStream ois;  
    }//EO inner class FileInputStreamerContext
    
    public static final class UTFNullHandlerOIS extends ObjectInputStream { 
        
        public UTFNullHandlerOIS(final InputStream in) throws IOException{ super(in) ; }//EOM 
        
        @Override
        public Object readUnshared() throws IOException, ClassNotFoundException {
            byte tc = this.readByte() ;
            //if null return the flag which would be converted to null by the client 
            if(tc == TC_NULL) return TC_NULL ;
            else if(tc == TC_MAX) return Utils.EOF_PLACEHOLDER ;
            else if(tc == TC_STRING) return super.readUTF() ; 
            else if(tc == TC_OBJECT) return super.readUnshared();
            else throw new StreamCorruptedException("telling byte " + tc + " is unrecognized") ;
        }//EOM 
        
        @Override
        public String readUTF() throws IOException {
            try{ 
                return (String) this.readUnshared() ;
            }catch(ClassNotFoundException cnfe) { 
                throw new IOException(cnfe) ; 
            }//EO catch block 
        }//EOM 
        
    }//EO inner class UTFNullHandlerOIS
	
}//EOC 
