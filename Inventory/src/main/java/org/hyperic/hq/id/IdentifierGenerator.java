package org.hyperic.hq.id;

import java.io.Serializable;

import org.hibernate.HibernateException;
import org.hibernate.dialect.Dialect;
import org.hibernate.engine.SessionImplementor;
import org.hibernate.id.PersistentIdentifierGenerator;

public class IdentifierGenerator implements PersistentIdentifierGenerator {

    public Serializable generate(SessionImplementor session, Object object)
        throws HibernateException {
       return Long.valueOf(object.hashCode());
    }

    public String[] sqlCreateStrings(Dialect dialect) throws HibernateException {
       return new String[0];
    }

    public String[] sqlDropStrings(Dialect dialect) throws HibernateException {
       return new String[0];
    }

    public Object generatorKey() {
      return "CustomIdentifierGenerator";
    }

}
