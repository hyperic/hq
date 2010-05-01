// ========================================================
// Copyright (c) 2002 Novadeck (France)
// ========================================================
package org.novadeck.jxla.data;


/** This class acts like a counter */
public class SimpleData extends SerializableSimpleData implements Comparable
{
  
  private long _counter;
  
  /** create a counter with an initial value of 0 */
  public SimpleData ()
  {
    _counter = 0;
  }
  /** create a counter with an initial value
   * @param l initila value of the counter
   */
  public SimpleData ( long l )
  {
    _counter = l;
  }
  
  /** get the count value
   * @return count value
   */
  public long getCount ()
  {
    return _counter;
  }
  
  /** increments the current value by 1 */
  public void inc ()
  {
    _counter++;
  }
  /** increments current value by more than one
   * @param l value to add ti the counter
   */
  public void add (long l)
  {
    if (l>0) _counter = _counter + l;
  }
  
  /** to order Objects by counter decreasing
   * @param obj element to compare to
   * @return if obj is before of after current object
   */
  public int compareTo (java.lang.Object obj)
  {
    SimpleData d= (SimpleData)obj;
    return ( (int) ( d.getCount () - getCount () ) );
  }
  
}
