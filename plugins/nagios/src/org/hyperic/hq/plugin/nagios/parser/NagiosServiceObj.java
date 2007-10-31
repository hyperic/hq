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
import org.hyperic.util.StringUtil;

public class NagiosServiceObj
    extends NagiosObj
{
    private static final Pattern
        _hostnameEx = Pattern.compile("^\\s*host_name"),
        _chkCmdEx   = Pattern.compile("^\\s*check_command"),
        _displayNameEx   = Pattern.compile("^\\s*display_name"),
        _descriptionEx   = Pattern.compile("^\\s*service_description"),
        _hostgroupNameEx = Pattern.compile("^\\s*hostgroup_name");
    //maps to the check_command
    private String _cmdName,
                   _displayName = "",
                   _description = "";
    private ArrayList _hostnames = new ArrayList(),
                      _hostgroups = new ArrayList();
    private Map _args = new HashMap();
    private Set _resources;
    // List of NagiosHostObjs
    private ArrayList _nagiosHostObjs;
    private NagiosCommandObj _cmdObj;

    protected NagiosServiceObj()
    {
        super();
    }

    public String getDesc()
    {
        return _description;
    }

    public List getHostObjs()
    {
        return Collections.unmodifiableList(_nagiosHostObjs);
    }

    public String getCmdLine(NagiosHostObj hostObj)
    {
        String host = hostObj.getHostname();
        String rtn = _cmdObj.getCmdExec();
        for (Iterator i=_args.entrySet().iterator(); i.hasNext(); )
        {
            Map.Entry entry = (Map.Entry)i.next();
            String arg = (String)entry.getKey();
            String val = (String)entry.getValue();
            rtn = StringUtil.replace(rtn, "$"+arg+"$", val);
        }
        for (Iterator i=_resources.iterator(); i.hasNext(); )
        {
            NagiosResourceObj obj = (NagiosResourceObj)i.next();
            String arg = obj.getKey();
            String val = obj.getValue();
            rtn = StringUtil.replace(rtn, arg, val);
        }
        rtn = StringUtil.replace(rtn, "$HOSTADDRESS$", hostObj.getAddress());
        rtn = StringUtil.replace(rtn, "$HOSTNAME$", host);
        rtn = StringUtil.replace(rtn, "$SERVICEDESC$", _description);
        return rtn;
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
            if (_hostnameEx.matcher(line).find()) {
                setHostnames(line);
            } else if (_hostgroupNameEx.matcher(line).find()) {
                setHostGroupName(line);
            } else if (_descriptionEx.matcher(line).find()) {
                setDescription(line);
            } else if (_displayNameEx.matcher(line).find()) {
                setDisplayName(line);
            } else if (_chkCmdEx.matcher(line).find()) {
                setCmd(line);
            }
        }
        if ((_hostnames.size() == 0 &&
            _hostgroups.size() == 0) ||
            _cmdName == null) {
            throw new NagiosParserException(cfgBlock);
        }
    }

    private void setCmd(String line)
    {
        line = removeInlineComments(line);
        String[] buf = line.split("\\s+");
        String tmp = join(" ", Arrays.asList(buf), 1);
        buf = tmp.split("!");
        _cmdName = buf[0];
        List list = new ArrayList(Arrays.asList(buf));
        list.remove(0);
        int i=1;
        for (Iterator it=list.iterator(); it.hasNext(); )
        {
            String name = "ARG"+i++;
            String val = (String)it.next();
            _args.put(name, val);
        }
    }

    public void populateData(String config)
    {
    }

    void resolveDependencies(NagiosParser parser)
    {
        try
        {
            if (_resources == null)
            {
                Integer type = new Integer(RESOURCE_TYPE);
                _resources = parser.get(type);
            }
            if (_cmdObj == null)
            {
                Integer type = new Integer(COMMAND_TYPE);
                _cmdObj = (NagiosCommandObj)parser.get(type, _cmdName);
            }
            if (_nagiosHostObjs == null)
            {
                _nagiosHostObjs = new ArrayList(
                    _hostgroups.size()+_hostnames.size());
                Integer type = new Integer(HOSTGROUP_TYPE);
                for (Iterator i=_hostgroups.iterator(); i.hasNext(); )
                {
                    String hostgroup = (String)i.next();
                    NagiosHostGroupObj hostgroupObj =
                        (NagiosHostGroupObj)parser.get(type, hostgroup);
                    _nagiosHostObjs.addAll(hostgroupObj.getHostObjs());
                }
                type = new Integer(HOST_TYPE);
                for (Iterator i=_hostnames.iterator(); i.hasNext(); )
                {
                    String host = (String)i.next();
                    NagiosHostObj hostObj =
                        (NagiosHostObj)parser.get(type, host);
                    _nagiosHostObjs.add(hostObj);
                }
            }
        }
        catch (NagiosParserInternalException e) {
            debug(e);
        }
        catch (NagiosTypeNotSupportedException e) {
            debug(e);
        }
    }

    private void setHostGroupName(String line)
    {
        line = removeInlineComments(line);
        String[] hostgroup = line.split("\\s+");
        hostgroup = hostgroup[hostgroup.length-1].split("\\s*,\\s*");
        _hostgroups.ensureCapacity(hostgroup.length);
        _hostgroups.addAll(Arrays.asList(hostgroup));
    }

    private void setHostnames(String line)
    {
        line = removeInlineComments(line);
        String[] hosts = line.split("\\s+");
        hosts = hosts[hosts.length-1].split("\\s*,\\s*");
        _hostnames.ensureCapacity(hosts.length);
        _hostnames.addAll(Arrays.asList(hosts));
    }

    private void setDescription(String line)
    {
        line = removeInlineComments(line);
        String[] desc = line.split("\\s+");
        _description = join(" ", Arrays.asList(desc), 1);
    }

    private void setDisplayName(String line)
    {
        line = removeInlineComments(line);
        String[] name = line.split("\\s+");
        _displayName = name[name.length-1];
    }

    public void importObject()
    {
    }

    public String getKey()
    {
        return _description;
    }

    public int getType()
    {
        return SERVICE_TYPE;
    }

    public String toString()
    {
        return "filename -> "+_filename+
               "\nServiceName -> "+_displayName+
               "\nDescrption -> "+_description+
               "\nHostnames -> "+join(",", _hostnames)+
               "\nHostgroups -> "+join(",", _hostgroups)+
               "\nCommandName -> "+_cmdName+
               "\nArgs -> "+join(" ", _args.values());
    }

    public boolean equals(Object rhs)
    {
        if (this == rhs)
            return true;
        if (rhs instanceof NagiosServiceObj)
            return equals((NagiosServiceObj)rhs);
        return false;
    }

    public int hashCode()
    {
        return _description.hashCode();
    }

    private boolean equals(NagiosServiceObj rhs)
    {
        if (rhs._cmdName.equals(_cmdName))
            return true;
        return false;
    }

    public int compareTo(Object rhs)
        throws ClassCastException
    {
        if (rhs instanceof NagiosServiceObj) {
            return _cmdName.compareTo(((NagiosServiceObj)rhs)._cmdName);
        } else {
            throw new ClassCastException();
        }
    }
}
