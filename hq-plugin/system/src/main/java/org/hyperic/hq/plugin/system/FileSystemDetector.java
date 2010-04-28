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

package org.hyperic.hq.plugin.system;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.hyperic.sigar.FileInfo;
import org.hyperic.sigar.FileSystem;
import org.hyperic.sigar.FileSystemMap;
import org.hyperic.sigar.Sigar;
import org.hyperic.sigar.SigarException;
import org.hyperic.util.StringUtil;
import org.hyperic.util.config.ConfigResponse;
import org.hyperic.util.config.EncodingException;
import org.hyperic.util.security.MD5;

import org.hyperic.hq.appdef.shared.AIServiceValue;
import org.hyperic.hq.product.PluginException;
import org.hyperic.hq.product.PluginManager;

public class FileSystemDetector
    extends SystemServerDetector {

    private static final String PROP_DISCOVER_NFS =
        "system.nfs.discover";
    private boolean _discoverNfs;

    public void init(PluginManager manager) throws PluginException {
        super.init(manager);

        _discoverNfs =
            "true".equals(manager.getProperty(PROP_DISCOVER_NFS));
    }

    protected String getServerType() {
        return SystemPlugin.FILE_SERVER_NAME;
    }

    protected ArrayList getSystemServiceValues(Sigar sigar, ConfigResponse config)
        throws SigarException {
        ArrayList services = new ArrayList();

        FileSystem[] fslist = sigar.getFileSystemList();

        for (int i=0; i<fslist.length; i++) {
            FileSystem fs = fslist[i];
            String dirName = fs.getDirName();

            switch (fs.getType()) {
              case FileSystem.TYPE_LOCAL_DISK:
              case FileSystem.TYPE_NETWORK:
                break;
              default:
                continue;
            }

            String genericType = fs.getTypeName();
            String type = fs.getSysTypeName();
            String mntInfo = "";

            //e.g. avoid "C:\ mounted on C:\"
            if (!fs.getDevName().equals(dirName)) {
                mntInfo = " mounted on " + dirName;
            }

            String info =
                "File System " + fs.getDevName() + 
                mntInfo +
                " (" + genericType + "/" + type + ")";

            if ((fs.getType() == FileSystem.TYPE_NETWORK) &&
                !_discoverNfs)
            {
                getLog().debug(PROP_DISCOVER_NFS +
                               "=false, skipping " + info);
                continue;
            }

            AIServiceValue svc = 
                createSystemService(SystemPlugin.FILE_MOUNT_SERVICE,
                                    getFullServiceName(info),
                                    SystemPlugin.PROP_FS,
                                    dirName);
            services.add(svc);
        }

        String[] fileTypes = {
            SystemPlugin.FILE_SERVICE,
            SystemPlugin.DIR_SERVICE,
            SystemPlugin.DIR_TREE_SERVICE,
            SystemPlugin.SCRIPT_SERVICE,
        };

        for (int i=0; i<fileTypes.length; i++) {
            String type = fileTypes[i];
            services.addAll(getFileServices(sigar, type));
            services.addAll(getFileServiceImports(sigar, type));
        }

        return services;
    }

    private boolean canRead(File file) {
        if (!file.exists()) {
            log.warn(file + ": does not exist");
            return false;
        }
        if (!file.canRead()) {
            log.warn(file + ": cannot read");
            return false;
        }        
        return true;
    }

    private List getFileServiceImports(Sigar sigar, String type) {

        List services = new ArrayList();
        String prop = "system." + type + ".import";
        String val = getManagerProperty(prop);
        if (val == null) {
            return services;
        }

        log.debug("Importing " + prop + "=" + val);
        List files = StringUtil.explode(val, ",");
        boolean isDirType = 
            type.indexOf(SystemPlugin.DIR_NAME) != -1;

        for (int i=0; i<files.size(); i++) {
            String path = (String)files.get(i);
            boolean recurse = path.charAt(0) == '+';
            if (recurse) {
                path = path.substring(1);
            }

            File file = new File(path);
            if (!canRead(file)) {
                continue;
            }

            boolean isDir = file.isDirectory();
            if (isDirType && !isDir) {
                log.warn(file + ": not a directory");
                continue;
            }
            //as-is
            if ((isDirType && isDir) || !isDir) {
                AIServiceValue svc =
                    createSystemService(type, path,
                                        SystemPlugin.PROP_PATH, path);
                setFileProperties(sigar, svc, path);
                services.add(svc);
                continue;
            }

            //import a directory of files or scripts
            addFileServices(services, recurse, file, sigar, type);
        }        

        return services;
    }

    private void addFileServices(List services, boolean recurse,
                                 File dir, Sigar sigar, String type) {
        String[] dents = dir.list();
        if (dents == null) {
            log.warn(dir + ": failed to list");
            return;
        }

        for (int i=0; i<dents.length; i++) {
            File ent = new File(dir, dents[i]);
            String path = ent.getPath();
            if (!canRead(ent)) {
                continue;
            }

            if (ent.isDirectory()) {
                if (recurse) {
                    addFileServices(services, recurse,
                                    ent, sigar, type);
                }
                continue;
            }
            AIServiceValue svc =
                createSystemService(type, path,
                                    SystemPlugin.PROP_PATH, path);
            setFileProperties(sigar, svc, path);
            services.add(svc);
        }
    }

    //File services are created by hand, the FileControlPlugin
    //registers these services w/ SystemServerDetector so we
    //can auto-discovery inventory properties here
    private List getFileServices(Sigar sigar, String type) {

        boolean isDebug = log.isDebugEnabled();

        List serviceConfigs = getServiceConfigs(type);
        
        List services = new ArrayList();

        for (int i=0; i<serviceConfigs.size(); i++) {
            ConfigResponse serviceConfig = 
                (ConfigResponse)serviceConfigs.get(i);
            
            String name =
                serviceConfig.getValue(SystemPlugin.PROP_RESOURCE_NAME);
            String file =
                serviceConfig.getValue(SystemPlugin.PROP_PATH);
            
            AIServiceValue svc = createSystemService(type, name);

            if (isDebug) {
                log.debug("Getting cprops for " +
                          name + " " + type + "=" + file);
            }
            
            setFileProperties(sigar, svc, file);
            services.add(svc);
        }
        
        return services;
     }

    private void setFileProperties(Sigar sigar, AIServiceValue svc, String file) {

        ConfigResponse cprops = new ConfigResponse();
        boolean isDirectory = new File(file).isDirectory();

        if (!isDirectory) {
            try {
                cprops.setValue("md5",
                                MD5.getDigestString(new File(file)));
            } catch (IOException e) {
                getLog().debug("Error getting md5 for " + file + ": " + e);
            }
        }

        if (!isWin32()) { //XXX not quite right on win32
            try {
                FileInfo info = sigar.getFileInfo(file);
                cprops.setValue("permissions", info.getPermissionsString());
            } catch (SigarException e) {
            }
        }

        try {
            FileSystemMap mounts = sigar.getFileSystemMap();
            FileSystem fs = mounts.getMountPoint(file);
            if (fs != null) {
                cprops.setValue("fs", fs.getDirName());
            }
        } catch (SigarException e) {
        }

        try {
            svc.setCustomProperties(cprops.encode());
        } catch (EncodingException e) {
            getLog().error("Error encoding cprops: " + e.getMessage());
        }
    }
}
