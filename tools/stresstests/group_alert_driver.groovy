import org.hyperic.hq.galerts.server.session.GalertDefPartition
import org.hyperic.hq.galerts.server.session.GalertManagerEJBImpl as adM
import org.hyperic.hq.galerts.server.session.GalertDef
import org.hyperic.hq.galerts.processor.Gtrigger
import org.hyperic.hq.zevents.ZeventManager
import org.hyperic.hq.measurement.server.session.MeasurementZevent
import org.hyperic.hq.product.MetricValue

/**
 * This script publishes events that trigger group alerts. The user must specify 
 * the strategy for generating zevents for each group alert definition 
 * in question using an event generator closure. Group Alert Zevent Inserter 
 * threads are created to publish the zevents in parallel.
 */


//SCRIPT VARIABLES ASSIGNED HERE


/**
 * The closure passed to each group alert event inserter used for generating 
 * zevents.
 *
 * @param gtriggers The list of Gtrigger objects associated with 
 *                  the alert definition.
 * @param random A source of randomness.
 * @return The list of Zevent objects that may (or may not) trigger 
 *         the alert definition.
 */
 def eventGenerator1 = {List gtriggers, Random random -> 
 					    def trigger = gtriggers.get(0)
 
 					    def zEvents = new ArrayList()
 					    
 					    trigger.getInterestedEvents().each {
 					        def zEvent = 
 					            new MeasurementZevent(it.getId(), new MetricValue(1))
 					        zEvents.add(zEvent)
 					    }
 					    
 					    return zEvents
 					}

// map of AlertDefIdAndPartition objects to event generator closures
def alertDefId2EventGenerator = 
    [(new AlertDefIdAndPartition(10001, GalertDefPartition.NORMAL)) : eventGenerator1]

def numZeventsPerInserter = 10      // number of zevents inserted per thread

def pauseTimeBetweenInserts = 1000      // per thread pause time between each batch of zevents inserted (msec)

def pauseBetweenInserterStarts = 2000   // pause time between starting each group alert zevent inserter thread (msec)

def totalRunTime = 1000                 // total test run time (msec)


// SCRIPT EXECUTION STARTS HERE

runTest(alertDefId2EventGenerator, 
        numZeventsPerInserter, 
        pauseTimeBetweenInserts, 
        pauseBetweenInserterStarts, 
        totalRunTime)


//HELPER FUNCTIONS

def runTest(alertDefId2EventGenerator, 
            numZeventsPerInserter, 
            pauseTimeBetweenInserts, 
            pauseBetweenInserterStarts, 
            totalRunTime) {

    def startTime = System.currentTimeMillis()

    def inserterThreads = startGalertInserters(alertDefId2EventGenerator, 
            								   numZeventsPerInserter, 
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
      totalNumInserted+=it.getNumZeventsInserted() 
      println(it.toString()+" thread is still active ="+it.isAlive()+
              ", total num zevents inserted="+it.getNumZeventsInserted())
    }
    
    println("Group alert inserters ran for "+
            (System.currentTimeMillis()-startTime)+
            " msec, inserting "+totalNumInserted+ " zevents")
    
}

def startGalertInserters(alertDefId2EventGenerator, 
        				 numZeventsPerInserter, 
        			     pauseTimeBetweenInserts, 
        			     pauseBetweenInserterStarts) {

	def inserterThreads = new ArrayList(alertDefId2EventGenerator.size())

	for (alertDefEntry in alertDefId2EventGenerator) {
		def galertInserter = 
		    startGalertInserterThread(alertDefEntry.key,
		        					  alertDefEntry.value,
		        					  numZeventsPerInserter,
						              pauseTimeBetweenInserts)
		inserterThreads.add(galertInserter)
		Thread.sleep(pauseBetweenInserterStarts)
	}

	return inserterThreads
}

def startGalertInserterThread(alertDefIdAndPartition,
        					  eventGenerator,
        					  numZeventsPerInsert,
		   				      pauseTimeBetweenInserts) {
 
	def galertInserter = 
	    new GalertZeventInserter(alertDefIdAndPartition, 
	        				    eventGenerator,
	        				    numZeventsPerInsert, 
	        			        pauseTimeBetweenInserts)
	galertInserter.start()
	return galertInserter
}

// KEY MAPPING GALERT DEFS TO EVENT GENERATORS (INCLUDES GALERT DEF ID AND PARTITION)

private class AlertDefIdAndPartition {
    
    private Integer gAlertDefId
    private GalertDefPartition partition
    
    def AlertDefIdAndPartition(Integer gAlertDefId, GalertDefPartition partition) {
        this.gAlertDefId = gAlertDefId
        this.partition = partition
    }
    
    def getDefId() {
        return this.gAlertDefId
    }
    
    def getPartition() {
        return this.partition
    }
    
}

//GROUP ALERT ZEVENT INSERTER CLASS

private class GalertZeventInserter extends Thread {

	private volatile boolean running
  	private volatile int totalNumZeventsInserted
	private alertDefIdAndPartition
	private numZevents
	private generator
	private pauseTime

  	
  	def GalertZeventInserter(alertDefIdAndPartition, 
	 		   			    eventGenerator,
		    			    numZeventsPerInsert,
		    			    pauseTimeBetweenInserts) {
                  
    	super("group alert zevent inserter, alert def id="+
    	        alertDefIdAndPartition.getDefId()+ 
    	        " : "+ alertDefIdAndPartition.getPartition())
    	setDaemon(true)
    	this.alertDefIdAndPartition = alertDefIdAndPartition
    	numZevents = numZeventsPerInsert
    	generator = eventGenerator
    	pauseTime = pauseTimeBetweenInserts
  		running = true
  	}
 
  	def shutdown() {
    	running = false
  	}
  	
  	def getNumZeventsInserted() {
  		return totalNumZeventsInserted
  	}
  	 
  	void run() {   
    	def random = new Random()
    	def triggers = getTriggers()

    	while (running) {
      		Thread.sleep(pauseTime)
      
      		def zevents = getZevents(numZevents, triggers, random)

      		//println(name+" "+zevents)
			
      		publishZevents(zevents)
    	}  
  	}
  	
  	private def publishZevents(zevents) {
  	  	totalNumZeventsInserted+=zevents.size()
  		ZeventManager.getInstance().enqueueEvents(zevents)
  	}
  	
  	private def getTriggers() {    	
    	return adM.one.getTriggersById(alertDefIdAndPartition.getDefId(), 
    	        					   alertDefIdAndPartition.getPartition())
  	}  	
 
  	private def getZevents(numZevents, triggers, random) {
  	    def zevents = new ArrayList(numZevents)  	    
  	    
  	    for (i in 1..numZevents) {
	  		def events = generator(triggers, random)
  	  	 	zevents.addAll(events)  	            
  	    }
  	    
  	    return zevents
  	}

}