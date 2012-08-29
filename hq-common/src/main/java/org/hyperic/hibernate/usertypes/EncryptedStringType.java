/*
 * NOTE: This copyright does *not* cover user programs that use HQ
 * program services by normal system calls through the application
 * program interfaces provided as part of the Hyperic Plug-in Development
 * Kit or the Hyperic Client Development Kit - this is merely considered
 * normal use of the program, and does *not* fall under the heading of
 * "derived work".
 * 
 * Copyright (C) [2004-2012], VMWare, Inc.
 * This file is part of HQ.
 * 
 * HQ is free software; you can redistribute it and/or modify
 * it under the terms version 2 of the GNU General Public License as
 * published by the Free Software Foundation. This program is distributed
 * in the hope that it will be useful, but WITHOUT ANY WARRANTY; without
 * even the implied warranty of MERCHANTABILITY or FITNESS FOR A
 * PARTICULAR PURPOSE. See the GNU General Public License for more
 * details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307
 * USA.
 */
package org.hyperic.hibernate.usertypes;

import java.io.Serializable;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Properties;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hibernate.Hibernate;
import org.hibernate.HibernateException;
import org.hibernate.usertype.ParameterizedType;
import org.hibernate.usertype.UserType;
import org.hyperic.util.security.SecurityUtil;
import org.jasypt.encryption.pbe.PBEStringEncryptor;
import org.jasypt.exceptions.EncryptionOperationNotPossibleException;
import org.jasypt.hibernate.type.AbstractEncryptedAsStringType;

/**
 * One way Encryptor custom type. 
 * Returns a {@link LazyDecryptableValue} proxy which would perform the decryption on demand. 
 */
public class EncryptedStringType implements UserType, ParameterizedType {

    private static final Log log = LogFactory.getLog(EncryptedStringType.class);
    private static final int nullableSqlType = Hibernate.STRING.sqlType() ; 
    protected PBEStringEncryptor encryptor ; 
    
    private DelegateAbstractEncryptedAsStringType delegate  ; 
    
    public EncryptedStringType() {
        this.delegate = new DelegateAbstractEncryptedAsStringType() ;         
    }//EOM 
    
    /**
     * Used to expose the {@link AbstractEncryptedAsStringType#checkInitialization()} as well as to<br> 
     * pass its internal encryptor, once initialized to the outer class
     * 
     * Note: Cannot simply subclass as the {@link AbstractEncryptedAsStringType#nullSafeGet(ResultSet, String[], Object)} 
     * and {@link AbstractEncryptedAsStringType#nullSafeSet(PreparedStatement, Object, int)} are final  
     */
    private final class DelegateAbstractEncryptedAsStringType extends AbstractEncryptedAsStringType{ 
        
        @Override
        protected final Object convertToObject(String string) { 
            return null;
        }//EOM 

        @Override
        public final Class returnedClass() {
            return null;
        }//EOM  

        /**
         * Instantiates the internal encryptor and assign to the outer class member. 
         */
        public final void lazyInit() {
            if(EncryptedStringType.this.encryptor == null) { 
                this.checkInitialization() ; 
                EncryptedStringType.this.encryptor = this.encryptor ;
            }//EO if not yet initialized 
        }//EOM 
        
    }
    
    public final void setParameterValues(final Properties parameters) {
        this.delegate.setParameterValues(parameters) ; 
    }//EOM 
    
    public Object deepCopy(Object value) throws HibernateException {
        return this.delegate.deepCopy(value) ; 
    }//EOM 
    
    public final Object assemble(final Serializable cached, final Object owner) throws HibernateException {
        return this.delegate.assemble(cached, owner)  ; 
    }//EOM
    
    public int hashCode(Object x) throws HibernateException {
        return this.delegate.hashCode(x) ;  
    }//EOM 
    
    public final Serializable disassemble(final Object value) throws HibernateException {
        return this.delegate.disassemble(value) ; 
    }//EOM 
    
    protected final Object convertToObject(final String value) {
        return new LazyDecryptableValue(this.encryptor, value) ; 
    }//EOM
    
    /**
     * @param object @{link {@link String} or {@link LazyDecryptableValue} instances (but not limited to)  
     * @return String representation
     */
    protected final String convertToString(final Object object) {
        if(object == null) return null ; 
        else if (object instanceof LazyDecryptableValue) { 
            return (object == null ? null : ((LazyDecryptableValue)object).get()) ;
        }else 
            return object.toString() ; 
    }//EOM 
    
    /**
     * @return {@link LazyDecryptableValue} class 
     */
    public Class<LazyDecryptableValue> returnedClass() {
        return LazyDecryptableValue.class ; 
    }//EOM 
    
    /**
     * Converts the encrypted columnar value to a new {@link LazyDecryptableValue} instance. 
     * <b>Note:</b> Decryption would be deferred to the first {@link LazyDecryptableValue#get()} 
     * @return {@link LazyDecryptableValue} instance containing the encrypted columnar value
     */
    public final Object nullSafeGet(final ResultSet rs, final String[] names, final Object owner)
            throws HibernateException, SQLException {
        this.delegate.lazyInit() ; 
        final String value = rs.getString(names[0]);
        return rs.wasNull() ? null : convertToObject(value);
    }//EOM 
    
    public final int[] sqlTypes() {
        return this.delegate.sqlTypes() ; 
    }//EOM 

    public final boolean equals(Object x, Object y) throws HibernateException {
        return this.delegate.equals(x, y) ; 
    }//EOM 

    public final boolean isMutable() {
        return this.delegate.isMutable() ; 
    }//EOM 

