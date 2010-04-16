import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.PrintStream;
import java.sql.Connection;
import java.sql.Driver;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

import javax.naming.OperationNotSupportedException;

import org.dbunit.database.DatabaseConnection;
import org.dbunit.database.IDatabaseConnection;
import org.dbunit.database.QueryDataSet;
import org.dbunit.dataset.IDataSet;
import org.dbunit.dataset.xml.FlatXmlDataSet;
import org.dbunit.operation.DatabaseOperation;

public class DatabaseExport {
    private static Map<String, Long> _seqMap = new HashMap<String, Long>();
    private static List<String> _tables;
    private static String _workingDir;
    private static boolean _debug;
    private static String _targetUrl;
    private static String _sourceUrl;
    private static String _targetPass;
    private static String _targetUser;
    private static String _sourcePass;
    private static String _sourceUser;
    private static PrintStream _out;
    private static final String _logCtx = DatabaseExport.class.getName();
    private static final String LOG_FILE = "dbmigration.log";

    public static void main(String[] args) throws Exception {
        final long start = System.currentTimeMillis();
        getArgs(args);
        String fs = File.separator;
        String logFile = _workingDir + fs + LOG_FILE;
        _out = new PrintStream(logFile);
        Connection connExp = getConnectionExport();
        Connection connImp = getConnectionImport();
        try {
            checkSchemaSpecVersions(connExp, connImp);
            System.out.println("Starting dbmigration.  To monitor progress tail file: " + logFile);
            _tables = Collections.unmodifiableList(getTables(connExp, _sourceUser));
            exportDataSetPerTable(connExp);
            importDataSetPerTable(connImp);
            importBigTables(connExp, connImp);
            importSequences(connImp);
            connImp.commit();
            validateTransfer(connExp, connImp);
            final long end = System.currentTimeMillis();
            System.out.println("migration successful, process took " + (end-start)/1000/60/60 +
                               " hours");
        } finally {
            connExp.close();
            connImp.close();
            _out.close();
        }
    }

    private static void validateTransfer(Connection connExp, Connection connImp) throws Exception {
        for (final String table : _tables) {
            _out.println("validating table=" + table);
            int rowsExp = getNumRows(connExp, table);
            int rowsImp = getNumRows(connImp, table);
            if (rowsExp != rowsImp) {
                throw new Exception("ERROR: validation failed for table=" + table +
                                    ", has " + rowsExp + " rows in source vs " + rowsImp + 
                                    " rows in target");
            }
        }
        _out.println("validation successful");
    }

    private static int getNumRows(Connection conn, String table) throws SQLException {
        Statement stmt = null;
        ResultSet rs = null;
        try {
            stmt = conn.createStatement();
            String sql = "select count(*) from " + table;
            rs = stmt.executeQuery(sql);
            if (rs.next()) {
                return rs.getInt(1);
            }
            return 0;
        } finally {
            DBUtil.close(_logCtx, null, stmt, rs);
        }
    }

    private static void checkSchemaSpecVersions(Connection connExp, Connection connImp)
    throws Exception {
        String schemaSpecExp = getSchemaSpec(connExp);
        String schemaSpecImp = getSchemaSpec(connImp);
        if (schemaSpecExp == null) {
            throw new Exception("ERROR: HQ schema version not found in source database");
        } else if (schemaSpecImp == null) {
            throw new Exception("ERROR: HQ schema version not found in target database");
        } else if (!schemaSpecExp.equals(schemaSpecImp)) {
            throw new Exception("ERROR: schema spec versions of the source and target databases " +
                                "do not match.  Make sure both databases are running the same " +
                                "version of HQ.  Most likely solution is to upgrade the source " +
                                "database.  sourceSpec=" + schemaSpecExp + 
                                ", targetSpec=" + schemaSpecImp);
        }
    }

    private static String getSchemaSpec(Connection conn) throws SQLException {
        Statement stmt = null;
        ResultSet rs = null;
        try {
            stmt = conn.createStatement();
            String sql = "select PROPVALUE from EAM_CONFIG_PROPS where propkey = 'CAM_SCHEMA_VERSION'";
            rs = stmt.executeQuery(sql);
            if (rs.next()) {
                return rs.getString(1);
            }
            return null;
        } finally {
            DBUtil.close(_logCtx, null, stmt, rs);
        }
    }

