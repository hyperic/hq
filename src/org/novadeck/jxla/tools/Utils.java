// ========================================================
// Copyright (c) 2002 Novadeck (France)
// ========================================================
package org.novadeck.jxla.tools;

import org.apache.oro.text.regex.MalformedPatternException;
import org.apache.oro.text.regex.Pattern;

public class Utils
{
  
  //============================================================================
  /** Compile a regular expression into pattern
   * @param regexp the regular expression to convert
   * @return null if an error ocurres, a regexp compiled else
   */
  public static Pattern compileRE ( String regexp )
  {
    try
    {
      return Constants.COMPILER.compile ( regexp );
    }
    catch (MalformedPatternException e)
    {
      return null;
    }
  }
  
  //============================================================================
  /** test if a string match a pattern ( using regexp )
   * @param str the string to test
   * @param p the pattern to match
   * @return true or false
   */
  public static boolean match ( String str, Pattern p )
  {
    return Constants.MATCHER.matches ( str, p );
  }
  
  //============================================================================
  public static final boolean isEmpty ( String s )
  {
    return ( (s==null) || (s.length ()==0)) ;
  }
  
  //============================================================================
  public static final boolean isComment ( String s )
  {
    return ( isEmpty (s) || s.charAt (0) == '#' );
  }
  
  /** defines output value representing Internet Explorer browser */
  public static final String UA_IE         = "msie";
  /** defines output value representing Opera browser */
  public static final String UA_OPERA      = "opera";
  /** defines output value representing Konqueror browser */
  public static final String UA_KONQUEROR  = "konqueror";
  /** defines output value representing Netscape browser */
  public static final String UA_NETSCAPE   = "mozilla";
  /** defines output value representing Lynx browser */
  public static final String UA_LYNX       = "lynx";
  /** defines output value representing bot simulating browser */
  public static final String UA_BOT        = "bot";
  /** defines output value representing php simulating browser */
  public static final String UA_PHP        = "php";
  /** defines output value representing NetBox ( NetGem Setup Box)  browser */
  public static final String UA_NETBOX     = "NetBox - NetGem";
  
  
  public static String getUserAgent ( String s )
  {
    if ( isEmpty ( s ) )
      return null;
    s= s.toLowerCase ();
    if( s.indexOf ( UA_BOT ) >=0)
      return "Bots";
    else
      if ( s.indexOf ( UA_IE ) >=0)
        return "Internet Explorer";
      else
        if( s.indexOf ( UA_OPERA ) >=0)
          return "Opera";
        else
          if( s.indexOf ( UA_KONQUEROR ) >=0)
            return "Konqueror";
          else
            if( s.indexOf ( UA_NETBOX ) >=0)
              return "NetBox";
            else
              if( s.indexOf ( UA_NETSCAPE ) >=0)
                return "NetScape";
              else
                if( s.indexOf ( UA_LYNX ) >=0)
                  return "Lynx";
                else
                  if( s.indexOf ( UA_PHP ) >=0)
                    return "PHP";
                  else
                    return "Unkown Browser";
  }
  
  public static final boolean canOutputHit ( String uri )
  {
    if ( uri == null )
      return false;
    return ( !uri.endsWith (".jpg") && !uri.endsWith (".png") &&
    !uri.endsWith (".gif") &&!uri.endsWith (".jpeg"));
  }
  
}

