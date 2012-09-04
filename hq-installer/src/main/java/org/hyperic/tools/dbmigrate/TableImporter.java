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
import java.io.BufferedWriter;
import java.io.EOFException;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.io.Serializable;
import java.io.StreamCorruptedException;
import java.io.Writer;
import java.math.BigDecimal;
import java.nio.ByteBuffer;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.Statement;
import java.sql.Types;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Hashtable;
import java.util.List;
import java.util.concurrent.BlockingDeque;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Future;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.zip.GZIPInputStream;

import org.apache.tools.ant.Project;
import org.hyperic.tools.dbmigrate.Forker.ForkContext;
import org.hyperic.tools.dbmigrate.Forker.ForkWorker;
import org.hyperic.tools.dbmigrate.Forker.WorkerFactory;
import org.hyperic.tools.dbmigrate.TableExporter.UTFNullHandlerOOS;
import org.hyperic.tools.dbmigrate.TableImporter.Worker;
import org.hyperic.tools.dbmigrate.TableProcessor.Table;
import org.hyperic.util.MultiRuntimeException;

/**
 * Streams serialized data from file(s) concurrently into a target database (optimized for postgres)  
 * @author guy
 *
 */
public class TableImporter extends TableProcessor<Worker> {
	
	private StringBuilder preImportActionsInstructions;
	private StringBuilder tablesList ; 
	private final String MIGRATION_FUNCTIONS_DIR = "/sql/migrationScripts/" ; 
	private final String IMPORT_SCRIPTS_FILE = MIGRATION_FUNCTIONS_DIR+ "import-scripts.sql" ; 
	
	private int noOfReindexers = 3 ; //no of workers handling the index recreations

