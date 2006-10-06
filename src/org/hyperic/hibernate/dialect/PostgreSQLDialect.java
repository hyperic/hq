package org.hyperic.hibernate.dialect;

public class PostgreSQLDialect 
    extends org.hibernate.dialect.PostgreSQLDialect
{
    public String getCascadeConstraintsString() {
        return " cascade ";
    }

    public boolean dropConstraints() {
        return false;
    }

	public String getCreateSequenceString(String sequenceName) {
        return "create sequence " + sequenceName + " start " + 
            HypericDialect.SEQUENCE_START + 
           " increment 1 cache " + HypericDialect.SEQUENCE_CACHE;
    }
}
