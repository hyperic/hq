import java.io.FileNotFoundException;
import java.io.PrintStream;
import java.sql.Connection;
import java.sql.Driver;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

public class VerifySchema {
    private static int _updateRows;
    private static int _numRows;
    private static int _totalRows;
    private static String _prefix;
    private static String _modelDB;
    private static String _upgradeDB;
    private static final String excludes =
        "('HQ_METRIC_DATA', 'EAM_MEASUREMENT_DATA')";
    /**
     * {@link Map} of colIndex to {@link List} of column values
     */
    private static Map<Integer, List<String>> _valMap =
        new HashMap<Integer, List<String>>();
    /**
     * {@link Map} of colIndex to the maximum width
     */
    private static Map<Integer, Integer> _colMap =
        new HashMap<Integer, Integer>();
    private static String _user;
    private static String _pass;
    private static Boolean _debug = false;
    private static String _upgradeUser;
    private static String _upgradePass;
    private static String _dbtype;

    public static void main(String[] args) throws Exception {
        getArgs(args);
        if (_dbtype.equalsIgnoreCase("mysql")) {
            _modelDB = "hqdbmodel";
            _upgradeDB = "nipuna";
            checkMySqlDB(_modelDB, _user, _pass);
            checkMySqlDB(_upgradeDB, _upgradeUser, _upgradePass);
//            checkMySqlDB("ams");
//            checkMySqlDB("hqdb");
        } else if (_dbtype.equalsIgnoreCase("oracle")) {
//            checkOraDB("QADB", "hqmodel", "hqmodel");
//            checkOraDB("QADB", "nipuna", "nipuna");
            checkOraDB(_modelDB, _user, _pass);
            checkOraDB(_upgradeDB, _upgradeUser, _upgradePass);
        }
//        checkPGDB("hqdb", "hqadmin");
//        checkPGDB("hqdbhi5", "hyperic");
    }

    private static void getArgs(String[] args) {
        for (int i=0; i<args.length; i++) {
            String arg = args[i];
            if (arg.equalsIgnoreCase("--upgradedb")) {
                _upgradeDB = args[++i];
            } else  if (arg.equalsIgnoreCase("--modeldb")) {
                _modelDB = args[++i];
            } else  if (arg.equalsIgnoreCase("--dbtype")) {
                _dbtype = args[++i];
            } else  if (arg.equalsIgnoreCase("--modeluser")) {
                _user = args[++i];
            } else  if (arg.equalsIgnoreCase("--modelpass")) {
                _pass = args[++i];
            } else  if (arg.equalsIgnoreCase("--upgradeuser")) {
                _upgradeUser = args[++i];
            } else  if (arg.equalsIgnoreCase("--upgradepass")) {
                _upgradePass = args[++i];
            } else  if (arg.equalsIgnoreCase("--debug")) {
                _debug = Boolean.valueOf(args[++i]);
            }
        }
        _upgradeUser = (_upgradeUser == null) ? _user : _upgradeUser;
        _upgradePass = (_upgradePass == null) ? _pass : _upgradePass;
        if (_upgradeDB == null) {
            System.err.println("ERROR --upgradedb missing");
            System.exit(1);
        }
        if (_modelDB == null) {
            System.err.println("ERROR --modeldb missing");
            System.exit(1);
        }
        if (_user == null) {
            System.err.println("ERROR --modeluser missing");
            System.exit(1);
        }
        if (_pass == null) {
            System.err.println("ERROR --modelpass missing");
            System.exit(1);
        }
        if (_upgradeUser == null) {
            System.err.println("ERROR --upgradeuser missing");
            System.exit(1);
        }
        if (_upgradePass == null) {
            System.err.println("ERROR --upgradepass missing");
            System.exit(1);
        }
        if (_dbtype == null) {
            System.err.println("ERROR --dbtype missing");
            System.exit(1);
        }
    }

    public static void checkPGDB(String db, String user)
    throws Exception {
        Connection conn = null;
        Statement stmt = null;
        try {
            _prefix = db;
            conn = getPGConnection(db, user, user);
            stmt = conn.createStatement();
            getColumnInfo(db, stmt);
            getConstraintInfo(db, stmt);
            getIndexInfo(db, stmt);
        } finally {
            close(conn, stmt, null);
        }
    }

