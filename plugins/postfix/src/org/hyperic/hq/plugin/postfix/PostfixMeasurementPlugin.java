/*
 * NOTE: This copyright does *not* cover user programs that use HQ
 * program services by normal system calls through the application
 * program interfaces provided as part of the Hyperic Plug-in Development
 * Kit or the Hyperic Client Development Kit - this is merely considered
 * normal use of the program, and does *not* fall under the heading of
 * "derived work".
 * 
 * Copyright (C) [2004, 2005, 2006], Hyperic, Inc.
 * This file is part of HQ.
 * 
 * HQ is free software; you can redistribute it and/or modify
 * it under the terms version 2 of the GNU General Public License as
 * published by the Free Software Foundation. This program is distributed
 * in the hope that it will be useful, but WITHOUT ANY WARRANTY; without
 * even the implied warranty of MERCHANTABILITY or FITNESS FOR A
 * PARTICULAR PURPOSE. See the GNU General Public License for more
 * details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307
 * USA.
 */

package org.hyperic.hq.plugin.postfix;

import java.util.HashMap;
import java.util.Properties;

import java.io.File;

import org.hyperic.hq.product.MeasurementPlugin;
import org.hyperic.hq.product.Metric;
import org.hyperic.hq.product.MetricNotFoundException;
import org.hyperic.hq.product.MetricUnreachableException;
import org.hyperic.hq.product.MetricValue;
import org.hyperic.hq.product.PluginException;

public class PostfixMeasurementPlugin
    extends MeasurementPlugin
{
    private static HashMap cache = new HashMap();

    private MetricValue getValue(File file, String attr)
        throws MetricNotFoundException {

        HashMap metrics = (HashMap)cache.get(file);
        if (metrics == null) {
            metrics = new HashMap();
            cache.put(file, metrics);
        }

        Integer val = (Integer)metrics.get(attr);
        if (val == null) {
            // recharge the cache if the value is not found
            int qMetrics[] = getQueueMetrics(file);

            metrics.put("QueuedMessages", new Integer(qMetrics[0]));
            metrics.put("QueueSize", new Integer(qMetrics[1]));
            
            val = (Integer)metrics.get(attr);
        }

        // remove the metric from the cache to force a refresh
        // next time around
        val = (Integer)metrics.remove(attr);
        if (val == null) {
            throw new MetricNotFoundException("No metric mapped to " +
                                              " metric: " + attr);
        }

        return new MetricValue(val.intValue());
    }

    public MetricValue getValue(Metric metric)
        throws PluginException,
               MetricNotFoundException,
               MetricUnreachableException
    {
        String domain = metric.getDomainName();

        if (domain.equals("system.avail")) {
            String path = metric.getObjectProperty("Arg");
            double avail =
                new File(path).isDirectory() ?
                    Metric.AVAIL_UP : Metric.AVAIL_DOWN;

            return new MetricValue(avail);
        }

        if (domain.equals("queue")) {
            Properties props = metric.getObjectProperties();
            String qpath = props.getProperty(PostfixServerDetector.PROP_PATH);
            String attr = metric.getAttributeName();

            File queue = new File(qpath);
            // make sure we can read the queue.
            if (!queue.canRead()) {
                throw new MetricNotFoundException("cannot read "
                    + qpath + ", check permissions!");
            }

            // make sure the queue is a directory.
            if (!queue.isDirectory()) {
                throw new MetricNotFoundException("invalid queue: " + qpath);
            }

            return getValue(queue, attr);
        }

        throw new MetricNotFoundException(domain);
    }

    private int[] getQueueMetrics(File queue)
    {

        File queueContents[] = queue.listFiles();
        int messageCount = 0;
        int queueSize = 0;

        for (int i=0; i<queueContents.length; i++) {

            if (queueContents[i].isDirectory()) {
                // check hashed directory
                int tmpVal[] = getQueueMetrics(queueContents[i]);
                messageCount += tmpVal[0];
                queueSize += tmpVal[1];
            }
            else if (queueContents[i].isFile()) {
                // count the queued message
                messageCount += 1;

                // and add the size of the message to the total
                queueSize += (int)queueContents[i].length();
            }
            else {
                String absPath = queueContents[i].getAbsolutePath();
                getLog().error(absPath + " is not a directory or a file!");
            }

        }

        int retVal[] = { messageCount, queueSize };
        return retVal;
    }

}
