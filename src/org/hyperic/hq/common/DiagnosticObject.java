package org.hyperic.hq.common;

/**
 * Interface for diagnostic objects.  Implementing classes are registered with
 * DiagnosticThread addDiagnosticObject() method.
 * @see DiagnosticThread
 */
public interface DiagnosticObject {

    public String getStatus();
}
