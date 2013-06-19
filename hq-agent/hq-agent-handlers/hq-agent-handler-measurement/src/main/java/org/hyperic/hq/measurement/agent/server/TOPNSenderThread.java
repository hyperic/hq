package org.hyperic.hq.measurement.agent.server;

import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hyperic.hq.agent.server.AgentStartException;
import org.hyperic.hq.agent.server.AgentStorageProvider;
import org.hyperic.hq.bizapp.client.AgentCallbackClientException;
import org.hyperic.hq.bizapp.client.MeasurementCallbackClient;
import org.hyperic.hq.bizapp.client.StorageProviderFetcher;
import org.hyperic.hq.bizapp.shared.lather.TopNSendReport_args;
import org.hyperic.hq.plugin.system.TopReport;

public class TOPNSenderThread implements Runnable {

    private static final int SEND_INTERVAL = 60000;
    private static final int MAX_BATCHSIZE = 5;
    public static final String DATA_FOLDERNAME = "topn_records";

    private volatile boolean _shouldDie; // Should I shut down?
    private final Log _log;
    private final AgentStorageProvider _storage;
    private final MeasurementCallbackClient client;


    TOPNSenderThread(Properties bootProps, AgentStorageProvider storage)
            throws AgentStartException {
        _log = LogFactory.getLog(TOPNSenderThread.class);
        _shouldDie = false;
        _storage = storage;
        this.client = setupClient();
    }


    private MeasurementCallbackClient setupClient() throws AgentStartException {
        StorageProviderFetcher fetcher;
        fetcher = new StorageProviderFetcher(_storage);
        return new MeasurementCallbackClient(fetcher);
    }

    void die() {
        _shouldDie = true;
    }



    public void run() {
        while (_shouldDie == false) {
            boolean success;

            try {
                Thread.sleep(SEND_INTERVAL);
            } catch (InterruptedException exc) {
                _log.info("TopN sender interrupted");
                return;
            }

            List<TopReport> lst = new ArrayList<TopReport>();
            try {
            for (TopReport report : _storage.<TopReport> getObjectsFromFolder(DATA_FOLDERNAME)) {
                lst.add(report);
                if (lst.size() > MAX_BATCHSIZE) {
                    break;
                }
            }
            } catch (Throwable t) {
                _log.error(t.getMessage());
            }

            // If we don't have anything to send -- move along
            if (lst.isEmpty()) {
                continue;
            }

            _log.debug("Sending " + lst.size() + " TopN entries " + "to server");
            success = false;
            try {
                TopNSendReport_args report = new TopNSendReport_args();
                report.setTopReports(lst);
                this.client.topNSendReport(report);
                success = true;
            } catch (AgentCallbackClientException exc) {
                // Don't dump stack trace, it's a normal condition
                _log.error("Error sending TOPN data to server: " + exc.getMessage());
            }

            if (success) {
                // TODO : delete the sent files
            }
        }
    }

}
