package org.hyperic.hq.plugin.nagios.parser;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintStream;
import java.io.Reader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

public class NagiosCfgParser implements NagiosParser
{
    private final String logCtx = this.getClass().getName();
    private static final Pattern
        _define  = Pattern.compile("^\\s*define"),
        _end     = Pattern.compile("^\\s*}"),
        _cfgFile = Pattern.compile("^\\s*cfg_file"),
        _cfgDir  = Pattern.compile("^\\s*cfg_dir"),
        _resourceFile  = Pattern.compile("^\\s*resource_file"),
        _resource = Util.RESOURCE_PATTERN;

    private Map _map = new HashMap();
    private PrintStream _debugOut;
    private List _cfgFiles = new ArrayList();

    public NagiosCfgParser()
    {
    }

    public NagiosCfgParser(PrintStream debugOut)
    {
        _debugOut = debugOut;
    }

    public Set get(Integer type)
    {
        Set set;
        if (null == (set = (Set)_map.get(type)))
            return Collections.EMPTY_SET;

        return Collections.unmodifiableSet(set);
    }

    public Set getServiceObjs()
    {
        Integer type = new Integer(NagiosObj.SERVICE_TYPE);
        return get(type);
    }

    public Set getHostObjs()
    {
        Integer type = new Integer(NagiosObj.HOST_TYPE);
        return get(type);
    }

    public NagiosObj get(Integer type, String name)
        throws NagiosParserInternalException,
               NagiosTypeNotSupportedException
    {
        Set set;
        if (null == (set = (Set)_map.get(type)))
        {
            String msg = "ERROR: could not find proper bucket with"+
                         " unique key, "+name+
                         ", and for object type "+
                         "'"+NagiosObj.getTypeName(type.intValue())+"'";
            throw new NagiosParserInternalException(msg);
        }
        for (Iterator i=set.iterator(); i.hasNext(); )
        {
            NagiosObj obj = (NagiosObj)i.next();
            String key = obj.getKey();
            if (name.hashCode() == key.hashCode() &&
                obj.getKey().equals(key)) {
                return obj;
            }
        }
        String msg = "ERROR: could not find nagios object"+
                     " name, "+name+", and object type "+
                     "'"+NagiosObj.getTypeName(type.intValue())+"'";
        throw new NagiosParserInternalException(msg);
    }

    private void resolveDependencies()
    {
        Set set;
        Integer type = new Integer(NagiosObj.HOSTGROUP_TYPE);
        if (null != (set = (Set)_map.get(type)))
        {
            for (Iterator i=set.iterator(); i.hasNext(); )
            {
                NagiosHostGroupObj obj = (NagiosHostGroupObj)i.next();
                obj.resolveMembers(this);
                Util.debug(_debugOut, obj+"\n");
            }
        }

        Util.debug(_debugOut, "!RESOLVING SERVICES!");
        type = new Integer(NagiosObj.SERVICE_TYPE);
        if (null != (set = (Set)_map.get(type)))
        {
            for (Iterator i=set.iterator(); i.hasNext(); )
            {
                NagiosServiceObj obj = (NagiosServiceObj)i.next();
                obj.resolveDependencies(this);
                Util.debug(_debugOut, obj+"\n");
            }
        }

        Util.debug(_debugOut, "!RESOLVING HOST TEMPLATES!");
        type = new Integer(NagiosObj.HOST_TEMPL_TYPE);
        if (null != (set = (Set)_map.get(type)))
        {
            for (Iterator i=set.iterator(); i.hasNext(); )
            {
                NagiosTemplateHostObj obj = (NagiosTemplateHostObj)i.next();
                obj.resolveDependencies(this);
                Util.debug(_debugOut, obj+"\n");
            }
        }

        Util.debug(_debugOut, "!RESOLVING HOSTS!");
        type = new Integer(NagiosObj.HOST_TYPE);
        if (null != (set = (Set)_map.get(type)))
        {
            for (Iterator i=set.iterator(); i.hasNext(); )
            {
                NagiosHostObj obj = (NagiosHostObj)i.next();
                obj.resolveDependencies(this);
                Util.debug(_debugOut, obj+"\n");
            }
        }
    }

    public void parse(String file)
        throws IOException
    {
        setNagiosFiles(file);
        parseNagiosCfg();
    }

    private void parseNagiosCfg()
        throws IOException
    {
        BufferedReader reader = null;
        for (Iterator i=_cfgFiles.iterator(); i.hasNext(); )
        {
            try
            {
                File fileObj = (File)i.next();
                if (fileObj.isDirectory())
                    continue;
                String file = fileObj.getPath();
                Util.debug(_debugOut, "!PARSING CFG FILE! "+file);
                reader = new BufferedReader(new FileReader(file));
                readCfgFile(reader, file);
            }
            finally {
                closeReader(reader, logCtx);
            }
        }
        resolveDependencies();
    }

