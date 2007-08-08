package org.hyperic.perftest

import org.hyperic.util.PrintfFormat

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
            // println "Shutting down pool [${p}]"
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
        def sfmt = new PrintfFormat('%-40s %-6s %-6s %-6s %-6s %-6s')
        def dfmt = new PrintfFormat('%-40s %-6.1f %-6.1f %-6.1f %-6d %-6.1f')
        println sfmt.sprintf(['', 'min', 'max', 'avg', 'oops', 'stddev'] as Object[])
        for (r in runData) {
        	println "$r.name:"
			for (t in r.tasks) {
			    def successRuns = t.timings.successRuns
			    def min     = successRuns.min() / 1000.0
			    def max     = successRuns.max() / 1000.0
			    def totTime = successRuns.sum() / 1000.0
			    def avg     = totTime / successRuns.size()
			    def oops    = t.timings.num_oops
			    
			    //println successRuns
			    
                def devsum = 0
                for (s in successRuns) {
                    devsum += (s - avg * 1000) * (s - avg * 1000)
                }
			    devsum /= (successRuns.size - 1)
			    def stddev = Math.sqrt(devsum) / 1000
        
			    println dfmt.sprintf(["    ${t.name}", 
			                          min as float,
			                          max as float,
			                          avg as float,
			                          oops as int,
			                          stddev as float] as Object[])
			}
            println ""
        }
    }
}
