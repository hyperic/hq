package org.hyperic.tools.dbmigrate;

import static org.hyperic.tools.dbmigrate.Utils.COMMIT_INSTRUCTION_FLAG;
import static org.hyperic.tools.dbmigrate.Utils.ROLLBACK_INSTRUCTION_FLAG;

import java.io.EOFException;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.Statement;
import java.util.Hashtable;
import java.util.List;
import java.util.concurrent.BlockingDeque;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Future;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.zip.GZIPInputStream;

import org.apache.tools.ant.Project;
import org.hyperic.tools.dbmigrate.Forker.ForkContext;
import org.hyperic.tools.dbmigrate.Forker.ForkWorker;
import org.hyperic.tools.dbmigrate.TableImporter.Worker;
import org.hyperic.tools.dbmigrate.TableProcessor.Table;
import org.hyperic.util.MultiRuntimeException;

public class TableImporter extends TableProcessor<Worker> {
	
	private StringBuilder tablesClause;
	private final String POST_MIGRATE_ACTIONS_FILE_RELATIVE_PATH = "/sql/migration-post-configure.sql";

	public TableImporter() {}//EOM 

	@Override
	protected final void addTableToSink(final LinkedBlockingDeque<Table> sink, final Table table) {
	    super.addTableToSink(sink, table);
	    
	    if(this.tablesClause == null) this.tablesClause = new StringBuilder(table.name) ;  
	    this.tablesClause.append(",").append(table.name) ; 
	}//EOM 
	
	@Override
	protected final void beforeFork(final ForkContext<Table, Worker> context, final LinkedBlockingDeque<Table> sink) throws Throwable {
		MultiRuntimeException thrown = null;

		Connection conn = null;
		Statement stmt = null;
		try {
			final Project project = getProject();
			conn = getConnection(project.getProperties(), false);
			stmt = conn.createStatement();
			
			log("About to 'TRUNCATE CASCADE'") ; 
			
			String tableName = null ; 
			for (Table table : this.tablesContainer.tables.values()) {
			    if(!table.shouldTruncate) continue ;
			    tableName = table.name ; 
				log("Adding table truncation statement for table " + tableName + " to batch");
				stmt.addBatch("truncate table " + tableName + " CASCADE");
			}//EO while there are more tables 

			log("Executing table truncation statements batch...");
			stmt.executeBatch();
		} catch (Throwable t) {
			thrown = new MultiRuntimeException(t);
		} finally {
			Utils.close(thrown != null ? ROLLBACK_INSTRUCTION_FLAG : COMMIT_INSTRUCTION_FLAG, new Object[] { stmt, conn });
			log("TRUNCATE CASCADE was sucessful.");

			if (thrown != null) throw thrown;
		}//EO catch block 
	}//EOM

