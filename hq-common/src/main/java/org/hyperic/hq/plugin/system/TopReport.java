package org.hyperic.hq.plugin.system;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.Serializable;

import org.hyperic.util.encoding.Base64;

public class TopReport implements Serializable {

    private static final long serialVersionUID = 1L;
    private long creatTime;
    private String xmlData;

    public long getCreatTime() {
        return creatTime;
    }

    public void setCreatTime(long creatTime) {
        this.creatTime = creatTime;
    }

    public String getXmlData() {
        return xmlData;
    }

    public void setXmlData(String xmlData) {
        this.xmlData = xmlData;
    }

    public String encode() throws IOException {

        ByteArrayOutputStream bOs = null;
        DataOutputStream dOs = null;

        try {
            bOs = new ByteArrayOutputStream();
            dOs = new DataOutputStream(bOs);
            dOs.writeUTF(getXmlData());
            dOs.writeLong(getCreatTime());
            return Base64.encode(bOs.toByteArray());

        } finally {
            dOs.close();
            bOs.close();
        }
    }

    public static TopReport decode(String val) throws IOException {

        ByteArrayInputStream bIs = null;
        DataInputStream dIs = null;
        TopReport report = new TopReport();
        try {
            bIs = new ByteArrayInputStream(Base64.decode(val));
            dIs = new DataInputStream(bIs);
            report.xmlData = dIs.readUTF();
            report.creatTime = dIs.readLong();
            return report;

        } finally {
            dIs.close();
            bIs.close();
        }
    }

}
