import org.hyperic.hq.perftest.WebTask

def allPlatformPagesTask  = new WebTask('Examine all of the platform pages') { hq ->
    def p = hq.randomPlatform
    hq.jumpTo(p, monitor : 'indicators')
}

def allServicePagesTask = new WebTask('Examine all of the service pages') { hq ->
    def v = hq.randomService
    hq.jumpTo(v, monitor : 'indicators')
}

execute('Appdef Load Run', [
    [ task : allPlatformPagesTask, max_threads : 3, num_times : 5 ],                            
    [ task : allServicePagesTask, max_threads : 3, num_times : 5 ],                            
])

