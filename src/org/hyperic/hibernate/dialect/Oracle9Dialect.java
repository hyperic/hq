package org.hyperic.hibernate.dialect;

import java.sql.Types;

/**
 * HQ customized Oracle dialect to (re)define default
 * JDBC sql types to native db column type mapping
 * for backwards compatibility, :(
 */
public class Oracle9Dialect 
    extends org.hibernate.dialect.Oracle9Dialect
{
    public Oracle9Dialect() {
        registerColumnType(Types.VARBINARY, 2000, "blob");
    }

	public String getCreateSequenceString(String sequenceName) {
        return new StringBuffer()
            .append("create sequence ")
            .append(sequenceName)
            .append(" start with ")
            .append(HypericDialect.SEQUENCE_START)
            .append(" increment by 1 cache ")
            .append(HypericDialect.SEQUENCE_CACHE)
            .toString();
    }
}
