package org.hyperic.hq.dao;

import org.hyperic.hq.common.ApplicationException;

/**
 */
public class DAOException extends ApplicationException
{
    public DAOException()
    {
        super();
    }

    public DAOException(String s)
    {
        super(s);
    }

    public DAOException(Throwable t)
    {
        super(t);
    }

    public DAOException(String s, Throwable t)
    {
        super(s, t);
    }
}
