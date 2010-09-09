/**
 * NOTE: This copyright does *not* cover user programs that use HQ
 * program services by normal system calls through the application
 * program interfaces provided as part of the Hyperic Plug-in Development
 * Kit or the Hyperic Client Development Kit - this is merely considered
 * normal use of the program, and does *not* fall under the heading of
 *  "derived work".
 *
 *  Copyright (C) [2009-2010], VMware, Inc.
 *  This file is part of HQ.
 *
 *  HQ is free software; you can redistribute it and/or modify
 *  it under the terms version 2 of the GNU General Public License as
 *  published by the Free Software Foundation. This program is distributed
 *  in the hope that it will be useful, but WITHOUT ANY WARRANTY; without
 *  even the implied warranty of MERCHANTABILITY or FITNESS FOR A
 *  PARTICULAR PURPOSE. See the GNU General Public License for more
 *  details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program; if not, write to the Free Software
 *  Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307
 *  USA.
 *
 */

//$Id: MultipleHiLoPerTableGenerator.java 9720 2006-03-31 00:11:54Z epbernard $
package org.hyperic.hibernate.id;

import java.io.Serializable;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.util.LinkedList;
import java.util.Properties;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hibernate.HibernateException;
import org.hibernate.LockMode;
import org.hibernate.MappingException;
import org.hibernate.dialect.Dialect;
import org.hibernate.engine.SessionImplementor;
import org.hibernate.engine.TransactionHelper;
import org.hibernate.exception.JDBCExceptionHelper;
import org.hibernate.id.Configurable;
import org.hibernate.id.IdentifierGeneratorFactory;
import org.hibernate.id.PersistentIdentifierGenerator;
import org.hibernate.mapping.Table;
import org.hibernate.type.Type;
import org.hibernate.util.PropertiesHelper;
import org.hyperic.hq.context.Bootstrap;
import org.hyperic.util.jdbc.DBUtil;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * 
 * A hilo <tt>IdentifierGenerator</tt> that returns a <tt>Long</tt>, constructed
 * using a hi/lo algorithm. The hi value MUST be fetched in a seperate
 * transaction to the <tt>Session</tt> transaction so the generator must be able
 * to obtain a new connection and commit it. If running in a Spring-managed
 * transaction, the TransactionTemplate will be used to suspend the existing
 * transaction and create a new one. Else, an attempt will be made to open a new
 * JDBC connection to do the work.
 * 
 * A hilo <tt>IdentifierGenerator</tt> that uses a database table to store the
 * last generated values. A table can contains several hi values. They are
 * distinct from each other through a key
 * <p/>
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
public class HQMultipleHiLoPerTableGenerator
    extends TransactionHelper implements PersistentIdentifierGenerator, Configurable {

    private static final Log log = LogFactory.getLog(HQMultipleHiLoPerTableGenerator.class);

    private LinkedList seqs = new LinkedList();

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
    private String query;
    private String insert;
    private String update;
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
        return new String[] { new StringBuffer().append("create table ").append(tableName).append(
            " ( ").append(pkColumnName).append(" ").append(
            dialect.getTypeName(Types.VARCHAR, keySize, 0, 0)).append(",  ")
            .append(valueColumnName).append(" ").append(dialect.getTypeName(Types.INTEGER)).append(
                ", ").append("primary key (" + pkColumnName + ") ").append(" ) ").toString() };
    }

    public String[] sqlDropStrings(Dialect dialect) throws HibernateException {
        StringBuffer sqlDropString = new StringBuffer().append("drop table ");
        if (dialect.supportsIfExistsBeforeTableName())
            sqlDropString.append("if exists ");
        sqlDropString.append(tableName).append(dialect.getCascadeConstraintsString());
        if (dialect.supportsIfExistsAfterTableName())
            sqlDropString.append(" if exists");
        return new String[] { sqlDropString.toString() };
    }

    public Object generatorKey() {
        return tableName;
    }

    
    protected Serializable doWorkInCurrentTransaction(Connection conn, String sql)
        throws SQLException {
        return updateSequence(conn);
    }

    private Integer updateSequence(Connection connection) throws SQLException {
        int result;
        int rows;
        do {
            // The loop ensures atomicity of the
            // select + update even for no transaction
            // or read committed isolation level

            // sql = query;
            log.debug(query);
            PreparedStatement qps = connection.prepareStatement(query);
            PreparedStatement ips = null;
            try {
                // qps.setString(1, key);
                ResultSet rs = qps.executeQuery();
                boolean isInitialized = rs.next();
                if (!isInitialized) {
                    result = 0;
                    ips = connection.prepareStatement(insert);
                    // ips.setString(1, key);
                    ips.setInt(1, initialHi);
                    ips.execute();
                } else {
                    result = rs.getInt(1);
                }
                rs.close();
            } catch (SQLException sqle) {
                log.error("could not read or init a hi value", sqle);
                throw sqle;
            } finally {
                if (ips != null) {
                    ips.close();
                }
                qps.close();
            }

            // sql = update;
            PreparedStatement ups = connection.prepareStatement(update);
            try {
                ups.setInt(1, result + 1);
                ups.setInt(2, result);
                // ups.setString( 3, key );
                rows = ups.executeUpdate();
            } catch (SQLException sqle) {
                log.error("could not update hi value in: " + tableName, sqle);
                throw sqle;
            } finally {
                ups.close();
            }
        } while (rows == 0);
        return new Integer(result);
    }

    public synchronized Serializable generate(SessionImplementor session, Object obj)
        throws HibernateException {
        Number num;
        do {
            num = _generate(session, obj);
        } while (!numberIsValid(num));
        return num;
    }

    private int executeInNewTransaction(TransactionTemplate transactionTemplate,
                                   final SessionImplementor session) {
        if (transactionTemplate != null) {
            return transactionTemplate.execute(new TransactionCallback<Integer>() {
                //We are in a Spring managed environment
                public Integer doInTransaction(TransactionStatus status) {
                    try {
                        return updateSequence(Bootstrap.getBean(DBUtil.class).getConnection());
                    } catch (SQLException sqle) {
                        throw JDBCExceptionHelper.convert(session.getFactory()
                            .getSQLExceptionConverter(), sqle,
                            "could not get or update next value", null);
                    }
                }
            });
        } else {
             //Use Hibernate's JDBC delegation
             return (Integer) doWorkInNewTransaction(session);
        }
    }

    private synchronized Number _generate(final SessionImplementor session, Object obj)
        throws HibernateException {
        TransactionTemplate transactionTemplate = null;
        if (Bootstrap.hasAppContext()) {
            // We are running in Spring-managed server environment. Suspend the
            // existing Spring-managed transaction and work in a new one.
            transactionTemplate = new TransactionTemplate((PlatformTransactionManager) Bootstrap
                .getBean("transactionManager"));
            transactionTemplate
                .setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
           
        }
        if (maxLo < 1) {
            // keep the behavior consistent even for boundary usages
            int val = executeInNewTransaction(transactionTemplate, session);

            if (val == 0) {
                val = executeInNewTransaction(transactionTemplate, session);
            }
            Number num = IdentifierGeneratorFactory.createNumber(val, returnClass);
            if (log.isTraceEnabled()) {
                log.trace(this + " created seq: " + keyValue + " / " + num);
            }
            return num;
        } else if (lo > maxLo) {
            int hival = executeInNewTransaction(transactionTemplate, session);
            lo = (hival == 0) ? 1 : 0;
            hi = hival * (maxLo + 1);
            log.debug("new hi value: " + hival);
        }
        Number num = IdentifierGeneratorFactory.createNumber(hi + lo++, returnClass);
        if (log.isTraceEnabled()) {
            log.trace(this + " created seq: " + keyValue + " / " + num);
        }
        return num;
    }

    private synchronized boolean numberIsValid(Number num) {
        if (seqs.contains(num)) {
            log.warn("sequence generator generated sequence " + keyValue + "/" + num +
                     " which is a duplicate sequence, retrying");
            return false;
        }
        seqs.add(num);
        while (seqs.size() >= 10) {
            seqs.removeLast();
        }
        return true;
    }

    public void configure(Type type, Properties params, Dialect dialect) throws MappingException {
        tableName = PropertiesHelper.getString(ID_TABLE, params, DEFAULT_TABLE);
        pkColumnName = PropertiesHelper.getString(PK_COLUMN_NAME, params, DEFAULT_PK_COLUMN);
        valueColumnName = PropertiesHelper.getString(VALUE_COLUMN_NAME, params,
            DEFAULT_VALUE_COLUMN);
        initialHi = PropertiesHelper.getInt(INITIAL_HI, params, DEFAULT_INITIAL_HI);
        String schemaName = params.getProperty(SCHEMA);
        String catalogName = params.getProperty(CATALOG);
        keySize = PropertiesHelper.getInt(PK_LENGTH_NAME, params, DEFAULT_PK_LENGTH);
        keyValue = PropertiesHelper.getString(PK_VALUE_NAME, params, params.getProperty(TABLE));

        if (tableName.indexOf('.') < 0) {
            tableName = Table.qualify(catalogName, schemaName, tableName);
        }

        query = "select " + valueColumnName + " from " +
                dialect.appendLockHint(LockMode.UPGRADE, tableName) + " where " + pkColumnName +
                " = '" + keyValue + "'" + dialect.getForUpdateString();

        update = "update " + tableName + " set " + valueColumnName + " = ? where " +
                 valueColumnName + " = ? and " + pkColumnName + " = '" + keyValue + "'";

        insert = "insert into " + tableName + "(" + pkColumnName + ", " + valueColumnName + ") " +
                 "values('" + keyValue + "', ?)";

        // hilo config
        maxLo = PropertiesHelper.getInt(MAX_LO, params, Short.MAX_VALUE);
        lo = maxLo + 1; // so we "clock over" on the first invocation
        returnClass = type.getReturnedClass();

    }
}