    public static void checkOraDB(String db, String user, String pass)
    throws Exception {
        Connection conn = null;
        Statement stmt = null;
        try {
            _prefix = user;
            conn = getOraConnection(db, user, pass);
            stmt = conn.createStatement();
            getColumnInfo(db, stmt);
            getConstraintInfo(db, stmt);
            getIndexInfo(db, stmt);
        } finally {
            close(conn, stmt, null);
        }
    }

    public static void checkMySqlDB(String db, String user, String pass)
    throws Exception {
        Connection conn = null;
        Statement stmt = null;
        try {
            _prefix = db;
            conn = getMySqlConnection(db, user, pass);
            stmt = conn.createStatement();
            stmt.execute("use information_schema");
            getColumnInfo(db, stmt);
            getConstraintInfo(db, stmt);
            getIndexInfo(db, stmt);
        } finally {
            close(conn, stmt, null);
        }
    }

    private static void getIndexInfo(String db, Statement stmt)
    throws SQLException, FileNotFoundException {
        if (isMySQL(stmt)) {
            getIndexInfoMySql(db, stmt);
        } else if (isPG(stmt)) {
            getIndexInfoPG(db, stmt);
        } else if (isOra(stmt)) {
            getIndexInfoOra(db, stmt);
        }
    }
    
    private static void getIndexInfoOra(String db, Statement stmt)
    throws SQLException, FileNotFoundException {
        String sql =
            "SELECT c.TABLE_NAME, cc.COLUMN_NAME, c.UNIQUENESS, cc.COLUMN_POSITION " +
            "\nFROM   user_indexes c, user_ind_columns cc " +
            "\nWHERE  c.INDEX_NAME = cc.INDEX_NAME " +
            "\nORDER  by c.TABLE_NAME, c.INDEX_NAME, cc.COLUMN_POSITION, cc.COLUMN_NAME";
        sql = sql.replace(":db", db);
        debug(sql);
        stmt.execute(sql);
        printResultSet(stmt, new PrintStream("/tmp/" + _prefix + ".indexes"));
    }

    private static void debug(String sql) {
        if (_debug) System.out.println("\n" + sql);
    }

    private static void getIndexInfoPG(String db, Statement stmt)
    throws SQLException, FileNotFoundException {
        String sql = "SELECT tablename,indexname,indexdef"+
            "\nFROM pg_indexes WHERE schemaname = 'public'"+
            "\nORDER BY tablename,indexname,indexdef";
/*
SELECT c.relname, a.attname, pg_catalog.format_type(a.atttypid, a.atttypmod),
       a.attnotnull, a.attnum
FROM pg_catalog.pg_class c
LEFT JOIN pg_catalog.pg_namespace n ON n.oid = c.relnamespace
JOIN pg_catalog.pg_attribute a ON a.attrelid = c.oid
WHERE pg_catalog.pg_table_is_visible(c.oid)
AND n.nspname = 'public' AND a.attnum > 0 AND NOT a.attisdropped
ORDER BY c.relname, a.attname, a.attnum
 */
        debug(sql);
        stmt.execute(sql);
        printResultSet(stmt, new PrintStream("/tmp/" + _prefix + ".indexes"));
    }

    private static void getIndexInfoMySql(String db, Statement stmt)
    throws SQLException, FileNotFoundException {
        String sql = 
            "SELECT TABLE_NAME, NON_UNIQUE, INDEX_NAME, " +
            "SEQ_IN_INDEX, COLUMN_NAME" +
            "\nFROM STATISTICS where INDEX_SCHEMA = ':db'" +
            "\nORDER BY TABLE_NAME, NON_UNIQUE, INDEX_SCHEMA, INDEX_NAME, " +
            "SEQ_IN_INDEX, COLUMN_NAME";
        sql = sql.replace(":db", db);
        debug(sql);
        stmt.execute(sql);
        printResultSet(stmt, new PrintStream("/tmp/" + _prefix + ".indexes"));
    }

