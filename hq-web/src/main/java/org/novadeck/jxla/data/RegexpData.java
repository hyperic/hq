// ========================================================
// Copyright (c) 2002 Novadeck (France)
// ========================================================
package org.novadeck.jxla.data;

import org.apache.oro.text.regex.MalformedPatternException;
import org.apache.oro.text.regex.MatchResult;
import org.apache.oro.text.regex.Pattern;

import org.novadeck.jxla.tools.Constants;
import org.novadeck.jxla.tools.Utils;


import java.util.ArrayList;
import java.util.Collections;

public class RegexpData extends SimpleData
{
  public static final String    HOSTNAME    = "host";
  public static final String    REMOTE_IP   = "remote_ip";
  public static final String    REMOTE_HOST = "remote_host";
  public static final String    USER        = "user";
  public static final String    URI         = "uri";
  public static final String    QUERY       = "query";
  public static final String    STATUS      = "status";
  public static final String    REFERER     = "referer";
  public static final String    USER_AGENT  = "agent";
  public static final String    LANGUAGE    = "lang";
  public static final String    SIZE        = "size";
  public static final String    TIME_TAKEN  = "time_taken";
  // This is a bit of a hack.  The idea is that if we have the time
  // in seconds and the log file isn't meant for human consumption, then 
  // we should just write the number of seconds to the file.  This will
  // be faster than formatting the date cleanly.
  public static final String    DATE_MSEC   = "date_msec";
  
  public static final char      YEAR        = 'y';
  public static final char      MONTH       = 'm';
  public static final char      DAY         = 'd';
  public static final char      HOUR        = 'h';
  public static final char      OFFSET      = 'o';
  
  public static final char      WILDCARD    = '*';
  public static final char      IDENTIFIER  = '$';
  public static final char      DELIMITER   = '"';
  public static final char      SPACE       = ' ';
  public static final char      SLASH       = '/';
  public static final char      DOUBLEPOINT = ':';
  public static final char      CROCHET_O   = '[';
  public static final char      CROCHET_F   = ']';
  public static final char      TIRET       = '-';
  
  
  private static RegexpData  _lastMatched  =null;
  
  
  private int   _host;
  private int   _ip;
  private int   _user;
  private int   _uri;
  private int   _query;
  private int   _status;
  private int   _referer;
  private int   _agent;
  private int   _lang;
  private int   _size;
  private int   _time_taken;
  private int   _date_msec;
  
  private int   _year;
  private int   _month;
  private int   _day;
  private int   _hour;
  private int   _offset;
  
  private Pattern       _pattern    = null;
  private MatchResult   _res;
  public String         initialRegexp = null;
  public String         compiledRegexp  = null;
  
  /**
   *
   *
   */
  private RegexpData ()
  {
  }
  
