/*
 * NOTE: This copyright does *not* cover user programs that use HQ
 * program services by normal system calls through the application
 * program interfaces provided as part of the Hyperic Plug-in Development
 * Kit or the Hyperic Client Development Kit - this is merely considered
 * normal use of the program, and does *not* fall under the heading of
 * "derived work".
 * 
 * Copyright (C) [2004-2008], Hyperic, Inc.
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

package org.hyperic.util;

import java.io.File;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.text.CharacterIterator;
import java.text.NumberFormat;
import java.text.StringCharacterIterator;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.StringTokenizer;

public class StringUtil {
    
    /**
     * @param source The source string to perform replacements on.
     * @param find The substring to find in source.
     * @param replace The string to replace 'find' within source
     * @return The source string, with all occurrences of 'find' replaced with 'replace'
     */
    public static String replace(String source, String find, String replace) {

        if (source  == null || find == null || replace == null) {  
            return source;
        }

        int sourceLen = source.length();
        int findLen = find.length();
        if (sourceLen == 0 || findLen == 0) {   
            return source;
        } 

        StringBuffer buffer = new StringBuffer();

        int idx, fromIndex;
                
        for (fromIndex = 0;
             (idx = source.indexOf(find, fromIndex)) != -1;
             fromIndex = idx + findLen)
        {
            buffer.append(source.substring(fromIndex, idx));
            buffer.append(replace);
        }
        if (fromIndex == 0) {
            return source;
        }
        buffer.append(source.substring(fromIndex));

        return buffer.toString();
    }

    /**
     * @param source The source string to perform replacements on.
     * @param find The substring to find in source.     
     * @return The source string, with all occurrences of 'find' removed
     */
    public static String remove(String source, String find) {

        if (source == null || find == null) {
            return source;
        }

        String retVal = null;
        int sourceLen = source.length();
        int findLen = find.length();
        StringBuffer remove = new StringBuffer(source);
        
        try {
            if (source != null && sourceLen > 0 && find != null &&   
                findLen > 0)
            {
                int fromIndex, idx;
                
                for (fromIndex = 0, idx=0;
                     (fromIndex = source.indexOf(find, idx)) != -1;
                     idx = fromIndex + findLen)
                {
                    remove.delete(fromIndex, findLen + fromIndex);
                }
                                
                retVal = remove.toString();
            }
        } catch (Exception e) {
            // XXX This should never happen.
            //     O'RLY?
            e.printStackTrace();
            retVal = null;
        }

        return retVal;
    }
    
    /**
     * Print out everything in an Iterator in a user-friendly string format.
     *
     * @param i An iterator to print out.
     * @param delim The delimiter to use between elements.
     * @return The Iterator's elements in a user-friendly string format.
     */
    public static String iteratorToString(Iterator i, String delim) {
        return iteratorToString(i, delim, "");
    }

    /**
     * Print out everything in an Iterator in a user-friendly string format.
     *
     * @param i An iterator to print out.
     * @param delim The delimiter to use between elements.
     * @param quoteChar The character to quote each element with.
     * @return The Iterator's elements in a user-friendly string format.
     */
    public static String iteratorToString(Iterator i, String delim,
                                          String quoteChar) { 
        Object elt = null;
        StringBuffer rstr = new StringBuffer();
        String s;

        while (i.hasNext()) {
            if (rstr.length() > 0) {
                rstr.append(delim);
            }
            elt = i.next();
            if (elt == null) {
                rstr.append("NULL");
            }
            else {
                s = elt.toString();
                if (quoteChar != null) {
                    rstr.append(quoteChar).append(s).append(quoteChar);
                }
                else {
                    rstr.append(s);
                }
            }
        }

        return rstr.toString();
    }

    /**
     * Print out a List in a user-friendly string format.
     *
     * @param list A List to print out.
     * @param delim The delimiter to use between elements.
     * @return The List in a user-friendly string format.
     */
    public static String listToString(List list, String delim) {
        if (list == null) {
            return "NULL";
        }
        Iterator i = list.iterator();
        return iteratorToString(i, delim, null);
    }

    /**
     * Print out a List in a user-friendly string format.
     *
     * @param list A List to print out.
     * @return The List in a user-friendly string format.
     */
    public static String listToString(List list) {
        return listToString(list, ",");
    }

    /**
     * Print out an array as a String
     */
    public static String arrayToString(Object[] array) {
        return arrayToString(array, ',');
    }

    /**
     * Print out an array as a String.
     * 
     * XXX: Isn't this the same as ArrayUtil.toString()? 
     */
    public static String arrayToString(boolean[] array) {
        if (array == null) {
            return "null";
        }
        String rstr = "";
        char delim = ',';
        for (int i=0; i<array.length; i++) {
            if (i > 0) {
                rstr += delim;
            }
            rstr += array[i];
        }
        return rstr;
    }

    /**
     * Print out an array as a String
     * @param array The array to print out
     * @param delim The delimiter to use between elements.
     */
    public static String arrayToString(Object[] array, char delim) {
        if (array == null) {
            return "null";
        }
        String rstr = "";
        for (int i=0; i<array.length; i++) {
            if (i>0) {
                rstr += delim;
            }
            rstr += array[i];
        }
        return rstr;
    }

    /**
     * Print out an array as a String
     */
    public static String arrayToString(int[] array) {
        if (array == null) {
            return "null";
        }
        String rstr = "";
        for (int i=0; i<array.length; i++) {
            if (i > 0) {
                rstr += ",";
            }
            rstr += array[i];
        }
        return rstr;
    }

    /**
     * Create a string formulated by inserting a delimiter in between
     * consecutive array elements.
     *
     * @param objs  List of objects to implode (elements may not be null)
     * @param delim String to place inbetween elements
     * @return A string with objects in the list seperated by delim
     */
    public static String implode(List objs, String delim) {
        StringBuffer buf = new StringBuffer();
        int size = objs.size();

        for (int i=0; i<size - 1; i++) {
            buf.append(objs.get(i).toString() + delim);
        }

        if (size != 0) {
            buf.append(objs.get(size - 1).toString());
        }

        return buf.toString();
    }

    /**
     * Split a string on delimiter boundaries, and place each element
     * into an array.
     *
     * @param s     String to split up
     * @param delim Delimiting token, ala StringTokenizer
     * @return an ArrayList comprised of elements split by the tokenizing
     */

    public static List<String> explode(String s, String delim) {
        ArrayList<String> res = new ArrayList<String>();
        if (s != null) {
            StringTokenizer tok = new StringTokenizer(s, delim);

            while(tok.hasMoreTokens()) {
                res.add(tok.nextToken());
            }
        }
        
        return res;
    }

    /* the following code..explodeQuoted is based on code from bash-4.0/subst.c
     * XXX can be optimized if needed, but functionality first.
     */
    private static final char QUOTE = '\'';
    private static final char DOUBLEQUOTE = '"';
    private static final char BACKSLASH = '\\';

    private static boolean spctabnl(char c) {
        return (c == ' ') || (c == '\t') || (c == '\n');
    }

    private static int skipSingleQuoted(String str, int slen, int sind) {
        int i = sind;

        while ((i < slen) && str.charAt(i) != QUOTE) {
            i++;
        }
        if (i < slen) {
            i++;
        }
        return i;
    }

    private static int skipDoubleQuoted(String str, int slen, int sind) {
        int i = sind;
        int pass_next = 0;

        while (i < slen) {
            char c = str.charAt(i);
            if (pass_next != 0) {
                pass_next = 0;
                i++;
            }
            else if (c == BACKSLASH) {
                pass_next++;
                i++;
            }
            else if (c != DOUBLEQUOTE) {
                i++;
            }
            else {
                break;
            }
        }

        if (i < slen) {
            i++;
        }
        return i;
    }

    private static String extractDoubleQuoted(String str) {
        int slen = str.length();
        int i=0;
        int pass_next=0;
        int dquote=0;
        StringBuffer temp = new StringBuffer(slen);

        while (i < slen) {
            char c = str.charAt(i);
            if (pass_next != 0) {
                if (dquote == 0) {
                    temp.append('\\');
                }
                pass_next = 0;
                temp.append(c);
            }
            else if (c == BACKSLASH) {
                pass_next++;
            }
            else if (c != DOUBLEQUOTE) {
                temp.append(c);
            }
            else {
                dquote ^= 1;
            }
            i++;
        }

        if (dquote != 0) {
            throw new IllegalArgumentException("Unbalanced quotation marks");
        }

        return temp.toString();
    }

    private static String extractSingleQuoted(String str) {
        char first = str.charAt(0);
        char last = str.charAt(str.length()-1);
        if (first == QUOTE) {
            if (last == QUOTE) {
                return str.substring(1, str.length()-1);
            }
            else {
                throw new IllegalArgumentException("Unbalanced quotation marks");
            }
        }
        else {
            return str;
        }
    }

    public static String extractQuoted(String str) {
        if (str.length() == 0) {
            return str;
        }
        if (str.charAt(0) == QUOTE) {
            str = extractSingleQuoted(str);
        }
        else if (str.charAt(0) == DOUBLEQUOTE) {
            str = extractDoubleQuoted(str);
        }
        return str;
    }

    private static String[] splitCommandLine(String str, boolean extract) {
        List list = new ArrayList();
        int slen;
        char c;
        int i=0;
        int tokstart=0;

        if ((str == null) || ((str = str.trim()).length() == 0)) {
            return new String[0];
        }

        slen = str.length();

        while (true) {
            if (i < slen) {
                c = str.charAt(i);
            }
            else {
                c = '\0';
            }

            if (c == BACKSLASH) {
                i++;
                if (i < slen) {
                    i++;
                }
            }
            else if (c == QUOTE) {
                i = skipSingleQuoted(str, slen, ++i);
            }
            else if (c == DOUBLEQUOTE) {
                i = skipDoubleQuoted(str, slen, ++i);
            }
            else if ((c == '\0') || spctabnl(c)) {
                String token = str.substring(tokstart, i);
                if (extract) {
                    token = extractQuoted(token);
                }
                list.add(token);
                while ((i < slen) && spctabnl(str.charAt(i))) {
                    i++;
                }
                if (i < slen) {
                    tokstart = i;
                }
                else {
                    break;
                }
            }
            else {
                i++;
            }
        }
        return (String[])list.toArray(new String[list.size()]);
    }

    /**
     * Split a string up by whitespace, taking into account quoted
     * subcomponents.  If there is an uneven number of quotes, a
     * parse error will be thrown.
     *
     * @param arg String to parse
     * @return an array of elements, the argument was split into
     * @throws IllegalArgumentException indicating there was a quoting error
     */
    public static String[] explodeQuoted(String arg) {
        return splitCommandLine(arg, true);
    }

    /**
     * Remove a prefix from a string.  If value starts with prefix, it will be
     * removed, the resultant string is trimmed and returned.
     * @return If value starts with prefix, then this method returns value with
     * the prefix removed, and the resultant string trimmed.  If value does not
     * start with prefix, value is returned as-is.
     */
    public static String removePrefix(String value, String prefix) {
        if (!value.startsWith(prefix)) {
            return value;
        }
        return value.substring(prefix.length()).trim();
    }

    /**
     * @return the plural of word.  This is done by applying a few
     * rules.  These cover most (but not all) cases:
     * 1. If the word ends in s, ss, x, o, or ch, append es
     * 2. If the word ends in a consonant followed by y, drop the y
     *    and add ies
     * 3. Append an s and call it a day.
     * The ultimate references is at http://en.wikipedia.org/wiki/English_plural
     */
    public static String pluralize (String word) {
        if (word.endsWith("s") ||
            word.endsWith("x") ||
            word.endsWith("o") ||
            word.endsWith("ch"))
        {
            return word + "es";
        }
        if (word.endsWith("y")) {
            // Odd case to avoid StringIndexOutOfBounds later
            if (word.length() == 1) {
                return word;
            }
            // Check next-to-last letter
            char next2last = word.charAt(word.length()-2);
            if (next2last != 'a' &&
                next2last != 'e' &&
                next2last != 'i' &&
                next2last != 'o' &&
                next2last != 'u' &&
                next2last != 'y')
            {
                return word.substring(0, word.length()-1) + "ies";
            }
        } 

        return word + "s";
    }

    /**
     * @return The stack trace for the given Throwable as a String.
     */
    public static String getStackTrace(Throwable t) {

        if (t == null) {
            return "THROWABLE-WAS-NULL (at " +
                getStackTrace(new Exception()) + ")";
        }

        try {
            StringWriter sw = new StringWriter();
            PrintWriter pw = new PrintWriter(sw);
            
            t.printStackTrace(pw);

            Throwable cause = t.getCause();
            if (cause != null) {
                return sw.toString() + getStackTrace(cause);
            }
            
            return sw.toString();
        } catch(Exception e) {
            return "\n\nStringUtil.getStackTrace "
                + "GENERATED EXCEPTION: '" + e.toString() + "' \n\n";
        }
    }

    /**
     * @param s A string that might contain unix-style path separators.
     * @return The correct path for this platform (i.e, if win32, replace / with \).
     */
    public static String normalizePath(String s) {
        return StringUtil.replace(s, "/", File.separator);
    }

    public static String formatDuration(long duration) {
        return formatDuration(duration, 0, false);
    }

    public static String formatDuration(long duration, int scale,
                                        boolean minDigits)
    {
        long hours, mins;
        int digits;
        double millis;

        hours = duration / 3600000;
        duration -= hours * 3600000;

        mins = duration / 60000;
        duration -= mins * 60000;

        millis = (double)duration / 1000;

        StringBuffer buf = new StringBuffer();
        
        if (hours > 0 || minDigits == false) {
            buf.append(hours < 10 && minDigits == false ?
                       "0" + hours :
                       String.valueOf(hours)).append(':');
            minDigits = false;
        }
                              
        if (mins > 0 || minDigits == false) {
            buf.append(mins < 10 && minDigits == false ?
                       "0" + mins :
                       String.valueOf(mins)).append(':');
            minDigits = false;
        }

        // Format seconds and milliseconds
        NumberFormat fmt = NumberFormat.getInstance();
        digits = (minDigits == false ||
                  (scale == 0 && millis >= 9.5) ? 2 : 1);
        fmt.setMinimumIntegerDigits(digits);
        fmt.setMaximumIntegerDigits(2);         // Max of 2
        fmt.setMinimumFractionDigits(0);        // Don't need any
        fmt.setMaximumFractionDigits(scale);
        
        buf.append(fmt.format(millis));
        
        return buf.toString();
    }

    public static String repeatChars(char c, int nTimes) {
        char[] arr = new char[nTimes];

        for (int i=0; i<nTimes; i++) {
            arr[i] = c;
        }

        return new String(arr);
    }

    /** Capitalizes the first letter of str.
     *
     * @param str The string to capitalize.
     * @return A new string that is <code>str</code> capitalized.
     *         Returns <code>null</code> if str is null.
     */
    public static String capitalize(String str) {
        if (str == null) {
            return null;
        }
        else if (str.trim().equals("")) {
            return str;
        }
        String result =
            str.substring(0,1).toUpperCase() +
            str.substring(1, str.length());

        return result;
    }
    
    /**
     * Return a variant of 'str' which contains the beginning and end of
     * the string, but places '...' in the middle to limit the maximum
     * length of the string.
     * 
     * @param str     String to shorten
     * @param maxLen  Maximum length of the returned string
     */
    public static String dotProximate(String str, int maxLen) {
        int strLen = str.length();
        int toChop;
        
        if (strLen <= maxLen)
            return str;

        if (maxLen <= 3)
            return "...";
                        
        toChop = strLen - maxLen + 3;
        return str.substring(0, strLen / 2 - toChop / 2 - 1) + "..." + 
               str.substring(strLen / 2 + toChop / 2);
    }
    
    /**
     * Do a case-insensitive search for a substring
     */
    public static boolean stringDoesNotExist(String source, String sub) {
        return (sub != null) && (sub.length() > 0) &&
               (source.toLowerCase().indexOf(sub.toLowerCase()) < 0);
    }
    
    /**
     * Escapes a minimal set of metacharacters with their
     * regular expression escape codes.
     */
    public static String escapeForRegex(String source, boolean wildcard) {
        if (source == null) {
        	return null;
        }
        
    	StringBuilder result = new StringBuilder();
        StringCharacterIterator iterator = new StringCharacterIterator(source);
        char character =  iterator.current();
        
        while (character != CharacterIterator.DONE) {
            // All literals need to have backslashes doubled.
        	if (character == '.') {
        		result.append("\\.");
        	} else if (character == '\\') {
        		result.append("\\\\");
        	} else if (character == '?') {
        		result.append("\\?");
        	} else if (character == '+') {
        		result.append("\\+");
        	} else if (character == '{') {
        		result.append("\\{");
        	} else if (character == '}') {
        		result.append("\\}");
        	} else if (character == '[') {
        		result.append("\\[");
        	} else if (character == ']') {
        		result.append("\\]");
        	} else if (character == '(') {
        		result.append("\\(");
        	} else if (character == ')') {
        		result.append("\\)");
        	} else if (character == '^') {
        		result.append("\\^");
        	} else if (character == '$') {
        		result.append("\\$");
        	} else if (character == '|') {
        		result.append("\\|");        		
        	} else if (character == '*') {
                if (wildcard) {        		
                	result.append(".*");
                } else {
                	result.append("\\*");
                }
        	} else {
        		//the char is not a special one
        		//add it to the result as is
        		result.append(character);
        	}
        	character = iterator.next();
        }
        return result.toString();        
    }

    public static boolean isNullOrEmpty(String str) {
        return str == null || str.trim().length() == 0;
    }
}