    private static void getConstraintInfo(String db, Statement stmt)
    throws SQLException, FileNotFoundException {
        if (isMySQL(stmt)) {
            getConstraintInfoMySql(db, stmt);
        } else if (isPG(stmt)) {
            getConstraintInfoPG(db, stmt);
        } else if (isOra(stmt)) {
            getConstraintInfoOra(db, stmt);
        }
    }

    private static void getConstraintInfoOra(String db, Statement stmt)
    throws SQLException, FileNotFoundException {
        String sql = 
            "SELECT  c.TABLE_NAME, c.CONSTRAINT_NAME, cc.COLUMN_NAME, " +
            "r.TABLE_NAME, rc.COLUMN_NAME, c.CONSTRAINT_TYPE, cc.POSITION " +
            "\nFROM  user_constraints c, user_constraints r, user_cons_columns cc, " +
            "user_cons_columns rc " +
            "\nWHERE c.OWNER not in ('SYS','SYSTEM') " +
            /*and     c.CONSTRAINT_TYPE = 'R'*/
            "\nAND   c.R_OWNER = r.OWNER " +
            "\nAND   c.R_CONSTRAINT_NAME = r.CONSTRAINT_NAME " +
            "\nAND   c.CONSTRAINT_NAME = cc.CONSTRAINT_NAME " +
            "\nAND   c.OWNER = cc.OWNER " +
            "\nAND   r.CONSTRAINT_NAME = rc.CONSTRAINT_NAME " +
            "\nAND   r.OWNER = rc.OWNER " +
            "\nAND   cc.POSITION = rc.POSITION " +
            "\nORDER BY c.TABLE_NAME, c.CONSTRAINT_NAME, cc.POSITION ";
        sql = sql.replace(":db", db);
        debug(sql);
        stmt.execute(sql);
        printResultSet(stmt, new PrintStream("/tmp/" + _prefix + ".constraints"));
    }

    private static void getConstraintInfoPG(String db, Statement stmt) {
    }

    private static void getConstraintInfoMySql(String db, Statement stmt)
    throws SQLException, FileNotFoundException {
        String sql = 
            "SELECT CONSTRAINT_NAME, TABLE_NAME, COLUMN_NAME, ORDINAL_POSITION, " +
                "REFERENCED_TABLE_NAME" +
        	"\nFROM KEY_COLUMN_USAGE" +
        	"\nWHERE TABLE_SCHEMA = ':db'" +
        	"\nORDER BY TABLE_NAME, CONSTRAINT_NAME, COLUMN_NAME, ORDINAL_POSITION";
        sql = sql.replace(":db", db);
        debug(sql);
        stmt.execute(sql);
        printResultSet(stmt, new PrintStream("/tmp/" + _prefix + ".constraints"));
    }

    private static void getColumnInfo(String db, Statement stmt)
    throws SQLException, FileNotFoundException {
        if (isMySQL(stmt)) {
            getColumnInfoMySql(db, stmt);
        } else if (isPG(stmt)) {
            getColumnInfoPG(db, stmt);
        } else if (isOra(stmt)) {
            getColumnInfoOra(db, stmt);
        }
    }

    private static void getColumnInfoOra(String db, Statement stmt)
    throws SQLException, FileNotFoundException {
        String sql =
            "SELECT distinct TABLE_NAME, COLUMN_NAME, DATA_TYPE, " +
            "DATA_PRECISION, DATA_SCALE, NULLABLE " +
            "\nFROM ALL_TAB_COLUMNS " +
            "\nWHERE owner not like '%SYS' " +
            "\nAND owner != 'SYSTEM' " +
            "\nAND owner != 'XDB' " +
            "\nAND table_name not like '%$%' " +
            "\nORDER BY TABLE_NAME, COLUMN_NAME, DATA_TYPE, " +
            "DATA_PRECISION, DATA_SCALE";
        debug(sql);
        stmt.execute(sql);
        printResultSet(stmt, new PrintStream("/tmp/" + _prefix + ".column"));
    }