  public RegexpData (String s)
  {
    initialRegexp = s;
    _host       = -1;
    _ip         = -1;
    _user       = -1;
    _uri        = -1;
    _query      = -1;
    _status     = -1;
    _referer    = -1;
    _agent      = -1;
    _lang       = -1;
    _size       = -1;
    _time_taken = -1;
    _date_msec  = -1;
    _year       = -1;
    _month      = -1;
    _day        = -1;
    _hour       = -1;
    _offset     = -1;
    
    // analyze s as a simple regexep
    /* This made sense when jxla was a stand-alone app, but we can't just
     * quit the RT thread in CAM.  So, comment this out.  It is okay, because
     * it just means that we will waste some time reading a file that we can't
     * parse.
    if ( Utils.isEmpty ( s ) )
    { quit ( "regexp is empty "); }
     */
    int length  = s.length ();
    int current = 0;
    char[] arr  = s.toCharArray ();
    int pos     = 1;
    
    StringBuffer regexp = new StringBuffer ();
    
    while ( current < length )
    {
      switch ( arr[current] )
      {
        case YEAR:
          regexp.append ( "([0-9]{2,4})" );
          current ++;
          _year   =   pos++;
          break;
        case MONTH:
          regexp.append ( "([0-9a-zA-Z]{1,3})" );
          current ++;
          _month  =   pos++;
          break;
        case DAY:
          regexp.append ( "([ 0-9]{1,2})" );
          current ++;
          _day    =   pos++;
          break;
        case HOUR:
          regexp.append ( "([0-9]{1,2}:[0-9]{1,2}:[0-9]{1,2})" );
          current ++;
          _hour   =   pos++;
          break;
        case OFFSET:
          regexp.append ( "([-+][0-9]{1,4})" );
          current ++;
          _offset   =   pos++;
          break;
        case IDENTIFIER:
          // load the parameter
          current++;
          StringBuffer sb = new StringBuffer ();
          boolean found = false;
          while ( (current<length) && ( !found ) )
          {
            sb.append ( arr[current] );
            current++;
            found = true;
            if ( HOSTNAME.equals ( sb.toString () ) )
            {
              regexp.append ( "([a-zA-Z0-9\\.\\-_]*)" );
              _host = pos ++;
            }
            else if ( REMOTE_IP.equals ( sb.toString () ) )
            {
              regexp.append ( "([0-9\\.]*)" );
              _ip       = pos ++;
            }
            else if ( REMOTE_HOST.equals ( sb.toString () ) )
            {
              regexp.append ( "([a-zA-Z0-9\\-\\.]*)" );
              _ip       = pos ++;
            }
            else if ( USER.equals (sb.toString () ) )
            {
              regexp.append ( "([^ ]*)" );
              _user     = pos ++;
            }
            else if ( URI.equals ( sb.toString () ) )
            {
              regexp.append ( "(http://[^/]*)?(\\/[^ ]*)" );
              // Increment here is to skip the host portion of
              // the URI. (If it exists)
             pos++;
              _uri      = pos ++;
            }
            else if ( QUERY.equals ( sb.toString () ) )
            {
              regexp.append ( "([^ ]*)" );
              _query    = pos ++;
            }
            else if ( STATUS.equals ( sb.toString () ) )
            {
              regexp.append ( "([0-9]*)" );
              _status   = pos ++;
            }
            else if ( REFERER.equals ( sb.toString () ) )
            {
              regexp.append ( "\"([^\"]*)\"" );
              _referer  = pos ++;
            }
            else if ( USER_AGENT.equals ( sb.toString () ) )
            {
              regexp.append ( "\"([^\"]*)\"" );
              _agent    = pos ++;
            }
            else if ( LANGUAGE.equals ( sb.toString () ) )
            {
              regexp.append ( "\"([^\"]*)\"" );
              _lang     = pos ++;
            }
            else if ( SIZE.equals ( sb.toString () ) )
            {
              regexp.append ( "([0-9\\-][0-9]*)" );
              _size  = pos ++;
            }
            else if ( TIME_TAKEN.equals ( sb.toString () ) )
            {
              regexp.append ( "([0-9\\-][0-9]*)" );
              _time_taken  = pos ++;
            }
            else if ( DATE_MSEC.equals ( sb.toString () ) )
            {
              regexp.append ( "([0-9][0-9]*)" );
              _date_msec  = pos ++;
            }
            else
            {
              found = false;
            }
          }
          /* Again, we don't want to quit, but we don't care about wasting
           * time in this case.
          if (!found)
            quit ( "bad regexp look at " + sb) ;
           */
          break;
        case WILDCARD:
          current ++;
          regexp.append ( "[^ ]*" );
          break;
        case DELIMITER:
          regexp.append ( DELIMITER );
          current ++;
          break;
        case SPACE:
          regexp.append ( SPACE );
          current ++;
          break;
        case SLASH:
          regexp.append ( SLASH );
          current ++;
          break;
        case DOUBLEPOINT:
          regexp.append ( DOUBLEPOINT );
          current ++;
          break;
        case CROCHET_O:
          regexp.append ( '\\' );
          regexp.append ( CROCHET_O);
          current ++;
          break;
        case CROCHET_F:
          regexp.append ( '\\' );
          regexp.append ( CROCHET_F );
          current ++;
          break;
        case TIRET:
          regexp.append ( TIRET );
          current ++;
          break;
        default:
      }
    }
    regexp.append ('$');
    compiledRegexp = regexp.toString ();
    try
    {
      _pattern  = Constants.COMPILER.compile ( compiledRegexp );
    }
    catch(MalformedPatternException e)
    {
      // We have complete control over the input to this funtion, so we should
      // never get here.
    }
  }
  
