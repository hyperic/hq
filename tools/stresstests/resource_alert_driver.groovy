import org.hyperic.hq.common.util.Messenger
import org.hyperic.hq.events.EventConstants
import org.hyperic.hq.events.AbstractEvent
import org.hyperic.hq.events.ext.AbstractTrigger
import org.hyperic.hq.measurement.ext.MeasurementEvent
import org.hyperic.hq.product.MetricValue
import org.hyperic.hq.events.server.session.AlertDefinitionManagerEJBImpl as adM
import org.hyperic.hq.events.server.session.RegisteredTriggerManagerEJBImpl as rtM

/**
 * This script publishes events that trigger resource alert definitions. The 
 * user must specify the strategy for generating events for each alert definition 
 * in question using an event generator closure. If the alert definition 
 * corresponds to a resource type alert, the script will find all the child 
 * resource alert definitions and use the generation strategy specified for 
 * the resource type alert definition on all its child resource type alert
 * definitions. Alert Event Inserter threads are created to publish the 
 * events in parallel. One thread will be created per resource type alert 
 * definition.
 */


//SCRIPT VARIABLES ASSIGNED HERE


/**
 * The closure passed to each resource alert event inserter used for generating 
 * events.
 *
 * @param abstractTriggers The list of AbstractTrigger objects associated with 
 *                         the alert definition.
 * @param random A source of randomness.
 * @return The list of AbstractEvent objects that may (or may not) trigger 
 *         the alert definition.
 */
 def eventGenerator1 = {List abstractTriggers, Random random -> 
 							def trigger = abstractTriggers.get(0)
 							Integer[] metricId = trigger.getInterestedInstanceIDs(null)
 							return [new MeasurementEvent(metricId[0], new MetricValue(100))]
 					    }

// map of alert definition Ids to event generator closures
def alertDefId2EventGenerator = [10005 : eventGenerator1]

def numAlertEventsPerInserter = 10      // number of resource alert events inserted per thread

def pauseTimeBetweenInserts = 1000      // per thread pause time between each alert event insert (msec)

def pauseBetweenInserterStarts = 2000   // pause time between starting each resource alert event inserter thread (msec)

def totalRunTime = 1000                 // total test run time (msec)


// SCRIPT EXECUTION STARTS HERE

runTest(alertDefId2EventGenerator, 
        numAlertEventsPerInserter, 
        pauseTimeBetweenInserts, 
        pauseBetweenInserterStarts, 
        totalRunTime)


//HELPER FUNCTIONS

