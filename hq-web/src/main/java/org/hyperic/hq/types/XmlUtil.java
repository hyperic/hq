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
