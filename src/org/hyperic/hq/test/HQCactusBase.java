package org.hyperic.hq.test;

/**
 */
public class HQCactusBase extends MockBeanTestBase
{
    protected boolean isCactusMode()
    {
        return true;
    }

    public HQCactusBase(String name)
    {
        super(name);
    }
}