    /**
     * Extracts the unencrypted value from the {@link LazyDecryptableValue} instance and ensures its encrypted prior to<br>  
     * binding it to the resultset parameter 
     */
    public final void nullSafeSet(final PreparedStatement st, final Object value, final int index) throws HibernateException, SQLException {
        this.delegate.lazyInit() ; 
        
        if (value == null) {
            st.setNull(index, nullableSqlType); 
        } else { 
            String strValue = this.convertToString(value) ; 
            
            if(!SecurityUtil.isMarkedEncrypted(strValue)) { 
                // [HHQ-5592] MAYA temporarily save the cleartext value
                // for debugging purposes only
                String originalValue = strValue;
                if (log.isDebugEnabled()) {
                    log.debug("value before encryption=" + originalValue);
                }
                strValue = this.encryptor.encrypt(strValue) ; 

                if (log.isDebugEnabled()) {
                    log.debug("value after encryption=" + strValue);
                }                
                // MAYA Note: this might throw an exception,
                // if encryption was defective
                try {
                    String decryptedValue = this.encryptor.decrypt(strValue);
                    if (!originalValue.equals(decryptedValue)) {
                        StringBuilder logMessageBuilder = new StringBuilder("original value={");
                        logMessageBuilder.append(originalValue).append("} differs from the decrypted value={")
                                .append(decryptedValue).append("}");
                        log.error(logMessageBuilder.toString());
                    } // EO if original and decrypted values differ
                } catch (EncryptionOperationNotPossibleException e) {
                    log.warn("could not decrypt value=" + value);
                    throw e;
                } // EO catch  EncryptionOperationNotPossibleException           
                
            }//EO if value was not already encrypted 
             
            st.setString(index, strValue) ; 
        }//EO else if value was not null 
    }//EOM 

    public final Object replace(final Object original, final Object target, final Object owner) throws HibernateException {
        return original ; 
    }//EOM 
    
    /**
     * Immutable container for encrypted values used to defer their decryption to to first retrieval request.<br> 
     *  
     */
    public static final class LazyDecryptableValue implements Serializable { 
        
        private static final long serialVersionUID = -8158319101596665352L;
        
        private String value ; 
        private boolean isDecrypted ; 
        private PBEStringEncryptor encryptor ;     
        
        /**
         * Used by client code to create new encrypted values 
         * @param clearTextValue Whose encryption would be deferred to the flush opetaion  
         */
        public LazyDecryptableValue(final String clearTextValue) {
            this.value = clearTextValue ;
            this.isDecrypted = true ; 
        }//EOM
        

        /**
         * Used by hibernate during object hydration. 
         * @param encryptor {@link PBEStringEncryptor} delegate with which to decrypt the value. 
         * @param encryptedValue 
         */
        public LazyDecryptableValue(final PBEStringEncryptor encryptor, final String encryptedValue) { 
            this.encryptor = encryptor ; 
            this.value = encryptedValue ;
        }//EOM 
        
        /**
         * @return The decrypted value of the original encrypted {@link #value} member's value.
         * <b>Note:</b> decrypted value is cached on first decryption for subsequent get requests.   
         */
        public final String get() { 
            final boolean debug = log.isDebugEnabled();
            try {
                if(!this.isDecrypted) {
                    this.isDecrypted = true ; 
                    if(SecurityUtil.isMarkedEncrypted(value)) {  
                        if (debug) {
                            log.debug("value before decryption=" + value);
                        }
                        this.value = this.encryptor.decrypt(this.value) ; 
                        if (debug) {
                            log.debug("value after decryption=" + this.value);
                        }                    
                    } else {
                        if (debug) {
                            log.debug("LazyDecryptableValue is supposed to be encrypted, but is not marked encrypted=" +
                                      this.value);
                        }
                    }//EO if not clear text as it is 
                }//EO else if not yet decrypted 
                return this.value ; 
            } catch (EncryptionOperationNotPossibleException e) {
                log.warn("could not decrypt value=" + value);
                throw e;
            }
        }//EOM
        
        @Override
        public final int hashCode() {
            final int prime = 31;
            int result = 1;
            result = prime * result + (isDecrypted ? 1231 : 1237);
            result = prime * result + ((value == null) ? 0 : value.hashCode());
            return result;
        }//EOM 
        
        @Override
        public final boolean equals(final Object obj) {
            if (this == obj) return true;
            if (obj == null) return false;
            if (getClass() != obj.getClass()) return false;
            final LazyDecryptableValue other = (LazyDecryptableValue) obj;
            if (isDecrypted != other.isDecrypted)
                return false;
            if (value == null) {
                if (other.value != null)
                    return false;
            } else if (!value.equals(other.value))
                return false;
            return true;
        }//EOM 
        
        /**
         * @param original
         * @param clearTextValue
         * @return new {@link LazyDecryptableValue} instance if the original's value is not the same as the clearTextValue or the<br>
         * original if the values are the same.
         * <b>Note:</b> the new reference is an indication to hibernate that the field is dirty and should be persisted.  
         */
        public static final LazyDecryptableValue set(final LazyDecryptableValue original, final String clearTextValue) {
            boolean isEqual = true ; 
             
            if(original == null) isEqual = false ; 
            else if (original.value == null) {
                if (clearTextValue != null) isEqual = false;
            } else if (!original.value.equals(clearTextValue)) isEqual = false;
            
            return (isEqual ? original : new LazyDecryptableValue(clearTextValue)) ; 
        }//EOM
        
    }//EO inner class  

}//EOC 
