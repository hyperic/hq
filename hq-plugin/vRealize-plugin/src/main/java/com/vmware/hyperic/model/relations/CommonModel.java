/**
 * 
 */
package com.vmware.hyperic.model.relations;

import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.bind.Unmarshaller;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

@XmlRootElement
@XmlAccessorType(XmlAccessType.FIELD)
public class CommonModel {
    private static Log log = LogFactory.getLog(CommonModel.class);

    @XmlElement(name = "relation")
    protected List<Relation> relations;

    public List<Relation> getRelations() {
        if (relations == null) {
            relations = new ArrayList<Relation>();
        }
        return this.relations;
    }

    /**
     * Convert the model to XML
     * 
     * @param model
     * @param outputStream
     */
    public void marshallAsXml(OutputStream outputStream) {
        try {

            JAXBContext jc = JAXBContext.newInstance(CommonModel.class);

            Marshaller marshaller = jc.createMarshaller();
            marshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, true);
            marshaller.marshal(this, outputStream);
        } catch (JAXBException jaxbEx) {
            throw new RuntimeException(jaxbEx);
        } finally {
            safeClose(outputStream);
        }
    }

    /**
     * Convert XML to model
     * 
     * @param fileName
     * @param inputStream
     */
    public void unmarshallFromXml(InputStream inputStream) {
        try {

            JAXBContext jc = JAXBContext.newInstance(CommonModel.class);

            Unmarshaller unmarshaller = jc.createUnmarshaller();

            CommonModel model = (CommonModel) unmarshaller.unmarshal(inputStream);

            this.getRelations().clear();
            this.getRelations().addAll(model.getRelations());

        } catch (JAXBException jaxbEx) {
            throw new RuntimeException(jaxbEx);
        } finally {
            safeClose(inputStream);
        }
    }

    /**
     * Safely close the stream
     * 
     * @param stream
     */
    private void safeClose(Closeable stream) {
        if (stream != null) {
            try {
                stream.close();
            } catch (IOException e) {
                log.info("Unexpected exception caught: " + e, e);
            }
        }
    }
}
