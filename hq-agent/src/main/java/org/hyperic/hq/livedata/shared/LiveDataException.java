package org.hyperic.hq.livedata.shared;

import org.hyperic.util.NestedException;

public class LiveDataException extends NestedException {

    public LiveDataException() {
        super();
    }

    public LiveDataException(String s) {
        super(s);
    }

    public LiveDataException(Throwable t) {
        super(t);
    }

    public LiveDataException(String s, Throwable t) {
        super(s, t);
    }
}
