package org.hyperic.hq.hibernate.dialect;

/**
 *
 */
public class PostgreSQLDialect extends org.hibernate.dialect.PostgreSQLDialect
{
    public String getCascadeConstraintsString()
    {
        return " cascade ";
    }

    public boolean dropConstraints()
    {
        return false;
    }
}
