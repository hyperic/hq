package org.hyperic.hq.product.pluginxml;

import java.io.File;
import java.io.IOException;

import org.hyperic.hq.product.GenericPlugin;
import org.hyperic.util.xmlparser.XmlTagException;

public class ScriptTag extends EmbedTag {

    ScriptTag(BaseTag parent) {
        super(parent);
    }

    public String getName() {
        return "script";
    }

    protected File getSubDirectory(String pdk) {
        return new File(pdk, "scripts");
    }

    protected void writeFile(File file)
        throws XmlTagException {

        super.writeFile(file);

        if (!GenericPlugin.isWin32()) {
            try {
                //XXX lame.
                Runtime.getRuntime().exec("chmod +x " + file);
            } catch (IOException e) {}
        }
    }
}
