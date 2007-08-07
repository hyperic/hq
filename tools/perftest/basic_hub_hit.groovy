import org.hyperic.hq.perftest.WebTask

def basicTask = new WebTask('My basic task') { hq ->
    hq.jumpTo('hub', platforms : [page_size : 30])
    hq.jumpTo('hub', platforms : [page_size : 'unlimited'])
}

execute('Simple Test Run', [
    [ task : basicTask, max_threads : 3, num_times : 5 ],                            
])

