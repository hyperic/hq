// ========================================================
// Copyright (c) 2002 Novadeck (France)
// ========================================================
package org.novadeck.jxla;

import java.io.*;
import java.util.*;

import org.hyperic.hq.product.RtStat;
import org.hyperic.hq.product.logparse.BaseLogParser;
import org.novadeck.jxla.tools.*;
import org.novadeck.jxla.config.*;
import org.novadeck.jxla.data.*;
import org.hyperic.hq.product.logparse.BaseLogParser;

import org.apache.oro.text.GlobCompiler;
import org.apache.oro.text.regex.MalformedPatternException;
import org.apache.oro.text.regex.Perl5Matcher;
import org.apache.oro.text.regex.Pattern;

import org.apache.log4j.Logger;

/**
 *  Main program, parse log files and analyse them.
 */


public class LogParse extends BaseLogParser
{
    Config cfg;

    public LogParse() {
        super();
    }

    public void initConfig(double timeMultiplier, String regex)
    {
        cfg = new Config(regex);
        Line.setTimeMultiplier(timeMultiplier);
    }                      

    public RtStat parseLine(String current)
    {
        Line line = RegexpData.getLine(current);
        if (line != null) {
            RtStat found = new RtStat(id, svcType);
            found.recompute(line.getURI(), line.getDate(), line.getTimeTaken(),
                    new Integer(line.getStatus().intValue()));
            return found;
        }
        return null;
    }
}
