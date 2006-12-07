package org.hyperic.hq.plugin.vmware;

import org.hyperic.hq.product.LogFileTailPlugin;
import org.hyperic.hq.product.PluginException;
import org.hyperic.hq.product.TrackEvent;
import org.hyperic.sigar.FileInfo;
import org.hyperic.util.config.ConfigResponse;

public class VMwareEventLogPlugin extends LogFileTailPlugin {

    private static final String[] LOG_LEVELS = {
        "error", //Error
        "warning", //Warning
        "info,question,answer", //Info
        "debug" //Debug
    };

    private VMwareEventLogParser parser;
    private String source;

    public String[] getLogLevelAliases() {
        return LOG_LEVELS;
    }

    public void configure(ConfigResponse config) throws PluginException {
        super.configure(config);

        this.parser = new VMwareEventLogParser();

        //XXX name of the file is long and ugly
        this.source = "VM eventlog";
    }

    public TrackEvent processLine(FileInfo info, String line) {
        VMwareEventLogParser.Entry entry;

        try {
            entry = this.parser.parse(line);
            if (entry == null) {
                return null;
            }
        } catch (Exception e) {
            getLog().error("Error parsing line: '" + line + "'", e);
            return null;
        }

        return newTrackEvent(entry.time,
                             entry.type,
                             this.source,
                             entry.subject + " - " + entry.body);
    }

    public String getDefaultLogFile(String installPath) {
        return "event-${vm.name}.vmx.log"; //XXX lousy default
    }
}
