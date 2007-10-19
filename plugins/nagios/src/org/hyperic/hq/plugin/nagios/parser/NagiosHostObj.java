package org.hyperic.hq.plugin.nagios.parser;

import java.util.regex.Pattern;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.net.InetAddress;
import java.net.UnknownHostException;

public class NagiosHostObj
    extends NagiosObj
{
    private static final String inet = "(([0-9]{1,3})\\.){3}([0-9]{1,3})";
    private static final Pattern _aliasEx = Pattern.compile("^\\s*alias"),
                                 _useEx = Pattern.compile("^\\s*use"),
                                 _hostnameEx = Pattern.compile("^\\s*host_name"),
                                 _nameEx = Pattern.compile("^\\s*name"),
                                 _addressEx = Pattern.compile("^\\s*address"),
                                 _contactsEx = Pattern.compile("^\\s*contacts"),
                                 _contactGroupsEx =
                                    Pattern.compile("^\\s*contact_groups"),
                                 _inetAddrEx = Pattern.compile(inet);
    private List _contactGroups,
                 _contacts;

    private String _alias,
                   _name,
                   _hostname,
                   _address;

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
            } else if (_useEx.matcher(line).find()) {
                setInheritAttrs(line);
            } else if (_nameEx.matcher(line).find()) {
                setName(line);
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

    private void setInheritAttrs(String line)
    {
        Set set;
//        if (null == (set = (Set)_allNagiosObjs.get(new Integer(getType()))))
//            return;
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

    private void setName(String line)
    {
        line = removeInlineComments(line);
        String[] name = line.split("\\s+");
        _name = name[name.length-1];
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
//        String[] contacts = line.replaceAll("\\s+$", "").trim().split(",");
        String[] contacts = line.trim().split(",");
        _contacts = Arrays.asList(contacts);
    }

    private void setContactGroups(String line)
    {
//        String[] contacts = line.replaceAll("\\s+$", "").trim().split(",");
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
               "\nName -> "+_name+
               "\nAlias -> "+_alias+
               "\nAddr  -> "+_address+
               "\nContacts  -> "+_contacts+
               "\nContactGroups  -> "+_contactGroups;
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
        if (_name == null)
            return _hostname.hashCode();
        return _name.hashCode();
    }

    void resolveDependencies(NagiosParser parser)
    {
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
