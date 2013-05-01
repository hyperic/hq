package org.hyperic.hq.agent.diagnostics;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.concurrent.atomic.AtomicBoolean;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hyperic.hq.agent.AgentConfig;
import org.hyperic.hq.stats.AbstractStatsWriter;
import org.hyperic.util.TimeUtil;

public class AgentDiagnostics extends Thread {
    
    private static final String BASE_NAME = "agent-diags-<day>";
    private static final long MINUTE = 1000*60;
    private final Log log = LogFactory.getLog(AgentDiagnostics.class);
    private final Collection<AgentDiagnosticObject> diags = new ArrayList<AgentDiagnosticObject>();
    private String currFilename = null;
    private FileWriter currFile = null;
    private final AtomicBoolean shutdown = new AtomicBoolean(false);
    private String basedir;
    private static final AgentDiagnostics instance = new AgentDiagnostics();
    
    private AgentDiagnostics() {
        super("AgentDiagnostics");
        setDaemon(true);
    }
    
    public void die() {
        shutdown.set(true);
        interrupt();
    }
    
    public void addDiagnostic(AgentDiagnosticObject o) {
        synchronized(diags) {
            diags.add(o);
        }
    }
    
    public void setConfig(AgentConfig config) {
        basedir = config.getLogDir().getAbsolutePath();
    }
    
    public void run() {
        log.info("starting agent diagnostics thread");
        while (!shutdown.get()) {
            try {
                Thread.sleep(10*MINUTE);
                if (basedir != null) {
                    writeDiags();
                }
            } catch (Throwable t) {
                if (!shutdown.get()) {
                    log.error(t,t);
                } else {
                    log.info("Agent Diagnostic Thread exiting");
                }
            }
        }
    }
    
    private void writeDiags() throws IOException {
        final StringBuilder status = new StringBuilder(1024);
        status.append("\n\n#=======================\n#")
              .append(TimeUtil.toString(System.currentTimeMillis()))
              .append("\n#=======================");
        Collection<AgentDiagnosticObject> diagObjs;
        synchronized(diags) {
            diagObjs = Collections.unmodifiableCollection(diags);
        }
        for (final AgentDiagnosticObject o : diagObjs) {
            status.append("\n\n#").append(o.getDiagName()).append("\n");
            status.append(o.getDiagStatus());
        }
        final String filename = getFilename();
        if (currFilename == null) {
            currFile = new FileWriter(filename, true);
            currFilename = filename;
        } else if (!filename.equals(currFilename)) {
            if (currFile!=null){
                currFile.close();
            }
            AbstractStatsWriter.gzipFile(currFilename);
            currFile = new FileWriter(filename, true);
            currFilename = filename;
        }
        currFile.append(status);
        currFile.flush();
    }
    
    private final String getFilename() {
        SimpleDateFormat f = new SimpleDateFormat("EEEE");
        String day = f.format(new Date(System.currentTimeMillis()));
        String fs = File.separator;
        return basedir + fs + BASE_NAME.replace("<day>", day) + ".txt";
    }

    public static AgentDiagnostics getInstance() {
        return instance;
    }

}