def runTest(alertDefId2EventGenerator, 
            numAlertEventsPerInserter, 
            pauseTimeBetweenInserts, 
            pauseBetweenInserterStarts, 
            totalRunTime) {

    def startTime = System.currentTimeMillis()

    def inserterThreads = startResourceAlertInserters(alertDefId2EventGenerator, 
		    										  numAlertEventsPerInserter, 
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
    
    def totalNumInserted = 0

    inserterThreads.each {
      totalNumInserted+=it.getNumAlertEventsInserted() 
      println(it.toString()+" thread is still active ="+it.isAlive()+
              ", total num events inserted="+it.getNumAlertEventsInserted())
    }
    
    println("Resource alert inserters ran for "+
            (System.currentTimeMillis()-startTime)+
            " msec, inserting "+totalNumInserted+ " events")
    
}

def startResourceAlertInserters(alertDefId2EventGenerator, 
        					    numAlertEventsPerInserter, 
        			            pauseTimeBetweenInserts, 
        			            pauseBetweenInserterStarts) {

	def inserterThreads = new ArrayList(alertDefId2EventGenerator.size())

	for (alertDefEntry in alertDefId2EventGenerator) {
		def resourceAlertInserter = 
		    startResourceAlertInserterThread(alertDefEntry.key,
		        							 alertDefEntry.value,
		        							 numAlertEventsPerInserter,
						                     pauseTimeBetweenInserts)
		inserterThreads.add(resourceAlertInserter)
		Thread.sleep(pauseBetweenInserterStarts)
	}

	return inserterThreads
}

def startResourceAlertInserterThread(alertDefId,
        						     eventGenerator,
        						     numAlertEventsPerInsert,
		   						     pauseTimeBetweenInserts) {
 
	def resourceAlertInserter = 
	    new ResourceAlertEventInserter(alertDefId, 
	        						   eventGenerator,
	        						   numAlertEventsPerInsert, 
	        			               pauseTimeBetweenInserts)
	resourceAlertInserter.start()
	return resourceAlertInserter
}

//RESOURCE ALERT EVENT INSERTER CLASS

private class ResourceAlertEventInserter extends Thread {

	private volatile boolean running
  	private volatile int totalNumAlertEventsInserted
	private alertDefId
	private numAlertEvents
	private generator
	private pauseTime

  	
  	def ResourceAlertEventInserter(alertDefId, 
	 		   					   eventGenerator,
		    			 		   numAlertEventsPerInsert,
		    			 		   pauseTimeBetweenInserts) {
                  
    	super("resource alert event inserter, alert def id="+alertDefId)
    	setDaemon(true)
    	this.alertDefId = alertDefId
    	numAlertEvents = numAlertEventsPerInsert
    	generator = eventGenerator
    	pauseTime = pauseTimeBetweenInserts
  		running = true
  	}
 
  	def shutdown() {
    	running = false
  	}
  	
  	def getNumAlertEventsInserted() {
  		return totalNumAlertEventsInserted
  	}
  	 
  	void run() {   
    	def random = new Random()
    	def allTriggers = getTriggers()

    	while (running) {
      		Thread.sleep(pauseTime)
      
      		def alertEvents = getAlertEvents(numAlertEvents, allTriggers, random)

      		// println(name+" "+alertEvents)
			
      		publishAlertEvents(alertEvents, random)
    	}  
  	}
  	
  	private def publishAlertEvents(alertEvents, random) {
  		// The events should be batched randomly.
  		// Sending all the events in a single batch will be most 
  		// efficient, but we want to mix it up a little to simulate 
  		// real world situations (and stress the system more).
  		// Let's send events in 1 to 4 batches randomly.
		int numBatches = random.nextInt(4)+1     		
  		int batchSize = alertEvents.size() / numBatches
			
  		def sender = new Messenger();

  		totalNumAlertEventsInserted += alertEvents.size()
  		
  		for (i in 0..numBatches) {
  		    int startIndex = i*batchSize
  			int endIndex = Math.min(startIndex+batchSize, alertEvents.size())    
  			
  			if (startIndex < endIndex) {
  	  			def batch = new ArrayList(alertEvents.subList(startIndex, endIndex))
          		sender.publishMessage(EventConstants.EVENTS_TOPIC, batch);      			    
  			}
  		}  	    
  	}
  	
  	private def getTriggers() {
    	// if the alert definition is a resource type alert definition, we must 
    	// get the triggers for each specific resource definition of that type
    	def isResourceType = adM.one.isResourceTypeAlertDefinition(alertDefId)
    	
    	def alertDefIds = null
    	    
    	if (isResourceType) {
    	    def resourceAlertDefs = adM.one.findChildAlertDefinitions(alertDefId)
    	    alertDefIds = resourceAlertDefs.collect {return it.id}
    	} else {
    	    alertDefIds = [alertDefId]
    	}
    	
    	// println("generating alerts for alert defs: "+alertDefIds)
    	
    	def allTriggers = new ArrayList()

		for (alertDefId in alertDefIds) {
	  	  	def registeredTriggers = rtM.one.getAllTriggersByAlertDefId(alertDefId)
		
	  	  	def triggersPerResourceAlert = 
	  	  	    registeredTriggers.collect { 
    	    				def tv = it.getRegisteredTriggerValue()
    	    				def trigger = Class.forName(tv.getClassname()).newInstance();
            				trigger.init(tv);
            				return trigger
    						}
	  	  	allTriggers.add(triggersPerResourceAlert)
		}
    	
    	// a list of list elements
    	return allTriggers
  	}  	
 
  	private def getAlertEvents(numAlertEvents, triggers, random) {
  	    def alertEvents = new ArrayList(numAlertEvents)  	    
  	    
  	    for (i in 1..numAlertEvents) {
  	        triggers.each {
  	  	        def events = generator(it, random)
  	  	        alertEvents.addAll(events)  	            
  	        }
  	    }
  	    
  	    return alertEvents
  	}

}  