  public String getCompiledRegexp ()
  {
    return compiledRegexp;
  }
  //============================================================================
  public boolean match ( String s )
  {
    _globalCounter  ++;
    _res =null;
    if (Constants.MATCHER.matches ( s, _pattern ))
    {
      inc ();
      _lastMatched  = this;
      _res = Constants.MATCHER.getMatch ();
      return true;
    }
    return false;
  }
  
  //============================================================================
  private String getExpr ( int rank )
  {
    if ( (_res == null ) || (rank <= 0 ) )  return null;
    return _res.group ( rank );
  }
  
  //============================================================================
  /**
   * @return  */
  public String getHost ()
  {   return getExpr ( _host );    }
  
  public String getRemoteIP ()
  {
    String ip = getExpr ( _ip );
    return ip;
  }
  
  public String getUser ()
  {   return getExpr ( _user );    }
  
  public String getURI ()
  {   
      String uri = getExpr(_uri);
      String query = getExpr(_query);

      if (query != null &&
          !query.equals("-")) {
          return uri + "?" + query;
      } else {
          return uri;
      }
  }
  
  public String getStatus ()
  {   return getExpr ( _status );  }
  
  public String getReferer ()
  {   return getExpr ( _referer ); }
  
  public String getAgent ()
  {   return getExpr ( _agent );   }
  
  public String getLanguage ()
  {   return getExpr ( _lang );    }
  
  public long getSize ()
  {
    if ( (_res == null ) || (_size<= 0 ) )  return 0;
    String s = getExpr ( _size );
    if (Utils.isEmpty ( s )  || "-".equals ( s ) ) return 0;
    else return Long.parseLong ( s );
  }

  public long getTimeTaken ()
  {
    String s = getExpr ( _time_taken);
    if (Utils.isEmpty (s )) return 0;
    return Long.parseLong ( s.trim() );
  }

  public long getDateMSec()
  {
    String s = getExpr ( _date_msec);
    if (Utils.isEmpty (s )) return -1;
    return Long.parseLong ( s.trim() );
  }

  public int getHour ()
  {
    String s = getExpr ( _hour);
    if (Utils.isEmpty (s )) return 0;
    String hourStr = s.trim ();
    int firstcolon = hourStr.indexOf(':');
    s = hourStr.substring( 0, firstcolon);
    if (Utils.isEmpty (s )) return 0;
    return Integer.parseInt ( s );
  }

  public String getOffset ()
  {
    if (_offset != -1) {
      return getExpr( _offset ).trim ();
    }
    return null;
  }

  public int getMinute ()
  {
    String s = getExpr ( _hour);
    if (Utils.isEmpty (s )) return 0;
    String hourStr = s.trim ();
    int firstcolon = hourStr.indexOf( ':' );
    int secondcolon = hourStr.indexOf( ':', firstcolon + 1);
    s = hourStr.substring( firstcolon + 1, secondcolon );
    return Integer.parseInt ( s );
  }

  public int getSecond ()
  {
    String s = getExpr ( _hour);
    if (Utils.isEmpty (s )) return 0;
    String hourStr = s.trim ();
    int firstcolon = hourStr.indexOf( ':' );
    int secondcolon = hourStr.indexOf( ':', firstcolon + 1);
    s = hourStr.substring( secondcolon + 1 );
    if (Utils.isEmpty (s )) return 0;
    return Integer.parseInt ( s );
  }
  
