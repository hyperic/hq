package org.hyperic.tools.ant.dbupgrade;

import java.io.PrintStream;
import java.io.PrintWriter;
import java.lang.reflect.Field;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
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
import org.jasypt.encryption.pbe.PBEStringEncryptor;


public class SST_ColumnEncyptor extends SchemaSpecTask{
    
    private String table ; 
    private String pkColumn ; 
    private int noOfUpdateColumns ;
    private String columnsClause;
    private String updateColumnsClause ; 
    private int batchSize ; 
    private PBEStringEncryptor encryptor;
    //private HQDialect dialect;
    private DatabaseType enumDatabaseType ; 
    
    private static AtomicInteger pages ;
    
    public SST_ColumnEncyptor(){
    }//EOM 
    
    public final void setTable(final String table) { 
        this.table = table ; 
    }//EOM 
    
    public final void setPkColumn(final String pkColumn) { 
        this.pkColumn = pkColumn; 
    }//EOM 
    
    public final void setBatchSize(final String batchSize) { 
        this.batchSize = (batchSize == null ? 1000 : Integer.parseInt(batchSize)) ;  
    }//EOM 
    
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
                if(column.length() == 1 || columns.contains(columns)) continue ;
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
           // this.dialect = HQDialectUtil.getHQDialect(conn) ;
            this.enumDatabaseType = DatabaseType.valueOf(conn.getMetaData().getDatabaseProductName()) ;
            
        }catch(Throwable t) { 
            throw new BuildException(t) ; 
        }//EO catch block 
    }//EOM 
    
    @Override
    public final void execute() throws BuildException { 
        
        PreparedStatement ps = null ; 
        ResultSet rs = null ;
        NestedBuildException thrownExcpetion = null ; 
        ExecutorService executorPool = null ; 

        try{ 
            if(this.batchSize == 0) this.batchSize = 1000 ; 
            
            final long before = System.currentTimeMillis() ; 
            
            //test
            ps = this._conn.prepareStatement("select count("+this.pkColumn+") from " + this.table) ; 
            rs = ps.executeQuery() ;
            rs.next() ; 
            final int iNoOfExistingRecords = rs.getInt(1) ; 
            if(iNoOfExistingRecords == 0) return ; 
            final int iNoOfchunks =  (iNoOfExistingRecords+this.batchSize-1)/this.batchSize ;
            
            this.log("No of chunks: " + iNoOfchunks);
            
            rs.close() ; 
            ps.close();   
            rs = null ; 
            ps = null ; 
            
            final CountDownLatch inverseSemaphore = new CountDownLatch(iNoOfchunks) ; 
            pages = new AtomicInteger(iNoOfchunks-1) ; 

            Connection conn = null ; 
            
            int iNoOfWorkers = 4 ; 
            if(iNoOfWorkers  > iNoOfchunks) iNoOfWorkers = iNoOfchunks ;
            
            this.log("[SST_ColumnEncryptor.execute()]: Starting update"); 
            
            final String selectStatement = this.enumDatabaseType.generatePagedQuery(
                    this.table, 
                    this.columnsClause, 
                    this.pkColumn) ;  
            
            final String updateStatement = this.enumDatabaseType.generateUpdateQuery(  
                    this.table, 
                    this.updateColumnsClause,
                    this.pkColumn) ; 
            
            final List<Future<String>> workersFutures = new ArrayList<Future<String>>(iNoOfWorkers) ; 
            executorPool = Executors.newFixedThreadPool(iNoOfWorkers) ;
            Future<String> workerFuture = null ; 
            
            for(int i=0 ; i < iNoOfWorkers; i++) { 
                conn = this.getNewConnection() ;  
                conn.setAutoCommit(false) ; 
                workerFuture = executorPool.submit(new Worker(inverseSemaphore, conn, selectStatement, 
                        updateStatement, this.newEncryptor())) ;
                workersFutures.add(workerFuture) ; 
            }//EO while there are more exeuctors 
            
            inverseSemaphore.await() ;
            
            //now verify that there no exceptions were returned from the workers 
            for(Future<String> workerResponse : workersFutures) { 
                //should throw an exceptions if one was thrown from a worker thread 
                workerResponse.get() ; 
            }//EO while there are more worker responses 

            this.log("[SST_ColumnEncryptor.execute()]: after all workers are finished overall time in millis: " + (System.currentTimeMillis()-before));
            
        }catch(Throwable t) {
            thrownExcpetion = new NestedBuildException(t) ; 
        }finally{
            try{ 
                if(executorPool != null) executorPool.shutdown() ;
                
                if(rs != null) rs.close() ; 
                if(ps != null) ps.close() ;
                
            }catch(Throwable t){
                if(thrownExcpetion == null) { 
                    thrownExcpetion = new NestedBuildException(t) ; 
                }else { 
                    thrownExcpetion.addThrowable(t) ; 
                }//EO if an exception was already thrown 
            }//EO catch block
            
            if(thrownExcpetion != null) {
                log(thrownExcpetion, Project.MSG_ERR) ; 
                throw thrownExcpetion ;  
            }//EO if there was an error 
        }//EO catch block
    }//EOM 
    
    
   /* @Override
    public void execute_old() throws BuildException {
        
        PreparedStatement ps = null ; 
        ResultSet rs = null ;
        Statement updateStatement = null ;
        NestedBuildException thrownExcpetion = null ; 
        try{ 
            //test
            ps = this._conn.prepareStatement("select count(*) from " + this.table) ; 
            rs = ps.executeQuery() ;
            rs.next() ; 
            final int iNoOfExistingRecords = rs.getInt(1) ; 
            final int iBatchSize = 1000 ; 
            final int iNoOfchunks =  (iNoOfExistingRecords+iBatchSize-1)/iBatchSize ;
            
            rs.close() ; 
            ps.close(); 
            
            int iRowNum = 0 ;  
            final String UPDATE_STATEMENT = String.format("SELECT %s, %s FROM %s", this.pkColumn, 
                    this.columns, this.table) ; 
            String sQuery = null ; 
            
            long total = 0 ; 
            
            System.out.println("Starting update"); 
            
            for(int i=0; i< iNoOfchunks; i++) {
                
                long before = System.currentTimeMillis() ; 
                
                updateStatement = this._conn.createStatement(ResultSet.TYPE_SCROLL_SENSITIVE, ResultSet.CONCUR_UPDATABLE);
                sQuery = dialect.getLimitBuf(UPDATE_STATEMENT, iRowNum, iBatchSize) ; 
                rs = updateStatement.executeQuery(sQuery) ; 
               
                iRowNum += iBatchSize ;
                
                while(rs.next()) { 
                    rs.updateString(2, this.encryptor.encrypt(rs.getString(2))) ; 
                    rs.updateRow() ;   
                }///EO while there are more records
                
                this._conn.commit() ;
                rs.close() ; 
                updateStatement.close() ;
                
                total += (System.currentTimeMillis()-before) ;
                
            }//EO while there are more records 
            
            System.out.println("Total in millis: " +  (total) + " per batch in millis: " + ((total/iNoOfchunks)) );
        }catch(Throwable t) {
            thrownExcpetion = new NestedBuildException(t) ; 

            try{ 
                this._conn.rollback() ;
            }catch(Throwable innerT) { 
                thrownExcpetion.addThrowable(t) ; 
            }//EO catch block 
        }finally{
            try{ 
                if(rs != null) rs.close() ; 
                if(ps != null) ps.close() ; 
                
                if(updateStatement != null) updateStatement.close() ; 

            }catch(Throwable t){
                if(thrownExcpetion == null) { 
                    thrownExcpetion = new NestedBuildException(t) ; 
                }else { 
                    thrownExcpetion.addThrowable(t) ; 
                }//EO if an exception was already thrown 
            }//EO catch block
            
            if(thrownExcpetion != null) throw thrownExcpetion ;  
        }//EO catch block
        
        
    }//EOM 
*/    
    
    public static void main(String[] args) throws Throwable{
        test() ; 
    }//EOM 
    
    private static final void test() throws Throwable{ 
         final DBUpgrader upgrader = getOracleVMC_PDB04Upgrader() ; 
        //final DBUpgrader upgrader = getMysqlUpgrader() ; 
         //final DBUpgrader upgrader = getPostgresUpgrader() ;
         //final DBUpgrader upgrader = getOracleUpgrader_nipuna(); 
         
         final SST_ColumnEncyptor test = new SST_ColumnEncyptor() ;
         test.setTable("GUY_TEST") ; 
         test.setPkColumn("TEST_PK") ; 
         test.setColumns("COL2") ; 
         test.setBatchSize("1000") ; 
         
         final int iNoOfRecords = 800000 ; 
         
         Connection conn = null ; 
         PreparedStatement ps = null ; 
         ResultSet rs = null ;
         
         try{ 
             conn = upgrader.getConnection() ;  
             test.initialize(conn, upgrader) ; 
             
             ps = test._conn.prepareStatement("truncate table " + test.table) ; 
             ps.executeUpdate() ; 
             test._conn.commit() ; 
             ps.close();
             
             ps = test._conn.prepareStatement("insert into "+test.table+" values(?,?,?)") ; 
             for(int i=0; i< iNoOfRecords; i++) { 
                 ps.setInt(1,i) ; 
                 ps.setString(2, "key_" + i) ; 
                 ps.setString(3, "value_" + i) ;
                 ps.addBatch() ; 
             }//EO while there are more records to insert 
     
             //commit 
             final int results[] = ps.executeBatch() ; 
             test._conn.commit() ; 
             ps.close() ;
             
             test.execute() ; 
             
         }catch(Throwable t) { 
             t.printStackTrace() ; 
         }finally{ 
             if(test._conn !=  null) test._conn.close() ; 
         }//EO catch block
     }//EOM
    
    
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
        
        public String call() throws Exception { 
            
            final String msgPrefix = "[Encryptor Worker ("+Thread.currentThread().getName()+")]:" ;
            
            ResultSet rs = null ;
            PreparedStatement selectStatement = null, updateStatement = null ; 
            NestedBuildException thrownExcpetion = null ; 
           
            final DatabaseType enumDatabaseType = SST_ColumnEncyptor.this.enumDatabaseType ;
            final int iNoOfEncryptableColumns = SST_ColumnEncyptor.this.noOfUpdateColumns ; 

            int iCurrentPageNumber = 0 ; 
            final int iBatchSize = SST_ColumnEncyptor.this.batchSize ; 
            String colVal = null ; 
            while((iCurrentPageNumber = pages.getAndDecrement()) >= 0) { 
                
                long total = 0 ; 

                try{
                    long before = System.currentTimeMillis() ; 
                           
                    long beforeSelect = System.currentTimeMillis() ; 
                   
                    selectStatement = this.conn.prepareStatement(this.selectStatement) ;
                    enumDatabaseType.bindPageInfo(selectStatement, iCurrentPageNumber, iBatchSize) ; 
                    rs = selectStatement.executeQuery() ; 
                    rs.setFetchSize(iBatchSize) ;
                    
                    long afterSelect = (System.currentTimeMillis()-beforeSelect) ; 
                   
                    long beforeBatch = System.currentTimeMillis() ;
                    updateStatement = conn.prepareStatement(this.updateStatement) ;
                    
                    long beforeLoop= System.currentTimeMillis() ;
                    while(true) {
                        
                        long beforeSingleLoop = System.currentTimeMillis() ;
                        if(!rs.next()) break ; 
                        long afterSingleLoop = (System.currentTimeMillis()-beforeSingleLoop) ;
                       // System.out.println(msgPrefix + " rs.next : " + afterSingleLoop);
                        
                        //index starts from 2
                        for(int i=1; i <= iNoOfEncryptableColumns; i++) { 
                            colVal = rs.getString(i+1) ; 
                            
                            if(colVal != null && !colVal.substring(0, 3).equalsIgnoreCase("enc")) 
                                                                colVal = encryptor.encrypt(colVal) ;  
                            
                            updateStatement.setString(i, colVal) ;
                        }//EO while there are more columns to encrypt 
                        
                        //set the where clause binding param to the next binding param index 
                        updateStatement.setString(iNoOfEncryptableColumns+1, rs.getString(1)) ; 
                        
                        updateStatement.addBatch() ; 
                       
                    }///EO while there are more records
                    long afterLoop= (System.currentTimeMillis()-beforeLoop) ;
                    
                    long beforeExecuteBatch= System.currentTimeMillis() ;
                    updateStatement.executeBatch() ; 
                    long afterExecuteBatch = (System.currentTimeMillis()-beforeExecuteBatch) ;
                    
                    long beforeCommit = System.currentTimeMillis() ;
                    this.conn.commit() ;
                    long afterCommit = (System.currentTimeMillis()-beforeCommit) ;
                    long afterBatch = (System.currentTimeMillis()-beforeBatch) ;
                    
                    rs.close() ; 
                    selectStatement.close() ; 
                    updateStatement.close() ;
                            
                    total += (System.currentTimeMillis()-before) ;
                            
                    log(msgPrefix +  (total) + " select: " + afterSelect + " batch update: " + afterBatch + " commit time: " + afterCommit + " execute Batch: " + afterExecuteBatch + " loop: " + afterLoop ) ;
                }catch(Throwable t) {
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
                        if(thrownExcpetion == null) { 
                            thrownExcpetion = new NestedBuildException(msgPrefix, t) ; 
                        }else { 
                            thrownExcpetion.addThrowable(t) ; 
                        }//EO if an exception was already thrown 
                    }//EO catch block
                
                    this.countdownSemaphore.countDown() ;
                    log(msgPrefix +" after chunk countdown " + this.countdownSemaphore.getCount());
                    
                    if(thrownExcpetion != null) { 
                        log(thrownExcpetion, Project.MSG_ERR) ; 
                        throw thrownExcpetion ;  
                    }//EO if there was an error 
                }//EO catch block
                
            }//EO while there are more pages to work on
            
            log(msgPrefix +" exiting with chunks left: " + this.countdownSemaphore.getCount()) ;
            return null;
        }//EOM 
        
        
        /*
        public String call() throws Exception { 
            PreparedStatement ps = null ; 
            ResultSet rs = null ;
            Statement updateStatement = null ;
            NestedBuildException thrownExcpetion = null ; 
           
            final MarkedStringEncryptor encryptor = SST_ColumnEncyptor.this.encryptor ; 
            final DatabaseType enumDatabaseType = SST_ColumnEncyptor.this.enumDatabaseType ; 
            final String statement = SST_ColumnEncyptor.this.statement; 

            int iCurrentPageNumber = 0, offset = 0 ; 
            final int iBatchSize = SST_ColumnEncyptor.this.batchSize ; 
            
            while((iCurrentPageNumber = pages.getAndDecrement()) >= 0) { 
                
                //System.out.println(Thread.currentThread().getName() + " working on chunk " +  iCurrentPageNumber);
             
                String sQuery = null ; 
                long total = 0 ; 

                try{ 
                    long before = System.currentTimeMillis() ; 
                           
                    updateStatement = this.conn.createStatement(ResultSet.TYPE_SCROLL_SENSITIVE, ResultSet.CONCUR_UPDATABLE);
                    
                    //offset = (iCurrentPageNumber == 0 ? 0 : (iCurrentPageNumber*iBatchSize)) ; 
                    //sQuery = dialect.getLimitBuf(SST_ColumnEncyptor.this.statement, offset, iBatchSize) ; 
                    sQuery = enumDatabaseType.addPagination(statement, iCurrentPageNumber, iBatchSize) ;
                    rs = updateStatement.executeQuery(sQuery) ; 
                            
                    while(rs.next()) { 
                        rs.updateString(2, encryptor.encrypt(rs.getString(2))) ; 
                        rs.updateRow() ;   
                    }///EO while there are more records
                            
                    this.conn.commit() ;
                    rs.close() ; 
                    updateStatement.close() ;
                            
                    total += (System.currentTimeMillis()-before) ;
                            
                    System.out.println("Total in millis: " +  (total)) ;
                }catch(Throwable t) {
                    thrownExcpetion = new NestedBuildException(t) ; 
    
                    try{ 
                        this.conn.rollback() ;
                    }catch(Throwable innerT) { 
                        thrownExcpetion.addThrowable(t) ; 
                    }//EO catch block 
                }finally{
                    try{ 
                        if(rs != null) rs.close() ; 
                        if(ps != null) ps.close() ; 
                        
                        if(updateStatement != null) updateStatement.close() ; 
    
                    }catch(Throwable t){
                        if(thrownExcpetion == null) { 
                            thrownExcpetion = new NestedBuildException(t) ; 
                        }else { 
                            thrownExcpetion.addThrowable(t) ; 
                        }//EO if an exception was already thrown 
                    }//EO catch block
                
                    this.countdownSemaphore.countDown() ;
                    System.out.println(Thread.currentThread().getName()  + " after countdown " + this.countdownSemaphore.getCount());
                    
                    if(thrownExcpetion != null) throw thrownExcpetion ;  
                }//EO catch block
                
            }//EO while there are more pages to work on
            
            System.out.println(Thread.currentThread().getName()  + " exiting " + this.countdownSemaphore.getCount()) ;
            return null;
        }//EOM 
        
    */
    }//EO inner class Worker
    
    private static final DBUpgrader getMysqlUpgrader() throws Throwable { 
        Class.forName("com.mysql.jdbc.Driver") ;
        
        final DBUpgrader upgrader = new DBUpgrader() ; 
        upgrader.setJdbcPassword("hqadmin") ; 
        upgrader.setJdbcUser("hqadmin") ; 
        upgrader.setJdbcUrl("jdbc:mysql://localhost:3306/hqdb") ;
        injectEncryptor(upgrader) ; 
        return upgrader ; 
    }//EOM 
    
    private static final DBUpgrader getOracleUpgrader_nipuna() throws Throwable{ 
        Class.forName("oracle.jdbc.OracleDriver") ;
        
        final DBUpgrader upgrader = new DBUpgrader() ; 
        upgrader.setJdbcUser("nipuna") ; 
        upgrader.setJdbcPassword("nipuna") ; 
        
        upgrader.setJdbcUrl("jdbc:oracle:thin:@10.17.188.158:1521:ORCL") ;
        //upgrader.setJdbcUrl("jdbc:oracle:thin:@sof-apm-206:1521:ORCL") ;
        injectEncryptor(upgrader) ; 
        return upgrader ; 
    }//EOM 
    
    private static final DBUpgrader getOracleVMC_PDB04Upgrader() throws Throwable{ 
        Class.forName("oracle.jdbc.OracleDriver") ;
        
        final DBUpgrader upgrader = new DBUpgrader() ; 
        upgrader.setJdbcUser("hqadmin") ; 
        upgrader.setJdbcPassword("hqadmin") ; 
        
        upgrader.setJdbcUrl("jdbc:oracle:thin:@vmc-pdb04:1521:ORCL") ;
        injectEncryptor(upgrader) ; 
        return upgrader ; 
    }//EOM 
    
    
    
    private static final DBUpgrader getOracleUpgrader_rivka() throws Throwable{ 
        Class.forName("oracle.jdbc.OracleDriver") ;
        
        final DBUpgrader upgrader = new DBUpgrader() ; 
        upgrader.setJdbcUser("rivi66") ; 
        upgrader.setJdbcPassword("rivi66") ; 
        
        upgrader.setJdbcUrl("jdbc:oracle:thin:@10.17.188.158:1521:ORCL") ;
        injectEncryptor(upgrader) ; 
        return upgrader ; 
    }//EOM
    
    private static final DBUpgrader getPostgresUpgrader() throws Throwable{ 
        Class.forName("org.postgresql.Driver") ;
        
        final DBUpgrader upgrader = new DBUpgrader() ; 
        upgrader.setJdbcUser("hqadmin") ; 
        upgrader.setJdbcPassword("hqadmin") ; 
        upgrader.setJdbcUrl("jdbc:postgresql://127.0.0.1:9432/hqdb?protocolVersion=2") ;
        injectEncryptor(upgrader) ; 
        return upgrader ; 
    }//EOM 
    
    private static final void injectEncryptor(final DBUpgrader upgrader) throws Throwable{
        upgrader.setEncryptionKey("sDbEncKeyPw") ; 
        final Field encryptorField = DBUpgrader.class.getDeclaredField("encryptor") ;
        encryptorField.setAccessible(true) ; 
        encryptorField.set(upgrader, upgrader.newEncryptor()) ; 
    }//EOM
    
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
    
    
    private enum DatabaseType { 
        
        MySQL{ 
            /*@Override
            public final String configureQuery(final String statement, final int iPageNumber, final int iPageSize) {
                int iOffset = (iPageNumber == 0 ? 0 : (iPageNumber*iPageSize)) ;
                return statement + " limit " + iOffset + "," + iPageSize ; 
            }//EOM 
            */ 
            
             @Override
             public final String generatePagedQuery(final String tableName, 
                                     final String columnsClause, final String pkColumnName) {
                 return String.format("SELECT %s, %s from %s limit ?,?", pkColumnName, columnsClause, 
                         tableName) ; 
             }//EOM 
             
             @Override
             public final PreparedStatement bindPageInfo(final PreparedStatement ps, 
                                 final int iPageNumber, final int iPageSize) throws SQLException{
                 int iOffset = (iPageNumber == 0 ? 0 : (iPageNumber*iPageSize)) ;
                 ps.setInt(1, iOffset) ; 
                 ps.setInt(2, iPageSize) ; 
                 return ps ; 
             }//EOM

        },//EO MySQL  
        Oracle{ 
            @Override
            public final String generatePagedQuery(final String tableName, 
                                final String columnsClause, final String pkColumnName) {
               // int iOffset = (iPageNumber == 0 ? 1 : (iPageNumber*iPageSize)) ;
                //return "select r, TEST_PK, COL2 from (select rownum r, " + statement.replaceAll("SELECT", "") + ") where r > " + iOffset + " and r < " + (iOffset+iPageSize) ;
                //(select test_pk from (select test_pk, rownum rn from guy_test) where rn >= " + iOffset +" and rn < "+ (iOffset+iPageSize) +")" ;
                
                return String.format("select %s, %s from %s where %s in " +
                        "(select %s from (select %s, rownum rn from %s) where rn > ? and rn <= ?)", 
                        pkColumnName, 
                        columnsClause, 
                        tableName, 
                        pkColumnName, 
                        pkColumnName, 
                        pkColumnName, 
                        tableName) ;
                
               /* return String.format("select rowid %s, %s from %s where %s in " +
                        "(select %s from (select %s, rownum rn from %s) where rn > ? and rn <= ?)", 
                        pkColumnName, 
                        columnsClause, 
                        tableName, 
                        pkColumnName, 
                        pkColumnName, 
                        pkColumnName, 
                        tableName) ;*/
            }//EOM 
            
            @Override
            public final String generateUpdateQuery(final String tableName, final String columnsClause, 
                    final String pkColumnName) { 
                //return super.generateUpdateQuery(tableName, columnsClause, "rowid") ;  
                return super.generateUpdateQuery(tableName, columnsClause, pkColumnName) ;
            }//EOM
            
            @Override
            public final PreparedStatement bindPageInfo(final PreparedStatement ps, 
                                    final int iPageNumber, final int iPageSize) 
                                                                       throws SQLException{
                final int iOffset = (iPageNumber == 0 ? 0 : (iPageNumber*iPageSize)) ;
                ps.setInt(1, iOffset) ; 
                ps.setInt(2, (iOffset+iPageSize) ) ; 
                return ps ; 
            }//EOM 
        },//EO Oracle
        PostgreSQL{
            @Override
            public final String generatePagedQuery(final String tableName, 
                                    final String columnsClause, final String pkColumnName) {
                return String.format("SELECT %s, %s from %s offset ? limit ?", pkColumnName, columnsClause, 
                        tableName) ; 
            }//EOM 
            
            @Override
            public final PreparedStatement bindPageInfo(final PreparedStatement ps, 
                                    final int iPageNumber, final int iPageSize) 
                                                                        throws SQLException{
                return MySQL.bindPageInfo(ps, iPageNumber, iPageSize) ; 
            }//EOM 
        };//EO Postgres 
        
        public abstract PreparedStatement bindPageInfo(final PreparedStatement ps, 
                            final int iPageNumber, final int iPageSize) throws SQLException ;
        
        public abstract String generatePagedQuery(final String tableName, final String columnsClause, 
                final String pkColumnName) ;  
        
        public String generateUpdateQuery(final String tableName, final String columnsClause, 
                final String pkColumnName) { 
            return String.format("update %s set %s where %s = ?", tableName, columnsClause, pkColumnName) ; 
        }//EOM 
    }//EO enum DatabaseType 
    

}//EOC 