	public TableImporter() {}//EOM 
	
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
                            return new IndexRestorationWorker(paramForkContext.getSemaphore(), conn, paramForkContext.getSink(), bigTablePauseLock) ; 
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
		return new Worker(context.getSemaphore(), conn, context.getSink(), stagingDir);
	}//EOM 
	
	private static final class TableBatchMetadata extends Table{
        final String insertSql;
        final int columnCount;
        final DBDataType[] columnStrategies;
        final int[] columnTypes;
        File batchFile;

        public TableBatchMetadata(final String tableName, final String insertSql, final int columnCount, final DBDataType[] columnStrategies,
                final int[] columnTypes, File batchFile, final AtomicInteger noOfProcessedRecords) {
            super(tableName) ;
            this.insertSql = insertSql;
            this.columnCount = columnCount;
            this.columnStrategies = columnStrategies;
            this.columnTypes = columnTypes;
            this.batchFile = batchFile;
            this.noOfProcessedRecords = noOfProcessedRecords ; 
        }//EOM 

        public TableBatchMetadata(final TableBatchMetadata copyConstructor, final File batchFile) {
            this(copyConstructor.name, copyConstructor.insertSql,
                    copyConstructor.columnCount,
                    copyConstructor.columnStrategies,
                    copyConstructor.columnTypes, batchFile, copyConstructor.noOfProcessedRecords);
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
	    
	    IndexRestorationWorker(final CountDownLatch countdownSemaphore, final Connection conn, final BlockingDeque<IndexRestorationTask> sink, final ReadWriteLock bigTablePauseLock) {  
            super(countdownSemaphore, conn, sink, IndexRestorationTask.class);
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
                   log("exited bigtable (writer)" + msgSuffix) ; 
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
        
        Worker(final CountDownLatch countdownSemaphore, final Connection conn, final BlockingDeque<Table> sink, final File outputDir){ 
            super(countdownSemaphore, conn, sink, Table.class);
            this.outputDir = outputDir;
            
            //execute database specific logic to optimize the bulk import performance (see postgres database type)  
            enumDatabaseType.optimizeForBulkImport(this.conn) ; 
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
            ObjectInputStream ois = null;
            try {
                final FileInputStream fis = new FileInputStream(batchMetadata.batchFile);
                final GZIPInputStream zis = new GZIPInputStream(fis) ; 
                ois = new UTFNullHandlerOIS(zis);
                int recordCount = 0;

                final int columnCount = batchMetadata.columnCount;
                final DBDataType[] columnStrategies = batchMetadata.columnStrategies;
                final int[] columnTypes = batchMetadata.columnTypes;
                final String logMsgPrefix = "[" + batchMetadata.name + "]: ";

                log(logMsgPrefix + " importing from file " + batchMetadata.batchFile);
                final String shortFileName = batchMetadata.batchFile.getName() ; 

                insertPs = this.conn.prepareStatement(batchMetadata.insertSql);
                
                Object oValue = null ; 
                
                while(true) { 
                    
                    for (int i = 1; i <= columnCount; i++) {
                        
                        oValue = ois.readUnshared();  
                        
                        if(oValue == Utils.EOF_PLACEHOLDER) break ; 
                        
                        columnStrategies[(i - 1)].bindStatementParam(i, oValue, insertPs, columnTypes[(i - 1)]);
                    }//EO while there are more columns
                    
                    if(oValue == Utils.EOF_PLACEHOLDER) break ;
                    
                    recordCount++;
                    
                    insertPs.addBatch();

                    if (recordCount % batchSize == 0) {
                        insertPs.executeBatch();
                        this.conn.commit();
                        log(logMsgPrefix + "Imported " + recordCount + " records so far from file " + shortFileName);
                    }//EO while the batchsize threshold was reached 
                }//EO while there are more records to read 
               
                if (recordCount % batchSize != 0) {
                    insertPs.executeBatch();
                    this.conn.commit();
                }//EO while there are more bytes available in the file 

                final int totalNumberOfRecordsSofar = batchMetadata.noOfProcessedRecords.addAndGet(recordCount) ; 
                log(logMsgPrefix + "Imported " + recordCount + " records overall from file " + batchMetadata.batchFile + "; Overall imoprted records for table " + batchMetadata.name + " so far: " + totalNumberOfRecordsSofar);
            } finally {
                Utils.close(new Object[] { insertPs, ois });
            }//EO catch block 
        }//EOM 

        private final TableBatchMetadata distributeBatches(Table table) throws Throwable {
          PreparedStatement selectPs = null;
    
          TableBatchMetadata singleBatchMetadata = null;
          final String tableName = table.name ; 
          try{
    
            selectPs = this.conn.prepareStatement("SELECT * FROM " + tableName);
            selectPs.executeQuery();
            final ResultSetMetaData rsmd = selectPs.getMetaData();
    
            final int columnCount = rsmd.getColumnCount();
            final DBDataType[] columnStrategies = new DBDataType[columnCount];
            final int[] columnTypes = new int[columnCount];
            final String insertSql = createInsertStatement(rsmd, tableName, columnStrategies, columnTypes);
    
            final File parentDir = new File(this.outputDir, tableName);
    
            final File[] batchFiles = parentDir.listFiles();
            
            if (batchFiles == null) { 
                log("No import data was found for table " + tableName) ; 
                return singleBatchMetadata ; 
            }//EO else if there are no files for the given table 
             
            final int iNoOfBatches = batchFiles.length;
            log("Partitioning the import of table " + tableName + " into " + iNoOfBatches + " units") ; 
            
            final TableBatchMetadata batchMetadataTemplate = new TableBatchMetadata(tableName, insertSql, columnCount, columnStrategies, columnTypes, null, table.noOfProcessedRecords);
            
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

        final String createInsertStatement(final ResultSetMetaData rsmd, final String tableName, final DBDataType[] columnStrategies,
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

            return statementBuilder.append(") values (").append(bindingParamsBuilder.toString()).append(")").toString(); 
        }//EOM 
    }//EO inner class Worker 
    
    public static final class UTFNullHandlerOIS extends ObjectInputStream { 
        
        public UTFNullHandlerOIS(final InputStream in) throws IOException{ super(in) ; }//EOM 
        
        @Override
        public Object readUnshared() throws IOException, ClassNotFoundException {
            byte tc = this.readByte() ;
            if(tc == TC_NULL) return null ;
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
        
    }//EOM 
	
}//EOC 
