/*
 * NOTE: This copyright does *not* cover user programs that use HQ
 * program services by normal system calls through the application
 * program interfaces provided as part of the Hyperic Plug-in Development
 * Kit or the Hyperic Client Development Kit - this is merely considered
 * normal use of the program, and does *not* fall under the heading of
 * "derived work".
 * 
 * Copyright (C) [2004, 2005, 2006], Hyperic, Inc.
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

package org.hyperic.hq.plugin.nagios;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.Reader;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.hyperic.util.StringUtil;

public class NagiosConfig {

    private static final String[] INHERIT_ATTRS = {
        "service_description", "check_command"
    };

    private static Log log =
        LogFactory.getLog(NagiosConfig.class.getName());

    private String configFile;

    public NagiosConfig(String config) {
        this.configFile  = config;
    }

    class Service {
        String name;
        String cmd;
        String args;

        public String toString() {
            return
                "name=" + this.name +
                ", cmd=" + this.cmd + 
                ", args=" + this.args;
        }
    }

    private interface ConfigReader {
        public boolean processLine(String line);
    }

    private class AttributeReader implements ConfigReader {
        Map attrs = new HashMap();

        public boolean processLine(String line) {
            int ix = line.indexOf('=');
            if (ix == -1) {
                return true;
            }
            String key = line.substring(0, ix).trim();
            String val = line.substring(ix+1).trim();
            handleAttribute(key, val);
            return true;
        }

        protected void handleAttribute(String key, String val) {
            this.attrs.put(key, val);
        }
    }

    private FilenameFilter cfgFilter = new FilenameFilter() {
        public boolean accept(File base, String name) {
            return name.endsWith(".cfg");
        }
    };
    
    private class MainReader extends AttributeReader {
        AttributeReader resources =
            new AttributeReader();

        GroupConfigReader groups =
            new GroupConfigReader();

        protected void handleAttribute(String key, String val) {
            ConfigReader reader;

            if (key.equals("cfg_file")) {
                reader = groups;
            }
            else if (key.equals("cfg_dir")) {
                //read $cfg_dir/*.cfg
                String[] configs =
                    new File(val).list(cfgFilter);
                if (configs == null) {
                    log.warn("No .cfg files in cfg_dir=" + val);
                    return;
                }
                for (int i=0; i<configs.length; i++) {
                    String file =
                        val + File.separator + configs[i];

                    readFile(file, groups);
                }
                return;
            }
            else if (key.equals("resource_file")) {
                reader = resources;
            }
            else {
                return;
            }

            readFile(val, reader);
        }
    }

    private class GroupConfigReader implements ConfigReader {
        private static final int TYPE_COMMANDS = 1;
        private static final int TYPE_SERVICES = 2;
        private static final int TYPE_HOSTS = 3;
        private static final int TYPE_HOST_GROUPS = 4;

        List commands = new ArrayList();
        List services = new ArrayList();
        Map service_templates = new HashMap();
        Map hosts = new HashMap();
        Map hostgroups = new HashMap();

        private Map attrs = null;
        private boolean inDefine = false;
        private int type;
        
        private void addServices(String host) {
            Map template =
                (Map)this.service_templates.get(this.attrs.get("use"));

            if (template != null) {
                for (int i=0; i<INHERIT_ATTRS.length; i++) {
                    String key = INHERIT_ATTRS[i];
                    if (this.attrs.get(key) == null) {
                        //inhert attribute from the template
                        this.attrs.put(key, template.get(key));
                    }
                }
            }

            //one or more hosts
            List hosts = StringUtil.explode(host, ",");
            for (int i=0; i<hosts.size(); i++) {
                host = (String)hosts.get(i);
                HashMap service = new HashMap(this.attrs.size()+1);
                service.putAll(this.attrs);
                service.put("host_name", host);
                this.services.add(service);
            }            
        }
        
        private void addServices() {
            String register = (String)this.attrs.get("register");
            if ("0".equals(register)) {
                //template
                this.service_templates.put(this.attrs.get("name"),
                                           this.attrs);
                return;
            }
            String host = (String)this.attrs.get("host_name");
            if (host != null) {
                addServices(host);
            }
            String group = (String)this.attrs.get("hostgroup_name");
            if (group != null) {
                //one or more group names
                List groups = StringUtil.explode(group, ",");
                for (int i=0; i<groups.size(); i++) {
                    group = (String)groups.get(i);
                    String members = (String)this.hostgroups.get(group);
                    if (members != null) {
                        addServices(members);
                    }
                    else {
                        log.debug("No members found for hostgroup_name=" + group);
                    }
                }
            }
        }

        public boolean processLine(String line) {
            if (line.startsWith("define ")) {
                line = line.substring(6).trim();
                int ix = line.indexOf('{');
                if (ix == -1) {
                    return false;
                }
                line = line.substring(0, ix).trim();
                if (line.equals("command")) {
                    this.type = TYPE_COMMANDS;
                }
                else if (line.equals("service")) {
                    this.type = TYPE_SERVICES;
                }
                else if (line.equals("hostgroup")) {
                    this.type = TYPE_HOST_GROUPS;
                }
                else if (line.equals("host")) {
                    this.type = TYPE_HOSTS;
                }
                else {
                    //we dont care about this file, stop reading.
                    return false;
                }

                this.inDefine = true;
                this.attrs = new HashMap();
                return true;
            }
            else if (line.charAt(0) == '}') {
                this.inDefine = false;
                switch (this.type) {
                  case TYPE_COMMANDS:
                    this.commands.add(attrs);
                    break;
                  case TYPE_SERVICES:
                    addServices();
                    break;
                  case TYPE_HOSTS:
                    this.hosts.put(this.attrs.get("host_name"),
                                   this.attrs.get("address"));
                    break;
                  case TYPE_HOST_GROUPS:
                    this.hostgroups.put(this.attrs.get("hostgroup_name"),
                                        this.attrs.get("members"));
                    break;
                }

                return true;
            }

            if (inDefine) {
                int ix = 0;
                int len = line.length();

                while (!Character.isWhitespace(line.charAt(ix)))
                {
                    if (++ix > len) {
                        return true;
                    }
                }

                String key = line.substring(0, ix).trim();
                String val = line.substring(ix).trim();
                this.attrs.put(key, val);
            }

            return true;
        }
    }

    public List parse()
        throws IOException {

        List services = new ArrayList();

        //start with etc/nagios.cfg
        //from there parse resource_config and any cfg_file
        MainReader main = new MainReader();
        parseFile(this.configFile, main);

        Map commandAliases = new HashMap();
        for (Iterator it=main.groups.commands.iterator();
             it.hasNext();)
        {
            Map cmd = (Map)it.next();
            String name = (String)cmd.get("command_name");
            String line = (String)cmd.get("command_line");

            for (Iterator vars=main.resources.attrs.entrySet().iterator();
                 vars.hasNext();)
            {
                Map.Entry entry = (Map.Entry)vars.next();
                String key = (String)entry.getKey();
                String val = (String)entry.getValue();
                line = StringUtil.replace(line, key, val);
            }

            commandAliases.put(name, line);
        }

        for (Iterator it=main.groups.services.iterator();
             it.hasNext();)
        {
            Map cfg = (Map)it.next();
            String host = (String)cfg.get("host_name");
            String cmd  = (String)cfg.get("check_command");
            String desc = (String)cfg.get("service_description");

            if ((host == null) ||
                (cmd == null) ||
                (desc == null))
            {
                log.warn("Invalid service entry: " + cfg);
                continue;
            }

            Service service = new Service();
            service.name = desc + " " + host;

            List args = StringUtil.explode(cmd, "!");
            String alias = (String)args.remove(0);
            cmd = (String)commandAliases.get(alias);
            if (cmd == null) {
                cmd = alias;
            }

            String address =
                (String)main.groups.hosts.get(host);
            if (address == null) {
                address = host;
            }

            //XXX there are more of these:
            //http://nagios.sourceforge.net/docs/1_0/macros.html
            cmd = StringUtil.replace(cmd, "$HOSTADDRESS$", address);
            cmd = StringUtil.replace(cmd, "$HOSTNAME$", host);
            cmd = StringUtil.replace(cmd, "$SERVICEDESC$", desc);

            for (int i=0; i<args.size(); i++) {
                String var = "$ARG" + (i+1) + "$";
                cmd = StringUtil.replace(cmd, var, (String)args.get(i));
            }

            //XXX whatif path has a space in it
            int ix = cmd.indexOf(' ');
            if (ix == -1) {
                service.cmd = cmd;
            }
            else {
                service.cmd = cmd.substring(0, ix);
                service.args = cmd.substring(ix+1);
            }

            services.add(service);
        }

        return services;
    }

    private void readFile(String configFile, ConfigReader config) {
        try {
            parseFile(configFile, config);
        } catch (FileNotFoundException e) {
            log.error("File '" + configFile + "' does not exist");
        } catch (IOException e) {
            log.error("Error parsing " + configFile, e);
        }    
    }
    
    private void parseFile(String configFile, ConfigReader config)
        throws IOException {

        log.debug("Parsing " + configFile);
        Reader reader = null;

        try {
            reader = new FileReader(configFile);
            BufferedReader buffer =
                new BufferedReader(reader);
            String line;

            while ((line = buffer.readLine()) != null) {
                line = line.trim();
                if ((line.length() == 0) ||
                    (line.charAt(0) == '#'))
                {
                    continue;
                }
                int ix = line.indexOf(';');
                if (ix != -1) {
                    line = line.substring(0, ix).trim();
                }
                if (line.length() == 0) {
                    continue;
                }
                if (!config.processLine(line)) {
                    break;
                }
            }
        } finally {
            if (reader != null) {
                try {
                    reader.close();
                } catch (IOException e) {}
            }
        }
    }

    public static void main(String[] args) throws Exception {
        NagiosConfig config = new NagiosConfig(args[0]);
        List services = config.parse();
        for (int i=0; i<services.size(); i++) {
            Service service = (Service)services.get(i);
            System.out.println("------------------------------");
            System.out.println("name=" + service.name);
            System.out.println("cmd=" + service.cmd);
            System.out.println("args=" + service.args);
        }

        System.out.println(services.size() + " total services");
    }
}
