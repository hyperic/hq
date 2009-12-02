// ========================================================
// Copyright (c) 2002 Novadeck (France)
// ========================================================
package org.novadeck.jxla;

import org.hyperic.hq.product.RtStat;
import org.hyperic.hq.product.logparse.BaseLogParser;
import org.novadeck.jxla.config.Config;
import org.novadeck.jxla.data.Line;
import org.novadeck.jxla.data.RegexpData;

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
