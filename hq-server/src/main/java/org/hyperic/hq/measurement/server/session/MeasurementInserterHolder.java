package org.hyperic.hq.measurement.server.session;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.stereotype.Component;

@Component
public class MeasurementInserterHolder implements ApplicationContextAware {

    private ApplicationContext ctx;
    private DataInserter availDataInserter;
    private DataInserter dataInserter;

    @Autowired
    public MeasurementInserterHolder(SynchronousAvailDataInserter synchronousAvailDataInserter) {
        this.availDataInserter = synchronousAvailDataInserter;
    }

    public void setAvailDataInserter(DataInserter d) {
        availDataInserter = d;
    }

    public DataInserter getAvailDataInserter() {
        return availDataInserter;
    }

    public void setDataInserter(DataInserter dataInserter) {
        this.dataInserter = dataInserter;
    }

    DataInserter getDataInserter() {
        if (dataInserter == null) {
            return ctx.getBean(SynchronousDataInserter.class);
        }
        return dataInserter;
    }

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        ctx = applicationContext;
    }

}
