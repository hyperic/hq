package org.hyperic.hq.measurement.agent.server;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Properties;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hyperic.hq.agent.server.AgentStartException;
import org.hyperic.hq.agent.server.AgentStorageException;
import org.hyperic.hq.agent.server.AgentStorageProvider;
import org.hyperic.hq.bizapp.client.AgentCallbackClientException;
import org.hyperic.hq.bizapp.client.MeasurementCallbackClient;
import org.hyperic.hq.bizapp.client.StorageProviderFetcher;
import org.hyperic.hq.bizapp.shared.lather.TopNSendReport_args;
import org.hyperic.hq.plugin.system.TopData;
import org.hyperic.hq.plugin.system.TopReport;

import com.thoughtworks.xstream.XStream;

public class TOPNSenderThread implements Runnable {

    private static final int PROP_RECSIZE = 2048; // 2k records.
    private static final int SEND_INTERVAL = 60000;
    private static final int MAX_BATCHSIZE = 1000;
    private static final String DATA_LISTNAME = "topn_spool";

    private volatile boolean _shouldDie; // Should I shut down?
    private final Log _log;
    private final AgentStorageProvider _storage;
    private final List<TopReport> _transitionQueue;
    private final MeasurementCallbackClient client;


    TOPNSenderThread(Properties bootProps, AgentStorageProvider storage)
            throws AgentStartException {
        _log = LogFactory.getLog(TOPNSenderThread.class);
        _shouldDie = false;
        _storage = storage;
        _transitionQueue = new ArrayList<TopReport>();
        this.client = setupClient();
        String info = bootProps.getProperty(DATA_LISTNAME);
        if (info != null) {
            _storage.addOverloadedInfo(DATA_LISTNAME, info);
        }

        try {
            // Create list early since we want a larger record size.
            _storage.createList(DATA_LISTNAME, PROP_RECSIZE);
        } catch (AgentStorageException ignore) {
        }
    }


    private MeasurementCallbackClient setupClient() throws AgentStartException {
        StorageProviderFetcher fetcher;
        fetcher = new StorageProviderFetcher(_storage);
        return new MeasurementCallbackClient(fetcher);
    }

    void die() {
        _shouldDie = true;
    }

    void processData(TopData data) {

        String xml;
        TopReport report = new TopReport();
        report.setCreatTime(System.currentTimeMillis());
        XStream xstream = new XStream();
        xml = xstream.toXML(data);
        report.setXmlData(xml);

        synchronized (_transitionQueue) {
            _transitionQueue.add(report);
        }
    }


    /**
     * This routine moves all the data from the transition queue into the
     * storage provider, so it can be shipped to the server.
     */
    private void processTransitionQueue() {
        synchronized (_transitionQueue) {
            for (TopReport report : _transitionQueue) {
                try {
                    _storage.addToList(DATA_LISTNAME, report.encode());
                } catch (Exception exc) {
                    _log.error("Unable to store data", exc);
                }
            }
            try {
                _storage.flush();
            } catch (Exception exc) {
                _log.error("Unable to flush storage", exc);
            }
        }
    }

    public void run() {
        while (_shouldDie == false) {
            boolean success;
            int numUsed;

            try {
                Thread.sleep(SEND_INTERVAL);
            } catch (InterruptedException exc) {
                _log.info("TopN sender interrupted");
                return;
            }

            // Before sending the data off, make sure our transition queue is
            // empty

            this.processTransitionQueue();

            numUsed = 0;
            List<TopReport> lst = new ArrayList<TopReport>();
            for (Iterator i = _storage.getListIterator(DATA_LISTNAME); (i != null) && i.hasNext()
                    && (numUsed < MAX_BATCHSIZE); numUsed++) {
                TopReport report;
                try {
                    report = TopReport.decode((String) i.next());
                } catch (IOException exc) {
                    _log.error("Error accessing record -- deleting: " + exc);
                    continue;
                }

                lst.add(report);
            }

            // If we don't have anything to send -- move along
            if (numUsed == 0) {
                continue;
            }

            _log.debug("Sending " + numUsed + " Response time entries " + "to server");
            success = false;
            try {
                TopNSendReport_args report = new TopNSendReport_args();
                report.setTopReports(lst);
                this.client.topNSendReport(report);
                success = true;
            } catch (AgentCallbackClientException exc) {
                // Don't dump stack trace, it's a normal condition
                _log.error("Error sending RT data to server: " + exc.getMessage());
            }

            if (success) {
                int j = 0;

                for (Iterator i = _storage.getListIterator(DATA_LISTNAME); (i != null) && i.hasNext() && (j < numUsed); j++) {
                    i.next();
                    i.remove();
                }

                try {
                    _storage.flush();
                } catch (AgentStorageException exc) {
                    _log.error("Failed to flush agent storage", exc);
                }

                if (j != numUsed) {
                    _log.error("Failed to remove " + (numUsed - j) + "records");
                }
            }
        }
    }

}
