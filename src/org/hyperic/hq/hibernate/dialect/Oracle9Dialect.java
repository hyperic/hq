package org.hyperic.hq.hibernate.dialect;

import java.sql.Types;

/**
 * HQ customized Oracle dialect to (re)define default
 * JDBC sql types to native db column type mapping
 * for backwards compatibility, :(
 */
public class Oracle9Dialect extends org.hibernate.dialect.Oracle9Dialect
{
    public Oracle9Dialect() {
        super();
        registerColumnType( Types.VARBINARY, 2000, "blob" );
    }
}
