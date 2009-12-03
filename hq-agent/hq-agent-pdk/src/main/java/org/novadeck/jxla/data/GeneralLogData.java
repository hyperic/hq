// ========================================================
// Copyright (c) 2002 Novadeck (France)
// ========================================================
package org.novadeck.jxla.data;

import java.util.*;

import java.io.Serializable;

public class GeneralLogData implements Serializable
{
  
//  public static long total =0;
  
  // differents counters
  protected HashMap   _referers;
  protected HashMap   _keywords;
  protected HashMap   _remote_ip;
  protected HashMap   _hits;
  protected HashMap   _pagesView;
  protected HashMap   _files;
  protected HashMap   _status;
  protected HashMap   _userAgents;
  protected HashMap   _users;
  protected long      _traffic;
  
  public GeneralLogData ()
  {
//    total++;
    _referers   = new HashMap ();
    _keywords   = new HashMap ();
    _remote_ip  = new HashMap ();
    _hits       = new HashMap ();
    _pagesView  = new HashMap ();
    _files      = new HashMap ();
    _status     = new HashMap ();
    _userAgents = new HashMap ();
    _users      = new HashMap ();
    _traffic    = 0;
  }
  
  
  //============================================================================
  //--------
  public void addReferer ( String s )
  {
    inc ( s, _referers );
  }
  //--------
  public void addKeywords ( String s )
  {
    inc ( s, _keywords );
  }
  //--------
  public void addRemoteIP ( String s )
  {
    inc ( s , _remote_ip );
  }
  public void addStatus ( String s )
  {
    inc ( s, _status );
  }
  public void addUserAgent ( String s )
  {
    inc ( s, _userAgents );
  }
  public void addUser ( String s )
  {
    inc ( s, _users );
  }
  public void addHit ( String s )
  {
    inc ( s , _hits );
  }
  public void addFile ( String s )
  {
    inc ( s , _files );
  }
  public void addPageView ( String s )
  {
    inc ( s , _pagesView);
  }
  //-----------------------
  protected void inc ( String key, HashMap map )
  {
    Object obj = map.get ( key );
    if (obj == null)
    {
      obj = new SimpleData ();
      map.put ( key, obj );
    }
    ((SimpleData)obj).inc ();
  }
  
  //--------
  protected long getCount ( HashMap map )
  {
    Set set = map.keySet ();
    long total = 0;
    for (Iterator ite = set.iterator (); ite.hasNext (); )
    {
      Object obj = map.get ( ite.next () );
      total += ((SimpleData)obj).getCount ();
    }
    return total;
  }
  //============================================================================
  public long getTraffic ()
  {
    return _traffic;
  }
  public void addTraffic (long l)
  {
    _traffic = _traffic + l;
  }
  //============================================================================
  public long getPages ()
  {
    return getCount (_pagesView);
  }
  //============================================================================
  public long getFiles ()
  {
    return getCount ( _files );
  }
  
  public long getHits ()
  {
    return getCount ( _hits );
  }
  
}
