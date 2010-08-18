/**
 * NOTE: This copyright does *not* cover user programs that use HQ
 * program services by normal system calls through the application
 * program interfaces provided as part of the Hyperic Plug-in Development
 * Kit or the Hyperic Client Development Kit - this is merely considered
 * normal use of the program, and does *not* fall under the heading of
 *  "derived work".
 *
 *  Copyright (C) [2009-2010], VMware, Inc.
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

package org.hyperic.hq.types;

import java.io.InputStream;
import java.io.OutputStream;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.bind.Unmarshaller;
import javax.xml.bind.helpers.DefaultValidationEventHandler;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

public class XmlUtil {
    private static JAXBContext CTX;
    
    private static Log LOG = LogFactory.getLog(XmlUtil.class);

    static {
        try {
            CTX = JAXBContext.newInstance("org.hyperic.hq.types");
        } catch (JAXBException e) {
            LOG.error("Error initializing context: " + e.getMessage());
        }
    }

    public static <T> T deserialize(Class<T> res, InputStream is)
        throws JAXBException
    {
        Unmarshaller u = CTX.createUnmarshaller();
        u.setEventHandler(new DefaultValidationEventHandler());
        return res.cast(u.unmarshal(is));
    }

    public static void serialize(Object o, OutputStream os, Boolean format)
        throws JAXBException
    {
        Marshaller m = CTX.createMarshaller();
        m.setEventHandler(new DefaultValidationEventHandler());
        m.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, format);
        m.marshal(o, os);
    }
}
