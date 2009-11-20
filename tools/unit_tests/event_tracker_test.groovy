import org.hyperic.hq.events.server.session.EventTrackerImpl as eventTrackerImpl
import org.hyperic.hq.measurement.ext.MeasurementEvent
import org.hyperic.hq.product.MetricValue

/**
 * This script tests concurrent insertion/deletion of triggered events. 
 * We are attempting to reproduce the bug seen in HHQ-731 by running 
 * this script in the groovy console.
 */

//SCRIPT VARIABLES ASSIGNED HERE

def triggerId = 100
def numEventsPerInsert = 10
def numEventInserterThreads = 5
def pauseTimeBetweenInserts = 1
def numEventDeleterThreads = 5
def pauseTimeBetweenDeletes = 1
def totalRunTime = 60000


//SCRIPT EXECUTION STARTS HERE

runTest(triggerId, 
        numEventInserterThreads,
        numEventsPerInsert, 
        pauseTimeBetweenInserts,
        numEventDeleterThreads,
        pauseTimeBetweenDeletes, 
        totalRunTime)


//HELPER FUNCTIONS

def runTest(triggerId, 
        	numEventInserterThreads,
            numEventsPerInsert, 
            pauseTimeBetweenInserts, 
            numEventDeleterThreads,
            pauseTimeBetweenDeletes, 
            totalRunTime) {

    def startTime = System.currentTimeMillis()

	def inserterThreads = startInserterThreads(numEventInserterThreads,
		     								   triggerId,
		     								   numEventsPerInsert,
		     								   pauseTimeBetweenInserts)
        
    def deleterThreads = startDeleterThreads(numEventDeleterThreads,
											 triggerId,
											 pauseTimeBetweenDeletes)
    
    def sleepTime = totalRunTime-(System.currentTimeMillis()-startTime)

    if (sleepTime > 0) Thread.sleep(sleepTime)
    
    inserterThreads.each {
        it.shutdown()
    }

    deleterThreads.each {
        it.shutdown()
    }
    
}


def startInserterThreads(numEventInserterThreads,
        			     triggerId,
        			     numEventsPerInsert,
        			     pauseTimeBetweenInserts) {
    
    def inserterThreads = new ArrayList()
    
    for (i in 1..numEventInserterThreads) {
    	def inserterThread = new EventInserter(triggerId, 
				   							   numEventsPerInsert, 
				   							   pauseTimeBetweenInserts)
    	inserterThread.start()
    	inserterThreads.add(inserterThread)
    }
    
    return inserterThreads
}

def startDeleterThreads(numEventDeleterThreads,
        				triggerId,
        				pauseTimeBetweenDeletes) {

    def deleterThreads = new ArrayList()
    
    for (i in 1..numEventDeleterThreads) {
    	def deleterThread = new EventDeleter(triggerId, pauseTimeBetweenDeletes)
    	deleterThread.start()
    	deleterThreads.add(deleterThread)
    }
    
    return deleterThreads
}

// EVENT DELETER CLASS

private class EventDeleter extends Thread {

    private tid
	private volatile boolean running
	private pauseTime

  	
  	def EventDeleter(triggerId, 
		    		 pauseTimeBetweenDeletes) {
                  
    	super("event deleter")
    	setDaemon(true)
    	tid = triggerId
    	pauseTime = pauseTimeBetweenDeletes
  		running = true
  	}
 
  	def shutdown() {
    	running = false
  	}
  	 
  	void run() {
  	    def eventTracker = eventTrackerImpl.one
  	    
    	while (running) {
      		Thread.sleep(pauseTime)
			
      		eventTracker.deleteReference(tid)
    	}  
  	}
 
  	private def getEvents(numEvents) {
  	    def events = new ArrayList(numEvents)
  	    
  	    // we assume the agent time is 60 secs behind the server time 
  	    // or metric reporting is lagging by 60 secs
  	    eventTime = System.currentTimeMillis() - 60000
  	    
  	    for (i in 1..numEvents) {
  	      events.add(new MeasurementEvent(1, new MetricValue(100, eventTime)))  
  	    }
  	    
  	    return events
  	}

}


//EVENT INSERTER CLASS

private class EventInserter extends Thread {

    private tid
	private volatile boolean running
	private numEvents
	private pauseTime

  	
  	def EventInserter(triggerId, 
		    		  numEventsPerInsert, 
		    		  pauseTimeBetweenInserts) {
                  
    	super("event inserter")
    	setDaemon(true)
    	tid = triggerId
    	numEvents = numEventsPerInsert
    	pauseTime = pauseTimeBetweenInserts
  		running = true
  	}
 
  	def shutdown() {
    	running = false
  	}
  	 
  	void run() {
  	    def eventTracker = eventTrackerImpl.one
  	    
  	    try {
  	    	while (running) {
  	      		Thread.sleep(pauseTime)
  				
  	      		def events = getEvents(numEvents)	
  	      		
  	      		events.each {
  	          		eventTracker.addReference(tid, it, 0)      		    
  	      		}
  	    	}    	        
  	    } finally {
  	        eventTracker.deleteReference(tid)
  	    }
  	}
 
  	private def getEvents(numEvents) {
  	    def events = new ArrayList(numEvents)
  	    
  	    // We assume the agent time is 60 secs behind the server time 
  	    // or metric reporting is lagging by 60 secs
  	    // NOTE: I think this is the condition causing HHQ-731
  	    def eventTime = System.currentTimeMillis() - 60000
  	    
  	    for (i in 1..numEvents) {
  	      events.add(new MeasurementEvent(1, new MetricValue(100, eventTime)))  
  	    }
  	    
  	    return events
  	}

}