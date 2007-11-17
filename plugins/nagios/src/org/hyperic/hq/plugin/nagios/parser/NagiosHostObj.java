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
import java.net.InetAddress;
import java.net.UnknownHostException;
import org.hyperic.util.StringUtil;

public class NagiosHostObj
    extends NagiosObj
{
    private static final String inet = "(([0-9]{1,3})\\.){3}([0-9]{1,3})";
    private static final Pattern _aliasEx = Pattern.compile("^\\s*alias"),
                                 _useEx = Pattern.compile("^\\s*use"),
                                 _hostnameEx = Pattern.compile("^\\s*host_name"),
                                 _checkCmdEx = Pattern.compile("^\\s*check_command"),
                                 _addressEx = Pattern.compile("^\\s*address"),
                                 _contactsEx = Pattern.compile("^\\s*contacts"),
                                 _contactGroupsEx =
                                    Pattern.compile("^\\s*contact_groups"),
                                 _inetAddrEx = Pattern.compile(inet);
    private List _contactGroups,
                 _contacts;

    private Map _args = new HashMap();
    private Set _resources;

    private String _alias,
                   _use,
                   _hostname,
                   _cmdName,
                   _address;

    NagiosCommandObj _cmdObj;

    protected NagiosHostObj()
    {
        super();
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
            if (_aliasEx.matcher(line).find()) {
                setAlias(line);
            } else if (_checkCmdEx.matcher(line).find()) {
                setCheckCmd(line);
            } else if (_useEx.matcher(line).find()) {
                setTemplateHost(line);
            } else if (_hostnameEx.matcher(line).find()) {
                setHostname(line);
            } else if (_addressEx.matcher(line).find()) {
                setAddress(line);
            } else if (_contactsEx.matcher(line).find()) {
                setContacts(line);
            } else if (_contactGroupsEx.matcher(line).find()) {
                setContactGroups(line);
            }
        }
        if (_alias == null || _address == null)
        {
            debug("ERROR:  config -> " +cfgBlock);
            throw new NagiosParserException(cfgBlock);
        }
    }

    private void setTemplateHost(String line)
    {
        line = removeInlineComments(line);
        String[] use = line.split("\\s+");
        _use = use[use.length-1];
    }

    public String getAddress()
    {
        return _address;
    }

    public String getKey()
    {
        return _hostname;
    }

    public String getHostname()
    {
        return _hostname;
    }

    private void setCheckCmd(String line)
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
        // Nagios can support ARG(n), n being 1-32
        // This must be accounted for even if there is no value
        for ( ; i<=32; i++)
        {
            String name = "ARG"+i;
            _args.put(name, "");
        }
    }

    private void setHostname(String line)
    {
        line = removeInlineComments(line);
        String[] host = line.split("\\s+");
        _hostname = host[host.length-1];
    }

    private void setAlias(String line)
    {
        line = removeInlineComments(line);
        String[] alias = line.split("\\s+");
        _alias = alias[alias.length-1];
    }

    private void setAddress(String line)
    {
        line = removeInlineComments(line);
        String[] addr = line.split("\\s+");
        _address = addr[addr.length-1];
    }

    private boolean isIPAddr(String ip)
    {
        if (_inetAddrEx.matcher(ip).matches())
            return true;
        return false;
    }

    private byte[] getBytes(String ipAddr)
    {
        byte[] rtn = new byte[4];
        String[] addr = ipAddr.split("\\.");
        rtn[0] = (byte)Integer.parseInt(addr[0]);
        rtn[1] = (byte)Integer.parseInt(addr[1]);
        rtn[2] = (byte)Integer.parseInt(addr[2]);
        rtn[3] = (byte)Integer.parseInt(addr[3]);
        return rtn;
    }

    private void setContacts(String line)
    {
        String[] contacts = line.trim().split(",");
        _contacts = Arrays.asList(contacts);
    }

    private void setContactGroups(String line)
    {
        String[] contacts = line.trim().split(",");
        _contactGroups = Arrays.asList(contacts);
    }

    public void importObject()
    {
    }

    public int getType()
    {
        return HOST_TYPE;
    }

    public void populateData(String config)
    {
    }

    public String toString()
    {
        return "Hostname -> "+_hostname+
               "\nAlias -> "+_alias+
               "\nAddr  -> "+_address+
               "\nContacts -> "+_contacts+
               "\nCmdObj   -> "+_cmdObj+
               "\nContactGroups -> "+_contactGroups;
    }

    public boolean equals(Object rhs)
    {
        if (this == rhs)
            return true;
        if (rhs instanceof NagiosHostObj)
            return equals((NagiosHostObj)rhs);
        return false;
    }

    public int hashCode()
    {
        return _hostname.hashCode();
    }

    void resolveDependencies(NagiosParser parser)
    {
        resolveInheritDependencies(parser);
        resolveCmdDependencies(parser);
        resolveResourceDependencies(parser);
    }

    private void resolveResourceDependencies(NagiosParser parser)
    {
        if (_resources == null)
        {
            Integer type = new Integer(RESOURCE_TYPE);
            _resources = parser.get(type);
        }
    }

    private void resolveInheritDependencies(NagiosParser parser)
    {
        try
        {
            if (_cmdName == null)
            {
                Integer type = new Integer(HOST_TEMPL_TYPE);
                NagiosTemplateHostObj obj =
                    (NagiosTemplateHostObj)parser.get(type, _use);
                _cmdName = obj.getCmdName();
            }
        }
        catch (NagiosParserInternalException e) {
            debug(e);
        }
        catch (NagiosTypeNotSupportedException e) {
            debug(e);
        }
    }

    private void resolveCmdDependencies(NagiosParser parser)
    {
        try
        {
            if (_cmdObj == null)
            {
                Integer type = new Integer(COMMAND_TYPE);
                _cmdObj = (NagiosCommandObj)parser.get(type, _cmdName);
            }
        }
        catch (NagiosParserInternalException e) {
            debug(e);
        }
        catch (NagiosTypeNotSupportedException e) {
            debug(e);
        }
    }

    public String getChkAliveCmd()
    {
        if (_cmdObj == null)
            return null;

        String host = getHostname();
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
        rtn = StringUtil.replace(rtn, "$HOSTADDRESS$", getAddress());
        rtn = StringUtil.replace(rtn, "$HOSTNAME$", host);
        rtn = StringUtil.replace(rtn, "$SERVICEDESC$", "");
        return rtn;
    }

    private boolean equals(NagiosHostObj rhs)
    {
        if (rhs._hostname.equals(_hostname))
            return true;
        return false;
    }

    public int compareTo(Object rhs)
        throws ClassCastException
    {
        if (rhs instanceof NagiosHostObj) {
            return _hostname.compareTo(((NagiosHostObj)rhs)._hostname);
        } else {
            throw new ClassCastException();
        }
    }
}
