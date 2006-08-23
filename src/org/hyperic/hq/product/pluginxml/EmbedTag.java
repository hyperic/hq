package org.hyperic.hq.product.pluginxml;

import java.io.File;
import java.io.IOException;

import org.hyperic.hq.product.ClientPluginDeployer;
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
