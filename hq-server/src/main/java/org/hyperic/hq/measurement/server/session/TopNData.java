package org.hyperic.hq.measurement.server.session;

import java.io.Serializable;
import java.util.Arrays;
import java.util.Date;

public class TopNData implements Serializable {

    private Date time;
    private int resourceId;
    private byte[] data;

    public TopNData() {
    }

    public Date getTime() {
        return time;
    }

    public void setTime(Date time) {
        this.time = time;
    }

    public int getResourceId() {
        return resourceId;
    }

    public void setResourceId(int resourceId) {
        this.resourceId = resourceId;
    }

    public byte[] getData() {
        return data;
    }

    public void setData(byte[] data) {
        this.data = data;
    }


    public TopNData(int resourceId, Date time, byte[] data) {
        this.resourceId = resourceId;
        this.time = time;
        this.data = data;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if ((o == null) || (getClass() != o.getClass())) {
            return false;
        }

        TopNData topNData = (TopNData) o;

        if (resourceId != topNData.resourceId) {
            return false;
        }
        if (!Arrays.equals(data, topNData.data)) {
            return false;
        }
        if (!time.equals(topNData.time)) {
            return false;
        }

        return true;
    }

    @Override
    public int hashCode() {
        int result = time.hashCode();
        result = (31 * result) + resourceId;
        result = (31 * result) + Arrays.hashCode(data);
        return result;
    }


    @Override
    public String toString() {
        return "TopNData{" +
                "time=" + time +
                ", resourceId=" + resourceId +
                '}';
    }

}
