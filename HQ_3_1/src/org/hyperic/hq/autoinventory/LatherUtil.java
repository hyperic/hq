/*
 * NOTE: This copyright does *not* cover user programs that use HQ
 * program services by normal system calls through the application
 * program interfaces provided as part of the Hyperic Plug-in Development
 * Kit or the Hyperic Client Development Kit - this is merely considered
 * normal use of the program, and does *not* fall under the heading of
 * "derived work".
 * 
 * Copyright (C) [2004, 2005, 2006], Hyperic, Inc.
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

package org.hyperic.hq.autoinventory;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

import org.hyperic.hq.bizapp.shared.lather.ScanConfigurationCoreValue;
import org.hyperic.hq.bizapp.shared.lather.ScanStateCoreLatherValue;
import org.hyperic.lather.LatherValue;
import org.hyperic.lather.xcode.LatherXCoder;
import org.hyperic.util.encoding.Base64;

/**
 * Utility methods to encode/decode LatherValue objects
 * for methods that are not invoked via the lather servlet.
 */
//XXX could be useful elsewhere, but only used by autoinventory atm.
public class LatherUtil {

    public static byte[] serialize(LatherValue value)
        throws IOException {

        LatherXCoder xCoder = new LatherXCoder();
        ByteArrayOutputStream bOs = new ByteArrayOutputStream();
        DataOutputStream dOs = new DataOutputStream(bOs);

        try {
            xCoder.encode(value, dOs);
        } catch (IOException e) {
            throw e;
        } catch (Exception e) {
            throw new IOException(e.getMessage());
        }
        return bOs.toByteArray();
    }

    public static String encode(LatherValue value)
        throws IOException {

        return Base64.encode(serialize(value));
    }

    public static LatherValue deserialize(byte[] value, Class resClass)
        throws IOException {

        LatherXCoder xCoder = new LatherXCoder();
        ByteArrayInputStream bIs =
            new ByteArrayInputStream(value);
        DataInputStream dIs = new DataInputStream(bIs);

        try {
            return xCoder.decode(dIs, resClass);
        } catch (IOException e) {
            throw e;
        } catch (Exception e) {
            throw new IOException(e.getMessage());
        }
    }

    public static LatherValue decode(String value, Class resClass)
        throws IOException {

        return deserialize(Base64.decode(value), resClass);
    }

    static byte[] serialize(ScanConfigurationCore core)
        throws AutoinventoryException {
        try {
            ScanConfigurationCoreValue value =
                new ScanConfigurationCoreValue(core);
            return serialize(value);
        } catch (Exception e) {
            String msg =
                "Error writing ScanConfiguration: " + e.getMessage();
            throw new AutoinventoryException(msg, e);
        }
    }
    
    static ScanConfigurationCore deserializeScanConfigurationCore(byte[] data) 
        throws AutoinventoryException {

        ScanConfigurationCoreValue value;
        try {
            value =
                (ScanConfigurationCoreValue)
                   deserialize(data,
                               ScanConfigurationCoreValue.class);
            return value.getCore();
        } catch (Exception e) {
            String msg =
                "Error reading ScanConfiguration: " + e.getMessage();
            throw new AutoinventoryException(msg, e);
        }
    }
    
    static String encode(ScanStateCore core)
        throws AutoinventoryException {

        ScanStateCoreLatherValue value =
            new ScanStateCoreLatherValue(core);

        try {
            return encode(value);
        } catch (IOException e) {
            throw new AutoinventoryException("Error writing ScanState", e);
        }
    }

    static ScanStateCore decodeScanStateCore(String data)
        throws AutoinventoryException {

        ScanStateCoreLatherValue value;

        try {
            value =
                (ScanStateCoreLatherValue)
                    decode(data, ScanStateCoreLatherValue.class);
        } catch (IOException e) {
            throw new AutoinventoryException("Error reading ScanState", e);
        }

        return value.getScanStateCore();
    }
}
