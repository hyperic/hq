/*
 * NOTE: This copyright does *not* cover user programs that use HQ
 * program services by normal system calls through the application
 * program interfaces provided as part of the Hyperic Plug-in Development
 * Kit or the Hyperic Client Development Kit - this is merely considered
 * normal use of the program, and does *not* fall under the heading of
 * "derived work".
 * 
 * Copyright (C) [2004-2008], Hyperic, Inc.
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

package org.hyperic.hq.plugin.groovy;

import groovy.lang.GroovyClassLoader;

import java.io.File;
import java.io.InputStream;
import java.util.Properties;

import org.hyperic.hq.product.PluginException;
import org.hyperic.hq.product.PluginManager;
import org.hyperic.hq.product.ProductPlugin;
import org.hyperic.hq.product.ScriptLanguagePlugin;

public class GroovyLanguagePlugin
    extends ProductPlugin
    implements ScriptLanguagePlugin {

    public void init(PluginManager manager)
        throws PluginException {

        super.init(manager);
        addScriptLanguage(this);
    }

    public String getExtension() {
        return "groovy";
    }

    public Class loadClass(ClassLoader loader,
                           Properties properties,
                           InputStream is)
        throws PluginException {

        GroovyClassLoader cl = new GroovyClassLoader(loader);

        try {
            return cl.parseClass(is);
        } catch (Exception e) {
            throw new PluginException(e.getMessage(), e);
        }
    }

    public Class loadClass(ClassLoader loader,
                           Properties properties,
                           File file)
        throws PluginException {

        GroovyClassLoader cl = new GroovyClassLoader(loader);

        try {
            return cl.parseClass(file);
        } catch (Exception e) {
            throw new PluginException(e.getMessage(), e);
        }
    }
}
