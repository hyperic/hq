package org.hyperic.hq.product.pluginxml;

import org.hyperic.hq.product.MonitoredPropertiesConfig;
import org.hyperic.util.xmlparser.XmlAttrException;
import org.hyperic.util.xmlparser.XmlEndAttrHandler;
import org.hyperic.util.xmlparser.XmlTagInfo;

public class MonitoredPropertiesTag
    extends BaseTag implements XmlEndAttrHandler {

    private MonitoredPropertiesConfig config;
    
    private static final String[] REQUIRED_ATTRS =
    { ATTR_PATH, ATTR_TYPE};

    private static final String TAG_NAME = "monitoredPrperties";

    public MonitoredPropertiesTag(BaseTag monitoredTag) {
        super(monitoredTag);
    }

    @Override
    public String getName() {
        return TAG_NAME;
    }

    public String[] getRequiredAttributes() {
        return REQUIRED_ATTRS;
    }

    public void endAttributes() throws XmlAttrException {
        final String path = getAttribute(ATTR_PATH);    
        final String type = getAttribute(ATTR_TYPE);
        this.config = new MonitoredPropertiesConfig(path, type);
    }

    void endTag() {
        this.data.addMonitoredConfig(this.config);
    }

    public XmlTagInfo[] getSubTags() {
        return new XmlTagInfo[] {
            new XmlTagInfo(new FolderTag(this),
                           XmlTagInfo.ONE_OR_MORE),
        };
    }
}
