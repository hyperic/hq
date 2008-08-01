package org.hyperic.ui.tapestry.components.general;

import java.util.Iterator;
import java.util.List;

import org.apache.commons.lang.StringUtils;
import org.apache.tapestry.IAsset;
import org.apache.tapestry.IMarkupWriter;
import org.apache.tapestry.IPage;
import org.apache.tapestry.IRender;
import org.apache.tapestry.IRequestCycle;
import org.apache.tapestry.TapestryUtils;
import org.apache.tapestry.engine.IEngineService;
import org.apache.tapestry.engine.ILink;
import org.apache.tapestry.html.RelationBean;

public abstract class Shell extends org.apache.tapestry.html.Shell{
    
    public static final String SHELL_ATTRIBUTE = "org.apache.tapestry.html.Shell";

    
    public abstract IRender getAfterDelegate();
    public abstract void setAfterDelegate(IRender delegate);
    
    protected void renderComponent(IMarkupWriter writer, IRequestCycle cycle)
    {
        TapestryUtils.storeUniqueAttribute(cycle, SHELL_ATTRIBUTE, this);

        long startTime = System.currentTimeMillis();
        boolean rewinding = cycle.isRewinding();
        boolean dynamic = getBuilder().isDynamic();

        if (!rewinding && !dynamic)
        {
            writeDocType(writer, cycle);

            IPage page = getPage();

            writer.begin("html");
            renderInformalParameters(writer, cycle);
            writer.println();
            writer.begin("head");
            writer.println();

            if (isDisableCaching())
                writeMetaTag(writer, "http-equiv", "content", "no-cache");

            if (getRenderContentType())
                writeMetaTag(writer, "http-equiv", "Content-Type", writer.getContentType());

            writeRefresh(writer, cycle);

            if (getRenderBaseTag())
                getBaseTagWriter().render(writer, cycle);

            writer.begin("title");

            writer.print(getTitle(), getRaw());
            writer.end(); // title
            writer.println();

            IRender delegate = getDelegate();

            if (delegate != null)
                delegate.render(writer, cycle);

            IRender ajaxDelegate = getAjaxDelegate();

            if (ajaxDelegate != null)
                ajaxDelegate.render(writer, cycle);
            
            IRender afterDelegate = getAfterDelegate();

            if (afterDelegate != null)
                afterDelegate.render(writer, cycle);


            IAsset stylesheet = getStylesheet();

            if (stylesheet != null)
                writeStylesheetLink(writer, stylesheet);

            Iterator i = (Iterator) getValueConverter().coerceValue(getStylesheets(), Iterator.class);

            while (i.hasNext())
            {
                stylesheet = (IAsset) i.next();

                writeStylesheetLink(writer, stylesheet);
            }
        }

        // Render the body, the actual page content

        IMarkupWriter nested = !dynamic ? writer.getNestedWriter() : writer;

        renderBody(nested, cycle);

        if (!rewinding)
        {
            List relations = getRelations();
            if (relations != null)
                writeRelations(writer, relations);

            StringBuffer additionalContent = getContentBuffer();
            if (additionalContent != null)
                writer.printRaw(additionalContent.toString());

            writer.end(); // head
        }

        if (!dynamic)
            nested.close();

        if (!rewinding && !dynamic)
        {
            writer.end(); // html
            writer.println();

            if (!isDisableTapestryMeta())
            {
                long endTime = System.currentTimeMillis();

                writer.comment("Render time: ~ " + (endTime - startTime) + " ms");
            }
        }

    }

    protected void cleanupAfterRender(IRequestCycle cycle)
    {
        super.cleanupAfterRender(cycle);

        cycle.removeAttribute(SHELL_ATTRIBUTE);
    }

    private void writeDocType(IMarkupWriter writer, IRequestCycle cycle)
    {
        // This is the real code
        String doctype = getDoctype();
        if (StringUtils.isNotBlank(doctype))
        {
            writer.printRaw("<!DOCTYPE " + doctype + ">");
            writer.println();
        }
    }

    private void writeStylesheetLink(IMarkupWriter writer, IAsset stylesheet)
    {
        writer.beginEmpty("link");
        writer.attribute("rel", "stylesheet");
        writer.attribute("type", "text/css");
        writer.attribute("href", stylesheet.buildURL());
        writer.println();
    }

    private void writeRefresh(IMarkupWriter writer, IRequestCycle cycle)
    {
        int refresh = getRefresh();

        if (refresh <= 0)
            return;

        // Here comes the tricky part ... have to assemble a complete URL
        // for the current page.

        IEngineService pageService = getPageService();
        String pageName = getPage().getPageName();

        ILink link = pageService.getLink(false, pageName);

        StringBuffer buffer = new StringBuffer();
        buffer.append(refresh);
        buffer.append("; URL=");
        buffer.append(StringUtils.replace(link.getAbsoluteURL(), "&amp;", "&"));

        writeMetaTag(writer, "http-equiv", "Refresh", buffer.toString());
    }

    private void writeMetaTag(IMarkupWriter writer, String key, String value, String content)
    {
        writer.beginEmpty("meta");
        writer.attribute(key, value);
        writer.attribute("content", content);
        writer.println();
    }

    private void writeRelations(IMarkupWriter writer, List relations)
    {
        Iterator i = relations.iterator();
        
        while (i.hasNext())
        {
            RelationBean relationBean = (RelationBean) i.next();
            if (relationBean != null)
                writeRelation(writer, relationBean);
        }
    }

    private void writeRelation(IMarkupWriter writer, RelationBean relationBean)
    {
        writer.beginEmpty("link");

        writeAttributeIfNotNull(writer, "rel", relationBean.getRel());
        writeAttributeIfNotNull(writer, "rev", relationBean.getRev());
        writeAttributeIfNotNull(writer, "type", relationBean.getType());
        writeAttributeIfNotNull(writer, "media", relationBean.getMedia());
        writeAttributeIfNotNull(writer, "title", relationBean.getTitle());
        writeAttributeIfNotNull(writer, "href", relationBean.getHref());
        
        writer.println();
    }

    private void writeAttributeIfNotNull(IMarkupWriter writer, String name, String value)
    {
        if (value != null)
            writer.attribute(name, value);
    }

}
