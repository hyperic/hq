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

        def runInfo = [name:runName, taskTimes:[]]
        for (t in tasks.task) {
        	runInfo.taskTimes << [taskName : t.name, 
        	                      timing : t.timings + ['avg':t.timings.total / t.timings.num_runs]]	    
        }
        runData << runInfo
    }
    
    def dumpReport() {
        for (r in runData) {
        	println "$r.name:"
        	for (t in r.taskTimes) {
				println "    $t.taskName: runs=${t.timing.num_runs} " + 
				        "min=${t.timing.min / 1000.0} " +
				        "max=${t.timing.max / 1000.0} " + 
				        "avg=${t.timing.avg / 1000.0}"
        	}
        }
    }
}
