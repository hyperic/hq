/**
 * NOTE: This copyright does *not* cover user programs that use HQ
 * program services by normal system calls through the application
 * program interfaces provided as part of the Hyperic Plug-in Development
 * Kit or the Hyperic Client Development Kit - this is merely considered
 * normal use of the program, and does *not* fall under the heading of
 *  "derived work".
 *
 *  Copyright (C) [2010], VMware, Inc.
 *  This file is part of HQ.
 *
 *  HQ is free software; you can redistribute it and/or modify
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

package org.hyperic.hq.product.validation;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;

import org.apache.log4j.ConsoleAppender;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.apache.log4j.PatternLayout;
import org.hyperic.hq.product.pluginxml.PluginData;
import org.hyperic.hq.product.pluginxml.PluginParser;
import org.xml.sax.EntityResolver;
import org.xml.sax.InputSource;

/**
 * Validates plugin XML files. Executed as part of each plugin's build cycle
 * (during process-resources phase)
 * @author jhickey
 * 
 */
public class PluginXmlValidator {

    private static final String[] SHARED_XML_LOCATIONS = { "process-plugin/src/main/resources",
                                                          "jvm-plugin/src/main/resources", "hibernate-plugin/src/main/resources" };

    private String pluginDir;

    /**
     * 
     * @param pluginDir The top level directory containing all the plugins under
     *        src control. Should be /path to local hq repo/hq-plugin
     */
    public PluginXmlValidator(String pluginDir) {
        this.pluginDir = pluginDir;
    }

    private class BuildTimeEntityResolver implements EntityResolver {
        private PluginData data;

        BuildTimeEntityResolver(PluginData data) {
            this.data = data;
        }

        private String resolveParentFile(String name) {
            if (this.data.getFile() != null) {
                // look for the file using the plugin's location
                File dir = new File(this.data.getFile()).getParentFile();
                while (dir != null) {
                    File resolved = new File(dir, name);

                    if (resolved.exists()) {
                        return resolved.toString();
                    }

                    dir = dir.getParentFile();
                }
            }

            return name;
        }

        private boolean isPluginFile(String name) {
            return name.startsWith(PluginData.PLUGINS_PREFIX);
        }

        private String resolvePluginFile(String name) throws FileNotFoundException {
            // Resolve any included XML paths beginning with pdk/plugins. Strip
            // off "pdk/plugins" in favor of location of shared XML under src
            // control
            for (String sharedLocation : SHARED_XML_LOCATIONS) {
                File referencedFile = new File(pluginDir + "/" + sharedLocation + "/" +
                                               name.substring(PluginData.PLUGINS_PREFIX.length()));
                if (referencedFile.exists()) {
                    return referencedFile.getAbsolutePath();
                }
            }
            throw new FileNotFoundException("Unable to find included XML file: " +
                                            name.substring(PluginData.PLUGINS_PREFIX.length()) +
                                            " under any shared locations in plugin dir: " +
                                            pluginDir);
        }

        // resolve external references to a set of shared metadata
        private String resolveFile(String name) throws FileNotFoundException {

            if (isPluginFile(name)) {
                return resolvePluginFile(name);
            } else {
                return resolveParentFile(name);
            }
        }

        @SuppressWarnings("unchecked")
        public InputSource resolveEntity(String publicId, String systemId) {
            try {
                String name = null;
                // WTF. certain xerces impls will pass the relative uri as-is
                // others prepend the file:// protocol.
                if (systemId.startsWith("/")) {
                    name = systemId;
                } else if (systemId.startsWith("file:/")) {
                    name = new URL(systemId).getFile();
                }

                if (name != null) {
                    String resolvedName;
                    if (name.startsWith("/")) {
                        name = name.substring(1);
                    }
                    resolvedName = resolveFile(name);
                    InputStream is = new FileInputStream(resolvedName);
                    this.data.getIncludes().add(resolvedName);
                    return new InputSource(is);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
            return null;
        }
    }

    public void validatePluginXML(String name) throws Exception {
        File descriptor = new File(name);
        if (!descriptor.exists()) {
            System.out.println("Skipping " + name + " validation " + "(does not exist)");
            return;
        }
        PluginParser parser = new PluginParser();
        PluginData data = new PluginData();
        FileInputStream is = null;

        data.setFile(name);
        try {
            System.out.println("Validating " + name);
            is = new FileInputStream(descriptor);
            parser.parse(is, data, new BuildTimeEntityResolver(data));
        } finally {
            if (is != null) {
                try {
                    is.close();
                } catch (IOException e) {
                }
            }
        }
    }

    public static void main(String[] args) {
        if (args.length != 1) {
            System.err.println("Usage: PluginXmlValidator <plugin dir>");
            System.exit(1);
        }
        Logger rootLogger = Logger.getRootLogger();
        rootLogger.setLevel(Level.INFO);
        PatternLayout layout = new PatternLayout(PatternLayout.TTCC_CONVERSION_PATTERN);
        rootLogger.addAppender(new ConsoleAppender(layout));
        final String pluginDir = args[0];
        // First try src/main/resources/etc/hq-plugin.xml. Java plugins should
        // have this.
        final String desc = "/src/main/resources/" + PluginData.PLUGIN_XML;
        String plugin = pluginDir + desc;
        if (!new File(plugin).exists()) {
            // Maybe it is an XML Plugin - try
            // src/main/resources/<pluginName>.xml
            final String pluginDirName = new File(pluginDir).getName();
            plugin = pluginDir + "/src/main/resources/" + pluginDirName + ".xml";
            if (!new File(plugin).exists()) {
                System.err.println("Unable to find plugin XML to validate");
                System.exit(1);
            }
        }
        PluginXmlValidator validator = new PluginXmlValidator(new File(pluginDir).getParent());
        try {
            validator.validatePluginXML(plugin);
        } catch (Exception e) {
            e.printStackTrace();
            System.exit(1);
        }
    }

}
