package org.hyperic.hq.hqu.grails.hqugapi;

import java.util.List;

import org.hyperic.hq.appdef.shared.AppdefEntityID;
import org.hyperic.hq.context.Bootstrap;
import org.hyperic.hq.measurement.server.session.Measurement;
import org.hyperic.hq.measurement.shared.MeasurementManager;
import org.hyperic.util.pager.PageControl;

public class MeasurementHQUGApi extends BaseHQUGApi{

    private MeasurementManager measurementMan = Bootstrap.getBean(MeasurementManager.class);

	public MeasurementHQUGApi() {
		super();
	}

	public List<Measurement> findMeasurements(AppdefEntityID id) {
		return measurementMan.findMeasurements(getSubject(), id, null, PageControl.PAGE_ALL);
	}
    
}
