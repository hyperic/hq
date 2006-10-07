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
        return new StringBuffer()
            .append("create sequence ")
            .append(sequenceName)
            .append(" start ")
            .append(HypericDialect.SEQUENCE_START)
            .append(" increment 1 cache ")
            .append(HypericDialect.SEQUENCE_CACHE)
            .toString();
    }
}
