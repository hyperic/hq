import org.hyperic.hq.perftest.WebTask

def getAllPlatformsTask = new WebTask('Get all platforms') { hq ->
    hq.jumpTo('hub', [platforms : [page_size : 'unlimited']])
}

def getAllServersTask = new WebTask('Get all servers') { hq ->
    hq.jumpTo('hub', [servers : [page_size : 'unlimited']])
}

def getAllServicesTask = new WebTask('Get all services') { hq ->
    hq.jumpTo('hub', [services : [page_size : 'unlimited']])
}

execute('Pound Platforms Hub', [
    [ task : getAllPlatformsTask, max_threads : 1, num_times : 20 ]
])

execute('Pound Servers Hub', [
    [ task : getAllServersTask, max_threads : 1, num_times : 20 ]
])

execute('Pound Services Hub', [
    [ task : getAllServicesTask, max_threads : 1, num_times : 20 ]
])

execute('Low Concurrency Hub Hit', [
    [ task : getAllPlatformsTask, max_threads : 3, num_times : 5 ],                            
    [ task : getAllServersTask,   max_threads : 3, num_times : 5 ],                            
    [ task : getAllServicesTask,  max_threads : 3, num_times : 5 ],                            
])

