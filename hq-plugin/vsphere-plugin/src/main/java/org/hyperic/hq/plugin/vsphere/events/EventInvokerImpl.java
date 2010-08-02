package org.hyperic.hq.plugin.vsphere.events;

import com.vmware.vim25.*;
import com.vmware.vim25.mo.PropertyCollector;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.core.task.TaskExecutor;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.concurrent.ConcurrentTaskScheduler;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.Date;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

/**
 * EventInvokerImpl
 * Investigating this - need to move to
 * listenerContainer with alternate handling/execution.
 *
 * @author Helena Edelson
 */
public class EventInvokerImpl implements EventInvoker {

    private static final Log logger = LogFactory.getLog(EventInvokerImpl.class.getName());

    /** Investigating */
    public Object invoke(PropertyCollector propertyCollector, EventHandler eventHandler, long duration) {
        if (propertyCollector != null) {
            doInvoke(propertyCollector, eventHandler, duration);
        }
        
        return null;
    }

    /** Investigating */
    private void doInvoke(final PropertyCollector propertyCollector, final EventHandler eventHandler, long duration) {
        ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
        TaskScheduler taskScheduler = new ConcurrentTaskScheduler(scheduler);

        /*ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.afterPropertiesSet();*/
        
        try {

            /*executor.execute(new Runnable() {
                public void run() {
                    listen(propertyCollector, eventHandler);
                }
            });*/

            taskScheduler.schedule(new Runnable() {
                public void run() {
                    listen(propertyCollector, eventHandler);
                }
            }, new Date(System.currentTimeMillis() + 0)); 
        }
        catch (Exception e) {
            logger.error(e);
            // handle
        }
        finally {
            scheduler.shutdown();
        }
    }

    /** Investigating */
    private void listen(PropertyCollector propertyCollector, EventHandler eventHandler) {
        String version = null;
        try {
            logger.debug("Waiting for new Updates...");
            long startTime = System.currentTimeMillis();
            UpdateSet update = propertyCollector.waitForUpdates(version);
            if (update != null && update.getFilterSet() != null) {

                eventHandler.handleUpdate(update);

                version = update.getVersion();
                logger.debug("taskScheduler took " + (System.currentTimeMillis() - startTime) + " millis.");
                logger.debug(" Current Version: " + version);
            }
        }
        catch (Exception e) {
            if (!(e instanceof RequestCanceled)) {
                // handle
            }
        }
    }

     
}
