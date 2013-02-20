package org.hyperic.hq.notifications.model;

import java.io.Serializable;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;

import org.hibernate.HibernateException;
import org.hibernate.usertype.UserType;

public enum InternalResourceDetailsType {
  //TODO~ page 354 on the hibernate course PDF
    BASIC,
    PROPERTIES, 
    VIRTUALDATA,
    ALL;

//    public static class InternalResourceDetailsTypeUserType implements UserType {
//        //TODO~ page 352 on the hibernate course PDF
//        protected final static int[] sqlTypes = new int[] {Types.VARCHAR};
//
//        public int[] sqlTypes() {
//            return sqlTypes;
//        }
//
//        public Class<InternalResourceDetailsType> returnedClass() {
//            return InternalResourceDetailsType.class;
//        }
//
//        public boolean equals(Object x, Object y) throws HibernateException {
//            return x==null?y==null:x.equals(y);
//        }
//
//        public int hashCode(Object x) throws HibernateException {
//            return x.hashCode();
//        }
//
//        public Object nullSafeGet(ResultSet rs, String[] names, Object owner) throws HibernateException, SQLException {
//            return ;
//        }
//
//        public void nullSafeSet(PreparedStatement st, Object value, int index) throws HibernateException, SQLException {
//        }
//
//        public Object deepCopy(Object value) throws HibernateException {
//            return ;
//        }
//
//        public boolean isMutable() {
//            return ;
//        }
//
//        public Serializable disassemble(Object value) throws HibernateExcepnalResourceDetailsTypeUserType implements UserType {
//  //TODO~ page 352 on the hibernate course PDF
//  protected final static int[] sqlTypes = new int[] {Types.VARCHAR};
//
//  public int[] sqlTypes() {
//      return sqlTypes;
//  }
//
//  public Class<InternalResourceDetailsType> returnedClass() {
//      return InternalResourceDetailsType.class;
//  }
//
//  public boolean equals(Object x, Object y) throws HibernateException {
//      return x==null?y==null:x.equals(y);
//  }
//
//  public int hashCode(Object x) throws HibernateException {
//      return x.hashCode();
//  }
//
//  public Object nullSafeGet(ResultSet rs, String[] names, Object owner) throws HibernateException, SQLException {
//      return ;
//  }
//
//  public void nullSafeSet(PreparedStatement st, Object value, int index) throws HibernateException, SQLException {
//  }
//
//  public Object deepCopy(Object value) throws HibernateException {
//      return ;
//  }
//
//  public boolean isMutable() {
//      return ;
//  }
//
//  public Serializable disassemble(Object value) throws HibernateException {
//      return ;
//  }
//
//  public Object assemble(Serializable cached, Object owner) throws HibernateException {
//      return ;
//  }
//
//  public Object replace(Object original, Object target, Object owner) throws HibernateException {
//      return ;
//  } 
//}tion {
//            return ;
//        }
//
//        public Object assemble(Serializable cached, Object owner) throws HibernateException {
//            return ;
//        }
//
//        public Object replace(Object original, Object target, Object owner) throws HibernateException {
//            return ;
//        } 
//    }
}
