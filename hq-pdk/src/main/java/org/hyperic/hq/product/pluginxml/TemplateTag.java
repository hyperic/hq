package org.hyperic.hq.product.pluginxml;

import org.hyperic.util.xmlparser.XmlTagInfo;

public class TemplateTag
    extends BaseTag {
    private static final String TAG_NAME = "template";

    public TemplateTag() {
        // TODO Auto-generated constructor stub
    }

    public TemplateTag(BaseTag parent) {
        super(parent);
    }

    /* (non-Javadoc)
     * @see org.hyperic.hq.product.pluginxml.BaseTag#getName()
     */
    @Override
    public String getName() {
        return TAG_NAME;
    }

    public XmlTagInfo[] getSubTags() {
        return new XmlTagInfo[] {
            new XmlTagInfo(new FolderTag(this),
                           XmlTagInfo.ONE_OR_MORE),
        };
    }
}