    private static void getArgs(String[] args) {
        //-s hqadmin -p hqadmin -t hqadmin -r hqadmin
        // -u jdbc:postgresql://localhost:5432/hqdb
        // -g jdbc:mysql://localhost:3306/hqdb
        for (int i=0; i<args.length; i++) {
            final String arg = args[i];
            if (arg.equals("-s")) {
                _sourceUser = args[++i];
            } else if (arg.equals("-p")) {
                _sourcePass = args[++i];
            } else if (arg.equals("-t")) {
                _targetUser = args[++i];
            } else if (arg.equals("-r")) {
                _targetPass = args[++i];
            } else if (arg.equals("-u")) {
                _sourceUrl = args[++i];
            } else if (arg.equals("-w")) {
                _workingDir = args[++i];
            } else if (arg.equals("-g")) {
                _targetUrl = args[++i];
            } else if (arg.equals("-d")) {
                _debug = true;
            }
        }
    }

    private static void exportDataSetPerTable(Connection conn) throws Exception {
        IDatabaseConnection connection = new DatabaseConnection(conn);
        // partial database export
        _out.print("dumping partial db...");
        for (final String table : _tables) {
            setSeqVal(table, conn);
            QueryDataSet dataSet = new QueryDataSet(connection);
            dataSet.addTable(table);
            String file = _workingDir+table+".xml.gz";
            GZIPOutputStream gstream = new GZIPOutputStream(new FileOutputStream(file));
            long start = now();
            _out.print("writing "+file+"...");
            FlatXmlDataSet.write(dataSet, gstream);
            gstream.finish();
            _out.println("done "+(System.currentTimeMillis()-start)+" ms");
        }
        _out.println("done");
        for (final Map.Entry<String, Long> entry : _seqMap.entrySet()) {
            final String seqName = entry.getKey();
            final Long seq = entry.getValue();
            _out.println(seqName+": "+seq);
        }
    }

    private static void setSeqVal(String table, Connection conn) {
        Statement stmt = null;
        ResultSet rs = null;
        try {
            String seq = table.toUpperCase()+"_ID_SEQ";
            String sql = "select nextval('"+seq+"')";
            stmt = conn.createStatement();
            stmt.execute(sql);
            sql = "select currval('"+seq+"')";
            rs = stmt.executeQuery(sql);
            if (rs.next()) {
                long val = rs.getLong(1);
                _seqMap.put(table.toUpperCase(), val);
                sql = "select setval('"+seq+"', "+val+", false)";
                stmt.execute(sql);
            }
        } catch (SQLException e) {
            // most likely sequence does not exist, which is fine
        } finally {
            DBUtil.close(_logCtx, null, stmt, rs);
        }
    }

    private static void importDataSetPerTable(Connection conn) throws Exception {
        IDatabaseConnection connection = new DatabaseConnection(conn);
        conn.setAutoCommit(false);
        long begin = now();
        _out.println("restoring db...");
        for (String table : _tables) {
            _out.print("restoring " + table + "...");
            long start = now();
            String file = _workingDir+table+".xml.gz";
            GZIPInputStream gstream = new GZIPInputStream(new FileInputStream(file));
            IDataSet dataset = new FlatXmlDataSet(gstream);
            DatabaseOperation.CLEAN_INSERT.execute(connection, dataset);
            _out.println("done " + (now()-start) + " ms");
        }
        conn.commit();
        _out.println("done restoring db in " + (now()-begin) + " ms");
    }

    private static final long now() {
        return System.currentTimeMillis();
    }

    private static void importSequences(Connection conn) throws Exception {
        if (isPG(conn)) {
            importPGSequences(conn);
        } else if (isOra(conn)) {
            importOraSequences(conn);
        } else if (isMySQL(conn)) {
            importMySQLSequences(conn);
        }
    }

    private static void importOraSequences(Connection conn) throws Exception {
        throw new OperationNotSupportedException();
    }

