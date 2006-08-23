package org.hyperic.hq.product.pluginxml;

public class ScriptTag extends EmbedTag {

    ScriptTag(BaseTag parent) {
        super(parent);
    }

    public String getName() {
        return "script";
    }
}
