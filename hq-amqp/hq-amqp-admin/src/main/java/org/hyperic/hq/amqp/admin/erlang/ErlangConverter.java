/*
 * NOTE: This copyright does *not* cover user programs that use HQ
 * program services by normal system calls through the application
 * program interfaces provided as part of the Hyperic Plug-in Development
 * Kit or the Hyperic Client Development Kit - this is merely considered
 * normal use of the program, and does *not* fall under the heading of
 * "derived work".
 *
 * Copyright (C) [2009-2010], VMware, Inc.
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

package org.hyperic.hq.amqp.admin.erlang;

import com.ericsson.otp.erlang.OtpErlangObject;
import org.hyperic.hq.amqp.admin.ConversionException;

/**
 * @author Helena Edelson
 */
public interface ErlangConverter {

    /**
     * Convert a Java object to a Erlang data type.
     * @param object the object to convert
     * @return the Erlang data type
     * @throws org.hyperic.hq.amqp.admin.ConversionException
     *          in case of conversion failure
     */
    OtpErlangObject toErlang(Object object) throws ConversionException;

    /**
     * Convert from a Erlang data type to a Java object.
     * @param erlangObject the Elang object to convert
     * @return the converted Java object
     * @throws org.hyperic.hq.amqp.admin.ConversionException
     *          in case of conversion failure
     */
    Object fromErlang(OtpErlangObject erlangObject) throws ConversionException;
    
}
