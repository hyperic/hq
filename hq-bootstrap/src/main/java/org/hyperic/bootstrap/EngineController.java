package org.hyperic.bootstrap;

import java.util.List;

public interface EngineController {

    int start(List<String> javaOpts);
    void stop();
}