    private static void importMySQLSequences(Connection conn) throws Exception {
        PreparedStatement pstmt = null;
        try {
            pstmt = conn.prepareStatement("truncate table HQ_SEQUENCE");
            pstmt.executeUpdate();
            pstmt.close();
            String sql = "insert into HQ_SEQUENCE (seq_name, seq_val)" +
                         " VALUES (?, ?)";
            pstmt = conn.prepareStatement(sql);
            for (Map.Entry<String, Long> entry : _seqMap.entrySet()) {
                String seqName = entry.getKey();
                Long val = entry.getValue();
                pstmt.setString(1, seqName);
                pstmt.setLong(2, (val/100)+1);
                int rows = pstmt.executeUpdate();
                _out.println(seqName + ": " + val + ", " + rows);
            }
        } finally {
            DBUtil.close(_logCtx, null, pstmt, null);
        }
    }

    private static void importPGSequences(Connection conn) throws Exception {
        throw new OperationNotSupportedException();
    }

    private static Connection getConnectionExport() throws Exception {
        String url = (_sourceUrl.contains("?")) ?
            _sourceUrl + "&protocolVersion=2" :
            _sourceUrl + "?protocolVersion=2";
        Driver driver = (Driver)Class.forName("org.postgresql.Driver").newInstance();
        Properties props = new Properties();
        props.setProperty("user",_sourceUser);
        props.setProperty("password",_sourcePass);
        return driver.connect(url, props);
    }

    private static Connection getConnectionImport() throws Exception {
        String url = (_targetUrl.contains("?")) ?
            _targetUrl + "&rewriteBatchedStatements=true&sessionVariables=FOREIGN_KEY_CHECKS=0" :
            _targetUrl + "?rewriteBatchedStatements=true&sessionVariables=FOREIGN_KEY_CHECKS=0";
        Driver driver = (Driver)Class.forName("com.mysql.jdbc.Driver").newInstance();
        Properties props = new Properties();
        props.setProperty("user",_targetUser);
        props.setProperty("password",_targetPass);
        return driver.connect(url, props);
    }

    private static Collection<BigTable> getBigTables()
    {
        Set<BigTable> list = new HashSet<BigTable>();
        list.add(new BigTable("EAM_EVENT_LOG", "id"));
        list.add(new BigTable("EAM_REQUEST_STAT", "id"));
        list.add(new BigTable("EAM_GALERT_AUX_LOGS", "id"));
        list.add(new BigTable("EAM_METRIC_AUX_LOGS", "id"));
        list.add(new BigTable("EAM_RESOURCE_AUX_LOGS", "id"));
        list.add(new BigTable("EAM_MEASUREMENT", "id")); 
        list.add(new BigTable("EAM_MEASUREMENT_DATA_1D", "timestamp,measurement_id"));
        list.add(new BigTable("EAM_MEASUREMENT_DATA_1H", "timestamp,measurement_id"));
        list.add(new BigTable("EAM_MEASUREMENT_DATA_6H", "timestamp,measurement_id"));
        list.add(new BigTable("HQ_METRIC_DATA_0D_0S", "timestamp,measurement_id"));
        list.add(new BigTable("HQ_METRIC_DATA_0D_1S", "timestamp,measurement_id"));
        list.add(new BigTable("HQ_METRIC_DATA_1D_0S", "timestamp,measurement_id"));
        list.add(new BigTable("HQ_METRIC_DATA_1D_1S", "timestamp,measurement_id"));
        list.add(new BigTable("HQ_METRIC_DATA_2D_0S", "timestamp,measurement_id"));
        list.add(new BigTable("HQ_METRIC_DATA_2D_1S", "timestamp,measurement_id"));
        list.add(new BigTable("HQ_METRIC_DATA_3D_0S", "timestamp,measurement_id"));
        list.add(new BigTable("HQ_METRIC_DATA_3D_1S", "timestamp,measurement_id"));
        list.add(new BigTable("HQ_METRIC_DATA_4D_0S", "timestamp,measurement_id"));
        list.add(new BigTable("HQ_METRIC_DATA_4D_1S", "timestamp,measurement_id"));
        list.add(new BigTable("HQ_METRIC_DATA_5D_0S", "timestamp,measurement_id"));
        list.add(new BigTable("HQ_METRIC_DATA_5D_1S", "timestamp,measurement_id"));
        list.add(new BigTable("HQ_METRIC_DATA_6D_0S", "timestamp,measurement_id"));
        list.add(new BigTable("HQ_METRIC_DATA_6D_1S", "timestamp,measurement_id"));
        list.add(new BigTable("HQ_METRIC_DATA_7D_0S", "timestamp,measurement_id"));
        list.add(new BigTable("HQ_METRIC_DATA_7D_1S", "timestamp,measurement_id"));
        list.add(new BigTable("HQ_METRIC_DATA_8D_0S", "timestamp,measurement_id"));
        list.add(new BigTable("HQ_METRIC_DATA_8D_1S", "timestamp,measurement_id"));
        return list;
    }
    
