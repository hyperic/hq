package org.hyperic.hq.measurement.server.session;

import java.util.List;

import javax.jms.Destination;

public interface IEvaluator {
    public List<Destination> getDestinations();
}
