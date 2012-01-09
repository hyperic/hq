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
package org.hyperic.util.encoding;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

/**
 * Factorization of Scot's code from {@link DbKeystoreManagerImpl}.<br>
 * Collection of utility methods for marshalling & unmarshalling Object to/from
 * a serialized<br>
 * Base64 representation.
 * 
 * @author guy
 * 
 */
public class ObjectCoder {

    /**
     * Unmarshals Object to/from a serialized Base64 representation.
     * <p>
     * <b>Pre-condition:</b>&nbspclsType must have a
     * <code>serialVersionUID</code> implementation.
     * </p>
     * 
     * @param encoded
     *            Base64 Serialized representation of the object to unmarshal.
     * @param clsType
     *            Class to unmarshal to.
     * @return Unmarshaled Object instance.
     * @throws IOException
     * @throws ClassNotFoundException
     */
    @SuppressWarnings("unchecked")
    public static final <T> T decode(final String sEncoded, Class<T> clsType)
            throws IOException, ClassNotFoundException {

        if (sEncoded == null) {
            return null;
        }

        byte[] serialized = Base64.decode(sEncoded);
        ByteArrayInputStream bis = null;
        ObjectInputStream in = null;
        try {
            bis = new ByteArrayInputStream(serialized);
            in = new ObjectInputStream(bis);
            Object rtn = in.readObject();
            return (T) rtn;
        } finally {
            if (in != null)
                in.close();
        }
    }// EOM

    /**
     * Marshals Object to/from a serialized Base64 representation.
     * <p>
     * <b>Pre-condition:</b>&nbspclsType must have a
     * <code>serialVersionUID</code> implementation.
     * </p>
     * 
     * @param alias
     *            Optional identification of the target object - unused at the
     *            moment.
     * @param obj
     *            Object to marshal
     * @throws IOException
     */
    public static final String encode(final String sAlias, final Object obj)
            throws IOException {
        if (obj == null) {
            return null;
        }
        ByteArrayOutputStream bos = null;
        ObjectOutputStream out = null;
        try {
            bos = new ByteArrayOutputStream();
            out = new ObjectOutputStream(bos);
            out.writeObject(obj);
            return new String(Base64.encode(bos.toByteArray()));
        } finally {
            if (out != null)
                out.close();
        }
    }// EOM

}// EOC
