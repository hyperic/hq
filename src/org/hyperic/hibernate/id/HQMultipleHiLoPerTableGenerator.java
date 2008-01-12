//$Id: MultipleHiLoPerTableGenerator.java 9720 2006-03-31 00:11:54Z epbernard $
package org.hyperic.hibernate.id;

import java.io.Serializable;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Types;
import java.util.Properties;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hibernate.HibernateException;
import org.hibernate.MappingException;
import org.hibernate.dialect.Dialect;
import org.hibernate.engine.SessionImplementor;
import org.hibernate.engine.TransactionHelper;
import org.hibernate.id.Configurable;
import org.hibernate.id.IdentifierGeneratorFactory;
import org.hibernate.id.PersistentIdentifierGenerator;
import org.hibernate.mapping.Table;
import org.hibernate.type.Type;
import org.hibernate.util.PropertiesHelper;
import org.hyperic.util.jdbc.DBUtil;

/**
 * 
 * A hilo <tt>IdentifierGenerator</tt> that returns a <tt>Long</tt>,
 * constructed using a hi/lo algorithm. The hi value MUST be fetched in a
 * seperate transaction to the <tt>Session</tt> transaction so the generator
 * must be able to obtain a new connection and commit it. Hence this
 * implementation may not be used when the user is supplying connections. In
 * this case a <tt>SequenceHiLoGenerator</tt> would be a better choice (where
 * supported).<br>
 * <br>
 * 
 * A hilo <tt>IdentifierGenerator</tt> that uses a database table to store the
 * last generated values. A table can contains several hi values. They are
 * distinct from each other through a key <p/>
 * <p>
 * This implementation is not compliant with a user connection
 * </p>
 * <p/>
 * 
 * <p>
 * Allowed parameters (all of them are optional):
 * </p>
 * <ul>
 * <li>table: table name (default <tt>hibernate_sequences</tt>)</li>
 * <li>primary_key_column: key column name (default <tt>sequence_name</tt>)</li>
 * <li>value_column: hi value column name(default
 * <tt>sequence_next_hi_value</tt>)</li>
 * <li>primary_key_value: key value for the current entity (default to the
 * entity's primary table name)</li>
 * <li>primary_key_length: length of the key column in DB represented as a
 * varchar (default to 255)</li>
 * <li>max_lo: max low value before increasing hi (default to Short.MAX_VALUE)</li>
 * </ul>
 * 
 * @author Emmanuel Bernard
 * @author <a href="mailto:kr@hbt.de">Klaus Richarz</a>.
 */
