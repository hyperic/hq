package org.hyperic.hq.galerts.strategies;

import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hyperic.hq.galerts.processor.FireReason;
import org.hyperic.hq.galerts.processor.Gtrigger;
import org.hyperic.hq.galerts.server.session.ExecutionReason;
import org.hyperic.hq.galerts.server.session.ExecutionStrategy;
import org.hyperic.hq.galerts.server.session.GalertDefPartition;


public class SimpleStrategy 
    implements ExecutionStrategy
{
    private static final Log _log = LogFactory.getLog(SimpleStrategy.class);

    private GalertDefPartition _partition;
    private FireReason         _lastReason;
    
    public void configure(GalertDefPartition partition, String defName, 
                          List triggers) 
    {
        _partition = partition;
        _log.warn("Configure called: partition=" + partition + 
                  " defName=" + defName + " triggers=" + triggers);
    }

    public void reset() {
        _lastReason = null;
    }

    public ExecutionReason shouldFire() {
        if (_lastReason != null)
            return new ExecutionReason(_lastReason.getShortReason(),
                                       _lastReason.getLongReason(),
                                       _partition);
        return null;
    }

    public void triggerFired(Gtrigger trigger, FireReason reason) {
        _lastReason = reason;
    }

    public void triggerNotFired(Gtrigger trigger) {
        _lastReason = null;
    }
}
