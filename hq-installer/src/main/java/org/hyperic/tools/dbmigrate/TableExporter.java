package org.hyperic.tools.dbmigrate;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.ObjectOutputStream;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.Statement;
import java.util.Hashtable;
import java.util.List;
import java.util.concurrent.BlockingDeque;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Future;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.zip.GZIPOutputStream;

import org.apache.tools.ant.Project;
import org.hyperic.tools.dbmigrate.Forker.ForkContext;
import org.hyperic.tools.dbmigrate.Forker.ForkWorker;
import org.hyperic.tools.dbmigrate.TableExporter.Worker;
import org.hyperic.tools.dbmigrate.TableProcessor.Table;
import org.hyperic.util.MultiRuntimeException;

public class TableExporter extends TableProcessor<Worker> {
    
    private static final String HQ_SERVER_CONF_RELATIVE_PATH = "/source-artifacts/conf/hq-server.conf";
    
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

    @Override
    protected final void afterFork(final ForkContext<Table,Worker> context, final List<Future<Table[]>> workersResponses, 
                final LinkedBlockingDeque<Table> sink) throws Throwable {
       
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

    public final class Worker extends ForkWorker<Table> {

        private final File outputDir;
        
        Worker(final CountDownLatch countdownSemaphore, final Connection conn, final BlockingDeque<Table> sink, final File outputDir) {
            super(countdownSemaphore, conn, sink);
            this.outputDir = outputDir;
        }//EOM 
        
        @Override
        protected final void callInner(final Table table) throws Throwable {
            
            Statement stmt = null;
            ResultSet rs = null;
            ObjectOutputStream ous = null;
            try{
                String tableName = table.name ; 
                String sql = "SELECT * FROM "  ;
                int partitionNo = 0 ; 
                
                if(table instanceof BigTable) { 
                    final BigTable bigTable = (BigTable) table ; 
                    sql = new StringBuilder(sql).append(tableName).append(" WHERE ").append(bigTable.partitionColumn).
                            append(" % ").append(bigTable.noOfPartitions).append(" = ").append(bigTable.partitionNumber).toString() ;  
                    
                    partitionNo = bigTable.partitionNumber ; 
                }else{ 
                    sql = sql + tableName ; 
                }//EO else if not a big table 
                
                final File tableParentDir = new File(this.outputDir, tableName);
                
                final String traceMsgPrefix = "----------------- [Table["+tableName+"];Partition["+partitionNo+"]]: " ;
                final String loopTraceMsgSuffix = " records so far." ;
                TableExporter.this.log(traceMsgPrefix + "Commencing the export into " + tableParentDir) ; 
                
                stmt = this.conn.createStatement(java.sql.ResultSet.TYPE_FORWARD_ONLY, java.sql.ResultSet.CONCUR_READ_ONLY) ; 
              
                //TODO: write comment for databaseversion support for the fetch size 
                enumDatabaseType.setFetchSize(batchSize, (table instanceof BigTable)/*bigTable*/, stmt) ;                 
                stmt.setQueryTimeout(queryTimeoutSecs) ; 
                rs = stmt.executeQuery(sql) ; 
              
                final ResultSetMetaData rsmd = rs.getMetaData();
                final int columnCount = rsmd.getColumnCount();

                final DBDataType[] columns = new DBDataType[columnCount];

                int recordCount = 0;
                int batchCounter = 0;
                
                while (rs.next()) {
                    
                    //create the table parent directory if not yet created 
                    if(recordCount == 0 && !tableParentDir.exists()) tableParentDir.mkdir();
                    
                    if (recordCount == maxRecordsPerTable) break;
                    
                    if (recordCount % batchSize == 0) {
                        TableExporter.this.log(traceMsgPrefix + "exported " + recordCount + loopTraceMsgSuffix ) ;   
                        ous = newOutputFile(tableName, batchCounter++, partitionNo, tableParentDir, ous);
                    }//EO if batch threshold was reached 

                    for (int i = 0; i < columnCount; i++) {
                      if (columns[i] == null) columns[i] = DBDataType.reverseValueOf(rsmd.getColumnType(i + 1));
                      columns[i].serialize(ous, rs, i + 1);
                    }//EO while there are more columns 

                    recordCount++;
                }//EO while there are more records 
                
                String recordCountMsgPart = null ; 
                if(recordCount == 0) { 
                    recordCountMsgPart = "No Records where exported" ; 
                }else { 
                    recordCountMsgPart = "Finished exporting " + recordCount + " records" ;
                    ous.writeObject(Utils.EOF_PLACEHOLDER) ; 
                }//EO if records were exported to file 
                
                final int totalNumberOfRecordsSofar = table.noOfProcessedRecords.addAndGet(recordCount) ;
                TableExporter.this.log(traceMsgPrefix + recordCountMsgPart +  
                        " for this partition Total records exported so far: " + totalNumberOfRecordsSofar) ;
            }finally {
              Utils.close(new Object[] { ous, rs, stmt });
            }//EO catch block 
        }//EOM 

        private final ObjectOutputStream newOutputFile(final String tableName, final int batchNo, final int partitionNumber, 
                        final File parentDir, final ObjectOutputStream existingOus) throws Throwable {
            if(existingOus != null) {
                existingOus.writeObject(Utils.EOF_PLACEHOLDER) ;
                existingOus.flush();
                existingOus.close();
            }//EO if existing output stream exists 
            
            final File outputFile = new File(parentDir, tableName + "_" + partitionNumber + "_" + batchNo + ".out");
            final FileOutputStream fos = new FileOutputStream(outputFile);
            final GZIPOutputStream gzos = new GZIPOutputStream(fos) ; 
            return new ObjectOutputStream(gzos);
            //return new ObjectOutputStream(fos);
        }//EOM 

        protected final void rollbackEntity(final String tableName, final Throwable reason) {
            log("Failed to export table " + tableName + ", will disacrd all export data for it; Reason: " + reason, Project.MSG_ERR);
            final File tableDir = new File(outputDir, tableName);
            Utils.deleteDirectory(tableDir);
        }//EOM 
    }//EO inner class Worker
    
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
            File file = new File("/work/workspaces/master-complete/hq/dist/installer/src/main/resources/data/sql/migration-post-configure.sql");
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
            stmt.executeQuery("select fmigrationPostConfigure('EAM_CONFIG_RESPONSE,EAM_MEASUREMENT,EAM_MEASUREMENT_BL,EAM_SERVICE,EAM_SERVER,EAM_PLATFORM,EAM_AGENT');");
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