public class HQMultipleHiLoPerTableGenerator extends TransactionHelper
    implements PersistentIdentifierGenerator, Configurable {

    private static final String logCtx = HQMultipleHiLoPerTableGenerator.class
        .getName();
    private static final Log log = LogFactory.getLog(logCtx);

    public static final String ID_TABLE = "table";
    public static final String PK_COLUMN_NAME = "primary_key_column";
    public static final String PK_VALUE_NAME = "primary_key_value";
    public static final String VALUE_COLUMN_NAME = "value_column";
    public static final String PK_LENGTH_NAME = "primary_key_length";
    public static final String INITIAL_HI = "initial_hi";
    public static final int DEFAULT_INITIAL_HI = 0;

    private static final int DEFAULT_PK_LENGTH = 255;
    public static final String DEFAULT_TABLE = "hibernate_sequences";
    private static final String DEFAULT_PK_COLUMN = "sequence_name";
    private static final String DEFAULT_VALUE_COLUMN = "sequence_next_hi_value";

    private String tableName;
    private String pkColumnName;
    private String valueColumnName;
    private String keyValue;

    // hilo params
    public static final String MAX_LO = "max_lo";

    private long hi;
    private int lo;
    private int maxLo;
    private int initialHi;
    private Class returnClass;
    private int keySize;

    public String[] sqlCreateStrings(Dialect dialect) throws HibernateException {
        return new String[] { new StringBuffer().append("create table ")
            .append(tableName).append(" ( ").append(pkColumnName).append(" ")
            .append(dialect.getTypeName(Types.VARCHAR, keySize, 0, 0)).append(
                ",  ").append(valueColumnName).append(" ").append(
                dialect.getTypeName(Types.INTEGER)).append(", ").append(
                "primary key (" + pkColumnName + ") ").append(" ) ").toString() };
    }

    public String[] sqlDropStrings(Dialect dialect) throws HibernateException {
        StringBuffer sqlDropString = new StringBuffer().append("drop table ");
        if (dialect.supportsIfExistsBeforeTableName())
            sqlDropString.append("if exists ");
        sqlDropString.append(tableName).append(
            dialect.getCascadeConstraintsString());
        if (dialect.supportsIfExistsAfterTableName())
            sqlDropString.append(" if exists");
        return new String[] { sqlDropString.toString() };
    }

    public Object generatorKey() {
        return tableName;
    }

    public Serializable doWorkInCurrentTransaction(Connection conn, String sql)
        throws SQLException {
        
        // Using the function nextseqval ensures atomicity of the
        // select + update

        Statement stmt = conn.createStatement();
        try {
            String args = "'" + keyValue + "', " + initialHi;
            String s = "select nextseqval(" + args + ")";
            ResultSet rs = stmt.executeQuery(s);
            if (rs.next())
                return new Integer(rs.getInt(1));
            else
                throw new SQLException("Error executing nextseqval() function");
        } catch (SQLException sqle) {
            log.error("could not read or init a hi value", sqle);
            throw sqle;
        } finally {
            DBUtil.closeStatement(logCtx, stmt);
        }
    }

    public synchronized Serializable generate(SessionImplementor session,
        Object obj) throws HibernateException {
        if (maxLo < 1) {
            // keep the behavior consistent even for boundary usages
            int val = ((Integer) doWorkInNewTransaction(session)).intValue();
            if (val == 0)
                val = ((Integer) doWorkInNewTransaction(session)).intValue();
            return IdentifierGeneratorFactory.createNumber(val, returnClass);
        }
        if (lo > maxLo) {
            int hival = ((Integer) doWorkInNewTransaction(session)).intValue();
            lo = (hival == 0) ? 1 : 0;
            hi = hival * (maxLo + 1);
            log.debug("new hi value: " + hival);
        }
        return IdentifierGeneratorFactory.createNumber(hi + lo++, returnClass);
    }

    public void configure(Type type, Properties params, Dialect dialect)
        throws MappingException {
        tableName = PropertiesHelper.getString(ID_TABLE, params, DEFAULT_TABLE);
        pkColumnName = PropertiesHelper.getString(PK_COLUMN_NAME, params,
            DEFAULT_PK_COLUMN);
        valueColumnName = PropertiesHelper.getString(VALUE_COLUMN_NAME, params,
            DEFAULT_VALUE_COLUMN);
        initialHi = PropertiesHelper.getInt(INITIAL_HI, params,
            DEFAULT_INITIAL_HI);
        String schemaName = params.getProperty(SCHEMA);
        String catalogName = params.getProperty(CATALOG);
        keySize = PropertiesHelper.getInt(PK_LENGTH_NAME, params,
            DEFAULT_PK_LENGTH);
        String keyValue = PropertiesHelper.getString(PK_VALUE_NAME, params,
            params.getProperty(TABLE));

        if (tableName.indexOf('.') < 0) {
            tableName = Table.qualify(catalogName, schemaName, tableName);
        }

        this.keyValue = keyValue;

        // hilo config
        maxLo = PropertiesHelper.getInt(MAX_LO, params, Short.MAX_VALUE);
        lo = maxLo + 1; // so we "clock over" on the first invocation
        returnClass = type.getReturnedClass();
    }
}
