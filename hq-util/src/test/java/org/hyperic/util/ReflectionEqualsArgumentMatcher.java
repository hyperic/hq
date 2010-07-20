package org.hyperic.util;

import org.apache.commons.lang.builder.EqualsBuilder;
import org.easymock.EasyMock;
import org.easymock.IArgumentMatcher;

/**
 * Implementation of {@link IArgumentMatcher} that checks for argument equality
 * using recursive reflection to validate all fields
 * @author jhickey
 * 
 */
public class ReflectionEqualsArgumentMatcher implements IArgumentMatcher {

    private Object expected;

    public ReflectionEqualsArgumentMatcher(Object expected) {
        this.expected = expected;
    }

    /**
     * 
     * @param in The expected event
     * @return null
     */
    public static <T extends Object> T eqObject(T in) {
        EasyMock.reportMatcher(new ReflectionEqualsArgumentMatcher(in));
        return null;
    }

    public void appendTo(StringBuffer buffer) {
        buffer.append("eqObject(");
        buffer.append(expected.getClass().getName());
        buffer.append(")");
    }

    public boolean matches(Object actual) {
        return EqualsBuilder.reflectionEquals(this.expected, actual);
    }

}