    private static void getColumnInfoPG(String db, Statement stmt)
    throws SQLException, FileNotFoundException {
        String sql = 
//            "SELECT a.attname, pg_catalog.format_type(a.atttypid, a.atttypmod), (" +
            "SELECT a.attname, pg_catalog.format_type(a.atttypid, a.atttypmod), " +
//            "SELECT a.attname, (" +
//                "\nSELECT substring(pg_catalog.pg_get_expr(d.adbin, d.adrelid) for 128)" +
//                "\nFROM pg_catalog.pg_attrdef d" +
//                "\nWHERE d.adrelid = a.attrelid AND d.adnum = a.attnum AND a.atthasdef" +
//                "\n),a.attnotnull, a.attnum" +
                "a.attnotnull, a.attnum" +
        	"\nFROM pg_catalog.pg_attribute a" +
        	"\n" +
        	"\nWHERE a.attrelid in (" +
        	    "\nSELECT c.oid"+
                "\nFROM pg_catalog.pg_class c "+
                "\nLEFT JOIN pg_catalog.pg_namespace n ON n.oid = c.relnamespace "+
                "\nWHERE pg_catalog.pg_table_is_visible(c.oid) "+
                "\nAND n.nspname = 'public' "+
//                "\nAND c.relname ~ '^(eam_measurement)$' "+
                "\nAND a.attnum > 0 AND NOT a.attisdropped"+
            "\n) ORDER BY a.attnum";
//"'44688' AND a.attnum > 0 AND NOT a.attisdropped"+
        debug(sql);
        stmt.execute(sql);
        printResultSet(stmt, new PrintStream("/tmp/" + _prefix + ".column"));
    }

    private static void getColumnInfoMySql(String db, Statement stmt)
    throws SQLException, FileNotFoundException {
        String sql =
            "SELECT distinct t.TABLE_NAME, COLUMN_NAME, " +
                "IS_NULLABLE, DATA_TYPE, " +
                "CHARACTER_MAXIMUM_LENGTH, NUMERIC_PRECISION, NUMERIC_SCALE, " +
                "COLUMN_TYPE" +
            "\nFROM tables t" +
            "\nJOIN columns c on c.table_name = t.table_name" +
            "\nWHERE c.table_schema = ':db' AND t.TABLE_TYPE != 'VIEW'" +
            "\nAND t.table_name not in " + excludes +
            "\nORDER BY t.TABLE_NAME, COLUMN_NAME, c.ORDINAL_POSITION";
        sql = sql.replace(":db", db);
        debug(sql);
        stmt.execute(sql);
        printResultSet(stmt, new PrintStream("/tmp/" + _prefix + ".column"));
    }

