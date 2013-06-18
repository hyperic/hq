package org.hyperic.hq.measurement.agent.server;

import java.io.IOException;
import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Properties;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hyperic.hq.agent.server.AgentStartException;
import org.hyperic.hq.agent.server.AgentStorageException;
import org.hyperic.hq.agent.server.AgentStorageProvider;
import org.hyperic.hq.bizapp.client.AgentCallbackClientException;
import org.hyperic.hq.bizapp.client.StorageProviderFetcher;
import org.hyperic.hq.product.RtStat;

public class TOPNSenderThread {

    private static final int PROP_RECSIZE = 2048; // 2k records.
    private static final int SEND_INTERVAL = 60000;
    private static final int MAX_BATCHSIZE = 1000;
    private static final String DATA_LISTNAME = "rt_spool";

    private volatile boolean _shouldDie; // Should I shut down?
    private Log _log;
    private RtCallbackClient _client;
    private AgentStorageProvider _storage;
    private LinkedList _transitionQueue;

    RtSenderThread(Properties bootProps, AgentStorageProvider storage) throws AgentStartException {
        _log = LogFactory.getLog(RtSenderThread.class);
        _shouldDie = false;
        _storage = storage;
        _client = setupClient();
        _transitionQueue = new LinkedList();

        String info = bootProps.getProperty(DATA_LISTNAME);
        if (info != null) {
            _storage.addOverloadedInfo(DATA_LISTNAME, info);
        }

        try {
            // Create list early since we want a larger record size.
            _storage.createList(DATA_LISTNAME, PROP_RECSIZE);
        } catch (AgentStorageException ignore) {
            // Most likely an agent update where the existing rt schedule
            // already exists. Will fall back to the old 1k size.
        }
    }

    private RtCallbackClient setupClient() throws AgentStartException {
        StorageProviderFetcher fetcher;

        fetcher = new StorageProviderFetcher(_storage);
        return new RtCallbackClient(fetcher);
    }

    void die() {
        _shouldDie = true;
    }

    void processData(Collection lst) {
        Iterator i = lst.iterator();

        while (i.hasNext()) {
            RtStat rec = (RtStat) i.next();
            String encoded;

            try {
                encoded = rec.encode();
            } catch (IOException e) {
                _log.error("Unable to encode data: " + e);
                continue;
            }
            synchronized (_transitionQueue) {
                _transitionQueue.add(encoded);
            }
        }
    }

    /**
     * This routine moves all the data from the transition queue into the
     * storage provider, so it can be shipped to the server.
     */
    private void processTransitionQueue() {
        synchronized (_transitionQueue) {
            for (Iterator i = _transitionQueue.iterator(); i.hasNext();) {
                String val = (String) i.next();

                i.remove();
                try {
                    _storage.addToList(DATA_LISTNAME, val);
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
                Thread.sleep(RtSenderThread.SEND_INTERVAL);
            } catch (InterruptedException exc) {
                _log.info("Rt sender interrupted");
                return;
            }

            // Before sending the data off, make sure our transition queue is
            // empty

            this.processTransitionQueue();

            numUsed = 0;
            LinkedList lst = new LinkedList();
            for (Iterator i = _storage.getListIterator(DATA_LISTNAME); (i != null) && i.hasNext()
                    && (numUsed < MAX_BATCHSIZE); numUsed++) {
                RtStat rec;

                try {
                    rec = RtStat.decode((String) i.next());
                } catch (IOException exc) {
                    _log.error("Error accessing record -- deleting: " + exc);
                    continue;
                }

                lst.add(rec);
            }

            // If we don't have anything to send -- move along
            if (numUsed == 0) {
                continue;
            }

            _log.debug("Sending " + numUsed + " Response time entries " + "to server");
            success = false;
            try {
                _client.RtSendReport(lst);
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
