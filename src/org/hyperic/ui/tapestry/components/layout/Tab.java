package org.hyperic.ui.tapestry.components.layout;

import org.apache.tapestry.components.Block;
import org.hyperic.util.StringUtil;

public class Tab {

    public static String LABEL_TEMPLATE = "<div class='%1%'>%2%</div>";

    private String _id;
    private String _label;
    private String _iconClass;
    private Block _block;
    private boolean _isClosable;

    public Tab(String id, String label, String iconClass, Block block,
            boolean isClosable) {
        _id = id;
        _label = label;
        _iconClass = iconClass;
        _block = block;
        _isClosable = isClosable;
    }

    public String getId() {
        return _id;
    }

    public void setId(String id) {
        _id = id;
    }

    public String getLabel() {
        String temp = StringUtil.replace(LABEL_TEMPLATE, "%1%", _iconClass);
        return StringUtil.replace(temp, "%2%", _label == null ? "" : _label);
    }

    public void setLabel(String label) {
        _label = label;
    }

    public String getIconClass() {
        return _iconClass;
    }

    public void setIconClass(String iconClass) {
        _iconClass = iconClass;
    }

    public Block getBlock() {
        return _block;
    }

    public void setBlock(Block block) {
        _block = block;
    }

    public boolean isClosable() {
        return _isClosable;
    }

    public void setClosable(boolean isClosable) {
        _isClosable = isClosable;
    }
}
