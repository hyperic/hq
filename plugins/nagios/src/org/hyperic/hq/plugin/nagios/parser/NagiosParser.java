package org.hyperic.hq.plugin.nagios.parser;

import java.io.IOException;
import java.util.Set;

public interface NagiosParser
{
    public Set get(Integer type);

    public NagiosObj get(Integer type, String name)
        throws NagiosParserInternalException,
               NagiosTypeNotSupportedException;

    public void parse(String file) throws IOException;
}
