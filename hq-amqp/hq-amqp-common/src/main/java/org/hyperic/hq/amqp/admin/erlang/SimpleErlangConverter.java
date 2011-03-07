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

import com.ericsson.otp.erlang.*;
import org.hyperic.hq.amqp.admin.ConversionException;

import java.util.ArrayList;

/**
 * @author Helena Edelson
 */
public class SimpleErlangConverter implements ErlangConverter {

    public Object fromErlang(ControlAction action, OtpErlangObject erlangObject) throws ConversionException {
		return convertErlangToBasicType(erlangObject);
	}

    public Object fromErlang(OtpErlangObject erlangObject) throws ConversionException {
		return convertErlangToBasicType(erlangObject);
	}

    public OtpErlangObject toErlang(Object obj) throws ConversionException {
        if (obj instanceof OtpErlangObject) {
            return (OtpErlangObject) obj;
        }
        if (obj instanceof Object[]) {
            Object[] objectsToConvert = (Object[]) obj;
            if (objectsToConvert.length != 0) {
                ArrayList<OtpErlangObject> tempList = new ArrayList<OtpErlangObject>();

                for (Object objectToConvert : objectsToConvert) {
                    OtpErlangObject erlangObject = convertBasicTypeToErlang(objectToConvert);
                    tempList.add(erlangObject);
                }
                OtpErlangObject ia[] = new OtpErlangObject[tempList.size()];
                return new OtpErlangList(tempList.toArray(ia));
            } else {
                return new OtpErlangList();
            }
        } else {
            return convertBasicTypeToErlang(obj);
        }
    }

    protected OtpErlangObject convertBasicTypeToErlang(Object obj) {
        if (obj instanceof byte[]) {
            return new OtpErlangBinary((byte[]) obj);
        } else if (obj instanceof Boolean) {
            return new OtpErlangBoolean((Boolean) obj);
        } else if (obj instanceof Byte) {
            return new OtpErlangByte((Byte) obj);
        } else if (obj instanceof Character) {
            return new OtpErlangChar((Character) obj);
        } else if (obj instanceof Double) {
            return new OtpErlangDouble((Double) obj);
        } else if (obj instanceof Float) {
            return new OtpErlangFloat((Float) obj);
        } else if (obj instanceof Integer) {
            return new OtpErlangInt((Integer) obj);
        } else if (obj instanceof Long) {
            return new OtpErlangLong((Long) obj);
        } else if (obj instanceof Short) {
            return new OtpErlangShort((Short) obj);
        } else if (obj instanceof String) {
            return new OtpErlangString((String) obj);
        } else {
            throw new ConversionException(
                    "Could not convert Java object of type [" + obj.getClass()
                            + "] to an Erlang data type.");
        }
    }

    protected Object convertErlangToBasicType(OtpErlangObject erlangObject) {
        try {
            if (erlangObject instanceof OtpErlangBinary) {
                return ((OtpErlangBinary) erlangObject).binaryValue();
            } else if (erlangObject instanceof OtpErlangAtom) {
                return ((OtpErlangAtom) erlangObject).atomValue();
            } else if (erlangObject instanceof OtpErlangBinary) {
                return ((OtpErlangBinary) erlangObject).binaryValue();
            } else if (erlangObject instanceof OtpErlangBoolean) {
                return extractBoolean(erlangObject);
            } else if (erlangObject instanceof OtpErlangByte) {
                return ((OtpErlangByte) erlangObject).byteValue();
            } else if (erlangObject instanceof OtpErlangChar) {
                return ((OtpErlangChar) erlangObject).charValue();
            } else if (erlangObject instanceof OtpErlangDouble) {
                return ((OtpErlangDouble) erlangObject).doubleValue();
            } else if (erlangObject instanceof OtpErlangFloat) {
                return ((OtpErlangFloat) erlangObject).floatValue();
            } else if (erlangObject instanceof OtpErlangInt) {
                return ((OtpErlangInt) erlangObject).intValue();
            } else if (erlangObject instanceof OtpErlangLong) {
                return ((OtpErlangLong) erlangObject).longValue();
            } else if (erlangObject instanceof OtpErlangShort) {
                return ((OtpErlangShort) erlangObject).shortValue();
            } else if (erlangObject instanceof OtpErlangString) {
                return ((OtpErlangString) erlangObject).stringValue();
            } else if (erlangObject instanceof OtpErlangPid) {
                return ((OtpErlangPid) erlangObject).toString();
            } else {
                throw new ConversionException(
                        "Could not convert Erlang object ["
                                + erlangObject.getClass() + "] to Java type.");
            }
        } catch (OtpErlangRangeException e) {
            throw new ConversionException(
                    "Could not convert Erlang object ["
                            + erlangObject.getClass() + "] to Java type.", e);
        }
    }

    public static boolean extractBoolean(OtpErlangObject erlangObject) {
        return ((OtpErlangBoolean) erlangObject).booleanValue();
    }

    public static String extractPid(OtpErlangObject value) {
        return ((OtpErlangPid) value).toString();
    }

    public static long extractLong(OtpErlangObject value) {
        return ((OtpErlangLong) value).longValue();
    }

}
