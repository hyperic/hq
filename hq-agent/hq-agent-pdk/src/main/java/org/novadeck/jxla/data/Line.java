// ========================================================
// Copyright (c) 2002 Novadeck (France)
// ========================================================
package org.novadeck.jxla.data;


import java.util.TimeZone;
import java.util.Date;
import java.util.Calendar;
import java.util.GregorianCalendar;
import org.novadeck.jxla.*;
import org.novadeck.jxla.config.*;

/** Only one instance of it is use for
 * computing logs.
 * All information represents are about one web request
 */
public class Line implements Cloneable
{
  private GregorianCalendar _date = new GregorianCalendar();
  private String  _host;
  private String  _remoteIP;
  private String  _uri;
  private String  _referer;
  private String  _userAgent;
  private Long    _status;
  private long    _size;
  private double _time_taken;
  private String  _user;
  private String  _keywords = null;
  private double _timeMultiplier;
  
  private static Line INSTANCE = new Line ();
  
  private Line ()
  {
  }
  
  /** empty all data information */  
  public void release ()
  {
    _host=null;
    _remoteIP=null;
    _uri=null;
    _referer=null;
    _userAgent=null;
    _status=null;
    _size=-1;
    _time_taken=-1;
    _user=null;
    _keywords = null;
  }
  
  /** update line with current information */  
  public static Line getLine ( String host, int day, int month, int year, 
                               int hour, int minute, int second,
                               String remoteIp, String uri, String referer,
                               String userAgent, String status, long size, 
                               String user, double time_taken, String offset,
                               long date_msec)
  {
    INSTANCE.release ();
    INSTANCE._host       = host;


    if (date_msec == -1) {
        if (offset != null) {
            INSTANCE._date.setTimeZone (TimeZone.getTimeZone( "GMT" + offset ));
        }
        else {
            INSTANCE._date.setTimeZone ( TimeZone.getTimeZone("GMT"));
        }
        INSTANCE._date.set ( Calendar.DAY_OF_MONTH, day );
        INSTANCE._date.set ( Calendar.MONTH, month );
        INSTANCE._date.set ( Calendar.YEAR, year );
        
        INSTANCE._date.set ( Calendar.HOUR_OF_DAY, hour );
        INSTANCE._date.set ( Calendar.MINUTE, minute );
        INSTANCE._date.set ( Calendar.SECOND, second );

        // This is kind of strange, but it is okay.  When Java creates a new
        // Date it is filled out with the current time, which means that
        // milliseconds has an actual value.  But we aren't creating a date that
        // represents "now", we are trying to represent the time in the log file.
        // No log files represent date/time information to the millisecond, so
        // we just zero that data out.  This means that all times start at the
        // beginning of the second, but we are looking for duration, not absolute
        // times, so this works.
        INSTANCE._date.set ( Calendar.MILLISECOND, 0 );
        
//        long timeMillis = INSTANCE._date.getTimeInMillis();
    } else {
        INSTANCE._date.setTimeInMillis(date_msec);
    }

    INSTANCE._remoteIP   = remoteIp;
    int i = uri.indexOf ('?');
    // DON'T suppress query string
    /*    if ( i>=0)
    {
      uri = uri.substring (0, i);
    }
    else
    {
      uri        = uri;
    }
    */

    INSTANCE._uri = uri;
    if (referer != null)
    {
      if (referer .startsWith ( "http://" ))
      {
        if  (referer.toLowerCase ().startsWith ("http://"+host) )
        {
          referer = null;
        }
      }
    }
    
    INSTANCE._referer    = referer;
    INSTANCE._userAgent  = userAgent;
    if (status != null) {
        INSTANCE._status = new Long (Integer.parseInt ( status ));
    } else {
        INSTANCE._status = new Long(0);
    }
    INSTANCE._size       = size;
    INSTANCE._time_taken = time_taken * INSTANCE._timeMultiplier;
    INSTANCE._user       = user;
    return INSTANCE;
  }
  
  
  
  /** Retrieve date of resquest
   * @return date of request
   */  
  public Date getDate ()
  {
    return  _date.getTime();
  }
  
  /** Retrieve date of resquest
   * @return date of request
   */  
  public String getLogDate ()
  {
    return  _date.getTime().toGMTString();
  }
  
  /** Retrieve the internal website name for this request
   * @return site name
   */  
  public String getHost ()
  {
    return _host;
  }
  
  /** Retrieve the uri of the requst
   * @return URI of the request
   */  
  public String getURI ()
  {
    return _uri;
  }
  /*
  public int getMonth ()
  {
    return _date.getMonth ();
  }
  */
  /** Retrieve the referer of request
   * @return referer
   */  
  public String getReferer ()
  {
    return this._referer;
  }
  
  /** Retrieve keywords from serach engine referers if any
   * @return search keyword or null
   */  
  public String getKeywords ()
  {
    return this._keywords;
  }
  
  /** Retrieve remote user computer name ( if reverse dns enabled) or his IP ( if not
   * )
   * @return remote user machine
   */  
  public String getRemoteIP ()
  {
    return this._remoteIP;
  }
  
  /** Retrieve status response of the request
   * @return status
   */  
  public Long getStatus ()
  {
    return this._status;
  }
  
  /** retrieve remote user UserAgent, using short names
   * @return user agent
   * @see org.novadeck.jxla.tools.Utils
   */  
  public String getUserAgent ()
  {
    return this._userAgent;
  }
  
  /** Retrive size if the response, used to updated site traffic information
   * @return size of HTTP response
   */  
  public long getSize ()
  {
    return this._size;
  }
  
  /** Retrive size if the response, used to updated site traffic information
   * @return size of HTTP response
   */  
  public double getTimeTaken ()
  {
    return this._time_taken;
  }

  /** Retrieve user logging name
   * @return user info
   */  
  public String getUser ()
  {
    return this._user;
  }
  
  
  /** Return true if line is correctly parsed, and ready to be used to update reports
   * @return if line is empty
   */  
  public boolean isLineEmpty ()
  {
    return (this._host == null );
  }
  
  public String toString ()
  {
    StringBuffer sb = new StringBuffer ();
    sb.append ( "host ==" + _host  + "\n");
    sb.append ( "uri ==" + _uri  + "\n");
    sb.append ( "remoteIP ==" + _remoteIP  + "\n");
    sb.append ( "referer ==" + _referer  + "\n");
    sb.append ( "status ==" + _status  + "\n");
    sb.append ( "date ==" + _date+ "\n");
    sb.append ( "agent ==" + _userAgent+ "\n");
    sb.append ( "time_taken ==" + _time_taken+ "\n");
    return sb.toString ();
  }

  public Object clone()
  {
    Line myline = new Line();

    myline._date       = (GregorianCalendar)_date.clone();
    myline._host       = _host;
    myline._remoteIP   = _remoteIP;
    myline._uri        = _uri;
    myline._referer    = _referer;
    myline._userAgent  = _userAgent;
    myline._status     = _status;
    myline._size       = _size;
    myline._time_taken = _time_taken;
    myline._user       = _user;
    myline._keywords   = _keywords;

    return myline;
  }

  static public void setTimeMultiplier(double t)
  {
      INSTANCE._timeMultiplier = t;
  }

  static public double getTimeMultiplier()
  {
      return INSTANCE._timeMultiplier;
  }
}
