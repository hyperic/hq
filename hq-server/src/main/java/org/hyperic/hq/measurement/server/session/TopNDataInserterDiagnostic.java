package org.hyperic.hq.measurement.server.session;

import org.hyperic.hq.common.DiagnosticObject;
import org.hyperic.hq.common.DiagnosticsLogger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class TopNDataInserterDiagnostic implements DiagnosticObject {

    private final TopNDataInserter dataInserter;
    private final String name = "Top Processes Data Inserter";

    @Autowired
    public TopNDataInserterDiagnostic(TopNDataInserter inserter, DiagnosticsLogger diagnosticLogger) {
        dataInserter = inserter;
        diagnosticLogger.addDiagnosticObject(this);
    }

    public String getName() {
        return name;
    }

    public String getShortName() {
        return name.replaceAll(" ", "");
    }

    public String getStatus() {
        int queueSize = dataInserter.getQueueSize();
        int flushInterval = dataInserter.getFlushInterval();
        int totalAddsToQueue = dataInserter.getTotalAddsToQueue();
        int totalOverflowCounter = dataInserter.getTotalOverflowCounter();
        int numberOfElementsInQueue = dataInserter.getNumberOfElementsInQueue();
        long totalAddToQueueTime = dataInserter.getTotalAddToQueueTime();
        long maxAddTime = dataInserter.getMaxAddTime();
        int totalFlushes = dataInserter.getTotalFlushes();
        long totalFlushTime = dataInserter.getTotalFlushTime();
        long maxFlushTime = dataInserter.getMaxFlushTime();

        StringBuffer res = new StringBuffer();

        res.append(getName() + " Diagnostics\n");
        res.append("    Configuration:\n");
        res.append("        Queue Size:  " + queueSize + "\n");
        res.append("        Flush Interval:  " + flushInterval + "\n\n");

        res.append("    Queue Adds:\n");
        res.append("        # Calls:    " + totalAddsToQueue + "\n");
        res.append("        Overflow Count:    " + totalOverflowCounter + "\n");
        res.append("        Number Of Current Entries In Queue:  " + numberOfElementsInQueue + "\n");
        if (0 != totalAddsToQueue) {
            res.append("        Average Add To Queue Time:  " + (totalAddToQueueTime / totalAddsToQueue) + " ms\n");
        }
        res.append("        Max Add To Queue Time:  " + maxAddTime + " ms\n\n");

        res.append("    Queue Flushes:\n");
        res.append("        # Calls:    " + totalFlushes + "\n");
        if (0 != totalFlushes) {
            res.append("        Average Flush Time:    " + (totalFlushTime / totalFlushes) + " ms\n");
        }
        res.append("        Max Flush Time:  " + maxFlushTime + " ms\n");

        return res.toString();
    }

    public String getShortStatus() {
        return getStatus();
    }
}
