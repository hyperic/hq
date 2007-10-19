package org.hyperic.hq.plugin.nagios.parser;

import java.util.regex.Pattern;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class NagiosHostGroupObj
    extends NagiosObj
{
    private static final Pattern
        _hostgroupnameEx = Pattern.compile("^\\s*hostgroup_name"),
        _nameEx          = Pattern.compile("^\\s*name"),
        _aliasEx         = Pattern.compile("^\\s*alias"),
        _membersEx       = Pattern.compile("^\\s*members"),
        _hostgroupMemEx  = Pattern.compile("^\\s*hostgroup_members");

    //maps to the check_command
    private String _hostgroupName,
                   _alias;

    private ArrayList _members      = new ArrayList(),
                      _hostgroupMem = new ArrayList();
    private int _hashCode = -1;
    // List of NagiosHostObjs
    private ArrayList _nagiosMemberObjs;

    protected NagiosHostGroupObj()
    {
        super();
    }

    void resolveMembers(NagiosCfgParser parser)
    {
        if (_nagiosMemberObjs != null)
            return;
        _nagiosMemberObjs = new ArrayList(_members.size());
        Integer type = new Integer(HOST_TYPE);
        try
        {
            for (Iterator i=_members.iterator(); i.hasNext(); )
            {
                String host = (String)i.next();
                NagiosHostObj hostObj = (NagiosHostObj)parser.get(type, host);
                _nagiosMemberObjs.add(hostObj);
            }
        }
        catch (NagiosParserInternalException e) {
            String msg = "exception thrown for hostgroup "+_hostgroupName+
                         " file -> "+_filename;
            debug(msg, e);
        }
        catch (NagiosTypeNotSupportedException e) {
            debug(e);
        }
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
            if (_hostgroupnameEx.matcher(line).find()) {
                setHostgroupName(line);
            } else if (_nameEx.matcher(line).find()) {
                setHostgroupName(line);
            } else if (_aliasEx.matcher(line).find()) {
                setAlias(line);
            } else if (_membersEx.matcher(line).find()) {
                setMembers(line);
            } else if (_hostgroupMemEx.matcher(line).find()) {
                setHostgroupMembers(line);
            }
        }
        if (_hostgroupName == null)
            throw new NagiosParserException(cfgBlock);
    }

    private void setHostgroupName(String line)
    {
        line = removeInlineComments(line);
        String[] name = line.split("\\s+");
        _hostgroupName = name[name.length-1];
    }

    private void setAlias(String line)
    {
        line = removeInlineComments(line);
        String[] alias = line.split("\\s+");
        _alias = join(" ", Arrays.asList(alias), 1);
    }

    private void setHostgroupMembers(String line)
    {
        line = removeInlineComments(line);
        String[] hosts = line.split("\\s+");
        String val = join(" ", Arrays.asList(hosts), 1);
        hosts = val.split("\\s*,\\s*");
        _hostgroupMem.ensureCapacity(hosts.length);
        _hostgroupMem.addAll(Arrays.asList(hosts));
    }

    private void setMembers(String line)
    {
        line = removeInlineComments(line);
        String[] hosts = line.split("\\s+");
        String val = join(" ", Arrays.asList(hosts), 1);
        hosts = val.split("\\s*,\\s*");
        _members.ensureCapacity(hosts.length);
        _members.addAll(Arrays.asList(hosts));
    }

    public List getHostObjs()
    {
        return Collections.unmodifiableList(_nagiosMemberObjs);
    }

    public void importObject()
    {
    }

    public String getKey()
    {
        return _hostgroupName;
    }

    public int hashCode()
    {
        if (_hashCode == -1) {
            String buf = _hostgroupName;
            _hashCode = buf.hashCode();
        }
        return _hashCode;
    }

    public int getType()
    {
        return HOSTGROUP_TYPE;
    }

    public void populateData(String config)
    {
    }

    public String toString()
    {
        return "HostgroupName -> "+_hostgroupName+
               "\nAlias -> "+_alias+
               "\nMembers -> "+join(",", _members)+
               "\nHostgroupMembers -> "+join(",", _hostgroupMem);
    }

    public boolean equals(Object rhs)
    {
        if (this == rhs)
            return true;
        if (rhs instanceof NagiosHostGroupObj)
            return equals((NagiosHostGroupObj)rhs);
        return false;
    }

    private boolean equals(NagiosHostGroupObj rhs)
    {
        if (rhs._hostgroupName.equals(_hostgroupName))
            return true;
        return false;
    }

    void resolveDependencies(NagiosParser parser)
    {
    }

    public int compareTo(Object rhs)
        throws ClassCastException
    {
        if (rhs instanceof NagiosHostGroupObj) {
            return _hostgroupName.
                compareTo(((NagiosHostGroupObj)rhs)._hostgroupName);
        } else {
            throw new ClassCastException();
        }
    }
}
