/**
 * NOTE: This copyright does *not* cover user programs that use HQ
 * program services by normal system calls through the application
 * program interfaces provided as part of the Hyperic Plug-in Development
 * Kit or the Hyperic Client Development Kit - this is merely considered
 * normal use of the program, and does *not* fall under the heading of
 *  "derived work".
 *
 *  Copyright (C) [2009-2010], VMware, Inc.
 *  This file is part of HQ.
 *
 *  HQ is free software; you can redistribute it and/or modify
 *  it under the terms version 2 of the GNU General Public License as
 *  published by the Free Software Foundation. This program is distributed
 *  in the hope that it will be useful, but WITHOUT ANY WARRANTY; without
 *  even the implied warranty of MERCHANTABILITY or FITNESS FOR A
 *  PARTICULAR PURPOSE. See the GNU General Public License for more
 *  details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program; if not, write to the Free Software
 *  Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307
 *  USA.
 *
 */

package org.hyperic.util;

import org.apache.commons.lang.builder.EqualsBuilder;
import org.easymock.EasyMock;
import org.easymock.IArgumentMatcher;

/**
 * Implementation of {@link IArgumentMatcher} that checks for argument equality
 * using recursive reflection to validate all fields
 * TODO this still only does a .equals on elements in Collections, so won't do deep
 * comparison of Collection members.  This could result in false positives
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