  public int getDay ()
  {
    String s = getExpr ( _day);
    if (Utils.isEmpty (s )) return 0;
    return Integer.parseInt ( s.trim () );
  }
  public int getMonth ()
  {
    String s = getExpr ( _month);
    if (Utils.isEmpty (s )) return -1;
    String month = s.toLowerCase ();
    //      jan feb mar apr may jun jul aug sep oct nov dec
    char c = month.charAt (0);
    if (Character.isDigit(c)) {
        return Integer.parseInt(month) - 1;
    }
    switch ( c )
    {
      case 'j' :
        if ( "jan".equals ( month ) )
          return 0;
        else if( "jun".equals ( month ) )
          return 5;
        else
          return 6;
      case 'f' :
        return 1;
      case 'm' :
        if ( "mar".equals ( month ) )
          return 2;
        else
          return 4;
      case 'a' :
        if ( "apr".equals ( month ) )
          return 3;
        else
          return 7;
      case 's' :
        return 8;
      case 'o' :
        return 9;
      case 'n' :
        return 10;
      default:
        return 11;
    }
  }
  public int getYear ()
  {
    String s = getExpr ( _year);
    if (Utils.isEmpty (s )) return 0;
    return Integer.parseInt ( s.trim () );
  }
  
  //============================================================================
  //----------------
  private static ArrayList    _availableRegexp  = new ArrayList ();
  private static int          _globalCounter    = 0;
  private static RegexpData[] _arrayRe          = null;
  
  //----------------
  public static void addRegexp ( RegexpData re )
  {
    if (_lastMatched == null) _lastMatched = re;
    if (! _availableRegexp.contains ( re ) )
    { _availableRegexp.add ( re );  }
    _arrayRe      =  (RegexpData[])_availableRegexp.toArray ( new RegexpData[0] );
  }
  
  public static RegexpData[] getRegexps ( )
  {
    return _arrayRe;
  }
  //----------------
  public static void updateList ()
  {
    if ( _availableRegexp.size () ==1) return;
    Collections.sort ( _availableRegexp );
    _arrayRe      =  (RegexpData[])_availableRegexp.toArray ( new RegexpData[0] );
  }
  
  public static Line getLine ( String s )
  {
    if ( Utils.isComment ( s ) )
    {
      return null;
    }
    s = s.trim ();
    // need to update list for first use
    if ( (_globalCounter % 1000) ==0 )
    {
      updateList ();
      _globalCounter =1;
    }
    
    int i=0;
    
    Line l = null;

    if (RegexpData._lastMatched.match ( s ) )
    {
      RegexpData re = _lastMatched;
      /////////////////
      l = Line.getLine ( re.getHost (), re.getDay (), re.getMonth (), 
                         re.getYear (), re.getHour (), re.getMinute (), 
                         re.getSecond (),re.getRemoteIP (), re.getURI (), 
                         re.getReferer (), re.getAgent (), re.getStatus (), 
                         re.getSize (), re.getUser (), re.getTimeTaken (),
                         re.getOffset (), re.getDateMSec () );
      return l;
    }
    while ( ( i < _arrayRe.length) && ( !_arrayRe[i].match ( s ) ) )
    {
      i++;
    }
    if (i < _arrayRe.length)
    {
      RegexpData re = _arrayRe[i];
      /////////////////
      l = Line.getLine ( re.getHost (), re.getDay (), re.getMonth (), 
                         re.getYear (), re.getHour (), re.getMinute (),
                         re.getSecond (), re.getRemoteIP (), re.getURI (),
                         re.getReferer (), re.getAgent (), re.getStatus (), 
                         re.getSize (), re.getUser (), re.getTimeTaken (),
                         re.getOffset (), re.getDateMSec () );
      _lastMatched = re;
    }
    return l;
  }
}
