package org.hyperic.hq.measurement.server.session;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class MeasurementInserterHolder {

    private DataInserter dataInserter;
    private DataInserter availDataInserter;

    @Autowired
    public MeasurementInserterHolder(SynchronousAvailDataInserter synchronousAvailDataInserter,
                                     SynchronousDataInserter synchronousDataInserter) {
        this.availDataInserter = synchronousAvailDataInserter;
        this.dataInserter = synchronousDataInserter;

    }

    public void setAvailDataInserter(DataInserter d) {
        availDataInserter = d;
    }

    public void setDataInserter(DataInserter d) {
        dataInserter = d;
    }

    public DataInserter getAvailDataInserter() {
        return availDataInserter;
    }

    DataInserter getDataInserter() {
        return dataInserter;
    }
}
