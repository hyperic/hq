package org.hyperic.hq.grouping;

import org.hyperic.hq.common.ApplicationException;

public class GroupException 
    extends ApplicationException
{
    public GroupException(String s) {
        super(s);
    }
    
    public GroupException(String s, Throwable t) {
        super(s, t);
    }
}