    private static void importBigTables(Connection connExport,
                                        Connection connImport)
    throws SQLException {
        final Collection<BigTable> tables = getBigTables();
        for (final BigTable table : tables) {
            setSeqVal(table.getTable(), connExport);
            final long start = now();
            try {
                setSeqVal(table.getTable(), connExport);
            } catch (Exception e) {
                // ignore, sequence just doesn't exist
            }
            _out.print("transferring large table " + table.getTable() + "...");
            transferTable(table, connExport, connImport);
            _out.println("done " + (now() - start) + " ms");
        }
    }

    private static void transferTable(BigTable table, Connection connExport,
                                      Connection connImport) throws SQLException {
        PreparedStatement pstmt = null;
        PreparedStatement exportPstmt = null;
        ResultSet rs = null;
        final int insertBatchSize = 20000;
        final int selectBatchSize = insertBatchSize*100;
        int offset = 0;
        try {
            Statement istmt = connImport.createStatement();
            String sql = "truncate table " + table.getTable();
            istmt.executeUpdate(sql);
            istmt.execute("SET UNIQUE_CHECKS = 0");
            istmt.close();
            sql = "select * from " + table.getTable() +
                  " order by " + table.getOrderBy() +
                  " limit ? offset ?";
            exportPstmt = connExport.prepareStatement(sql);
            while (true) {
                int batch = 0;
                exportPstmt.setInt(1, selectBatchSize);
                exportPstmt.setInt(2, (offset++ * selectBatchSize));
                rs = exportPstmt.executeQuery();
                _out.println("row " + ((offset-1) * selectBatchSize));
                boolean hasNext = false;
                if (pstmt != null) {
                    pstmt.clearBatch();
                }
                while (rs.next()) {
                    hasNext = true;
                    if (pstmt == null) {
                        pstmt = getPStmt(table.getTable(), connImport, rs);
                    }
                    ResultSetMetaData md = rs.getMetaData();
                    int count = md.getColumnCount();
                    for (int i = 1; i <= count; i++) {
                        pstmt.setObject(i, rs.getObject(i));
                    }
                    pstmt.addBatch();
                    batch++;
                    if ((batch % insertBatchSize) == 0) {
                        _out.print('.');
                        pstmt.executeBatch();
                        pstmt.clearBatch();
                    }
                }
                if (pstmt != null && (batch % insertBatchSize) != 0) {
                    pstmt.executeBatch();
                }
                _out.println();
                rs.close();
                if (!hasNext) {
                    break;
                }
            }
            if (pstmt != null) pstmt.close();
            istmt = connImport.createStatement();
            istmt.execute("SET UNIQUE_CHECKS = 1");
            istmt.close();
        } finally {
            DBUtil.close(_logCtx, null, exportPstmt, rs);
            DBUtil.close(_logCtx, null, pstmt, null);
        }
    }
    
