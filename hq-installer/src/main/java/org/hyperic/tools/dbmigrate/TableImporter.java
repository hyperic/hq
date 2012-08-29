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

import edu.emory.mathcs.backport.java.util.Arrays;


public class TableImporter extends TableProcessor<Worker> {
	
	private StringBuilder preImportActionsInstructions;
	private StringBuilder tablesList ; 
	private final String MIGRATION_FUNCTIONS_DIR = "/sql/migrationScripts/" ; 
	private final String IMPORT_SCRIPTS_FILE = MIGRATION_FUNCTIONS_DIR+ "import-scripts.sql" ; 
	
	private int noOfReindexers = 3 ; 

	public TableImporter() {}//EOM 
	
	
	public final void setNoOfReindexers(final int iNoOfReindexers) {
	    this.noOfReindexers = iNoOfReindexers ; 
	}//EOM 
	
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

        private final String createInsertStatement(final ResultSetMetaData rsmd, final String tableName, final DBDataType[] columnStrategies,
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
	
	//*****************************************************************************************************
	//DEBUG DEBUG DEBUG DEBUG DEBUG DEBUG DEBUG DEBUG DEBUG DEBUG DEBUG DEBUG DEBUG DEBUG DEBUG DEBUG DEBUG  
	//*****************************************************************************************************

	private final void traceTableContent(final ObjectInputStream ois, final FileInputStream fis) throws Throwable {
		int iNoOfColumns = ois.readInt();
		System.out.println("NO of Columns: " + iNoOfColumns);

		while (fis.available() > 0) {
			System.out.println("new record " + iNoOfColumns);

			for (int i = 0; i < iNoOfColumns; i++)
				System.out.println("col " + i + ": " + ois.readObject());
		}//EO while there are more bytes to read 
	}//EOM 

	public static void main(String[] args) throws Throwable {
	    testReadFromCSV() ;
	    ///testStringComparison() ; 
	    //testResultsetfromFunction() ; 
	   // testPostgresQueryTimeout() ; 
	    //testToCharVsCharArray() ;
	    //testWriteObjectVsUTF() ;
	 //   writeToFileWithInitialPlaceHolder() ;
	    //testReadfromFile() ; 
	    if(true) return ; 
	    
	    
	    final Table table = new Table("tableName") ;
	    final Table batch = new TableBatchMetadata("tableName", null, 0, null,null, null, null) ;
	    testTable(table) ; 
	    testTable(batch) ;
	}//EOM
	
	private static final void testStringComparison() throws Throwable { 
	    
	    final String s = "BEGIN" ; 
	    final int iHashCode = s.hashCode() ; 
	    
	    System.out.println("BEGIN".intern() == s);
	    
	    int iterations = 1000000 ; 
	    for(int i=0;i< iterations; i++) ; 
	    
	    long before = System.currentTimeMillis() ; 
	    
	    boolean equals = false ; 
	    int hashcode = 0 ; 
	    for(int i=0;i< iterations; i++) { 
	        
	        equals = "BEGIN".equals(s) ; 
	        
	    }//EO while
	    
	    System.out.println(" equals Took: " + (System.currentTimeMillis()-before)) ;
	    
	    before = System.currentTimeMillis() ;
	    for(int i=0;i< iterations; i++) { 
            
	        equals = "BEGIN".hashCode() ==  iHashCode; 
            
        }//EO while
	    
	    System.out.println(" hashcode Took: " + (System.currentTimeMillis()-before)) ; 
	     
	}//EOM 
	
	public static final void testToCharVsCharArray() throws Throwable { 
	    
	    final int  iLength = 1000 ; 
	    final StringBuilder builder = new StringBuilder(iLength) ; 
	    for(int i=0; i < iLength; i++) { 
	        builder.append(i % 100 == 0 ? '\'' : "a") ;
	    }//EO while  
	    
	    final String str = builder.toString() ; 
	    
	    final int inoIfIterations = 6000000 ; 
	    long total = 0 ; 
	    long before = 0 ; 
	    
	    /*for(int j=0; j < inoIfIterations; j++) { 
	        
	        before = System.currentTimeMillis() ;
	        
    	    final char[] arr = str.toCharArray() ; 
    	    for(int i=0; i < iLength; i++) { 
    	        if(arr[i] == '\\' || arr[i] == '\'') {} ;  
    	    }//EO while 
    	    total += (System.currentTimeMillis()-before) ;
    	    
	    }//EO while more iterations 
	    
	    System.out.println("char array " + total);
	    */
	    StringBuilder sb = new StringBuilder(iLength);
	    char chr  ; 
	    total = 0 ;  
	    
        for(int j=0; j < inoIfIterations; j++) {
            before = System.currentTimeMillis() ;
            sb = new StringBuilder() ; 
            
            for(int i=0; i < iLength; i++) {
                chr = str.charAt(i) ; 
                if(chr == '\\' || chr == '\'') {
                    sb.append(chr);
                } ;  
                sb.append(chr);
            }//EO while
            total += (System.currentTimeMillis()-before) ;
        }//EO while there are more iterations 
       
        System.out.println("append " + total);
        
        total = 0 ; 
        int lastIndex = 0 ; 
        for(int j=0; j < inoIfIterations; j++) {
            before = System.currentTimeMillis() ;
            sb = new StringBuilder(iLength) ; 
            lastIndex = 0 ; 
            
            for(int i=0; i < iLength; i++) {
                
                chr = str.charAt(i) ; 
                if(chr == '\\' || chr == '\'') {
                    sb.append(str.substring(lastIndex, i)).append(chr) ; 
                  //  System.out.println(lastIndex + " : " + i);
                    lastIndex = i ; 
                } ;  
               
            }//EO while
            if(sb.length() == 0) sb.append(str) ;
            else sb.append(str.substring(lastIndex, iLength)) ;
            
            total += (System.currentTimeMillis()-before) ;
            
            //System.out.println(sb + " " + sb.length()) ;
        }//EO while there are more iterations 
       
        System.out.println("substring at " + total);
	    
	}//EOM 
	
	public static final void testTable(final Table table) { 
	    System.out.println("in Table " + table.getClass());
	}//EOM
	
	public static final void testTable(final TableBatchMetadata table) { 
        System.out.println("in Batch Table " + table.getClass());
    }//EOM 

	public static final void assertEAM_CONFIG_RESPONSE(final Connection conn)
			throws Throwable {
		System.out.println("******************************* DESERIALIZING:");
		PreparedStatement ps = null;
		ResultSet rs = null;
		try {
			ps = conn.prepareStatement("select * FROM EAM_CONFIG_RESPONSE");
			rs = ps.executeQuery();
			while (rs.next()) {
				decode(rs.getBinaryStream(3));
				decode(rs.getBinaryStream(4));
				decode(rs.getBinaryStream(5));
				decode(rs.getBinaryStream(6));
				decode(rs.getBinaryStream(7));
			}
		} finally {
			Utils.close(new Object[] { rs, ps });
		}//EO catch block 
	}//EOM 
	
	private static final void testResultsetfromFunction() throws Throwable { 
	    final Connection conn = Utils.getPostgresConnection() ;
	    conn.setAutoCommit(false) ; 
	    Statement stmt = null ; 
	    ResultSet rs = null ; 
	    try{ 
	        stmt = conn.createStatement() ; 
	        rs = stmt.executeQuery("select * from test1()") ;
	        while(rs.next()) { 
	            System.out.println(rs.getString(1) + "|" + rs.getString(2) + "|" + rs.getString(3));
	        }//EO while there are more records
	    }catch(Throwable t) { 
	        t.printStackTrace() ; 
	    } finally {
            Utils.close(Utils.ROLLBACK_INSTRUCTION_FLAG, new Object[] { rs, stmt, conn });
        }//EO catch block 
	}//eOM 
	
	private static final void testReadfromFile() throws Throwable { 
	    final String fileName = "/work/workspaces/master-complete/hq/dist/installer/modules/hq-migration/target/hq-migration-5.0/tmp/export-data/data/HQ_METRIC_DATA_0D_1S" ; 
	    
	    final File parentDir = new File(fileName) ; 
	    final File[] batchFiles = parentDir.listFiles() ; 
	    
	    String tableName = "HQ_METRIC_DATA_0D_1S" ; 
	    Thread t = null ; 
	    for(int i=0; i < 5; i++) { 
	        t = new Thread(new TestWorker(batchFiles[i], tableName), "Worker_" + i) ;
	        t.start() ;
	        t.join() ; 
	    }//EO while there are more files 
	    
	}//EOM 
	
	private static final class TestWorker implements Runnable { 
	    
	    private final File file ;
	    private final String tableName ; 
	    
	    TestWorker(final File file, final String tableName ) { 
	        this.file = file ;
	        this.tableName = tableName ; 
	    }//EOM 
	    
	    public final void run() { 
	        
	        ObjectInputStream ois = null ; 
	        Connection conn = null ; 
	        ResultSet rs = null ; 
	        Statement stmt = null  ;
	        PreparedStatement insertPs = null ; 
	        boolean commit = true ;
	        try{ 
	            final FileInputStream fis = new FileInputStream(this.file) ; 
	            final GZIPInputStream gis = new GZIPInputStream(fis) ; 
	            ois = new ObjectInputStream(gis) ;
	            
	            conn = Utils.getPostgresConnection() ; 
	           
	            Utils.executeUpdate(conn, "TRUNCATE TABLE " + tableName) ;
	            Utils.executeUpdate(conn, "ALTER TABLE " + tableName + " DISABLE TRIGGER ALL");
	            
	            stmt = conn.createStatement() ; 
	            rs = stmt.executeQuery("select * from " + this.tableName) ;
	            final ResultSetMetaData rsmd = rs.getMetaData() ; 
	            
	            final int columnCount = rsmd.getColumnCount();
	            final DBDataType[] columnStrategies = new DBDataType[columnCount];
	            final int[] columnTypes = new int[columnCount];
	            final String insertSql = new TableImporter().new Worker(null,null,null,null).createInsertStatement(rsmd, tableName, columnStrategies, columnTypes);
	            Utils.close(Utils.NOOP_INSTRUCTION_FLAG, new Object[]{rs, stmt}) ; 

	            conn.setAutoCommit(false) ;
	            insertPs = conn.prepareStatement(insertSql);
	            
	            Object oValue = null ; 
	            int count = 1 ; 
	            int columnType = -1 ; 
	            while(true) { 
	                
	                for (int i = 1; i <= columnCount; i++) {
	                    try{ 
	                        oValue = ois.readObject() ;
	                        
	                        if(oValue == Utils.EOF_PLACEHOLDER) break ;
	                        columnType =  columnTypes[i-1] ; 
	                        
	                        switch(columnType) { 
	                        case Types.BIGINT : { 
	                            insertPs.setLong(i, (Long)oValue) ; 
	                        }break;
	                        case Types.NUMERIC : { 
	                            insertPs.setBigDecimal(i, (BigDecimal)oValue) ; 
	                        }break; 
	                        case Types.INTEGER: { 
                                insertPs.setInt(i, (Integer)oValue) ; 
                            }break; 
	                        default :{ 
	                            insertPs.setObject(i, oValue, columnType); 
	                        }
	                        }//EO switch
                   
	                        //insertPs.setObject(i , oValue, columnType) ;
	                        
	                       // System.out.println("count " + count + " value " + oValue + " type " + oValue.getClass() + " db type " + columnTypes[i-1]); 
	                        
	                    }catch(EOFException eofe) { 
	                        eofe.printStackTrace() ; 
	                        throw eofe ; 
	                    }
	                    
	                }//EO while there are more columns
	                
	                if(oValue == Utils.EOF_PLACEHOLDER) break ;
	                
	                count++ ; 
	                
	                insertPs.addBatch() ; 
	            }
	            
	            //insertPs.executeBatch() ; 
	        }catch(Throwable t) { 
	            t.printStackTrace() ; 
	            commit = false ; 
	            throw new RuntimeException(t); 
	        }finally{ 
	            try{ 
	                ois.close() ;
	            }catch(Throwable t) {
	                t.printStackTrace() ; 
	                throw new RuntimeException(t) ; 
	            }//eO inner catch block
	            
	            Utils.close((!commit ? Utils.ROLLBACK_INSTRUCTION_FLAG : Utils.COMMIT_INSTRUCTION_FLAG), new Object[]{conn}) ; 
	        }//EO catch block 
        }//EOM 
	    
	}//EOM
	
	private static final void testPostgresQueryTimeout() throws Throwable { 
	    final Connection conn = Utils.getPostgresConnection() ;
	    conn.setAutoCommit(false) ; 
	    Statement stmt = null ; 
	    System.out.println("started");
        final long before = System.currentTimeMillis() ;
	    try{ 
	         
	        final String sql = "select fmigrationPostConfigure('EAM_SERVICE_TYPE,EAM_SERVER_TYPE,EAM_METRIC_PROB,HQ_METRIC_DATA_4D_1S,EAM_CONTROL_SCHEDULE,HQ_METRIC_DATA_0D_1S,EAM_GALERT_DEFS,EAM_RESOURCE_EDGE,EAM_DASH_CONFIG,EAM_MEASUREMENT_BL,EAM_AIQ_PLATFORM,EAM_SERVICE_DEP_MAP,EAM_AUTOINV_HISTORY,EAM_VIRTUAL,EAM_AUDIT,EAM_ESCALATION,HQ_METRIC_DATA_4D_0S,EAM_MEASUREMENT_DATA_1D,EAM_CONTROL_HISTORY,HQ_METRIC_DATA_0D_0S,EAM_MEASUREMENT,EAM_UPDATE_STATUS,EAM_IP,EAM_MEASUREMENT_DATA_1H,EAM_OPERATION,EAM_CRITERIA,EAM_REGISTERED_TRIGGER,EAM_UI_ATTACH_MAST,EAM_SUBJECT_ROLE_MAP,HQ_METRIC_DATA_8D_0S,EAM_AIQ_IP,EAM_ROLE_OPERATION_MAP,EAM_UI_ATTACH_RSRC,HQ_METRIC_DATA_5D_0S,EAM_PLATFORM_SERVER_TYPE_MAP,EAM_UI_VIEW_RESOURCE,EAM_ALERT_ACTION_LOG,EAM_CALENDAR,EAM_RES_GRP_RES_MAP,EAM_SERVICE_REQUEST,HQ_AVAIL_DATA_RLE,EAM_APP_SERVICE,EAM_UI_ATTACH_ADMIN,EAM_ALERT_CONDITION,EAM_ALERT,EAM_SUBJECT,HQ_METRIC_DATA_8D_1S,EAM_METRIC_AUX_LOGS,EAM_SERVICE,EAM_UI_VIEW_MASTHEAD,EAM_EVENT_LOG,EAM_APPLICATION_TYPE,EAM_CALENDAR_ENT,HQ_METRIC_DATA_5D_1S,EAM_CRISPO_ARRAY,EAM_PRINCIPAL,EAM_RESOURCE_GROUP,EAM_ROLE,EAM_ROLE_CALENDAR,HQ_METRIC_DATA_7D_0S,EAM_NUMBERS,EAM_GALERT_ACTION_LOG,EAM_CRISPO_OPT,EAM_SRN,EAM_UI_PLUGIN,EAM_RESOURCE_TYPE,EAM_AGENT_TYPE,HQ_METRIC_DATA_7D_1S,EAM_ESCALATION_ACTION,EAM_CPROP,EAM_MEASUREMENT_CAT,EAM_CPROP_KEY,EAM_RESOURCE_AUX_LOGS,EAM_ERROR_CODE,EAM_EXEC_STRATEGIES,EAM_AGENT,EAM_AGENT_PLUGIN_STATUS,EAM_CRISPO,EAM_ALERT_DEF_STATE,EAM_GALERT_AUX_LOGS,EAM_PLATFORM,EAM_UI_ATTACHMENT,EAM_PLATFORM_TYPE,EAM_ALERT_CONDITION_LOG,HQ_METRIC_DATA_2D_1S,EAM_GTRIGGER_TYPES,EAM_RESOURCE,HQ_METRIC_DATA_6D_1S,EAM_APP_TYPE_SERVICE_TYPE_MAP,EAM_UI_VIEW,EAM_UI_VIEW_ADMIN,EAM_CONFIG_PROPS,EAM_APPLICATION,EAM_AUTOINV_SCHEDULE,EAM_AIQ_SERVICE,HQ_METRIC_DATA_3D_0S,EAM_SERVER,EAM_CALENDAR_WEEK,HQ_METRIC_DATA_COMPAT,EAM_MEASUREMENT_TEMPL,EAM_ACTION,EAM_MEASUREMENT_DATA_6H,HQ_METRIC_DATA_1D_1S,EAM_ROLE_RESOURCE_GROUP_MAP,HQ_METRIC_DATA_2D_0S,EAM_EXEC_STRATEGY_TYPES,HQ_METRIC_DATA_6D_0S,EAM_GALERT_LOGS,EAM_GTRIGGERS,EAM_ESCALATION_STATE,EAM_RESOURCE_RELATION,EAM_MONITORABLE_TYPE,HQ_METRIC_DATA_3D_1S,EAM_AIQ_SERVER,EAM_PLUGIN,EAM_STAT_ERRORS,EAM_REQUEST_STAT,EAM_KEYSTORE,EAM_ALERT_DEFINITION,EAM_CONFIG_RESPONSE,HQ_METRIC_DATA_1D_0S')" ; 
	        stmt = conn.createStatement();
	        stmt.execute("set statement_timeout to 0") ;
	        stmt.executeQuery(sql) ;
	        stmt.execute("reset statement_timeout") ;
	        System.out.println("Success " + (System.currentTimeMillis()-before));
	    }catch(Throwable t) { 
	        System.out.println("failure " + (System.currentTimeMillis()-before));
	        t.printStackTrace() ; 
	    }finally{ 
	        Utils.close(Utils.ROLLBACK_INSTRUCTION_FLAG, new Object[]{stmt, conn}) ;  
	    }//EO catch blcok
	    
	    System.out.println("End");
	}//EOM 
	
	
	private static final void testWriteObjectVsUTF() throws Throwable { 
	    
	   /* class OOS extends ObjectOutputStream { 
	        private OutputStream out ; 
	        
            public OOS(OutputStream out) throws IOException{ 
                super(out) ;
                this.out = out ; 
            }//EOM 
            
            @Override
            public Object replaceObject(Object obj) throws IOException {
                return super.replaceObject(obj);
            }//EOM
            
            @Override
            public void writeUTF(String str) throws IOException {
              if(str == null) this.writeByte(TC_NULL) ; 
              else { 
                  this.writeByte(TC_STRING) ;
                  super.writeUTF(str) ;
              }
            }//EOM
        };
        
        class OIS extends ObjectInputStream { 
            
            public OIS(InputStream in) throws IOException{ 
                super(in) ;
            }//EOM 
            
            public final Object readNextObject() throws IOException { 
                byte tc = this.readByte() ;
                if(tc == TC_NULL) return null ; 
                else if(tc == TC_MAX) return Utils.EOF_PLACEHOLDER ;  
                else return super.readUTF();
            }//EOM 
            
            @Override
            public String readUTF() throws IOException {
                byte tc = this.readByte() ;
                if(tc == TC_NULL) return null ; 
                else if(tc == TC_MAX) throw new IOException() ;  
                else return super.readUTF();
            }//EOM 
        };*/
        
        final String fileName = "/tmp/oosTest.ser" ; 
        final File file = new File(fileName) ; 
        file.delete() ; 
        file.createNewFile() ; 
        final FileOutputStream fos = new FileOutputStream(fileName) ; 
        final int iLength = 1000000 ; 
        ObjectOutputStream oos = null ; 
        try{
            
            oos = new UTFNullHandlerOOS(fos) ; 
            
            final String value = "value_" ; 
            
            long before = System.currentTimeMillis() ;  
            
            for(int i=0; i < iLength; i++) { 
                if(i % 100 == 0) oos.writeUTF(null) ; 
                else oos.writeUTF(value + i) ; 
                
            }//EO While there are more elements to write
            System.out.println("Write took: " + (System.currentTimeMillis()-before)) ; 
            
            oos.writeByte(ObjectOutputStream.TC_MAX) ; 
            
        }catch(Throwable t) { 
            t.printStackTrace() ; 
        }finally{ 
            Utils.close(oos) ; 
        }//EO catch block 
        
        final FileInputStream fis = new FileInputStream(fileName) ;
        
        ObjectInputStream ois = null ; 
        try{ 
            ois = new UTFNullHandlerOIS(fis);
            
            long before = System.currentTimeMillis() ;  
           
            Object value = null ; 
            String sVal = null ; 
            while(true) {
                value = ois.readUnshared() ;
                
                if(value == Utils.EOF_PLACEHOLDER) { 
                    System.out.println("EOF");
                    break ; 
                }//EO if EOF 
                else sVal = (String) value ; 
                
            }//EO While there are more elements to write
            
            System.out.println("read took: " + (System.currentTimeMillis()-before)) ; 
            
          //  final IntContainer container1 =  (IntContainer)  ois.readObject() ; 
          //  System.out.println(container1.count);
            
        }catch(Throwable t) { 
            t.printStackTrace() ; 
        }finally{ 
            Utils.close(oos) ; 
        }//EO catch block 
    }//EOM
	
	
	private static final void testReadFromCSV() throws Throwable { 
	    
	    final String fileName = "/tmp/EAM_CONFIG_PROPS.csv" ;  
	    final File file = new File(fileName) ; 
	    
	    final BufferedReader reader = new BufferedReader(new FileReader(file)) ; 
	    
	    final List<String[]> rows = new ArrayList<String[]>() ; 
	    try{ 
	        char chrQuotes = '"' ; 
	        char delimiter = ',' ; 
	        boolean isInStringContext = false ; 
	        
	        int r ;
	        char chr  ; 
	        String[] arrRow = new String[7]  ; 
	        StringBuilder stringConstrcutor = new StringBuilder() ; 
	        int currentIndex = 0 ; 
	        while((r = reader.read()) != -1) {
	            
	           chr = (char) r ;
	           
	           if(chr == '\n') { 
	               arrRow[currentIndex] = stringConstrcutor.toString() ;
	               System.out.println(Arrays.toString(arrRow));
	               rows.add(arrRow) ; 
	               arrRow = new String[7] ; 
	               stringConstrcutor.setLength(0) ;
	               currentIndex = 0 ; 
	           }else if(chr == chrQuotes) { 
	               if(isInStringContext) { 
	                   isInStringContext = false ; 
	               }else{  
	                   isInStringContext = true ; 
	               }//EO else if open quotes 
	           }else if(chr == delimiter && !isInStringContext) { 
	               arrRow[currentIndex++] = stringConstrcutor.toString() ; 
	               stringConstrcutor.setLength(0) ; 
	           }//EO else if delimiter 
	           else { 
	               stringConstrcutor.append(chr) ; 
	           }//EO else if string constructor 
	        }//EO while there are more lines 
	           
        }catch(Throwable t) { 
            t.printStackTrace() ; 
        }finally{ 
            reader.close() ; 
        }//EO catch block
	    
	    
	    final Connection conn = Utils.getPostgresConnection() ;
        conn.setAutoCommit(false) ; 
        Statement stmt = null ; 
        ResultSet rs = null ;
        Writer writer = null ; 
        final String otuputFile = "/tmp/EAM_CONFIG_RESPONSE.csvout" ; 
        final File outputfile = new File(otuputFile) ;
        try{ 
            stmt = conn.createStatement() ; 
            rs = stmt.executeQuery("select * from eam_config_props") ;
            final ResultSetMetaData rsmd = rs.getMetaData() ; 
            final int noOfColumns = rsmd.getColumnCount() ; 
            
            writer = new BufferedWriter(new FileWriter(outputfile)) ;
            int iFlushThreshold = 10 ;
            String DELIMITER = "~" ; 
            String LINE_SEPARATOR = "\n" ; 
            		
            int iNoOfRecords = 0 ; 
            String value = null ; 
            while(rs.next()) { 
                for(int i=0; i < noOfColumns; i++) {
                    value = rs.getString(i+1) ; 
                    writer.write(value == null ? "" : value) ;
                    if(i < noOfColumns-1) writer.write(DELIMITER) ; 
                }//EO while there are more columns
                writer.write(LINE_SEPARATOR) ; 
                if(iNoOfRecords++ % iFlushThreshold == 0) writer.flush() ; 
            }//EO while there are more records
        }catch(Throwable t) { 
            t.printStackTrace() ; 
        } finally {
            Utils.close(Utils.ROLLBACK_INSTRUCTION_FLAG, new Object[] { rs, stmt, conn });
        }//EO catch block
	    	    
	    //Writer writer = new BufferedWriter(new FileWriter(outputfile)) ;
	  /*  try{ 
	        int iFlushThreshold = 10 ;
	        
	        final int iNoOfRows = rows.size() ;
	        String[] row = null ; 
	        for(int j=0; j < iNoOfRows; j++) {
	            row = rows.get(j) ; 
	            
	            for(int i=0; i< row.length; i++) { 
	                writer.write(row[i]) ; 
	                if(i < row.length-1) writer.write("~") ;
	            }//EO while there are more rows
	            writer.write("\n") ;
	            if(j % iFlushThreshold == 10) writer.flush() ; 
	        }//EO while there are more rows 
	    }catch(Throwable t) { 
	        t.printStackTrace() ; 
	    }finally{ 
	        writer.flush() ; 
	        writer.close() ; 
	    }//EO catch block 
*/	}//EOM 
	
	private static final void writeToFileWithInitialPlaceHolder() throws Throwable { 
	    
	    class OOS extends ObjectOutputStream { 
	        public OOS(OutputStream out) throws IOException{ super(out) ; this.enableReplaceObject(true) ; }//EOM 
	        
            @Override
            public Object replaceObject(Object obj) throws IOException {
                return super.replaceObject(obj);
            }//EOM
            
            @Override
            public void writeUTF(String str) throws IOException {
                if(str == null) this.writeObject(str) ; 
                else super.writeUTF(str) ;
            }
	    };
	    
	    final String fileName = "/tmp/oosTest.ser" ; 
	    final FileOutputStream fos = new FileOutputStream(fileName) ; 
	    fos.getChannel().position(4) ; 
	    OOS oos = null ; 
	    try{ 
	        oos = new OOS(fos) ;
	        final IntContainer container = new IntContainer() ; 
	        container.count = 1 ; 
	        oos.writeObject(container) ;
	        container.count = 20 ; 
	        
	        oos.flush() ; 
	        fos.getChannel().position(0) ;
	        byte[] bytes = java.nio.ByteBuffer.allocate(4).putInt(1001).array();

	        fos.write(bytes) ; 
	        
	        
	    }catch(Throwable t) { 
	        t.printStackTrace() ; 
	    }finally{ 
	        Utils.close(oos) ; 
	    }//EO catch block 
	    
	    final FileInputStream fis = new FileInputStream(fileName) ;
	    final byte[] count = new byte[4] ; 
	    fis.read(count) ; 
	    final ByteBuffer bb = ByteBuffer.wrap(count) ; 
	    //bb.order(ByteOrder.LITTLE_ENDIAN);
	    
	    System.out.println(bb.getInt()) ;  
	    ObjectInputStream ois = null ; 
	    try{ 
	        ois = new ObjectInputStream(fis);
            final IntContainer container =  (IntContainer)  ois.readObject() ; 
            System.out.println(container.count);
            
            System.out.println(ois.readObject());
            
          //  final IntContainer container1 =  (IntContainer)  ois.readObject() ; 
          //  System.out.println(container1.count);
            
        }catch(Throwable t) { 
            t.printStackTrace() ; 
        }finally{ 
            Utils.close(oos) ; 
        }//EO catch block 
	}//EOM 
	
	private static final class IntContainer implements Serializable{ 
	    int count ; 
	}//EOM 
	

	private static final void decode(final InputStream is) throws Throwable {
		if (is == null)
			return;
		try {
			ObjectInputStream objectStream = new ObjectInputStream(is);
			String key;
			while ((key = (String) objectStream.readObject()) != null) {
				String val = (String) objectStream.readObject();

				System.out.println(val);
			}//eO while there are more elements to decodes

		} catch (Throwable t) {
			t.printStackTrace();
		} finally {
			is.reset();
		}//EO catch block 
	}//EOM 

}//EOC 