    private static void close(Connection conn, Statement stmt, ResultSet rs) {
        try {
            if (rs != null) rs.close();
            if (stmt != null) stmt.close();
            if (conn != null) conn.close();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private static Connection getOraConnection(String db, String user, String pass)
    throws Exception {
//        String url = "jdbc:oracle:thin:@10.2.0.206:1522:" + db;
        String url = "jdbc:oracle:thin:@" + db;
        Driver driver = (Driver)Class.forName("oracle.jdbc.OracleDriver").newInstance();
        Properties props = new Properties();
        props.setProperty("user",user);
        props.setProperty("password",pass);
        debug("url=" + url);
        debug("user=" + user);
        debug("pass=" + pass);
        return driver.connect(url, props);
    }

    private static Connection getMySqlConnection(String db, String user,
                                                 String pass)
    throws Exception {
        String url = "jdbc:mysql://localhost:3306/" + db;
//        String url = "jdbc:mysql://10.2.0.210:3306/" + db;
        Driver driver = (Driver)Class.forName("com.mysql.jdbc.Driver").newInstance();
        Properties props = new Properties();
        props.setProperty("user",user);
        props.setProperty("password",pass);
        debug("url=" + url);
        debug("user=" + user);
        debug("pass=" + pass);
        return driver.connect(url, props);
    }

    private static Connection getPGConnection(String db, String user, String pass)
    throws Exception {
        String url = "jdbc:postgresql://localhost:5432/" + db;
        Properties props = new Properties();
        props.setProperty("user",user);
        props.setProperty("password",pass);
        Driver driver = (Driver)Class.forName("org.postgresql.Driver").newInstance();
        debug("url=" + url);
        debug("user=" + user);
        debug("pass=" + pass);
        return driver.connect(url, props);
    }

    private static void printResultSet(Statement stmt, PrintStream stream)
    throws SQLException {
        do {
            _updateRows = stmt.getUpdateCount();
            ResultSet rs = stmt.getResultSet();
            if (stmt.getUpdateCount() != -1) {
                continue;
            }
            if (rs == null) {
                break;
            }
            printData(rs, stmt, stream);
        } while (stmt.getMoreResults() == true);
        stream.println("rows: "+_numRows+"\n");
        if (_updateRows != -1) {
            stream.println("update rows: "+_updateRows+"\n");
        }
    }

    private static void printData(ResultSet rs, Statement stmt, PrintStream stream)
    throws SQLException {
        clearObjects();
        ResultSetMetaData md = rs.getMetaData();
        processColumnHeader(md);
        processColumns(rs);
        printColumnHeader(md, stream);
        printColumns(md, stream);
    }
    
    private static void printColumns(ResultSetMetaData md, PrintStream stream)
    throws SQLException {
        for (int i = 0; i < _numRows; i++) {
            for (int j = 1; j <= md.getColumnCount(); j++) {
                String val = "";
                if (_valMap.get(j).size() > 0) {
                    val = _valMap.get(j).remove(0);
                }
//XXX                stream.printf("%-" + _colMap.get(j) + "s ", val);
                stream.printf("%s,", val.toUpperCase());
            }
            stream.println();
        }
    }
    
    private static void printColumnHeader(ResultSetMetaData md,
                                          PrintStream stream)
    throws SQLException {
        int len = 0;
        for (int i=1; i<=md.getColumnCount(); i++) {
            len += _colMap.get(i)+1;
//XXX            stream.printf("%-"+_colMap.get(i)+"s ", md.getColumnName(i));
            stream.printf("%s,", md.getColumnName(i).toUpperCase());
        }
        StringBuffer buf = new StringBuffer(len);
        for (int i=0; i<len; i++) {
            buf.append("-");
        }
        stream.println("\n"+buf);
    }
    
    private static void processColumnHeader(ResultSetMetaData md)
    throws SQLException {
        for (int i=1; i<=md.getColumnCount(); i++) {
            int length = md.getColumnName(i).trim().length();
            length = (length == 0) ? 1 : length;
            _colMap.put(i, length);
            _valMap.put(i, new ArrayList<String>());
        }
    }

    private static void clearObjects() {
        _totalRows += _numRows;
        _numRows = 0;
        _colMap.clear();
        _valMap.clear();
    }

    private static void processColumns(ResultSet rs) throws SQLException {
        while (rs.next()) {
            _numRows++;
            ResultSetMetaData rsmd = rs.getMetaData();
            for (int i=1; i<=rsmd.getColumnCount(); i++) {
                Integer ind = new Integer(i);
                String val = null;
                if (rs.getObject(i) == null) {
                    val = "()";
                } else {
                    try {
                        // XXX ignoring BLOBs for now
                        if (rsmd.getColumnType(i) == -2) {
                        } else {
                            val = rs.getString(i).trim();
                        }
                    } catch (Exception e) {
                        val = "";
                    }
                }
                _valMap.get(ind).add(val);
                if (val.length() > ((Integer)_colMap.get(ind)).intValue()) {
                    _colMap.put(ind, new Integer(val.length()));
                }
            }
        }
    }

    private static String getUrl(Statement stmt) throws SQLException {
        return stmt.getConnection().getMetaData().getURL();
    }
    
    private static boolean isPG(Statement stmt) throws SQLException {
        String url = getUrl(stmt);
        if (-1 == url.toLowerCase().indexOf("postgresql")) {
            return false;
        }
        return true;
    }

    private static boolean isOra(Statement stmt) throws SQLException {
        String url = getUrl(stmt);
        if (-1 == url.toLowerCase().indexOf("oracle")) {
            return false;
        }
        return true;
    }

    private static boolean isMySQL(Statement stmt) throws SQLException {
        String url = getUrl(stmt);
        if (-1 == url.toLowerCase().indexOf("mysql")) {
            return false;
        }
        return true;
    }
}