    private void readCfgFile(BufferedReader reader, String file)
        throws IOException
    {
        String line;
        String lines = null;
        while (null != (line = reader.readLine()))
        {
            try
            {
                if (_define.matcher(line).find())
                {
                    Util.debug(_debugOut, line+"\n");
                    lines = readObj(line, reader);
                    NagiosObj obj = NagiosObj.getObject(line, lines,
                                                        file, _debugOut);
                    if (obj != null) {
                        Util.debug(_debugOut, obj+"\n");
                        addNagiosObj(obj);
                    }
                    else
                        Util.debug(_debugOut, "!Could not parse! -> "+lines);
                }
                else if (_resource.matcher(line).find())
                {
                    Util.debug(_debugOut, line+"\n");
                    NagiosObj obj = NagiosObj.getObject(line, line,
                                                        file, _debugOut);
                    if (obj != null) {
                        Util.debug(_debugOut, obj+"\n");
                        addNagiosObj(obj);
                    }
                    else
                        Util.debug(_debugOut, "!Could not parse! -> "+lines);
                }
            }
            catch (NagiosParserException e) {
                Util.debug(_debugOut, e);
            }
            catch (NagiosTypeNotSupportedException e) {
                Util.debug(_debugOut, e);
            }
        }
    }

    private String readObj(String firstLine, BufferedReader reader)
        throws IOException
    {
        String line;
        Util.debug(_debugOut, "!FIRST LINE! -> "+firstLine);
        StringBuffer rtn = new StringBuffer();
        while (null != (line = reader.readLine()))
        {
            Util.debug(_debugOut, "!LINE! -> "+line);
            if (_end.matcher(line).find()) {
                return rtn.toString();
            }
            rtn.append(line).append("\n");
        }
        return rtn.toString();
    }

    private void addNagiosObj(NagiosObj obj)
    {
        Set set;
        Integer type = new Integer(obj.getType());
        if (null == (set = (Set)_map.get(type)))
        {
            set = new HashSet();
            set.add(obj);
            _map.put(type, set);
        }
        else {
            set.add(obj);
        }
    }

    public static void main(String[] args)
        throws IOException
    {
        NagiosCfgParser p = new NagiosCfgParser(System.out);
        String file = "/usr/local/nagios/etc/nagios.cfg";
//        String file = "/usr/local/nagios/etc/nagios-master.cfg";
        p.parse(file);
        p.printHostCmds();
    }

    public void printHostCmds()
    {
        Set set;
        Integer type = new Integer(NagiosObj.SERVICE_TYPE);
        if (null != (set = (Set)_map.get(type)))
        {
            for (Iterator i=set.iterator(); i.hasNext(); )
            {
                NagiosServiceObj service = (NagiosServiceObj)i.next();
                List list = service.getHostObjs();
                Util.debug(_debugOut, "service -> "+service.getDesc());
                for (Iterator it=list.iterator(); it.hasNext(); )
                {
                    NagiosHostObj hostObj = (NagiosHostObj)it.next();
                    Util.debug(_debugOut, "host -> "+hostObj.getHostname());
                    Util.debug(_debugOut, service.getCmdLine(hostObj));
                }
            }
        }

        type = new Integer(NagiosObj.HOST_TYPE);
        if (null == (set = (Set)_map.get(type)))
            return;

        for (Iterator i=set.iterator(); i.hasNext(); )
        {
            NagiosHostObj host = (NagiosHostObj)i.next();
            Util.debug(_debugOut, "host -> "+host.getHostname());
            Util.debug(_debugOut, host.getChkAliveCmd());
        }
    }

    public void setNagiosFiles(String nagiosCfg)
        throws IOException
    {
        _cfgFiles.clear();
        String line;
        BufferedReader reader = null;
        try
        {
            reader = new BufferedReader(new FileReader(nagiosCfg));
            while (null != (line = reader.readLine()))
            {
                Util.debug(_debugOut, "!LINE! -> "+line);
                if (_cfgFile.matcher(line).find())
                {
                    String[] toks = line.split("\\=");
                    if (toks.length < 2)
                        continue;
                    String file = toks[1].trim();
                    if (!file.endsWith(".cfg"))
                        continue;
                    _cfgFiles.add(new File(file));
                }
                else if (_cfgDir.matcher(line).find())
                {
                    String[] toks = line.split("\\=");
                    if (toks.length < 2)
                        continue;
                    File dir = new File(toks[1]);
                    List list = Arrays.asList(dir.listFiles());
                    _cfgFiles.addAll(getValidFiles(list));
                }
                else if (_resourceFile.matcher(line).find())
                {
                    String[] toks = line.split("\\=");
                    if (toks.length < 2)
                        continue;
                    String file = toks[1];
                    _cfgFiles.add(new File(file.trim()));
                }
            }
        }
        finally {
            closeReader(reader, logCtx);
        }
    }

    private List getValidFiles(List list)
    {
        List rtn = new ArrayList();
        for (Iterator i=list.iterator(); i.hasNext(); )
        {
            File file = (File)i.next();
            String name = file.getName();
            if (name.endsWith(".cfg"))
                rtn.add(file);
        }
        return rtn;
    }

    private void closeReader(Reader reader, String logCtx)
    {
        if (reader == null)
            return;
        try {
            reader.close();
        }
        catch (IOException e) {
        }
    }
}
