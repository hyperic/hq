package org.hyperic.perftest

class Task implements Runnable { 
            Map     stats = [:]
            String  name
	private Closure closure
	
    def Task(String name, Closure c) {
    	this.name    = name
    	this.closure = c
    	resetTimings()
    }
    
    long getNow() {
        System.currentTimeMillis()
    }
    
    def resetTimings() {
        synchronized (stats) {
        	stats.clear()
        	stats.successRuns = []
        	stats.num_oops = 0
        }
    }
    
    def getTimings() {
        synchronized (stats) {
            return [successRuns : stats.successRuns + [],
                    num_oops    : stats.num_oops ]
        }
    }

    protected void executeClosure(Closure statsCollector) {
        statsCollector() {
            closure()
        }
    }
    
    void run() {
        def statsCollector = { subclosure ->
            long startTime = now
            subclosure()
            long totTime = now - startTime
            
            synchronized (stats) {
                stats.successRuns << totTime
            }
        }
        
        try {
        	executeClosure(statsCollector)
        } catch(Exception e) {
            e.printStackTrace()
            synchronized (stats) {
                stats.num_oops++
            }
        }
    }
}