    private static PreparedStatement getPStmt(String table, Connection conn,
                                              ResultSet rs) throws SQLException {
        StringBuilder buf = new StringBuilder("INSERT INTO ");
        StringBuilder vals = new StringBuilder(" VALUES (");
        buf.append(table).append(" (");
        ResultSetMetaData md = rs.getMetaData();
        int count = md.getColumnCount();
        for (int i = 1; i <= count; i++) {
            String name = md.getColumnName(i).trim();
            buf.append(name);
            if ((i) < count) {
                buf.append(", ");
            }
            if (i > 1) {
                vals.append(", ");
            }
            vals.append("?");
            String sBuf = buf.toString() + ")" + vals.toString() + ")";
        }
        buf.append(')');
        String sBuf = buf.toString() + vals.toString() + ")";
        if (_debug) _out.println(sBuf);
        return conn.prepareStatement(sBuf);
    }

    private static List<String> getTables(Connection conn, String tableOwner)
    throws Exception {
        List<String> rtn = null;
        if (isPG(conn)) {
            rtn = getPGTables(conn, tableOwner);
        } else if (isOra(conn)) {
            rtn = getOraTables(conn);
        } else if (isMySQL(conn)) {
            rtn = getMySQLTables(conn);
        }
        if (rtn == null) {
            throw new Exception("ERROR: cannot determine what type of database is being used " +
                                "for connectionUrl=" + conn.getMetaData().getURL());
        } else if (rtn.isEmpty()) {
            throw new Exception("ERROR: query to determine HQ tables had no results.  Make sure " +
                                "the sourceuser owns the HQ tables.");
        }
        return rtn;
    }

    private static List<String> getOraTables(Connection conn) throws Exception {
        return new ArrayList<String>();
    }

    private static List<String> getMySQLTables(Connection conn) throws Exception {
        return new ArrayList<String>();
    }

    private static List<String> getPGTables(Connection conn, String tableOwner)
    throws Exception {
        Statement stmt = null;
        ResultSet rs = null;
        Collection<BigTable> bigTables = getBigTables();
        StringBuilder buf = new StringBuilder();
        for (BigTable table : bigTables) {
            buf.append("'").append(table.getTable()).append("',");
        }
        String notIn = buf.toString().substring(0, buf.length()-1);
        List<String> rtn = new ArrayList<String>();
        try {
            stmt = conn.createStatement();
            String sql = "select upper(tablename) as tablename" +
                         " FROM pg_tables" +
                         " WHERE tableowner = ':owner'" +
                         " AND upper(tablename) not in (:vals)" +
                         " AND schemaname = 'public'" +
                         " ORDER BY tablename";
            sql = sql.replace(":owner", tableOwner);
            sql = sql.replace(":vals", notIn);
            System.out.println(sql);
            rs = stmt.executeQuery(sql);
            int table_col = rs.findColumn("tablename");
            while (rs.next()) {
                String table = rs.getString(table_col);
                if (bigTables.contains(new BigTable(table))) {
                    continue;
                }
                rtn.add(table);
            }
            return rtn;
        } finally {
            DBUtil.close(_logCtx, null, stmt, rs);
        }
    }

    private static boolean isPG(Connection conn) throws Exception {
        if (-1 == conn.getMetaData().getURL().toLowerCase().indexOf("postgresql")) {
            return false;
        }
        return true;
    }

    private static boolean isOra(Connection conn) throws Exception {
        if (-1 == conn.getMetaData().getURL().toLowerCase().indexOf("oracle")) {
            return false;
        }
        return true;
    }

    private static boolean isMySQL(Connection conn) throws Exception {
        if (-1 == conn.getMetaData().getURL().toLowerCase().indexOf("mysql")) {
            return false;
        }
        return true;
    }
    
    private static class BigTable {
        private final String _table;
        private final String _orderBy;
        private BigTable(final String table) {
            _table = table;
            _orderBy = null;
        }
        private BigTable(final String table, final String orderBy) {
            _table = table;
            _orderBy = orderBy;
        }
        private final String getOrderBy() {
            return _orderBy;
        }
        private final String getTable() {
            return _table;
        }
        public boolean equals(Object rhs) {
            if (this == rhs) {
                return true;
            } else if (rhs instanceof BigTable) {
                return equals((BigTable)rhs);
            }
            return false;
        }
        private boolean equals(BigTable rhs) {
            return _table.equals(rhs._table);
        }
        public int hashCode() {
            return _table.hashCode();
        }
    }

}
