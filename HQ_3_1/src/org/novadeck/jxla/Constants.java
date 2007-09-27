// ========================================================
// Copyright (c) 2002 Novadeck (France)
// ========================================================
package org.novadeck.jxla;

import java.text.*;
import java.util.*;
import java.io.*;

import org.novadeck.jxla.tools.*;
import org.novadeck.jxla.config.*;
import org.novadeck.jxla.data.*;

/**
 *  Constants, for presentation purpose
 *
 *  Output  xml encoding ,
 *  month and days of week litteral
 */

public class Constants {

  public static final String[] MONTH  = new String[12];
  public static final String[] DAYS   = new String[7];
  static {
    MONTH[0]		= "January";
    MONTH[1]		= "February";
    MONTH[2]		= "March";
    MONTH[3]		= "April";
    MONTH[4]		= "May";
    MONTH[5]		= "June";
    MONTH[6]		= "July";
    MONTH[7]		= "August";
    MONTH[8]		= "September";
    MONTH[9]		= "October";
    MONTH[10]		= "November";
    MONTH[11]		= "December";


    DAYS[0]     = "Sunday";
    DAYS[1]     = "Monday";
    DAYS[2]     = "Tuesday";
    DAYS[3]     = "Wednesday";
    DAYS[4]     = "Thursday";
    DAYS[5]     = "Friday";
    DAYS[6]     = "Saturday";
  }
  public static final String ENCODING     = "ISO-8859-1";

  public static final String HEADER_XML   = "<?xml version=\"1.0\" encoding=\""+ ENCODING +"\"?>";

}
