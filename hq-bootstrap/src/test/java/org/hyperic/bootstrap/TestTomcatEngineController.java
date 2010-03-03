package org.hyperic.bootstrap;

import java.util.ArrayList;
import java.util.List;

import org.hyperic.sigar.Sigar;
import org.junit.Test;

public class TestTomcatEngineController {

    @Test
    public void testStart() {
        TomcatEngineController controller = new TomcatEngineController(new ProcessManager(new Sigar()),"/Applications/HQ5/server-5.0.0/hq-engine" , "/Applications/HQ5/server-5.0.0","/Applications/HQ5/server-5.0.0/logs/hq-server.pid");
        final List<String> expectedOpts = new ArrayList<String>();
        expectedOpts.add("-XX:MaxPermSize=192m");
        expectedOpts.add("-Xmx512m");
        expectedOpts.add("-Xms512m");
        expectedOpts.add("-XX:+HeapDumpOnOutOfMemoryError");
        expectedOpts.add("-Dserver.home=/Applications/Evolution/server-5.0.0-EE");
        controller.start(expectedOpts);
    }
}
