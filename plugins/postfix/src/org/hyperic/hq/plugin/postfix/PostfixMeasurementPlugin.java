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
import org.hyperic.sigar.FileInfo;

import org.hyperic.hq.product.Metric;
import org.hyperic.hq.product.MetricNotFoundException;
import org.hyperic.hq.product.MetricUnreachableException;
import org.hyperic.hq.product.MetricValue;
import org.hyperic.hq.product.PluginException;
import org.hyperic.hq.product.SigarMeasurementPlugin;

public class PostfixMeasurementPlugin
    extends SigarMeasurementPlugin
{

    private static HashMap queueMetrics = new HashMap(2);

    public MetricValue getValue(Metric metric)
        throws PluginException,
               MetricNotFoundException,
               MetricUnreachableException
    {
        String domain = metric.getDomainName();

        if (domain.equals("sigar.ptql") || domain.equals("sigar")) {
            return super.getValue(metric);
        }

        if (domain.equals("system.avail")) {

            double avail;

            try {
                double val = super.getValue(metric).getValue();
                avail = Metric.AVAIL_UP;

                if (val != FileInfo.TYPE_DIR)
                    avail = Metric.AVAIL_DOWN;
            } catch (MetricNotFoundException e) {
                avail = Metric.AVAIL_DOWN;
            }

            return new MetricValue(avail);
        }

        if (domain.equals("queue")) {
            Properties props = metric.getObjectProperties();
            String qpath = props.getProperty(PostfixServerDetector.PROP_PATH);
            String index = props.getProperty(PostfixServerDetector.PROP_IDX);
            String attr = metric.getAttributeName() + index;

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

            Integer val = (Integer)queueMetrics.get(attr);
            if (val == null) {
                // recharge the cache if the value is not found
                cacheQueueMetrics(index, queue);

                val = (Integer)queueMetrics.get(attr);
                if (val == null) {
                    throw new MetricNotFoundException("No metric mapped to " +
                                                      " metric: " + attr);
                }
            }

            // remove the metric from the cache to force a refresh
            // next time around
            queueMetrics.remove(attr);

            return new MetricValue(val.intValue(), System.currentTimeMillis());
        }

        throw new MetricNotFoundException(domain);
    }

    private void cacheQueueMetrics(String index, File queue)
    {

        int qMetrics[] = getQueueMetrics(queue);

        /*
        XXX: putting the index into the key so we know which postfix
        server the metrics are coming from.  possibly want to look at
        using a different data structure.
        */
        //String qm = "QueuedMessages" + index;
        //String qs = "QueueSize" + index;
        queueMetrics.put("QueuedMessages" + index, new Integer(qMetrics[0]));
        queueMetrics.put("QueueSize" + index, new Integer(qMetrics[1]));

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
