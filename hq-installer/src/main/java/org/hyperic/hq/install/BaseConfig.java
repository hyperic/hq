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

package org.hyperic.hq.install;

import java.io.File;
import java.io.IOException;
import java.util.Hashtable;
import org.hyperic.tools.ant.installer.InstallerConfigSchemaProvider;
import org.hyperic.util.JDK;
import org.hyperic.util.StringUtil;
import org.hyperic.util.config.ConfigResponse;
import org.hyperic.util.config.ConfigSchema;
import org.hyperic.util.config.DirConfigOption;
import org.hyperic.util.config.EarlyExitException;
import org.hyperic.util.config.InvalidOptionValueException;
import org.hyperic.util.file.FileUtil;

public abstract class BaseConfig implements InstallerConfigSchemaProvider {

    public static final String PRODUCT = "HQ";

    public static final String INSTALLMODE_UPGRADE    = "upgrade";
    public static final String INSTALLMODE_QUICK      = "quick";
    public static final String INSTALLMODE_ORACLE     = "oracle";
    public static final String INSTALLMODE_POSTGRESQL = "postgresql";
    public static final String INSTALLMODE_MYSQL      = "mysql";
    public static final String INSTALLMODE_FULL       = "full";

    private String baseName;
    private Hashtable projectProps;
    private String installDir;
    
    public BaseConfig (String baseName) {
        this.baseName = baseName;
    }

    public void setInstallDir(String instDir) {
        installDir = instDir;
    }
    public String getInstallDir () { return installDir; }

    public void setProjectProperties(Hashtable props) {
        this.projectProps = props;
    }
    public Hashtable getProjectProperties () { return projectProps; }

    public String getProjectProperty (String key) {
        Object o = projectProps.get(key);
        if (o == null) return "null";
        return o.toString();
    }
    public void setProjectProperty (String key, String value) {
        projectProps.put(key, value);
    }

    public String getCompletionText (ConfigResponse config) {
        return null;
    }

    public String getBaseName() {
        return baseName;
    }

    public abstract String getName();

    public boolean isUpgrade () {
        String installMode = getProjectProperty("install.mode");
        return installMode.equals(INSTALLMODE_UPGRADE);
    }

    /**
     * Return the directory where the product will be installed.
     * This is the value the end-user entered during the installation, for
     * example, /usr/local/HQ or "C:\Program Files\Hyperic HQ"
     * @return the install dir.  The return value will always end with a 
     * File.separator
     */
    public String getInstallDir (ConfigResponse config) {
        String dir
            = StringUtil.normalizePath(config.getValue(getBaseName()
                                                       + ".installdir"));
        if (!dir.endsWith(File.separator)) dir += File.separator;
        return dir;
    }

    /**
     * The product installation directory.  This includes the product name
     * and version, for example /usr/local/HQ/server-1.2.9 on unix or 
     * "C:\Program Files\Hyperic HQ\server-1.2.9" on windows
     * @return the product install dir.  This value will always end
     * in a File.separator
     */
    public String getProductInstallDir (ConfigResponse config) {
        String installDir = getInstallDir(config);
        return installDir
            + getBaseName() + "-" + getProjectProperty("version")
            + File.separator;
    }
    
    public ConfigSchema getSchema (ConfigResponse previous,
                                   int iterationCount) 
        throws EarlyExitException {
        
        if (iterationCount == 2) {
            // Sanity check
            String installDir = getInstallDir(previous);
            File dir = new File(installDir);
            try {
                if (!FileUtil.canWrite(dir)) {
                    throw new EarlyExitException("Can't write to installation "
                                                 + "directory: " + installDir);
                }
            } catch (IOException e) {
                throw new EarlyExitException("Can't write to installation " +
                                             "directory: " + e.getMessage());
            }
        }
        if (isUpgrade()) return getUpgradeSchema(previous, iterationCount);
        return getInstallSchema(previous, iterationCount);
    }

    protected ConfigSchema getUpgradeSchema (ConfigResponse previous,
                                             int iterationCount)
        throws EarlyExitException {
        
        ConfigSchema schema = new ConfigSchema();
        if (iterationCount == 0) {
            schema.addOption(new UpgradeDirConfigOption(baseName+".upgradedir",
                                                        "Enter full path of " 
                                                        + getName()
                                                        + " to upgrade: ",
                                                        "", this));
            return schema;
        } else if (iterationCount == 1) {
            String upgradeDir = previous.getValue(baseName+".upgradedir");
            String installDir = new File(upgradeDir).getParent();

            if (installDir == null || installDir.startsWith("${")) {
                installDir = getDefaultInstallPath();
            }

            schema.addOption(new DirConfigOption(baseName + ".installdir",
                                                 getName() + " installation " +
                                                 "path",
                                                 installDir));
            return schema;
        }
        return null;
    }
    
    /**
     * A callback used by UpgradeDirConfigOption.  The UpgradeDirConfigOption's 
     * checkOptionIsValid method ensures that the directory the user specifies
     * for upgrade exists and that it contains an appropriate marker file as 
     * returned by the BaseConfig.getMarkerFiles method.  Then it calls into
     * this method.  This method lets each product implement further checks
     * to ensure that the directory is suitable for upgrade.  In particular, the
     * HQ server checks to see if a pointbase-backed server is going to be 
     * upgraded, and throws an error if it is because we don't upgrade pointbase
     */
    public void canUpgrade (String dir) throws InvalidOptionValueException {}
        

    /**
     * @return An array of Strings, each representing a marker file that would
     * indicate the presence of the product.  These files should be relative, 
     * and should not begin with a slash.  They should use UNIX-style slashes
     * for directories.  Look at ShellConfig.getMarkerFiles for an example.
     */
    protected abstract String[] getMarkerFiles ();

    protected ConfigSchema getInstallSchema (ConfigResponse previous,
                                             int iterationCount)
        throws EarlyExitException {

        ConfigSchema schema = new ConfigSchema();
        if ( iterationCount == 0 ) {
            // populate schema...

            if (installDir == null || installDir.startsWith("${")) {
                installDir = getDefaultInstallPath();
            }
            
            schema.addOption(new DirConfigOption(baseName + ".installdir",
                                                 getName() + " installation " +
                                                 "path",
                                                 installDir));
        }

        return schema;
    }
    
    private static String getDefaultInstallPath() {
        if (JDK.IS_WIN32) {
            return "C:\\Program Files";
        }
        return "/home/hyperic";
    }

    protected String getExtension() {
        if (JDK.IS_WIN32) {
            return ".exe";
        }
        else {
            return ".sh";
        }
    }
    
    protected String getScriptExtension() {
        if (JDK.IS_WIN32) {
            return ".bat";
        }
        else {
            return ".sh";
        }
    }    
}
