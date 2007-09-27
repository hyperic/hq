// ========================================================
// Copyright (c) 2002 Novadeck (France)
// ========================================================
package org.novadeck.jxla.data;


/** Associate a counter to a string. */
public class StringData extends SimpleData
{
  
  String _uri;
  
  /** Simple cionstructor
   * @param uri string associated tio counter
   */  
  public StringData (String uri)
  {
    super ();
    _uri = uri;
  }
  
  /** Construct a counter with an initial value
   * @param uri string associated tio counter
   *
   * @param l initial value of counter
   */  
  public StringData (String uri, long l)
  {
    super ( l );
    _uri = uri;
  }
  
  /** retriecve the string valure for this counter
   * @return string
   */  
  public String getData ()
  {
    return _uri;
  }
  
}