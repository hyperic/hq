/*
 * NOTE: This copyright does *not* cover user programs that use HQ
 * program services by normal system calls through the application
 * program interfaces provided as part of the Hyperic Plug-in Development
 * Kit or the Hyperic Client Development Kit - this is merely considered
 * normal use of the program, and does *not* fall under the heading of
 * "derived work".
 * 
 * Copyright (C) [2004, 2005, 2006], Hyperic, Inc.
 * This file is part of HQ.
 * 
 * HQ is free software; you can redistribute it and/or modify
 * it under the terms version 2 of the GNU General Public License as
 * published by the Free Software Foundation. This program is distributed
 * in the hope that it will be useful, but WITHOUT ANY WARRANTY; without
 * even the implied warranty of MERCHANTABILITY or FITNESS FOR A
 * PARTICULAR PURPOSE. See the GNU General Public License for more
 * details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307
 * USA.
 */

package org.hyperic.hq.appdef.shared;

import java.util.Comparator;
import java.io.Serializable;

/** A comparator that will order two AppdefEntityIDs according to
* the position of their instanceId in an int array. This order
* specification deliberately omits appdef type because this comparator
* is only intended for use within a compatible group where type is
* consistent.
*/
public class AppdefCompatGrpComparator implements Comparator<AppdefEntityID>, Serializable {

    private int[] orderSpec;

    public AppdefCompatGrpComparator ( int[] orderSpec ) {
        this.orderSpec = orderSpec;
    }

  /** Compare two AppdefEntityIDs and indicate their order
   *  according the the order specification array passed
   *  into the constructor.
   * @param instance of AppdefEntityID number 1
   * @param instance of AppdefEntityID number 2
   * @return -1,0,1 for less than, equals and greater than
   */
  public int compare (AppdefEntityID o1, AppdefEntityID o2) {
    int o1Int = ((AppdefEntityID)o1).getID();
    int o2Int = ((AppdefEntityID)o2).getID();
    int index1 = findIndex( o1Int );
    int index2 = findIndex( o2Int );

    if (index1 < index2)
      return -1;
    if (index1 > index2)
      return 1;

    return 0; // equal
  }

  // find and return index position of int.
  // if can't find, place at end.
  private int findIndex (int num) {
    for (int i=0; i<orderSpec.length; i++)
      if (orderSpec[i]==num)
        return i;
    return orderSpec.length;
  }

}
