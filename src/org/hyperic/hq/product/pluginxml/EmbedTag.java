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

package org.hyperic.hq.product.pluginxml;

import java.io.File;
import java.io.IOException;

import org.hyperic.hq.product.ClientPluginDeployer;
import org.hyperic.hq.product.ProductPlugin;
import org.hyperic.util.xmlparser.XmlTagException;
import org.hyperic.util.xmlparser.XmlTextHandler;

/**
 * Extract text embedded in plugin descriptor to a file.
 * Used for embedded scripts, mibs, etc.
 */
public class EmbedTag
    extends BaseTag
    implements XmlTextHandler {

    private static final String[] REQUIRED_ATTRS = {
        ATTR_NAME
    };

    private static final String[] OPTIONAL_ATTRS = {
        ATTR_TYPE
    };

    private String text;

    EmbedTag(BaseTag parent) {
        super(parent);
    }

    public String getName() {
        return "embed";
    }

    public String[] getOptionalAttributes() {
        return OPTIONAL_ATTRS;
    }

    public String[] getRequiredAttributes() {
        return REQUIRED_ATTRS;
    }

    public void handleText(String text) {
        this.text = text.trim();
    }

    protected String getType() {
        String type = getAttribute(ATTR_TYPE);
        if (type == null) {
            return getName();
        }
        else {
            return type;
        }
    }

    void write() throws XmlTagException {
        String name = getAttribute(ATTR_NAME);
        if (ProductPlugin.isGroovyScript(name)) {
            this.data.setProperty(name, this.text); //XXX
            return;
        }
        String pdk = this.data.getPdkDir();

        if (pdk == null) {
            return;
        }

        String pluginName = this.data.getPluginName();
        if (pluginName == null) {
            String msg =
                "Unable to determine plugin name for: " +
                this.data.file;
            throw new XmlTagException(msg);
        }

        ClientPluginDeployer deployer =
            new ClientPluginDeployer(pdk, pluginName);

        String type = getType();

        if (!deployer.isDeployableType(type)) {
            String msg =
                "Invalid " + getName() + " type=" + type;
            throw new XmlTagException(msg);
        }

        File file = deployer.getFile(type, name);
        if (file == null) {
            return;
        }

        if (deployer.upToDate(new File(this.data.file), file)) {
            return;
        }

        try {
            deployer.write(this.text, file);
        } catch (IOException e) {
            throw new XmlTagException(e.getMessage());
        }
    }

    void endTag() throws XmlTagException {
        super.endTag();

        try {
            write();
        } finally {
            this.text = null;
        }
    }
}
