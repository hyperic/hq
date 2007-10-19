package org.hyperic.hq.plugin.nagios.parser;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;

public class NagiosCommandObj
    extends NagiosObj
{
    private static final Pattern
        _cmdLineEx = Pattern.compile("^\\s*command_line"),
        _cmdNameEx = Pattern.compile("^\\s*command_name");

    private String _cmdLine,
                   _cmdName;

    private List _args = new ArrayList();

    void resolveDependencies(NagiosParser parser)
    {
    }

    protected NagiosCommandObj()
    {
        super();
    }

    public String getKey()
    {
        return _cmdName;
    }

    public String getCmdExec()
    {
        return _cmdLine;
    }

    public String getName()
    {
        return _cmdName;
    }

    protected void parseCfg(String cfgBlock)
        throws NagiosParserException
    {
        String[] lines = cfgBlock.split("\\n");
        for (int i=0; i<lines.length; i++)
        {
            String line = lines[i];
            if (_blankLine.matcher(line).find() ||
                _comment.matcher(line).find()) {
                continue;
            }
            if (_cmdLineEx.matcher(line).find()) {
                setCmdLine(line);
            } else if (_cmdNameEx.matcher(line).find()) {
                setCmdName(line);
            }
        }
        if (_cmdLine == null || _cmdName == null)
            throw new NagiosParserException(cfgBlock);
    }

    private void setCmdName(String line)
    {
        line = removeInlineComments(line);
        String[] buf = line.split("\\s+");
        _cmdName = buf[buf.length-1];
    }

    private void setCmdLine(String line)
    {
        line = removeInlineComments(line);
        line = line.replaceAll("\\s*command_line\\s*", "");
        String[] buf = line.split("\\s+");
        List list = Arrays.asList(buf);
        _cmdLine = join(" ", list);
    }

    public void importObject()
    {
    }

    public int getType()
    {
        return COMMAND_TYPE;
    }

    public void populateData(String config)
    {
    }

    public String toString()
    {
        return "Command Name -> "+_cmdName+
               "\nCommand Line -> "+_cmdLine;
    }

    public boolean equals(Object rhs)
    {
        if (this == rhs)
            return true;
        if (rhs instanceof NagiosCommandObj)
            return equals((NagiosCommandObj)rhs);
        return false;
    }

    public int hashCode()
    {
        return _cmdName.hashCode();
    }

    private boolean equals(NagiosCommandObj rhs)
    {
        if (rhs._cmdName.equals(_cmdName))
            return true;
        return false;
    }

    public int compareTo(Object rhs)
        throws ClassCastException
    {
        if (this == rhs)
            return 0;
        String this_buf = _cmdLine+_cmdName;
        NagiosCommandObj r = (NagiosCommandObj)rhs;
        String rhs_buf = r._cmdLine+r._cmdName;
        if (rhs instanceof NagiosCommandObj) {
            return this_buf.compareTo(rhs_buf);
        } else {
            throw new ClassCastException();
        }
    }
}
