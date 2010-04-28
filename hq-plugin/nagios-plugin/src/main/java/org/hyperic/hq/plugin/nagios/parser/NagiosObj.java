/*
 * NOTE: This copyright does *not* cover user programs that use HQ
 * program services by normal system calls through the application
 * program interfaces provided as part of the Hyperic Plug-in Development
 * Kit or the Hyperic Client Development Kit - this is merely considered
 * normal use of the program, and does *not* fall under the heading of
 * "derived work".
 * 
 * Copyright (C) [2004-2007], Hyperic, Inc.
 * This file is part of HQ.
 * 
 * HQ is free software; you can redistribute it and/or modify
 * it under the terms version 2 of the GNU General Public License as
 * published by the Free Software Foundation. This program is distributed
 * in the hope that it will be useful, but WITHOUT ANY WARRANTY; without
 * even the implied warranty of MERCHANTABILITY or FITNESS FOR A
 * PARTICULAR PURPOSE. See the GNU General Public License for more
 * details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307
 * USA.
 */

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

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

public abstract class NagiosObj
{
    protected String logCtx = getClass().getName();

    protected Log _log = LogFactory.getLog(logCtx);

    static final int HOST_TYPE    = 0,
                     SERVICE_TYPE = 1,
                     COMMAND_TYPE = 2,
                     CONTACT_TYPE = 3,
                     CONTACTGROUP_TYPE = 4,
                     HOSTGROUP_TYPE    = 5,
                     HOSTGROUPESCALATION_TYPE = 6,
                     SERVICEDEPENDENCY_TYPE   = 7,
                     SERVICESCALATION_TYPE    = 8,
                     TIMEPERIOD_TYPE          = 9,
                     RESOURCE_TYPE            = 10,
                     HOST_TEMPL_TYPE          = 11;

    private static final Pattern
        _host      = Pattern.compile("\\s+host\\s*\\{"),
        _service   = Pattern.compile("\\s+service\\s*\\{"),
        _hostgroup = Pattern.compile("\\s+hostgroup\\s*\\{"),
        _command   = Pattern.compile("\\s+command\\s*\\{"),
        _resource  = Util.RESOURCE_PATTERN;

    protected static final Pattern _comment   = Pattern.compile("^\\s*#"),
                                   _blankLine = Pattern.compile("^\\s*$");

    protected PrintStream _debugOut;
    protected String _filename;

    protected NagiosObj()
    {
    }

    public abstract String toString();
    public abstract String getKey();
    public abstract int hashCode();
    public abstract int getType();

    abstract void resolveDependencies(NagiosParser parser);

    protected abstract void parseCfg(String cfgBlock)
        throws NagiosParserException;

    public void setDebugInfo(String filename, PrintStream debugOut)
    {
        _filename = filename;
        _debugOut = debugOut;
    }

    static int getObjectType(String line)
        throws NagiosTypeNotSupportedException
    {
        if (_host.matcher(line).find()) {
            return HOST_TYPE;
        } else if (_service.matcher(line).find()) {
            return SERVICE_TYPE;
        } else if (_command.matcher(line).find()) {
            return COMMAND_TYPE;
        } else if (_resource.matcher(line).find()) {
            return RESOURCE_TYPE;
        } else if (_hostgroup.matcher(line).find()) {
            return HOSTGROUP_TYPE;
        }
        throw new NagiosTypeNotSupportedException("Type not supported for "+
                                                  line);
    }

    public static final String getTypeName(int type)
        throws NagiosTypeNotSupportedException
    {
        switch (type)
        {
            case HOST_TYPE:
                return "Host Type";
            case SERVICE_TYPE:
                return "Service Type";
            case COMMAND_TYPE:
                return "Command Type";
            case HOSTGROUP_TYPE:
                return "HostGroup Type";
            case RESOURCE_TYPE:
                return "Resource Type";
            default:
                throw new NagiosTypeNotSupportedException("Type not supported for "+
                                                          "numeric "+type);
        }
    }

    private static NagiosObj getHostObj(String cfgLines,
                                 String filename,
                                 PrintStream _debugOut)
        throws NagiosParserException
    {
        NagiosObj rtn;
        try
        {
            rtn = new NagiosHostObj();
            rtn.setDebugInfo(filename, _debugOut);
            rtn.parseCfg(cfgLines);
        }
        catch (NagiosParserException e)
        {
            rtn = new NagiosTemplateHostObj();
            rtn.setDebugInfo(filename, _debugOut);
            rtn.parseCfg(cfgLines);
        }
        return rtn;
    }

    public static final NagiosObj getObject(String firstLine,
                                            String cfgLines,
                                            String filename,
                                            PrintStream _debugOut)
        throws NagiosParserException,
               NagiosTypeNotSupportedException,
               UnknownHostException
    {
        NagiosObj rtn;
        int objType = getObjectType(firstLine);
        switch (objType)
        {
            case HOST_TYPE:
                rtn = getHostObj(cfgLines, filename, _debugOut);
                break;
            case SERVICE_TYPE:
                rtn = new NagiosServiceObj();
                rtn.setDebugInfo(filename, _debugOut);
                rtn.parseCfg(cfgLines);
                break;
            case COMMAND_TYPE:
                rtn = new NagiosCommandObj();
                rtn.setDebugInfo(filename, _debugOut);
                rtn.parseCfg(cfgLines);
                break;
            case HOSTGROUP_TYPE:
                rtn = new NagiosHostGroupObj();
                rtn.setDebugInfo(filename, _debugOut);
                rtn.parseCfg(cfgLines);
                break;
            case RESOURCE_TYPE:
                rtn = new NagiosResourceObj();
                rtn.setDebugInfo(filename, _debugOut);
                rtn.parseCfg(cfgLines);
                break;
            default:
                throw new NagiosTypeNotSupportedException();
        }
        return rtn;
    }

    protected String removeInlineComments(String nameValue)
    {
        return nameValue.trim().replaceAll("\\s*;.*$", "");
    }

    protected void debug(String buf, Throwable e)
    {
        if (_debugOut != null)
            Util.debug(_debugOut, buf, e);
        else
            _log.debug(buf, e);
    }

    protected void debug(Throwable e)
    {
        if (_debugOut != null)
            Util.debug(_debugOut, null, e);
        else
            _log.debug(e);
    }

    protected void debug(String buf)
    {
        if (_debugOut != null)
            Util.debug(_debugOut, buf);
        else
            _log.debug(buf);
    }

    protected String join(String delim, List l, int start)
    {
        if (l.size() == 0 || start >= l.size()) {
            return "";
        }
        StringBuffer buf = new StringBuffer();
        for (int i=start; i<l.size(); i++) {
            buf.append(l.get(i).toString()).append(delim);
        }
        return buf.substring(0, buf.length()-1).toString();
    }

    protected String join(String delim, Collection c)
    {
        return join(delim, new ArrayList(c), 0);
    }

    protected String join(String delim, List l)
    {
        return join(delim, l, 0);
    }
}
