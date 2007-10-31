package org.hyperic.hq.plugin.nagios.parser;

import java.io.PrintStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.regex.Pattern;

class Util
{
    static Pattern RESOURCE_PATTERN = Pattern.compile("^\\$\\w+\\$=");

    static void debug(PrintStream out, String msg, Throwable e)
    {
        if (out == null)
            return;

        String exception = "";

        if (msg != null)
            msg = msg+"\n";
        else
            msg = "";

        if (e != null)
        {
            StringWriter str = new StringWriter();
            e.printStackTrace(new PrintWriter(str));
            exception = str.toString();
        }
        out.println(msg+exception);
    }

    static void debug(PrintStream out, Throwable e)
    {
        debug(out, null, e);
    }

    static void debug(PrintStream out, String buf)
    {
        debug(out, buf, null);
    }

    String removeInlineComments(String nameValue)
    {
        return nameValue.trim().replaceAll("\\s*;.*$", "");
    }
}
