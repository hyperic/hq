// ========================================================
// Copyright (c) 2002 Novadeck (France)
// ========================================================
package org.novadeck.jxla.config;

import java.util.Collection;

import org.novadeck.jxla.data.RegexpData;


/** Configure all needed information from the config file */
public class Config
{
  
  //log files to analyze
  /** path list of log files to parse */

  public Config(String re)
  {
    addLogLineFormat (re);
  }

  /** add a valid line format
   * @param s valid expression for parsing log lines
   * @see RegexpData
   */
  public void addLogLineFormat ( String s )
  {
    RegexpData.addRegexp ( new RegexpData ( s ) );
  }

}
