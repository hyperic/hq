package org.hyperic.perftest

import edu.emory.mathcs.backport.java.util.concurrent.ThreadPoolExecutor
import edu.emory.mathcs.backport.java.util.concurrent.TimeUnit
import edu.emory.mathcs.backport.java.util.concurrent.ArrayBlockingQueue

class TaskExecutor {
    private List runData = []
    
    def execute(String runName, List tasks) {
        println "Executing run [${runName}]"
        def pools = []
        for (t in tasks) {
            println "Task [${t.task.name}] -- starting pool with ${t.max_threads} threads"
            def q = new ArrayBlockingQueue(t.num_times)
            def pool = new ThreadPoolExecutor(t.max_threads, t.max_threads,
                                              Long.MAX_VALUE, TimeUnit.SECONDS,
                                              q);
            pool.prestartAllCoreThreads()
            for (i in 0..<t.num_times) {
                q.add(t.task)
            }
            
            pools << pool
        }
        
        for (p in pools) {
            println "Shutting down pool [${p}]"
            p.shutdown()
            p.awaitTermination(Long.MAX_VALUE, TimeUnit.SECONDS)
        }

        def runInfo = [name:runName, tasks:[]]
        for (t in tasks.task) {
            runInfo.tasks << [name : t.name, timings : t.timings]
        	t.resetTimings()
        }
        runData << runInfo
    }
    
    def dumpReport() {
        for (r in runData) {
        	println "$r.name:"
			for (t in r.tasks) {
			    def successRuns = t.timings.successRuns
			    def min     = successRuns.min() / 1000.0
			    def max     = successRuns.max() / 1000.0
			    def totTime = successRuns.sum() / 1000.0
			    def avg     = totTime / successRuns.size()
			    def oops    = t.timings.num_oops
			    
			    println "    ${t.name} min=${min} max=${max} avg=${avg} " +
			            "oops=${oops}"
			}
        }
    }
}
