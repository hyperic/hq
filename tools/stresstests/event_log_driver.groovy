import org.hyperic.hq.product.TrackEvent
import org.hyperic.hq.appdef.shared.AppdefEntityID
import org.hyperic.hq.measurement.shared.ResourceLogEvent
import org.hyperic.hq.common.util.Messenger
import org.hyperic.hq.events.EventConstants
import org.hyperic.hq.product.LogTrackPlugin

/**
 * This script publishes resource log events for a given set of 
 * resources identified by their AppdefEntityIDs. Log Event Inserter 
 * threads are created to publish the log events in parallel.
 */
 
// SCRIPT VARIABLES ASSIGNED HERE

def appDefEntityIds = [new AppdefEntityID(3, 12448), 
                       new AppdefEntityID(3, 12449), 
                       new AppdefEntityID(3, 12450)]  // the list of AppdefEntityIDs

def numLogEventsPerInserter = 200       // number of resource log events inserted per thread

def pauseTimeBetweenInserts = 1000      // per thread pause time between each log event insert (msec)

def pauseBetweenInserterStarts = 2000   // pause time between starting each log event inserter thread (msec)

def totalRunTime = 60000                // total test run time (msec)


// SCRIPT EXECUTION STARTS HERE

runTest(appDefEntityIds, 
    	numLogEventsPerInserter, 
        pauseTimeBetweenInserts, 
        pauseBetweenInserterStarts, 
        totalRunTime)


//HELPER FUNCTIONS

def runTest(appDefEntityIds, 
        	numLogEventsPerInserter, 
            pauseTimeBetweenInserts, 
            pauseBetweenInserterStarts, 
            totalRunTime) {

    def startTime = System.currentTimeMillis()

    def inserterThreads = startLogEventInserters(appDefEntityIds, 
										         numLogEventsPerInserter, 
										         pauseTimeBetweenInserts, 
										         pauseBetweenInserterStarts)

    def sleepTime = totalRunTime-(System.currentTimeMillis()-startTime)

    if (sleepTime > 0) Thread.sleep(sleepTime)

    inserterThreads.each {
      it.shutdown()  
    }

    inserterThreads.each {
      while (it.isAlive())  {
        it.join(10000)
      }
    }

    inserterThreads.each {
      println(it.toString()+" thread is still active ="+it.isAlive()+
              ", total num log events inserted="+it.getNumLogEventsInserted())
    }
    
}

def startLogEventInserters(appDefEntityIds, 
        				   numLogEventsPerInserter, 
        			       pauseTimeBetweenInserts, 
        			       pauseBetweenInserterStarts) {

	def inserterThreads = new ArrayList(appDefEntityIds.size())

	for (appDefEntityId in appDefEntityIds) {
		def logEventInserter = startLogEventInserterThread(appDefEntityId,
						                     			   numLogEventsPerInserter,
						                                   pauseTimeBetweenInserts)
		inserterThreads.add(logEventInserter)
		Thread.sleep(pauseBetweenInserterStarts)
	}

	return inserterThreads
}

def startLogEventInserterThread(appDefEntityId,
		   						numLogEventsPerInsert,
		   						pauseTimeBetweenInserts) {
 
	def logEventInserter = new LogEventInserter(appDefEntityId, 
	        								    numLogEventsPerInsert, 
	        								    pauseTimeBetweenInserts)
	logEventInserter.start()
	return logEventInserter
}

//LOG EVENT INSERTER CLASS

private class LogEventInserter extends Thread {

	private volatile boolean running
  	private volatile int totalNumLogEventsInserted
	private appDefEntityId
	private numLogEvents
	private pauseTime

  	
  	def LogEventInserter(appDefEntityId, 
		    			 numLogEventsPerInsert, 
		    			 pauseTimeBetweenInserts) {
                  
    	super("log event inserter "+appDefEntityId)
    	setDaemon(true)
    	this.appDefEntityId = appDefEntityId
    	numLogEvents = numLogEventsPerInsert
    	pauseTime = pauseTimeBetweenInserts
  		running = true
  	}
 
  	def shutdown() {
    	running = false
  	}
  	
  	def getNumLogEventsInserted() {
  		return totalNumLogEventsInserted
  	}
 
  	void run() {   
    	def random = new Random()

    	while (running) {
      		Thread.sleep(pauseTime)
      
      		def logEvents = getLogEventsByResource(appDefEntityId, 
      		        							   numLogEvents, 
      		        							   random)

      		// println(name+" "+logEvents)

      		def sender = new Messenger();
      		sender.publishMessage(EventConstants.EVENTS_TOPIC, logEvents);
      		
      		totalNumLogEventsInserted += logEvents.size()
    	}  
  	}
 
  	def getLogEventsByResource(appDefEntityId, numLogEvents, random) {
      	def logEvents = new ArrayList(numLogEvents)
      	def time = System.currentTimeMillis()
      	def level = LogTrackPlugin.LOGLEVEL_DEBUG
      	def source = "dummy.log"
      	def snippet = "This is a message with a random number: "+random.nextInt()
      	def message = new StringBuffer()
      
      	for (i in 1..random.nextInt(5)) {
          	message.append(snippet).append(' ')
      	}
      
      	for (i in 1..numLogEvents) {
      		def trackEvent = new TrackEvent(appDefEntityId, 
      	        							time, 
      	        							level, 
      	        							source, 
      	        							message.toString())
      		logEvents.add(new ResourceLogEvent(trackEvent))
      	}
      
      	return logEvents
  	}

}  