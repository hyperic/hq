package org.hyperic.hq.alerts;
/**
 * RuntimeException thrown when parsing XML representing alert definitions
 * @author jhickey
 *
 */
public class AlertDefinitionXmlParserException extends RuntimeException {

    public AlertDefinitionXmlParserException(String message) {
        super(message);
    }
}
