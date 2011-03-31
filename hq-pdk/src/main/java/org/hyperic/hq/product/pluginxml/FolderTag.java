/**
 * 
 */
package org.hyperic.hq.product.pluginxml;

import org.hyperic.hq.product.MonitoredFolderConfig;
import org.hyperic.util.xmlparser.XmlAttrException;
import org.hyperic.util.xmlparser.XmlEndAttrHandler;
import org.hyperic.util.xmlparser.XmlTagInfo;

/**
 * @author amargalit
 *
 */
public class FolderTag
    extends BaseTag implements XmlEndAttrHandler{


    private static final String[] OPTIONAL_ATTRS =
            { ATTR_FILTER };

    private static final String[] REQUIRED_ATTRS =
        { ATTR_RECURSIVE, ATTR_PATH };

    private static final String TAG_NAME = "folder";
    
    private MonitoredFolderConfig monitoredFolderConfig;

    private MonitoredFolderConfig parentFolder = null;

    /**
     * @param parent
     */
    public FolderTag(BaseTag parent) {
        super(parent);
        if (parent instanceof FolderTag)
            parentFolder = ((FolderTag)parent).getMonitoredFolderConfig();
    }

    /* (non-Javadoc)
     * @see org.hyperic.hq.product.pluginxml.BaseTag#getName()
     */
    @Override
    public String getName() {
        return TAG_NAME;
    }

    public String[] getOptionalAttributes() {
        return OPTIONAL_ATTRS;
    }
    
    public String[] getRequiredAttributes() {
        return REQUIRED_ATTRS;
    }

    public void endAttributes() throws XmlAttrException {
        final String recStr = getAttribute(ATTR_RECURSIVE);
        final boolean recursive = recStr == null || recStr.trim().length() <= 0 ? false : Boolean.valueOf(getAttribute(ATTR_RECURSIVE)).booleanValue();    
        final String path = getAttribute(ATTR_PATH);    
        final String filter = getAttribute(ATTR_FILTER);
        this.monitoredFolderConfig = new MonitoredFolderConfig(path, filter, recursive);
    }
    
    void endTag() {
        if (parentFolder != null)
            parentFolder.addSubFolder(this.monitoredFolderConfig);
        else
            this.data.addMonitoredFolder(this.monitoredFolderConfig);
    }
    
    public MonitoredFolderConfig getMonitoredFolderConfig() {
        return monitoredFolderConfig;
    }
    
    public XmlTagInfo[] getSubTags() {
        return new XmlTagInfo[] {
            new XmlTagInfo(new FolderTag(this),
                           XmlTagInfo.ZERO_OR_MORE),
        };
    }
}
