package org.hyperic.hq.ui.contributions.ajax;

import org.apache.tapestry.IAsset;
import org.apache.tapestry.IRequestCycle;
import org.apache.tapestry.javascript.JavascriptManager;
import org.apache.tapestry.javascript.SimpleAjaxShellDelegate;

public class DojoOnePointOneShellDelegate extends SimpleAjaxShellDelegate {

    public static String LINE_FEED = "\n";

    public DojoOnePointOneShellDelegate(JavascriptManager jsMan) {
        super(jsMan);
    }

    protected void processTapestryPath(StringBuffer strBuf, IRequestCycle cycle,
            IAsset path) {
        strBuf.append(LINE_FEED).append("<script type=\"text/javascript\">")
              .append(LINE_FEED).append("dojo.registerModulePath(\"tapestry\", \"")
              .append(cycle.getAbsoluteURL(path.buildURL())).append("\");").append(LINE_FEED)
              .append("</script>").append(LINE_FEED);
    }

}
