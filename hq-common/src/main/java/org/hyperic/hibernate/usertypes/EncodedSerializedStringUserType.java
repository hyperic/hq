/**
 * NOTE: This copyright does *not* cover user programs that use HQ
 * program services by normal system calls through the application
 * program interfaces provided as part of the Hyperic Plug-in Development
 * Kit or the Hyperic Client Development Kit - this is merely considered
 * normal use of the program, and does *not* fall under the heading of
 *  "derived work".
 *
 *  Copyright (C) [2012], VMware, Inc.
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
package org.hyperic.hibernate.usertypes;

import java.io.Serializable;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;

import org.hibernate.HibernateException;
import org.hibernate.usertype.UserType;
import org.hibernate.util.EqualsHelper;
import org.hyperic.hq.common.SystemException;
import org.hyperic.util.encoding.ObjectCoder;

/**
 * Hibernate custom {@link String} user type responsible for the
 * serialization&encoding of objects into Base64 representation during an
 * update/insert operation and the<br>
 * decoding & deserialization during select operations.
 * 
 * @author guy
 * 
 * @param <T>
 *            Proxied class type
 */
public class EncodedSerializedStringUserType<T> implements UserType {

    private static final int[] m_arrSqlTypes = new int[] { Types.VARCHAR };

    private Class<T> m_clsReturnType;

    public EncodedSerializedStringUserType() {
    }// EOM

    /**
     * 
     * @param clsReturnType
     *            Proxied class type to be used in actual accessors and
     *            mutatros.
     */
    public EncodedSerializedStringUserType(final Class<T> clsReturnType) {
        this.m_clsReturnType = clsReturnType;
    }// EOM

    /**
     * @return int[] { {@link Types#VARCHAR}
     */
    public final int[] sqlTypes() {
        return m_arrSqlTypes;
    }// EOM

    /**
     * @return Value provided by subclasses
     */
    public final Class<T> returnedClass() {
        return this.m_clsReturnType;
    }// EOM

    /**
     * Delegates to the Hibernate EqualsHelper.
     * 
     * @see org.hibernate.util.EqualsHelper#equals
     */
    public final boolean equals(final Object x, final Object y)
            throws HibernateException {
        return EqualsHelper.equals(x, y);
    }// EOM

    /**
     * @return hashcode of the formal arg
     */
    public final int hashCode(Object x) throws HibernateException {
        return x.hashCode();
    }// EOM

    /**
     * Extracts the String from the resultset using the names[0] value, Decodes
     * from Base64 and finally deserializes.
     * 
     * @see ObjectCoder#decode(String, Class)
     * @param rs
     *            live & populated {@link ResultSet}
     * @param names
     *            [0] holds the column name
     * @param owner
     *            the containing entity
     */
    public final Object nullSafeGet(final ResultSet rs, final String[] names,
            final Object owner) throws HibernateException, SQLException {

        // retrieve the string from the resultset and if not null decode and
        // deserialize
        final String sEncodedValue = rs.getString(names[0]);
        Object oResult = null;

        try {
            oResult = ObjectCoder.decode(sEncodedValue, this.m_clsReturnType);
        } catch (Throwable t) {
            throw new SystemException(t);
        }// EO catch block

        return oResult;
    }// EOM

    /**
     * Serializes, and encoded the object to a string representation.
     * 
     * @param st
     *            {@link PreparedStatement} initialized with the insert/update
     *            query
     * @param value
     *            the Object to convert to encoded serialized string
     * @param index
     *            binding param index
     */
    public final void nullSafeSet(final PreparedStatement st,
            final Object value, final int index) throws HibernateException,
            SQLException {

        try {
            // encode the to Base64 andserialize it prior to persisting
            final String sEncodedValue = ObjectCoder.encode(null/* alias */,
                    value);
            // set the encoded value in the statement
            st.setString(index, sEncodedValue);
        } catch (Exception e) {
            throw new SystemException(e);
        }// EO catch block
    }// EOM

    /**
     * Does nothing.
     */
    public final Object deepCopy(final Object value) throws HibernateException {
        return value;
    }// EOM

    /**
     * @return true
     */
    public final boolean isMutable() {
        return true;
    }// EOM

    /**
     * Does nothing.
     */
    public Serializable disassemble(final Object value)
            throws HibernateException {
        return (Serializable) value;
    }// EOM

    /**
     * Does nothing.
     */
    public final Object assemble(final Serializable cached, final Object owner)
            throws HibernateException {
        return cached;
    }// EOM

    /**
     * Does nothing.
     */
    public final Object replace(final Object original, final Object target,
            final Object owner) throws HibernateException {
        return original;
    }// EOM

}// EOC
