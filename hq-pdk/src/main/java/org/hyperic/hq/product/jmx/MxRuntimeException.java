package org.hyperic.hq.product.jmx;

import org.hyperic.util.NestedRuntimeException;

public class MxRuntimeException
    extends NestedRuntimeException {

    public MxRuntimeException() {
        super();
    }

    public MxRuntimeException(String s) {
        super(s);
    }

    public MxRuntimeException(Throwable t) {
        super(t);
    }

    public MxRuntimeException(String s, Throwable t) {
        super(s, t);
    }
}
