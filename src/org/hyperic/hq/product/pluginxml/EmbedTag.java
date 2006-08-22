package org.hyperic.hq.product.pluginxml;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

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
        ATTR_NAME, ATTR_TYPE
    };

    private static final String[] OPTIONAL_ATTRS = {
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
        this.text = text;
    }

    protected File getSubDirectory(String pdk) {
        String type = getAttribute(ATTR_TYPE);
        //e.g. <embed type="script" name="foo"/>
        //check for pdk/scripts/
        String[] dirs = { type + 's', type };

        File dir = null;
        for (int i=0; i<dirs.length; i++) {
            dir = new File(pdk, dirs[i]);
            if (dir.exists()) {
                break;
            }
            dir = null;
        }

        return dir;
    }

    private void writeFile() throws XmlTagException {
        String name = getAttribute(ATTR_NAME);
        
        String pdk = this.data.getPdkDir();

        if (pdk == null) {
            return;
        }

        File dir = getSubDirectory(pdk);
        if (dir == null) {
            return;
        }

        String pluginName = this.data.getPluginName();
        if (pluginName == null) {
            String msg =
                "Unable to determine plugin name for: " +
                this.data.file;
            throw new XmlTagException(msg);
        }

        dir = new File(dir, pluginName);
        if (!dir.exists()) {
            if (!dir.mkdir()) {
                return;
            }
        }

        File file = new File(dir, name);

        FileOutputStream os = null;
    
        try {
            os = new FileOutputStream(file);
            os.write(this.text.getBytes());
        } catch (IOException e) {
            String msg =
                "Error writing '" + file + "': " +
                e.getMessage();
            throw new XmlTagException(msg);
        } finally {
            if (os != null) {
                try {
                    os.close();
                } catch (IOException e) {}
            }
        }
    }
    
    void endTag() throws XmlTagException {
        super.endTag();
        writeFile();
        this.text = null;
    }
}
