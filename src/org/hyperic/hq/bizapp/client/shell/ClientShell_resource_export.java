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

package org.hyperic.hq.bizapp.client.shell;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.util.Set;
import java.util.Iterator;

import org.hyperic.hq.appdef.shared.AgentValue;
import org.hyperic.hq.appdef.shared.AppdefEntityConstants;
import org.hyperic.hq.appdef.shared.AppdefEntityID;
import org.hyperic.hq.appdef.shared.AppdefGroupValue;
import org.hyperic.hq.appdef.shared.AppdefResourceTypeValue;
import org.hyperic.hq.appdef.shared.AppdefResourceValue;
import org.hyperic.hq.appdef.shared.AppdefUtil;
import org.hyperic.hq.appdef.shared.ApplicationValue;
import org.hyperic.hq.appdef.shared.IpValue;
import org.hyperic.hq.appdef.shared.PlatformTypeValue;
import org.hyperic.hq.appdef.shared.PlatformValue;
import org.hyperic.hq.appdef.shared.ServerTypeValue;
import org.hyperic.hq.appdef.shared.ServerValue;
import org.hyperic.hq.appdef.shared.ServiceTypeValue;
import org.hyperic.hq.appdef.shared.ServiceValue;
import org.hyperic.hq.appdef.ConfigResponseDB;
import org.hyperic.hq.bizapp.shared.AppdefBoss;
import org.hyperic.hq.bizapp.shared.MeasurementBoss;
import org.hyperic.hq.bizapp.shared.ProductBoss;
import org.hyperic.hq.bizapp.shared.resourceImport.BatchImportData;
import org.hyperic.hq.bizapp.shared.resourceImport.Parser;
import org.hyperic.hq.bizapp.shared.resourceImport.Validator;
import org.hyperic.hq.grouping.shared.GroupEntry;
import org.hyperic.hq.measurement.shared.DerivedMeasurementValue;

import org.hyperic.util.StringUtil;
import org.hyperic.util.config.ConfigResponse;
import org.hyperic.util.pager.PageControl;
import org.hyperic.util.pager.PageList;
import org.hyperic.util.shell.ShellCommandBase;
import org.hyperic.util.shell.ShellCommandExecException;
import org.hyperic.util.shell.ShellCommandUsageException;

