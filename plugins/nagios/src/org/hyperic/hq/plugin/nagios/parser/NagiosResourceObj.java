package org.hyperic.hq.plugin.nagios.parser;

import java.io.PrintStream;
import java.util.regex.Pattern;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.net.UnknownHostException;

public class NagiosResourceObj extends NagiosObj
{
    private String _key,
                   _val;

    protected NagiosResourceObj()
    {
        super();
    }

    public String toString()
    {
        return "Filename -> "+_filename+
               "\nResource Key -> "+_key+
               "\nResource Value -> "+_val;
    }

    public String getValue()
    {
        return _val;
    }

    public String getKey()
    {
        return _key;
    }

    public int hashCode()
    {
        return _key.hashCode();
    }

    public int getType()
    {
        return RESOURCE_TYPE;
    }

    void resolveDependencies(NagiosParser parser)
    {
    }

    protected void parseCfg(String cfgBlock)
        throws NagiosParserException
    {
        String[] toks = cfgBlock.split("=");
        if (toks.length != 2)
            throw new NagiosParserException("ERROR: Resource Type is invalid."+
                                            "  Line -> "+cfgBlock);
        _key = toks[0];
        _val = toks[1];
    }
}
