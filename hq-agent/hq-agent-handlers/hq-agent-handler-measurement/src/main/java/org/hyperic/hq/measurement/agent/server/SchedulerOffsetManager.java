package org.hyperic.hq.measurement.agent.server;

import java.util.Random;

import org.hyperic.hq.agent.server.AgentStorageProvider;
import org.hyperic.util.TimeUtil;

public class SchedulerOffsetManager {

    private static final String PROP_SCHEDULER_OFFSET_SEED = "agent.scheduler.offset.seed";

    private long offsetSeed;
    
    public SchedulerOffsetManager(AgentStorageProvider storage) {
	offsetSeed = getSchedulerOffsetSeed(storage);
    }
    
    private long getSchedulerOffsetSeed(AgentStorageProvider storage) {
	String offsetSeedString = storage.getValue(PROP_SCHEDULER_OFFSET_SEED);
	long offsetSeed;
	if (offsetSeedString != null) {
	    offsetSeed = Long.parseLong(offsetSeedString);
	} else {
	    Random rand = new Random();
	    offsetSeed = Math.abs(rand.nextLong());
	    storage.setValue(PROP_SCHEDULER_OFFSET_SEED, Long.toString(offsetSeed));
	}
	return offsetSeed;
    }
    
    public long getSchedluerOffsetForInterval(long interval) {
	long offset = offsetSeed % interval;
	// Round offset to the nearest minute
	offset -= (offset % TimeUtil.MILLIS_IN_MINUTE);
	return offset;
    }
    
}
