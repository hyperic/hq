package org.hyperic.perftest

class Task 
    implements Runnable
{
    Map     timings
    String  name
	Closure closure
	
    def Task(String name, Closure c) {
    	this.name    = name
    	this.closure = c
    	this.timings = [min:Long.MAX_VALUE, max:Long.MIN_VALUE, total:0, 
    	                num_runs:0]
    }
    
    long getNow() {
        System.currentTimeMillis()
    }
    
    protected void executeClosure() {
        closure()
    }
    
    void run() {
        long startTime = now
    	executeClosure()
        long totTime = now - startTime
        
        synchronized (timings) {
            if (totTime < timings.min)
                timings.min = totTime
            if (totTime> timings.max)
                timings.max = totTime
            timings.total += totTime
            timings.num_runs++
        }
    }
}