public class ClientShell_resource_export
    extends ShellCommandBase 
{
    private static final int INDENT = 2;

    private ClientShellAuthenticator auth;
    private ClientShell shell;

    private AppdefBoss aBoss;
    private MeasurementBoss mBoss;
    private ProductBoss pBoss;

    private PrintStream out, err;

    public ClientShell_resource_export(ClientShell shell) {
        this.shell = shell;
        this.auth = shell.getAuthenticator();
    }

    public void processCommand(String[] args)
        throws ShellCommandUsageException, ShellCommandExecException
    {
        BatchImportData batchData;
        FileOutputStream os = null;
        PrintStream ps = null;
        File exportFile;

        if (args.length != 1) {
            throw new ShellCommandUsageException(this.getSyntax());
        }

        exportFile = new File(args[0]);
        
        if (exportFile.exists()) {
            String overwrite;
            try {
                overwrite = super.getShell().
                    getInput("Export file already exists.  " +
                             "Overwrite (Y/N)? ");
            } catch (IOException e) {
                this.getErrStream().println("Error reading input: " + e);
                return;
            }
                
            if (overwrite.toUpperCase().startsWith("N")) {
                return;
            }
        }

        try {
            this.aBoss = shell.getBossManager().getAppdefBoss();
            this.mBoss = shell.getBossManager().getMeasurementBoss();
            this.pBoss = shell.getBossManager().getProductBoss();
        } catch (Exception e) {
            // Any exception spells doom
            throw new ShellCommandExecException("Error obtaining bosses: ", e);
        }

        this.out = getOutStream();
        this.err = getErrStream();

        try {
            os = new FileOutputStream(exportFile);
            ps = new PrintStream(os);

            out.println("Exporting HQ inventory to '" + args[0] + "' ... ");

            ps.println("<hq>");
            exportPlatforms(ps);
            exportApplications(ps);
            exportGroups(ps);
            ps.println("</hq>");

            // Validate the configuration
            this.out.print("Validating '"  + args[0] + "' ... ");
            batchData = Parser.parse(exportFile);
            Validator.validate(batchData);
            this.out.println("Done");
            
        } catch (Exception e) {
            // Any exception spells doom
            out.println();
            this.err.println("Error writing export file: " + e.getMessage());
            if(this.shell.isDeveloper()){
                e.printStackTrace(this.err);
            }
            return;
        } finally {
            if (ps != null)
                ps.close();
            try {
                if (os != null) os.close();
            } catch (IOException ignore) {}
        }
    }

    private String print(String prop) {
        return prop == null ? "" : prop;
    }

    private void exportGroups(PrintStream ps)
        throws Exception
    {
        PageList groups = this.aBoss.findAllGroups(auth.getAuthToken(),
                                                   PageControl.PAGE_ALL);
        
        for (Iterator i=groups.iterator(); i.hasNext();) {
            AppdefGroupValue group = (AppdefGroupValue)i.next();
            this.out.print("Exporting group '" + group.getName() + "' ... ");
            exportGroup(group, ps);
            this.out.println("Done");
        }
    }

    private void exportGroup(AppdefGroupValue g, PrintStream ps)
        throws Exception
    {
        String s1 = StringUtil.repeatChars(' ', INDENT);
        String s2 = StringUtil.repeatChars(' ', s1.length() +
                                           "<group ".length());

        ps.println(s1 + "<group name=\"" + g.getName() + "\"");

        if (g.isGroupCompat()) {
            ps.println(s2 + "type=\"compat\"");
        } else {
            // Assume adhoc
            ps.println(s2 + "type=\"adhoc\"");
        }

        int groupType = g.getGroupEntType();
        if (groupType == -1) {
            ps.println(s2 + "membertype=\"mixed\"");
        } else {
            ps.println(s2 + "membertype=\"" +
                       AppdefEntityConstants.typeToString(g.getGroupEntType()) +
                       "\"");
        }

        int resType = g.getGroupEntResType();
        if (resType != -1) {
            AppdefResourceTypeValue typeValue;

            switch (g.getGroupEntType()) {
            case AppdefEntityConstants.APPDEF_TYPE_PLATFORM:
                typeValue = aBoss.findPlatformTypeById(auth.getAuthToken(),
                                                       new Integer(resType));
                break;
            case AppdefEntityConstants.APPDEF_TYPE_SERVER:
                typeValue = aBoss.findServerTypeById(auth.getAuthToken(),
                                                     new Integer(resType));
                break;
            case AppdefEntityConstants.APPDEF_TYPE_SERVICE:
                typeValue = aBoss.findServiceTypeById(auth.getAuthToken(),
                                                      new Integer(resType));
                break;
            default:
                throw new Exception("Invalid resource type: " + resType);
            }

            ps.println(s2 + "membertypename=\"" + typeValue.getName() + "\"");
        }

        ps.println(s2 + "description=\"" + print(g.getDescription()) + "\"");
        ps.println(s2 + "location=\"" + print(g.getLocation()) + "\">");

        // Export group members
        PageList groupEntries = g.getGroupEntries();
        AppdefEntityID[] ids = new AppdefEntityID[groupEntries.size()];

        int j = 0;
        for (Iterator i=groupEntries.iterator(); i.hasNext();) {
            GroupEntry gEntry = (GroupEntry)i.next();
            int type = AppdefUtil.resNameToAppdefTypeId(gEntry.getType());

            AppdefEntityID id = new AppdefEntityID(type,
                                                   gEntry.getId());
            ids[j++] = id;
        }

        PageList members = aBoss.findByIds(auth.getAuthToken(), ids);
        for (Iterator i=members.iterator(); i.hasNext();) {
            AppdefResourceValue val = (AppdefResourceValue)i.next();
            exportGroupMember(val, ps, INDENT * 2);
        }
        ps.println(s1 + "</group>");
    }

    private void exportGroupMember(AppdefResourceValue a, 
                                   PrintStream ps, int indent)
        throws Exception
    {
        String s1 = StringUtil.repeatChars(' ', indent);
        String s2 = StringUtil.repeatChars(' ', s1.length() +
                                           "<groupmember ".length());

        ps.println(s1 + "<groupmember name=\"" + a.getName() + "\"");
        ps.println(s2 + "type=\"" +
                   AppdefEntityConstants.typeToString(a.getEntityId().
                                                      getType()) + "\" />");
    }

    private void exportApplications(PrintStream ps)
        throws Exception
    {
        PageList applications = this.aBoss.findAllApps(auth.getAuthToken(), 
                                                       PageControl.PAGE_ALL);

        for (Iterator i=applications.iterator(); i.hasNext();) {
            ApplicationValue a = (ApplicationValue)i.next();
            this.out.print("Exporting application '" + a.getName() + "' ... ");
            exportApplication(a, ps);
            this.out.println("Done");
        }
    }

    private void exportApplication(ApplicationValue a, PrintStream ps)
        throws Exception
    {
        String s1 = StringUtil.repeatChars(' ', INDENT);
        String s2 = StringUtil.repeatChars(' ', s1.length() +
                                           "<application ".length());

        ps.println(s1 + "<application name=\"" + a.getName() + "\"");
        ps.println(s2 + "location=\"" + print(a.getLocation()) + "\"");
        ps.println(s2 + "description=\"" + print(a.getDescription()) + "\"");
        ps.println(s2 + "businesscontact=\"" +
                   print(a.getBusinessContact()) + "\"");
        ps.println(s2 + "engcontact=\"" + print(a.getEngContact()) + "\"");
        ps.println(s2 + "opscontact=\"" + print(a.getOpsContact()) + "\">");

        // Export services.
        ps.println(s1 + s1 + "<services>");
        exportApplicationServices(a, ps, INDENT * 3);
        ps.println(s1 + s1 + "</services>");

        ps.println(s1 + "</application>");
    }

    private void exportApplicationServices(ApplicationValue a, PrintStream ps,
                                           int indent)
        throws Exception
    {
        String s1 = StringUtil.repeatChars(' ', indent);

        PageList services = 
            this.aBoss.findServicesByApplication(auth.getAuthToken(), a.getId(),
                                                 PageControl.PAGE_ALL);
        
        for (Iterator i = services.iterator(); i.hasNext();) {
            ServiceValue s = (ServiceValue)i.next();
            ps.println(s1 + "<service name=\"" + s.getName() + "\" />");
        }
    }

    private void exportPlatforms(PrintStream ps) 
        throws Exception
    {               
        PageList platforms = this.aBoss.findAllPlatforms(auth.getAuthToken(),
                                                         PageControl.PAGE_ALL);
        for (Iterator i=platforms.iterator(); i.hasNext();) {
            PlatformValue p = (PlatformValue)i.next();
            this.out.print("Exporting platform '"  + p.getName() + "' ... ");
            exportPlatform(p, ps);
            this.out.println("Done");
        }
    }

    private void exportPlatform(PlatformValue p, PrintStream ps)
        throws Exception
    {
        String s1 = StringUtil.repeatChars(' ', INDENT);
        String s2 = StringUtil.repeatChars(' ', s1.length() +
                                           "<platform ".length());
        PlatformTypeValue pt = p.getPlatformType();

        ps.println(s1 + "<platform name=\"" + print(p.getName()) + "\"");
        ps.println(s2 + "type=\"" + print(pt.getName()) + "\"");
        ps.println(s2 + "fqdn=\"" + print(p.getFqdn()) + "\"");
        ps.println(s2 + "certdn=\"" + print(p.getCertdn()) + "\"");
        ps.println(s2 + "comment=\"" + print(p.getCommentText()) + "\"");
        ps.println(s2 + "cpucount=\"" + p.getCpuCount() + "\"");
        ps.println(s2 + "description=\"" + print(p.getDescription()) + "\"");
        ps.println(s2 + "location=\"" + print(p.getLocation()) + "\">");

        // Export IP addresses
        IpValue ips[] = p.getIpValues();
        ps.println(s1 + s1 + "<ip_addresses>");
        for (int i=0; i < ips.length; i++) {
            exportIp(ips[i], ps, INDENT * 3);
        }
        ps.println(s1 + s1 + "</ip_addresses>");
        
        // Export Agent Connection
        AgentValue agent = p.getAgent();
        exportAgent(agent, ps, INDENT * 2);

        // Export Configuration
        AppdefEntityID id = 
            new AppdefEntityID(AppdefEntityConstants.APPDEF_TYPE_PLATFORM,
                               p.getId());
        ConfigResponseDB config =
            this.pBoss.getConfigResponse(auth.getAuthToken(), id);
        exportConfig(id, config, ps, INDENT * 2);

        // Export Servers
        PageList servers = 
            this.aBoss.findServersByPlatform(auth.getAuthToken(),
                                             p.getId(), PageControl.PAGE_ALL);
        for (Iterator i=servers.iterator(); i.hasNext();) {
            ServerValue s = (ServerValue)i.next();
            exportServer(s, ps, INDENT * 2);
        }

        ps.println(s1 + "</platform>");
    }

    private void exportIp(IpValue ip, PrintStream ps, int indent)
        throws Exception
    {
        String s1 = StringUtil.repeatChars(' ', indent);
        String s2 = StringUtil.repeatChars(' ', s1.length() +
                                           "<ip ".length());

        ps.println(s1 + "<ip address=\"" + print(ip.getAddress()) + "\"");
        ps.println(s2 + "netmask=\"" + print(ip.getNetmask()) + "\"");
        ps.println(s2 + "macaddress=\"" + print(ip.getMACAddress()) + "\">");
        ps.println(s1 + "</ip>");
    }

    private void exportAgent(AgentValue agent, PrintStream ps, int indent)
        throws Exception
    {
        String s1 = StringUtil.repeatChars(' ', indent);
        String s2 = StringUtil.repeatChars(' ', s1.length() +
                                           "<agentconn ".length());
        
        ps.println(s1 + "<agentconn address=\"" +
                   print(agent.getAddress()) + "\"");
        ps.println(s2 + "port=\"" + agent.getPort() + "\" />");
    }

    private void exportServer(ServerValue s, PrintStream ps, int indent)
        throws Exception
    {
        String s1 = StringUtil.repeatChars(' ', indent);
        String s2 = StringUtil.repeatChars(' ', s1.length() +
                                           "<server ".length());
        
        ServerTypeValue st = s.getServerType();

        ps.println(s1 + "<server name=\"" + s.getName() + "\"");
        ps.println(s2 + "installpath=\"" + s.getInstallPath() + "\"");
        ps.println(s2 + "type=\"" + st.getName() + "\"");
        ps.println(s2 + "description=\"" + print(s.getDescription()) + "\"");
        ps.println(s2 + "location=\"" + print(s.getLocation()) + "\"");
        // Set AI identifier only if it exists.
        if (s.getAutoinventoryIdentifier() != null) {
            ps.println(s2 + "autoinventoryidentifier=\"" +
                       print(s.getAutoinventoryIdentifier()) + "\">");
        }

        // Export Configuration
        AppdefEntityID id = 
            new AppdefEntityID(AppdefEntityConstants.APPDEF_TYPE_SERVER,
                               s.getId());
        ConfigResponseDB config =
            this.pBoss.getConfigResponse(auth.getAuthToken(), id);
        exportConfig(id, config, ps, INDENT * 3);

        // Export Services
        PageList services = 
            this.aBoss.findServicesByServer(auth.getAuthToken(),
                                            s.getId(), PageControl.PAGE_ALL);
        for (Iterator i=services.iterator(); i.hasNext();) {
            ServiceValue svc = (ServiceValue)i.next();
            exportService(svc, ps, INDENT * 3);
        }        

        ps.println(s1 + "</server>");
    }

    private void exportService(ServiceValue s, PrintStream ps, int indent)
        throws Exception
    {
        String s1 = StringUtil.repeatChars(' ', indent);
        String s2 = StringUtil.repeatChars(' ', s1.length() +
                                           "<service ".length());
        
        ServiceTypeValue st = s.getServiceType();

        ps.println(s1 + "<service name=\"" + s.getName() + "\"");
        ps.println(s2 + "type=\"" + st.getName() + "\"");

        if (s.getParentId() != null &&
            s.getParentId().intValue() != 0) {
            ps.println(s2 + "parentservice=\"" + s.getParentId() + "\"");
        }

        ps.println(s2 + "description=\"" + print(s.getDescription()) + "\"");
        ps.println(s2 + "location=\"" + print(s.getLocation()) + "\">");

        // Export Configuration
        AppdefEntityID id = 
            new AppdefEntityID(AppdefEntityConstants.APPDEF_TYPE_SERVICE,
                               s.getId());
        ConfigResponseDB config =
            this.pBoss.getConfigResponse(auth.getAuthToken(), id);
        exportConfig(id, config, ps, INDENT * 3);

        ps.println(s1 + "</service>");
    }

    private void exportConfig(AppdefEntityID id, ConfigResponseDB val,
                              PrintStream ps, int indent)
        throws Exception
    {
        byte[] resourceConfig = val.getProductResponse();
        byte[] controlConfig  = val.getControlResponse();
        byte[] metricConfig   = val.getMeasurementResponse();

        if (resourceConfig != null) {
            ConfigResponse config = ConfigResponse.decode(resourceConfig);
            exportConfigResponse(config, ps, "resourceConfig", indent);
        }

        if (controlConfig != null) {
            ConfigResponse config = ConfigResponse.decode(controlConfig);
            exportConfigResponse(config, ps, "controlConfig", indent);
        }

        if (metricConfig != null) {
            ConfigResponse config = ConfigResponse.decode(metricConfig);
            exportMetrics(id, config, ps, indent);
        }
    }

    private void exportMetrics(AppdefEntityID id, ConfigResponse config, 
                               PrintStream ps, int indent)
        throws Exception
    {
        String s1 = StringUtil.repeatChars(' ', indent);
        
        ps.println(s1 + "<metric>");
        exportConfigResponse(config, ps, "metricConfig", indent + INDENT);
        exportMeasurements(id, ps, indent + INDENT);
        ps.println(s1 + "</metric>");
    }

    private void exportConfigResponse(ConfigResponse config, PrintStream ps,
                                      String tag, int indent)
        throws Exception
    {
        String s1 = StringUtil.repeatChars(' ', indent);
        String s2 = StringUtil.repeatChars(' ', s1.length() +
                                           tag.length() + 2);

        Set keys = config.getKeys();
        Iterator i = keys.iterator();

        if (i.hasNext()) {
            String key = (String)i.next();
            ps.print(s1 + "<" + tag + " " + key + "=\"" +
                     config.getValue(key));
        
            for (;i.hasNext();) {
                key = (String)i.next();
                ps.println("\"");
                ps.print(s2 + key + "=\"" + config.getValue(key));
            }
            ps.println("\" />");
        } else {
            // Empty config response
            ps.println(s1 + "<" + tag + " />");
        }
    }

    private void exportMeasurements(AppdefEntityID id, PrintStream ps, 
                                    int indent)
        throws Exception
    {
        String s1 = StringUtil.repeatChars(' ', indent);
        String s2 = StringUtil.repeatChars(' ', s1.length() +
                                           "<collect ".length());

        PageList m = this.mBoss.findMeasurements(auth.getAuthToken(),
                                                 id, PageControl.PAGE_ALL);
        
        for (Iterator i = m.iterator(); i.hasNext();) {
            DerivedMeasurementValue dm = (DerivedMeasurementValue)i.next();
            
            ps.println(s1 + "<collect metric=\"" + 
                       dm.getTemplate().getAlias() + "\"");
            ps.println(s2 + "interval=\"" + dm.getInterval()/1000 + "\" />");
        }
    }

    public String getSyntaxArgs() {
        return "<export_file.xml>";
    }

    public String getSyntaxEx() {
        return "Use 'help " + this.getCommandName() + "' for details";
    }

    public String getUsageShort() {
        return "Export the HQ inventory.";
    }

    public String getUsageHelp(String[] args) {
        return getUsageShort();
    }
}
