/**
 * NOTE: This copyright does *not* cover user programs that use Hyperic
 * program services by normal system calls through the application
 * program interfaces provided as part of the Hyperic Plug-in Development
 * Kit or the Hyperic Client Development Kit - this is merely considered
 * normal use of the program, and does *not* fall under the heading of
 *  "derived work".
 *
 *  Copyright (C) [2010], VMware, Inc.
 *  This file is part of Hyperic.
 *
 *  Hyperic is free software; you can redistribute it and/or modify
 *  it under the terms version 2 of the GNU General Public License as
 *  published by the Free Software Foundation. This program is distributed
 *  in the hope that it will be useful, but WITHOUT ANY WARRANTY; without
 *  even the implied warranty of MERCHANTABILITY or FITNESS FOR A
 *  PARTICULAR PURPOSE. See the GNU General Public License for more
 *  details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program; if not, write to the Free Software
 *  Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307
 *  USA.
 *
 */
package org.hyperic.tools.ant;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Task;

/**
 * Upgrades UI Plugins by copying plugins from the old server that don't exist
 * in the new server TODO check plugin version numbers and copy existing plugins
 * if more recent in old server
 * @author jhickey
 * 
 */
public class PluginUpgrader
    extends Task {

    private String existingHquDir;
    private String newHquDir;

    public void execute() throws BuildException {
        File existingPluginDir = new File(existingHquDir);
        File newPluginDir = new File(newHquDir);
        if(!existingPluginDir.exists() || !newPluginDir.exists()) {
            return;
        }
        File[] plugins = existingPluginDir.listFiles();
        for (File plugin : plugins) {
            if (plugin.isDirectory() && !(new File(newPluginDir, plugin.getName()).exists())) {
                try {
                    log("Copying plugin: " + plugin + " to " + newPluginDir);
                    copyFolder(plugin, new File(newPluginDir, plugin.getName()));
                } catch (IOException e) {
                    log("Error copying plugin " + plugin.getName() + " to " + newPluginDir + ": " +
                        e.getMessage());
                }
            }
        }
    }

    private void copyFolder(File src, File dest) throws IOException {
        if (src.isDirectory()) {
            // if directory doesn't exist, create it
            if (!dest.exists()) {
                dest.mkdir();
            }

            // list all the directory contents
            String files[] = src.list();

            for (String file : files) {
                // construct the src and dest file structure
                File srcFile = new File(src, file);
                File destFile = new File(dest, file);
                // recursive copy
                copyFolder(srcFile, destFile);
            }
        } else {
            // if file, then copy it
            InputStream in = new FileInputStream(src);
            OutputStream out = new FileOutputStream(dest);
            copyStream(in, out);
            in.close();
            out.close();
        }
    }

    private void copyStream(InputStream is, OutputStream os) throws IOException {
        byte[] buf = new byte[2048];
        int bytesRead = 0;
        while (true) {
            bytesRead = is.read(buf);
            if (bytesRead == -1)
                break;
            os.write(buf, 0, bytesRead);
        }
    }

    public void setExistingHquDir(String existingHquDir) {
        this.existingHquDir = existingHquDir;
    }

    public void setNewHquDir(String newHquDir) {
        this.newHquDir = newHquDir;
    }

}
