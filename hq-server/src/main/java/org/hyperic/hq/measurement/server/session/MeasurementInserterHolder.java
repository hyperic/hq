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

package org.hyperic.hq.measurement.server.session;

import org.hyperic.hq.context.Bootstrap;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.stereotype.Component;

@Component
public class MeasurementInserterHolder implements ApplicationContextAware {

    private ApplicationContext ctx;
    private DataInserter<DataPoint> availDataInserter;
    private DataInserter<DataPoint> dataInserter;
    private DataInserter<TopNData> topNInserter;

    @Autowired
    public MeasurementInserterHolder(SynchronousAvailDataInserter synchronousAvailDataInserter) {
        this.availDataInserter = synchronousAvailDataInserter;
    }

    public void setAvailDataInserter(DataInserter<DataPoint> d) {
        availDataInserter = d;
    }

    public DataInserter<DataPoint> getAvailDataInserter() {
        return availDataInserter;
    }

    public void setDataInserter(DataInserter<DataPoint> dataInserter) {
        this.dataInserter = dataInserter;
    }

    DataInserter<DataPoint> getDataInserter() {
        if (dataInserter == null) {
            return ctx.getBean(SynchronousDataInserter.class);
        }
        return dataInserter;
    }

    public void setTopNInserter(DataInserter<TopNData> topNInserter) {
        this.topNInserter = topNInserter;
    }

    public DataInserter<TopNData> getTopNInserter() {
        if (topNInserter == null) {
            topNInserter = Bootstrap.getBean(TopNDataInserter.class);
        }
        return topNInserter;
    }

    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        ctx = applicationContext;
    }

}
