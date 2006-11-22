/**
 *
 */
package org.hyperic.hq.common.server.mbean;

import org.apache.commons.logging.LogFactory;
import org.apache.commons.logging.Log;
import org.jboss.util.threadpool.BasicThreadPool;

/**
 * ThreadPool MBean
 *
 * @jmx:mbean name="hyperic.jmx:type=Service,name=HQWorkManagerThreadPool"
 */
public class HQThreadPool implements HQThreadPoolMBean
{
    private static Log log = LogFactory.getLog(HQThreadPool.class);

    private static HQThreadPool instance;

    public static HQThreadPool getInstance()
    {
        return instance;
    }

    private int maximumPoolSize;
    private int maximumQueueSize;
    private int keepAliveTime;
    private String name;

    /** The thread pool */
    private BasicThreadPool threadPool = null;

    /**
     * Get thread pool name
     *
     * @jmx:managed-attribute
     */
    public String getName()
    {
        return name;
    }

    /**
     * set thread pool name
     *
     * @jmx:managed-attribute
     */
    public void setName(String name)
    {
        this.name = name;
    }

    /**
     * Get the maximum pool size
     *
     * @jmx:managed-attribute
     */
    public int getMaximumPoolSize()
    {
        return maximumPoolSize;
    }

    /**
     * Set the maximum pool size
     *
     * @jmx:managed-attribute
     */
    public void setMaximumPoolSize(int maximumPoolSize)
    {
        this.maximumPoolSize = maximumPoolSize;
    }

    /**
     * Get the maximum queue size
     *
     * @jmx:managed-attribute
     */
    public int getMaximumQueueSize()
    {
        return maximumQueueSize;
    }

    /**
     * set the maximum queue size
     *
     * @jmx:managed-attribute
     */
    public void setMaximumQueueSize(int maximumQueueSize)
    {
        this.maximumQueueSize = maximumQueueSize;
    }

    /**
     * get thread keep alive time
     *
     * @jmx:managed-attribute
     */
    public int getKeepAliveTime()
    {
        return keepAliveTime;
    }

    /**
     * set thread keep alive time
     *
     * @jmx:managed-attribute
     */
    public void setKeepAliveTime(int keepAliveTime)
    {
        this.keepAliveTime = keepAliveTime;
    }

    public void run(Runnable runnable)
    {
        threadPool.run(runnable);
    }

    /**
     * @jmx:managed-operation
     */
    public void init()
    {
    }

    /**
     * @jmx:managed-operation
     */
    public void start() throws Exception
    {
        if (log.isInfoEnabled()) {
            log.info("HQThreadPool: MaximumPoolSize=" + maximumPoolSize +
                     ", MaximumQueueSize="+ maximumQueueSize +
                     ", KeepAliveTime="+ keepAliveTime);
        }
        threadPool = new BasicThreadPool(name);
        threadPool.setMaximumPoolSize(maximumPoolSize);
        threadPool.setMaximumQueueSize(maximumQueueSize);
        threadPool.setKeepAliveTime((long)keepAliveTime);
        instance = this;
    }

    /**
     * @jmx:managed-operation
     */
    public void stop()
    {
        instance = null;
        threadPool = null;
        log.info("Stopping " + getClass().getName());
    }

    /**
     * @jmx:managed-operation
     */
    public void destroy()
    {
    }
}