	@Override
	protected final void afterFork(final ForkContext<Table,Worker> context, final List<Future<Table[]>> workersResponses,
	        final LinkedBlockingDeque<Table> sink) throws Throwable {
	        
	    MultiRuntimeException thrown = null;
	    try{
	       super.afterFork(context, workersResponses, sink);
	    }catch(Throwable t){
	        thrown = new MultiRuntimeException(t);
	    }finally{
	        if(!isDisabled){
                Connection conn = null;
                FileInputStream fis = null;
                Statement stmt = null;
                try{
                    final Project project = getProject();
                    conn = getConnection(project.getProperties());
                    
                    final File postMigrateActionsFile = new File(project.getBaseDir(), POST_MIGRATE_ACTIONS_FILE_RELATIVE_PATH);
                    fis = new FileInputStream(postMigrateActionsFile);
                    
                    final byte arrBytes[] = new byte[fis.available()];
                    fis.read(arrBytes);
                    final String fmigrationPostConfigureFuction = new String(arrBytes);
                  
                    stmt = conn.createStatement();
                    stmt.execute(fmigrationPostConfigureFuction);
                    stmt.executeQuery((new StringBuilder()).append("select fmigrationPostConfigure('").
                                                            append(this.tablesClause).append("');").toString());
                }catch(Throwable t2) {
                    Utils.printStackTrace(t2);
                    thrown = MultiRuntimeException.newMultiRuntimeException(thrown, t2);
                }finally{
                    Utils.close(new Object[] { conn, fis });
                }//EO catch block 
            }//EO if not disabled 
            
	        if(thrown != null) throw thrown ; 
	    }//EO catch block 
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

    public final class Worker extends ForkWorker<Table> {
        private final File outputDir;

        Worker(final CountDownLatch countdownSemaphore, final Connection conn, final BlockingDeque<Table> sink, final File outputDir){ 
            super(countdownSemaphore, conn, sink);
            this.outputDir = outputDir;
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
                ois = new ObjectInputStream(zis);
                //ois = new ObjectInputStream(fis);
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
                        try{ 
                            oValue = ois.readObject() ;
                        }catch(EOFException eofe) { 
                            eofe.printStackTrace() ; 
                            throw eofe ; 
                        }
                        if(oValue == Utils.EOF_PLACEHOLDER) break ; 
                        
                        columnStrategies[(i - 1)].bindStatementParam(i, oValue, insertPs, columnTypes[(i - 1)]);
                    }//EO while there are more columns
                    
                    if(oValue == Utils.EOF_PLACEHOLDER) break ;
                    
                    recordCount++;
                    
                    insertPs.addBatch();

                    if (recordCount % PROTECTIVE_BATCH_SIZE == 0) {
                        insertPs.executeBatch();
                        this.conn.commit();
                        log(logMsgPrefix + "Imported " + recordCount + " records so far from file " + shortFileName);
                    }//EO while the batchsize threshold was reached 
                }//EO while there are more records to read 
               
                /*while (fis.available() > 0) {
                    recordCount++;

                    for (int i = 1; i <= columnCount; i++) {
                        columnStrategies[(i - 1)].bindStatementParam(i, ois,
                                insertPs, columnTypes[(i - 1)]);
                    }//EO while there are more columns 

                    insertPs.addBatch();

                    if (recordCount % PROTECTIVE_BATCH_SIZE == 0) {
                        insertPs.executeBatch();
                        this.conn.commit();
                        log(logMsgPrefix + "Imported " + recordCount + " records so far from file " + shortFileName);
                    }//EO while the batchsize threshold was reached 
                }//EO while there are more bytes available in the file 
*/
                if (recordCount % PROTECTIVE_BATCH_SIZE != 0) {
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
            Utils.executeUpdate(this.conn, "ALTER TABLE " + tableName + " DISABLE TRIGGER ALL");
    
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
	    
	    testReadfromFile() ; 
	    if(true) return ; 
	    
	    
	    final Table table = new Table("tableName") ;
	    final Table batch = new TableBatchMetadata("tableName", null, 0, null,null, null, null) ;
	    testTable(table) ; 
	    testTable(batch) ;
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
	
	private static final void testReadfromFile() throws Throwable { 
	    final String fileName = "/work/workspaces/master-complete/hq/dist/installer/modules/hq-migration/target/hq-migration-5.0/tmp/export-data/data/EAM_RESOURCE_EDGE/EAM_RESOURCE_EDGE_0_1.out" ; 
	    
	    final FileInputStream fis = new FileInputStream(new File(fileName)) ; 
	    final GZIPInputStream gis = new GZIPInputStream(fis) ; 
	    final ObjectInputStream ois = new ObjectInputStream(gis) ;
	   
	    try{ 
    	    Object oValue = null ; 
    	    final int columnCount = 6; 
    	    int count = 1 ; 
    	    while(true) { 
    	        
    	        for (int i = 1; i <= columnCount; i++) {
                    try{ 
                        oValue = ois.readObject() ;
                        
                        System.out.println("count " + count + " value " + oValue); 
                        
                    }catch(EOFException eofe) { 
                        eofe.printStackTrace() ; 
                        throw eofe ; 
                    }
                    if(oValue == Utils.EOF_PLACEHOLDER) break ; 
                    
                }//EO while there are more columns
    	        
    	        if(oValue == Utils.EOF_PLACEHOLDER) break ;
    	        
    	        count++ ; 
    	        
    	        
    	    }
	    }finally{ 
	        ois.close() ;
	    }//EO catch block 
	}

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
