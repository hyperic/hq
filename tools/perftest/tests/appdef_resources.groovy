import org.hyperic.hq.perftest.WebTask
import org.hyperic.hq.perftest.HQClient

def allPlatformPagesTask  = new WebTask('Examine all of the platform pages') { hq ->
    def p = hq.randomPlatform
    hq.jumpTo(p, inventory : 'main')
    hq.jumpTo(p, monitor : 'indicators')
    hq.jumpTo(p, monitor : 'metric_data')
    hq.jumpTo(p, alert   : 'configure')
}

def allServerPagesTask = new WebTask('Examine all of the server pages') { hq ->
    def s = hq.randomServer
    hq.jumpTo(s, inventory : 'main')
    hq.jumpTo(s, monitor : 'indicators')
    hq.jumpTo(s, monitor : 'metric_data')
    hq.jumpTo(s, alert   : 'configure')
}

def allServicePagesTask = new WebTask('Examine all of the service pages') { hq ->
    def v = hq.randomService
    hq.jumpTo(v, inventory : 'main')
    hq.jumpTo(v, monitor : 'indicators')
    hq.jumpTo(v, monitor : 'metric_data')
    hq.jumpTo(v, alert   : 'configure')
}

HQClient.preload()

execute('Appdef Load Run (low concurrency)', [
    [ task : allPlatformPagesTask, max_threads : 3, num_times : 30 ],                            
    [ task : allServerPagesTask,   max_threads : 3, num_times : 30 ],                            
    [ task : allServicePagesTask,  max_threads : 3, num_times : 30 ],                            
])

execute('Appdef Load Run (medium concurrency)', [
    [ task : allPlatformPagesTask, max_threads : 20, num_times : 50 ],                            
    [ task : allServerPagesTask,   max_threads : 20, num_times : 50 ],                            
    [ task : allServicePagesTask,  max_threads : 20, num_times : 50 ],                            
])

