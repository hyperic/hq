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

package org.hyperic.hq.ui.taglib.display;

/**
 * One line description of what this class does.
 *
 * More detailed class description, including examples of usage if applicable.
 **/

public class StringUtil extends Object
{
   /**
    * Replace character at given index with the same character to upper case
    *
    * @param       oldString old string
    * @param       index of replacement
    * @return      String new string
    * @exception   StringIndexOutOfBoundsException &nbsp;
    *
    **/
   public static String toUpperCaseAt( String oldString, int index )
      throws NullPointerException, StringIndexOutOfBoundsException
   {
      int length = oldString.length();
      String newString = "";

      if( index >= length || index < 0 ) {
         throw new StringIndexOutOfBoundsException(
            "Index " + index
            + " is out of bounds for string length " + length );
      }

      //get upper case replacement
      String upper = String.valueOf( oldString.charAt( index ) ).toUpperCase();

      //avoid index out of bounds
      String paddedString = oldString + " ";

      //get reusable parts
      String beforeIndex = paddedString.substring( 0, index );
      String afterIndex = paddedString.substring( index + 1 );

      //generate new String - remove padding spaces
      newString = ( beforeIndex + upper + afterIndex ).substring( 0, length );

      return newString;
   }

}
