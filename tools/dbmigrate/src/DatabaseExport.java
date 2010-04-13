import java.io.FileInputStream;
import java.io.FileOutputStream;
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
    private static String _url;
    private static List<String> _tables;
    private static String _workingDir;
    private static boolean _debug;
    private static String _targetUrl;
    private static String _sourceUrl;
    private static String _targetPass;
    private static String _targetUser;
    private static String _sourcePass;
    private static String _sourceUser;
    private static final String _pgUser = "hqadmin";
    private static final String _logCtx = DatabaseExport.class.getName();

    public static void main(String[] args) throws Exception {
        final long start = System.currentTimeMillis();
        getArgs(args);
        Connection connExp = getConnectionExport();
        _tables = Collections.unmodifiableList(getTables(connExp, _pgUser));
        exportDataSetPerTable(connExp);
        Connection connImp = getConnectionImport();
        importDataSetPerTable(connImp);
        importBigTables(connExp, connImp);
        importSequences(connImp);
        connImp.commit();
        connExp.close();
        connImp.close();
        System.out.println("process took " +
            (System.currentTimeMillis()-start)/1000/60/60 + " hours");
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
        System.out.print("dumping partial db...");
        for (final String table : _tables) {
            setSeqVal(table, conn);
            QueryDataSet dataSet = new QueryDataSet(connection);
            dataSet.addTable(table);
            String file = _workingDir+table+".xml.gz";
            GZIPOutputStream gstream = new GZIPOutputStream(new FileOutputStream(file));
            long start = now();
            if (_debug) System.out.print("writing "+file+"...");
            FlatXmlDataSet.write(dataSet, gstream);
            gstream.finish();
            if (_debug) System.out.println("done "+(System.currentTimeMillis()-start)+" ms");
        }
        System.out.println("done");
        for (final Map.Entry<String, Long> entry : _seqMap.entrySet()) {
            final String seqName = entry.getKey();
            final Long seq = entry.getValue();
            if (_debug) System.out.println(seqName+": "+seq);
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
        System.out.print("restoring db...");
        for (String table : _tables) {
            if (_debug) System.out.print("restoring " + table + "...");
            long start = now();
            String file = _workingDir+table+".xml.gz";
            GZIPInputStream gstream = new GZIPInputStream(new FileInputStream(file));
            IDataSet dataset = new FlatXmlDataSet(gstream);
            DatabaseOperation.CLEAN_INSERT.execute(connection, dataset);
            if (_debug) System.out.println("done " + (now()-start) + " ms");
        }
        conn.commit();
        System.out.println("done restoring db in " + (now()-begin) + " ms");
    }

    private static final long now() {
        return System.currentTimeMillis();
    }

    private static void importSequences(Connection conn) throws Exception {
        if (isPG()) {
            importPGSequences(conn);
        } else if (isOra()) {
            importOraSequences(conn);
        } else if (isMySQL()) {
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
                if (_debug) System.out.println(seqName + ": " + val + ", " + rows);
            }
        } finally {
            DBUtil.close(_logCtx, null, pstmt, null);
        }
    }

    private static void importPGSequences(Connection conn) throws Exception {
        throw new OperationNotSupportedException();
    }

    private static Connection getConnectionExport() throws Exception {
        _url = _sourceUrl + "?protocolVersion=2";
        Driver driver = (Driver)Class.forName("org.postgresql.Driver").newInstance();
        Properties props = new Properties();
        props.setProperty("user",_sourceUser);
        props.setProperty("password",_sourcePass);
        return driver.connect(_url, props);
    }

    private static Connection getConnectionImport() throws Exception {
        _url = _targetUrl + "?rewriteBatchedStatements=true&sessionVariables=FOREIGN_KEY_CHECKS=0";
        Driver driver = (Driver)Class.forName("com.mysql.jdbc.Driver").newInstance();
        Properties props = new Properties();
        props.setProperty("user",_targetUser);
        props.setProperty("password",_targetPass);
        return driver.connect(_url, props);
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
            System.out.print(
                "transferring large table " + table.getTable() + "...");
            transferTable(table, connExport, connImport);
            System.out.println("done " + (now() - start) + " ms");
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
                if (_debug) System.out.println("row " + ((offset-1) * selectBatchSize));
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
                        if (_debug) System.out.print('.');
                        pstmt.executeBatch();
                        pstmt.clearBatch();
                    }
                }
                if (pstmt != null && (batch % insertBatchSize) != 0) {
                    pstmt.executeBatch();
                }
                if (_debug) System.out.println();
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
            if (_debug) System.out.println(sBuf);
        }
        buf.append(')');
        String sBuf = buf.toString() + vals.toString() + ")";
        if (_debug) System.out.println(sBuf);
        return conn.prepareStatement(sBuf);
    }

    private static List<String> getTables(Connection conn, String tableOwner)
    throws Exception {
        if (isPG()) {
            return getPGTables(conn, tableOwner);
        } else if (isOra()) {
            return getOraTables(conn);
        } else if (isMySQL()) {
            return getMySQLTables(conn);
        }
        return new ArrayList<String>();
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

    private static boolean isPG() {
        if (-1 == _url.toLowerCase().indexOf("postgresql")) {
            return false;
        }
        return true;
    }

    private static boolean isOra() {
        if (-1 == _url.toLowerCase().indexOf("oracle")) {
            return false;
        }
        return true;
    }

    private static boolean isMySQL() {
        if (-1 == _url.toLowerCase().indexOf("mysql")) {
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
        public boolean equals(BigTable rhs) {
            return _table.equals(rhs._table);
        }
        public int hashCode() {
            return _table.hashCode();
        }
    }

}
