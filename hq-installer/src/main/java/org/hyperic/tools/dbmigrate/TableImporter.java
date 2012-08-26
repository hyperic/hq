//TODO: work on the pre build action (db function + thefiguring out which table should be truncated and whose indices should be dropped 
package org.hyperic.tools.dbmigrate;

import static org.hyperic.tools.dbmigrate.Utils.COMMIT_INSTRUCTION_FLAG;
import static org.hyperic.tools.dbmigrate.Utils.ROLLBACK_INSTRUCTION_FLAG;

import java.io.EOFException;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.io.Serializable;
import java.io.StreamCorruptedException;
import java.math.BigDecimal;
import java.nio.ByteBuffer;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.Statement;
import java.sql.Types;
import java.util.Hashtable;
import java.util.List;
import java.util.concurrent.BlockingDeque;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Future;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.zip.GZIPInputStream;

import org.apache.tools.ant.Project;
import org.apache.tools.ant.util.FileUtils;
import org.hyperic.tools.dbmigrate.Forker.ForkContext;
import org.hyperic.tools.dbmigrate.Forker.ForkWorker;
import org.hyperic.tools.dbmigrate.TableExporter.UTFNullHandlerOOS;
import org.hyperic.tools.dbmigrate.TableImporter.Worker;
import org.hyperic.tools.dbmigrate.TableProcessor.Table;
import org.hyperic.util.MultiRuntimeException;


public class TableImporter extends TableProcessor<Worker> {
	
	private StringBuilder preImportActionsInstructions;
	private StringBuilder tablesList ; 
	private final String MIGRATION_FUNCTIONS_DIR = "/sql/migrationScripts/" ; 
	private final String IMPORT_SCRIPTS_FILE = MIGRATION_FUNCTIONS_DIR+ "import-scripts.sql" ; 

	public TableImporter() {}//EOM 

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
            else log("Pre import actions were sucessful.");
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
                    
                    stmt = conn.createStatement();
                    stmt.executeQuery((new StringBuilder()).append("select fmigrationPostConfigure('").
                                                            append(this.tablesList.toString()).append("')").toString());
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
            super(countdownSemaphore, conn, sink, Table.class);
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
                ois = new UTFNullHandlerOIS(zis);
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
            //Utils.executeUpdate(this.conn, "ALTER TABLE " + tableName + " DISABLE TRIGGER ALL");
    
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
	    testToCharVsCharArray() ; 
	    //testWriteObjectVsUTF() ;
	 //   writeToFileWithInitialPlaceHolder() ;
	    //testReadfromFile() ; 
	    if(true) return ; 
	    
	    
	    final Table table = new Table("tableName") ;
	    final Table batch = new TableBatchMetadata("tableName", null, 0, null,null, null, null) ;
	    testTable(table) ; 
	    testTable(batch) ;
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
